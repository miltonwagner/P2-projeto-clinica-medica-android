package com.example.clinicavidaplus;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Verifica se saiu um APK novo no GitHub Releases (canal da branch que
 * gerou este build) e oferece atualizar: baixa e abre o instalador.
 */
public final class Atualizador {

    private static final String BASE =
            "https://github.com/miltonwagner/P2-projeto-clinica-medica-android/releases/download/";

    /** Uma verificação por abertura do app. */
    private static boolean verificado;
    /** Versão que o usuário mandou atualizar enquanto liberava a permissão. */
    private static JSONObject pendente;

    private Atualizador() {
    }

    public static void verificar(Activity activity) {
        if (pendente != null && podeInstalar(activity)) {
            JSONObject versao = pendente;
            pendente = null;
            baixar(activity, versao);
            return;
        }
        if (verificado || BuildConfig.CANAL_ATUALIZACAO.isEmpty()) {
            return;
        }
        verificado = true;

        Handler principal = new Handler(Looper.getMainLooper());
        new Thread(() -> {
            JSONObject versao = buscar();
            if (versao != null && versao.optInt("versionCode") > BuildConfig.VERSION_CODE) {
                principal.post(() -> perguntar(activity, versao));
            }
        }).start();
    }

    private static JSONObject buscar() {
        HttpURLConnection conexao = null;
        try {
            URL url = new URL(BASE + BuildConfig.CANAL_ATUALIZACAO + "/versao.json?t="
                    + System.currentTimeMillis());
            conexao = (HttpURLConnection) url.openConnection();
            conexao.setConnectTimeout(8000);
            conexao.setReadTimeout(8000);
            conexao.setUseCaches(false);
            if (conexao.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return null;
            }
            StringBuilder texto = new StringBuilder();
            try (BufferedReader leitor = new BufferedReader(
                    new InputStreamReader(conexao.getInputStream(), StandardCharsets.UTF_8))) {
                String linha;
                while ((linha = leitor.readLine()) != null) {
                    texto.append(linha);
                }
            }
            return new JSONObject(texto.toString());
        } catch (Exception e) {
            return null;
        } finally {
            if (conexao != null) {
                conexao.disconnect();
            }
        }
    }

    private static void perguntar(Activity activity, JSONObject versao) {
        if (activity.isFinishing() || activity.isDestroyed()) {
            return;
        }
        String notas = versao.optString("notas", "").trim();
        String mensagem = "Versão " + versao.optString("versionName", "nova")
                + " disponível (você está na " + BuildConfig.VERSION_NAME + ")."
                + (notas.isEmpty() ? "" : "\n\nNovidades: " + notas);

        new AlertDialog.Builder(activity)
                .setTitle("Atualizar aplicativo?")
                .setMessage(mensagem)
                .setPositiveButton("Atualizar", (d, w) -> atualizar(activity, versao))
                .setNegativeButton("Depois", null)
                .show();
    }

    private static void atualizar(Activity activity, JSONObject versao) {
        if (!podeInstalar(activity)) {
            // Android 8+: liberar "instalar apps desconhecidos" uma única vez.
            pendente = versao;
            Toast.makeText(activity, "Permita instalar atualizações e volte ao app.",
                    Toast.LENGTH_LONG).show();
            activity.startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:" + activity.getPackageName())));
            return;
        }
        baixar(activity, versao);
    }

    private static boolean podeInstalar(Context context) {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.O
                || context.getPackageManager().canRequestPackageInstalls();
    }

    private static void baixar(Activity activity, JSONObject versao) {
        Context app = activity.getApplicationContext();
        File pasta = app.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        String nome = "ClinicaVidaPlus-" + System.currentTimeMillis() + ".apk";
        File arquivo = new File(pasta, nome);

        String apk = versao.optString("apk",
                BASE + BuildConfig.CANAL_ATUALIZACAO + "/ClinicaVidaPlus.apk");
        DownloadManager.Request pedido = new DownloadManager.Request(Uri.parse(apk))
                .setTitle("Atualizando Vida Plus")
                .setDescription("Versão " + versao.optString("versionName"))
                .setMimeType("application/vnd.android.package-archive")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalFilesDir(app, Environment.DIRECTORY_DOWNLOADS, nome);

        DownloadManager gerenciador = (DownloadManager) app.getSystemService(Context.DOWNLOAD_SERVICE);
        long id;
        try {
            limparAntigos(pasta);
            id = gerenciador.enqueue(pedido);
        } catch (Exception e) {
            activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(apk)));
            return;
        }
        Toast.makeText(activity, "Baixando a atualização...", Toast.LENGTH_SHORT).show();

        BroadcastReceiver concluido = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1) != id) {
                    return;
                }
                app.unregisterReceiver(this);
                if (arquivo.exists() && arquivo.length() > 0) {
                    instalar(app, arquivo);
                } else {
                    Toast.makeText(app, "Não foi possível baixar a atualização.",
                            Toast.LENGTH_LONG).show();
                }
            }
        };
        ContextCompat.registerReceiver(app, concluido,
                new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                ContextCompat.RECEIVER_EXPORTED);
    }

    private static void limparAntigos(File pasta) {
        File[] antigos = pasta == null ? null : pasta.listFiles();
        if (antigos == null) {
            return;
        }
        for (File antigo : antigos) {
            if (antigo.getName().endsWith(".apk") && !antigo.delete()) {
                antigo.deleteOnExit();
            }
        }
    }

    private static void instalar(Context app, File arquivo) {
        Uri uri = FileProvider.getUriForFile(app, app.getPackageName() + ".arquivos", arquivo);
        Intent instalar = new Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
        app.startActivity(instalar);
    }
}

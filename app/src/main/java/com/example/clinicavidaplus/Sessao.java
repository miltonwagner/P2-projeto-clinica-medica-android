package com.example.clinicavidaplus;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Centraliza a sessão do usuário: identifica o perfil (admin, médico ou
 * paciente), abre a tela inicial correta e faz o logout.
 * Usado pelo login e pela tela inicial, para manter o usuário conectado.
 */
public final class Sessao {

    public static final String PERFIL_PACIENTE = "paciente";
    public static final String PERFIL_MEDICO = "medico";
    public static final String PERFIL_ADMIN = "admin";

    private static final String TAG = "Sessao";
    private static final String PREFERENCIAS = "sessao_vidaplus";
    private static final String CHAVE_PERFIL = "perfil";

    private Sessao() {
    }

    public static String perfil(Context contexto) {
        return contexto.getSharedPreferences(PREFERENCIAS, Context.MODE_PRIVATE)
                .getString(CHAVE_PERFIL, null);
    }

    private static void salvarPerfil(Context contexto, String perfil) {
        SharedPreferences.Editor editor = contexto
                .getSharedPreferences(PREFERENCIAS, Context.MODE_PRIVATE)
                .edit();

        if (perfil == null) {
            editor.remove(CHAVE_PERFIL);
        } else {
            editor.putString(CHAVE_PERFIL, perfil);
        }

        editor.apply();
    }

    /** Tela inicial (raiz da navegação) de cada perfil. */
    public static Class<?> telaInicial(String perfil) {
        if (PERFIL_ADMIN.equals(perfil)) {
            return AdminMedicosActivity.class;
        }

        if (PERFIL_MEDICO.equals(perfil)) {
            return ListaConsultasMedicoActivity.class;
        }

        return ListaConsultasActivity.class;
    }

    /**
     * Identifica o perfil do usuário logado e abre a tela inicial dele.
     *
     * @param aoFalhar executado quando não for possível encaminhar
     *                 (ex.: cadastro inexistente ou sem conexão).
     */
    public static void encaminhar(Activity activity, Runnable aoFalhar) {
        FirebaseUser usuario = FirebaseAuth.getInstance().getCurrentUser();

        if (usuario == null) {
            falhar(aoFalhar);
            return;
        }

        usuario.getIdToken(false)
                .addOnSuccessListener(resultado -> {
                    Object admin = resultado.getClaims().get("admin");

                    if (Boolean.TRUE.equals(admin)) {
                        abrirInicio(activity, PERFIL_ADMIN);
                        return;
                    }

                    verificarMedico(activity, usuario, aoFalhar);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Erro ao verificar credenciais.", e);
                    aviso(activity, "Não foi possível verificar seu perfil. Verifique a conexão.");
                    falhar(aoFalhar);
                });
    }

    private static void verificarMedico(
            Activity activity,
            FirebaseUser usuario,
            Runnable aoFalhar
    ) {
        FirebaseFirestore.getInstance()
                .collection("medicos")
                .document(usuario.getUid())
                .get()
                .addOnSuccessListener(documento -> {
                    if (!documento.exists()) {
                        verificarPaciente(activity, usuario, aoFalhar);
                        return;
                    }

                    if (Boolean.TRUE.equals(documento.getBoolean("autorizado"))) {
                        abrirInicio(activity, PERFIL_MEDICO);
                    } else {
                        aviso(activity, "Seu acesso como médico ainda não foi autorizado.");
                        encerrar(activity);
                        falhar(aoFalhar);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Erro ao consultar perfil médico.", e);
                    aviso(activity, "Não foi possível consultar seu perfil. Verifique a conexão.");
                    falhar(aoFalhar);
                });
    }

    private static void verificarPaciente(
            Activity activity,
            FirebaseUser usuario,
            Runnable aoFalhar
    ) {
        FirebaseFirestore.getInstance()
                .collection("pacientes")
                .document(usuario.getUid())
                .get()
                .addOnSuccessListener(documento -> {
                    if (documento.exists()) {
                        abrirInicio(activity, PERFIL_PACIENTE);
                        return;
                    }

                    if (entrouComGoogle(usuario)) {
                        // Conta Google nova: completa o cadastro do paciente.
                        salvarPerfil(activity, null);
                        Intent intent = new Intent(activity, MainActivity.class);
                        intent.putExtra(MainActivity.EXTRA_COMPLETAR_CADASTRO_GOOGLE, true);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        activity.startActivity(intent);
                        activity.finish();
                        return;
                    }

                    aviso(activity, "Cadastro de paciente não encontrado. Faça seu cadastro para continuar.");
                    encerrar(activity);
                    falhar(aoFalhar);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Erro ao consultar perfil do paciente.", e);
                    aviso(activity, "Não foi possível consultar seu cadastro. Verifique a conexão.");
                    falhar(aoFalhar);
                });
    }

    private static boolean entrouComGoogle(FirebaseUser usuario) {
        for (UserInfo info : usuario.getProviderData()) {
            if ("google.com".equals(info.getProviderId())) {
                return true;
            }
        }

        return false;
    }

    private static void abrirInicio(Activity activity, String perfil) {
        salvarPerfil(activity, perfil);

        Intent intent = new Intent(activity, telaInicial(perfil));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(intent);
        activity.finish();
    }

    /** Desconecta do Firebase e do Google sem trocar de tela. */
    private static void encerrar(Context contexto) {
        FirebaseAuth.getInstance().signOut();
        GoogleSignIn.getClient(contexto, GoogleSignInOptions.DEFAULT_SIGN_IN).signOut();
        salvarPerfil(contexto, null);
    }

    /** Logout completo: volta para a tela inicial do app, sem histórico. */
    public static void sair(Activity activity) {
        encerrar(activity);

        Intent intent = new Intent(activity, IndexActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(intent);
        activity.finish();
    }

    private static void aviso(Context contexto, String mensagem) {
        Toast.makeText(contexto, mensagem, Toast.LENGTH_LONG).show();
    }

    private static void falhar(Runnable aoFalhar) {
        if (aoFalhar != null) {
            aoFalhar.run();
        }
    }
}

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
    private static final String CHAVE_AGENDAR_MEDICO = "agendar_medico";
    /** Pendência de entrar na fila ao vivo (em vez de agendar). */
    public static final String PREFIXO_FILA = "fila:";

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

    /**
     * Visitante escolheu um médico e tocou em "Agendar": guarda o médico
     * para abrir o agendamento logo depois do login ou do cadastro.
     */
    public static void guardarAgendamentoPendente(Context contexto, String medicoId) {
        contexto.getSharedPreferences(PREFERENCIAS, Context.MODE_PRIVATE)
                .edit()
                .putString(CHAVE_AGENDAR_MEDICO, medicoId)
                .apply();
    }

    public static void descartarAgendamentoPendente(Context contexto) {
        contexto.getSharedPreferences(PREFERENCIAS, Context.MODE_PRIVATE)
                .edit()
                .remove(CHAVE_AGENDAR_MEDICO)
                .apply();
    }

    private static String retirarAgendamentoPendente(Context contexto) {
        String medicoId = contexto.getSharedPreferences(PREFERENCIAS, Context.MODE_PRIVATE)
                .getString(CHAVE_AGENDAR_MEDICO, null);
        descartarAgendamentoPendente(contexto);
        return medicoId;
    }

    /** Tela inicial (raiz da navegação) de cada perfil. */
    public static Class<?> telaInicial(String perfil) {
        if (PERFIL_ADMIN.equals(perfil)) {
            return AdminMedicosActivity.class;
        }

        if (PERFIL_MEDICO.equals(perfil)) {
            return DashboardMedicoActivity.class;
        }

        return MarketplaceActivity.class;
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
                        // Cópia local do cadastro (ROOM) para o perfil abrir offline.
                        PacienteRepository.salvarDoFirestore(activity, documento);
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

        String medicoPendente = retirarAgendamentoPendente(activity);

        Intent intent = new Intent(activity, telaInicial(perfil));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        if (PERFIL_PACIENTE.equals(perfil) && medicoPendente != null) {
            // Continua o agendamento iniciado antes do login.
            Intent continuar;
            if (medicoPendente.startsWith(PREFIXO_FILA)) {
                continuar = new Intent(activity, FilaActivity.class);
                continuar.putExtra(FilaActivity.EXTRA_MEDICO_ID,
                        medicoPendente.substring(PREFIXO_FILA.length()));
            } else {
                continuar = new Intent(activity, AgendarActivity.class);
                continuar.putExtra(AgendarActivity.EXTRA_MEDICO_ID, medicoPendente);
            }
            activity.startActivities(new Intent[]{intent, continuar});
        } else {
            activity.startActivity(intent);
        }

        activity.finish();
    }

    /** Desconecta do Firebase e do Google sem trocar de tela. */
    private static void encerrar(Context contexto) {
        FirebaseAuth.getInstance().signOut();
        GoogleSignIn.getClient(contexto, GoogleSignInOptions.DEFAULT_SIGN_IN).signOut();
        salvarPerfil(contexto, null);
    }

    /** Logout completo: volta para o marketplace como visitante, sem histórico. */
    public static void sair(Activity activity) {
        FirebaseUser atual = FirebaseAuth.getInstance().getCurrentUser();
        if (atual != null) {
            PacienteRepository.apagar(activity, atual.getUid());
        }
        encerrar(activity);
        descartarAgendamentoPendente(activity);

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

package com.example.clinicavidaplus;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Não é um teste de verdade: percorre as telas com as contas de teste e salva
 * um PNG de cada uma em files/telas (usado pelo workflow "Fotos das telas").
 * Senha via argumento de instrumentação "senha".
 */
@RunWith(AndroidJUnit4.class)
public class CapturaTelasTest {

    private static final String TAG = "CapturaTelas";
    private static final String EMAIL_PACIENTE = "adriel.paciente@teste.com";
    private static final String EMAIL_MEDICO = "adriel.medico@teste.com";

    private final Context contexto = InstrumentationRegistry.getInstrumentation().getTargetContext();
    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private File pasta;
    private String senha;

    @Test
    public void capturarTodasAsTelas() throws Exception {
        Bundle args = InstrumentationRegistry.getArguments();
        senha = args.getString("senha", "");
        pasta = new File(contexto.getFilesDir(), "telas");
        pasta.mkdirs();

        // Médico online, aceitando ao vivo e agendamento (restaurado no fim).
        String medicoId = entrar(EMAIL_MEDICO, Sessao.PERFIL_MEDICO);
        Map<String, Object> estadoOriginal = new HashMap<>();
        DocumentSnapshot docMedico = Tasks.await(db.collection("medicos").document(medicoId).get(), 30, TimeUnit.SECONDS);
        estadoOriginal.put("online", docMedico.getBoolean("online") != null && docMedico.getBoolean("online"));
        estadoOriginal.put("atendimento", docMedico.getString("atendimento") == null ? Medico.AGENDAMENTO : docMedico.getString("atendimento"));
        Map<String, Object> aoVivo = new HashMap<>();
        aoVivo.put("online", true);
        aoVivo.put("atendimento", Medico.AMBOS);
        Tasks.await(db.collection("medicos").document(medicoId).update(aoVivo), 30, TimeUnit.SECONDS);
        Medico medico = Medico.de(Tasks.await(db.collection("medicos").document(medicoId).get(), 30, TimeUnit.SECONDS));

        // ---- Visitante ----
        sair();
        foto("01-inicio", tela(IndexActivity.class));
        foto("02-login", tela(LoginActivity.class));
        foto("03-cadastro", tela(MainActivity.class));
        foto("04-esqueceu-senha", tela(EsqueceuSenhaActivity.class));
        foto("05-localizacao", tela(EnderecoActivity.class));
        foto("06-marketplace-visitante", tela(MarketplaceActivity.class));
        foto("07-perfil-medico", tela(PerfilMedicoActivity.class).putExtra(PerfilMedicoActivity.EXTRA_MEDICO, medico));

        // ---- Paciente ----
        String pacienteId = entrar(EMAIL_PACIENTE, Sessao.PERFIL_PACIENTE);
        foto("10-marketplace-paciente", tela(MarketplaceActivity.class));
        foto("11-agendar", tela(AgendarActivity.class).putExtra(AgendarActivity.EXTRA_MEDICO_ID, medicoId));
        foto("12-agendamento-sucesso", tela(AgendamentoSucessoActivity.class)
                .putExtra("medico", medico.nome)
                .putExtra("especialidade", medico.especialidade)
                .putExtra("data", "10/11/2026")
                .putExtra("horario", "15:00"));
        foto("13-minhas-consultas", tela(ListaConsultasActivity.class));
        foto("14-carteira-paciente", tela(CarteiraActivity.class));
        foto("15-perfil-paciente", tela(PerfilActivity.class));
        foto("16-fila-aguardando", tela(FilaActivity.class).putExtra(FilaActivity.EXTRA_MEDICO_ID, medicoId));

        // ---- Médico ----
        entrar(EMAIL_MEDICO, Sessao.PERFIL_MEDICO);
        foto("20-painel-medico", tela(DashboardMedicoActivity.class));
        String filaId = chamarDaFila(medicoId, pacienteId);
        foto("21-painel-em-atendimento", tela(DashboardMedicoActivity.class));
        foto("22-agenda-medico", tela(ListaConsultasMedicoActivity.class));
        foto("23-pacientes", tela(AnotacoesActivity.class));
        foto("24-prontuario", tela(ProntuarioActivity.class)
                .putExtra(ProntuarioActivity.EXTRA_PACIENTE_ID, pacienteId)
                .putExtra(ProntuarioActivity.EXTRA_PACIENTE_NOME, "Paciente de teste"));
        foto("25-carteira-medico", tela(CarteiraActivity.class));
        foto("26-perfil-medico", tela(PerfilActivity.class));
        Tasks.await(db.collection("medicos").document(medicoId).update(estadoOriginal), 30, TimeUnit.SECONDS);

        // ---- Paciente: "É a sua vez" e limpeza da fila ----
        entrar(EMAIL_PACIENTE, Sessao.PERFIL_PACIENTE);
        // "É a sua vez" abre o Jitsi sozinho: espera a sala conectar.
        foto("17-videochamada", tela(FilaActivity.class).putExtra(FilaActivity.EXTRA_MEDICO_ID, medicoId), 25000);
        for (QueryDocumentSnapshot d : Tasks.await(db.collection("fila")
                .whereEqualTo("pacienteId", pacienteId).get(), 30, TimeUnit.SECONDS)) {
            String status = d.getString("status");
            if (EntradaFila.AGUARDANDO.equals(status) || EntradaFila.EM_ATENDIMENTO.equals(status)) {
                Tasks.await(d.getReference().update("status", EntradaFila.CANCELADO), 30, TimeUnit.SECONDS);
            }
        }
        Log.i(TAG, "fila usada: " + filaId);
        sair();
    }

    private Intent tela(Class<? extends Activity> classe) {
        return new Intent(contexto, classe).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    }

    /** Abre a tela, espera o Firestore carregar e salva o PNG. Falha de uma tela não para as outras. */
    private void foto(String nome, Intent intent) {
        foto(nome, intent, 6000);
    }

    private void foto(String nome, Intent intent, long esperaMs) {
        try (ActivityScenario<Activity> cenario = ActivityScenario.launch(intent)) {
            Thread.sleep(esperaMs);
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            Bitmap imagem = InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
            try (FileOutputStream saida = new FileOutputStream(new File(pasta, nome + ".png"))) {
                imagem.compress(Bitmap.CompressFormat.PNG, 100, saida);
            }
            Log.i(TAG, "ok " + nome);
        } catch (Throwable erro) {
            Log.e(TAG, "falhou " + nome, erro);
        }
    }

    private String entrar(String email, String perfil) throws Exception {
        auth.signOut();
        Tasks.await(auth.signInWithEmailAndPassword(email, senha), 30, TimeUnit.SECONDS);
        contexto.getSharedPreferences("sessao_vidaplus", Context.MODE_PRIVATE)
                .edit().putString("perfil", perfil).commit();
        return auth.getCurrentUser().getUid();
    }

    private void sair() {
        auth.signOut();
        contexto.getSharedPreferences("sessao_vidaplus", Context.MODE_PRIVATE)
                .edit().remove("perfil").commit();
    }

    private String chamarDaFila(String medicoId, String pacienteId) throws Exception {
        for (QueryDocumentSnapshot d : Tasks.await(db.collection("fila")
                .whereEqualTo("medicoId", medicoId)
                .whereEqualTo("pacienteId", pacienteId)
                .whereEqualTo("status", EntradaFila.AGUARDANDO).get(), 30, TimeUnit.SECONDS)) {
            Tasks.await(d.getReference().update("status", EntradaFila.EM_ATENDIMENTO), 30, TimeUnit.SECONDS);
            return d.getId();
        }
        return null;
    }
}

package com.example.clinicavidaplus;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.firebase.firestore.DocumentSnapshot;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Repository: coordena o banco remoto (Firestore, fonte da verdade) e o local (ROOM).
 * Toda operação no ROOM roda em thread de fundo (ExecutorService) e o resultado
 * volta para a thread da tela, então a interface nunca trava.
 */
public final class PacienteRepository {

    private static final String TAG = "PacienteRepository";
    private static final ExecutorService DISCO = Executors.newSingleThreadExecutor();
    private static final Handler TELA = new Handler(Looper.getMainLooper());

    public interface Callback {
        void pronto(Paciente paciente);
    }

    private PacienteRepository() {
    }

    /** Copia para o ROOM o documento pacientes/{uid} lido do Firestore. */
    public static void salvarDoFirestore(Context contexto, DocumentSnapshot documento) {
        if (documento == null || !documento.exists()) {
            return;
        }
        salvar(contexto, new Paciente(documento.getId(),
                documento.getString("nome"),
                documento.getString("email"),
                documento.getString("telefone"),
                documento.getString("nascimento")));
    }

    public static void salvar(Context contexto, Paciente paciente) {
        Context app = contexto.getApplicationContext();
        DISCO.execute(() -> {
            try {
                AppDatabase.get(app).pacienteDao().salvar(paciente);
                Log.d(TAG, "Paciente salvo no ROOM: " + paciente.getUid());
            } catch (Exception e) {
                Log.e(TAG, "Falha ao salvar no ROOM", e);
            }
        });
    }

    /** Busca a cópia local; responde na thread da tela (null se não houver). */
    public static void buscar(Context contexto, String uid, Callback callback) {
        Context app = contexto.getApplicationContext();
        DISCO.execute(() -> {
            Paciente paciente = null;
            try {
                paciente = AppDatabase.get(app).pacienteDao().buscar(uid);
            } catch (Exception e) {
                Log.e(TAG, "Falha ao ler o ROOM", e);
            }
            Paciente resultado = paciente;
            TELA.post(() -> callback.pronto(resultado));
        });
    }

    /** Ao sair da conta, apaga a cópia local daquele paciente. */
    public static void apagar(Context contexto, String uid) {
        Context app = contexto.getApplicationContext();
        DISCO.execute(() -> AppDatabase.get(app).pacienteDao().apagar(uid));
    }
}

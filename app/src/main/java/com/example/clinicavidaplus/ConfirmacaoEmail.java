package com.example.clinicavidaplus;

import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Pede o e-mail de confirmação da consulta ao serviço vidaplus-email (pasta email-api).
 * O app não guarda senha de e-mail: manda só o token do Firebase do usuário logado;
 * o serviço confere o token, lê a consulta no Firestore e envia para o paciente.
 * Paciente agendou -> "Consulta agendada"; médico confirmou -> "Consulta confirmada".
 */
public final class ConfirmacaoEmail {

    private static final String TAG = "ConfirmacaoEmail";
    private static final String ENDPOINT = "https://vidaplus-email.vercel.app/api/confirmar";
    private static final ExecutorService FILA = Executors.newSingleThreadExecutor();

    private ConfirmacaoEmail() {
    }

    /** Dispara em segundo plano; falha no e-mail nunca atrapalha o agendamento. */
    public static void enviar(String agendamentoId) {
        FirebaseUser usuario = FirebaseAuth.getInstance().getCurrentUser();
        if (usuario == null || agendamentoId == null) {
            return;
        }
        usuario.getIdToken(false)
                .addOnSuccessListener(resultado -> FILA.execute(() -> postar(resultado.getToken(), agendamentoId)))
                .addOnFailureListener(e -> Log.w(TAG, "Sem token para o e-mail", e));
    }

    private static void postar(String token, String agendamentoId) {
        HttpURLConnection conexao = null;
        try {
            conexao = (HttpURLConnection) new URL(ENDPOINT).openConnection();
            conexao.setRequestMethod("POST");
            conexao.setConnectTimeout(15000);
            conexao.setReadTimeout(30000);
            conexao.setDoOutput(true);
            conexao.setRequestProperty("Content-Type", "application/json");
            conexao.setRequestProperty("Authorization", "Bearer " + token);
            byte[] corpo = ("{\"agendamentoId\":\"" + agendamentoId + "\"}").getBytes(StandardCharsets.UTF_8);
            try (OutputStream saida = conexao.getOutputStream()) {
                saida.write(corpo);
            }
            Log.i(TAG, "E-mail de confirmação: HTTP " + conexao.getResponseCode());
        } catch (Exception e) {
            Log.w(TAG, "Falha ao pedir o e-mail de confirmação", e);
        } finally {
            if (conexao != null) {
                conexao.disconnect();
            }
        }
    }
}

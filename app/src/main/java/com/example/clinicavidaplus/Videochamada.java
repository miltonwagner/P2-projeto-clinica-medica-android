package com.example.clinicavidaplus;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import org.jitsi.meet.sdk.JitsiMeetActivity;
import org.jitsi.meet.sdk.JitsiMeetConferenceOptions;
import org.jitsi.meet.sdk.JitsiMeetUserInfo;

import java.net.URL;
import java.util.Map;

/**
 * Videochamada da consulta (Jitsi Meet). Antes de entrar, esta tela (transparente)
 * pede câmera e microfone ao Android — sem isso a sala abre, mas sem vídeo/áudio.
 * Sala única por consulta: "VidaPlus" + id do documento (fila ou agendamento).
 */
public class Videochamada extends AppCompatActivity {

    private static final String SERVIDOR = "https://meet.ffmuc.net";
    private static final String EXTRA_SALA = "videochamada_sala";
    private static final String EXTRA_NOME = "videochamada_nome";
    private static final String[] PERMISSOES = {Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO};

    public static String sala(String idConsulta) {
        return "VidaPlus" + idConsulta;
    }

    public static void entrar(Activity activity, String idConsulta, String nome) {
        Intent intent = new Intent(activity, Videochamada.class);
        intent.putExtra(EXTRA_SALA, sala(idConsulta));
        intent.putExtra(EXTRA_NOME, nome);
        activity.startActivity(intent);
    }

    private final ActivityResultLauncher<String[]> pedirPermissoes =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), this::aoResponder);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (temTodas()) {
            abrirJitsi();
        } else {
            pedirPermissoes.launch(PERMISSOES);
        }
    }

    private boolean temTodas() {
        for (String permissao : PERMISSOES) {
            if (ContextCompat.checkSelfPermission(this, permissao) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private void aoResponder(Map<String, Boolean> respostas) {
        if (!Boolean.TRUE.equals(respostas.get(Manifest.permission.CAMERA))) {
            Toast.makeText(this, "Sem a câmera liberada o médico não consegue te ver. "
                    + "Libere em Configurações > Apps > Vida Plus > Permissões.", Toast.LENGTH_LONG).show();
        }
        // Entra mesmo assim: pelo menos vê/ouve a outra pessoa.
        abrirJitsi();
    }

    private void abrirJitsi() {
        try {
            JitsiMeetUserInfo usuario = new JitsiMeetUserInfo();
            usuario.setDisplayName(getIntent().getStringExtra(EXTRA_NOME));

            JitsiMeetConferenceOptions opcoes = new JitsiMeetConferenceOptions.Builder()
                    .setServerURL(new URL(SERVIDOR))
                    .setRoom(getIntent().getStringExtra(EXTRA_SALA))
                    .setSubject("Consulta Vida Plus")
                    .setUserInfo(usuario)
                    // Entra com câmera e microfone ligados.
                    .setVideoMuted(false)
                    .setAudioMuted(false)
                    .setConfigOverride("startWithVideoMuted", false)
                    .setConfigOverride("startWithAudioMuted", false)
                    .setConfigOverride("disableInitialGUM", false)
                    .setFeatureFlag("welcomepage.enabled", false)
                    .setFeatureFlag("prejoinpage.enabled", false)
                    .setFeatureFlag("lobby-mode.enabled", false)
                    .setFeatureFlag("invite.enabled", false)
                    .setFeatureFlag("add-people.enabled", false)
                    .setFeatureFlag("calendar.enabled", false)
                    .setFeatureFlag("call-integration.enabled", false)
                    .setFeatureFlag("live-streaming.enabled", false)
                    .setFeatureFlag("recording.enabled", false)
                    .setFeatureFlag("meeting-password.enabled", false)
                    .setFeatureFlag("unsaferoomwarning.enabled", false)
                    .build();

            JitsiMeetActivity.launch(this, opcoes);
        } catch (Throwable e) {
            Log.e("Videochamada", "Falha ao abrir o Jitsi", e);
            Toast.makeText(this, "Não foi possível abrir a videochamada: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
        finish();
    }
}

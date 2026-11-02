package com.example.clinicavidaplus;

import android.app.Activity;
import android.widget.Toast;

import org.jitsi.meet.sdk.JitsiMeetActivity;
import org.jitsi.meet.sdk.JitsiMeetConferenceOptions;
import org.jitsi.meet.sdk.JitsiMeetUserInfo;

import java.net.URL;

/**
 * Videochamada da consulta pelo SDK do Jitsi Meet, aberta dentro do app.
 * A sala é derivada do id do documento (fila ou agendamento), então só o
 * médico e o paciente daquela consulta caem nela.
 */
public final class Videochamada {

    /** Servidor Jitsi público sem login obrigatório para abrir a sala. */
    private static final String SERVIDOR = "https://meet.ffmuc.net";

    private Videochamada() {
    }

    public static String sala(String idConsulta) {
        return "VidaPlus" + idConsulta;
    }

    public static void entrar(Activity activity, String idConsulta, String nome) {
        try {
            JitsiMeetUserInfo usuario = new JitsiMeetUserInfo();
            usuario.setDisplayName(nome);

            JitsiMeetConferenceOptions opcoes = new JitsiMeetConferenceOptions.Builder()
                    .setServerURL(new URL(SERVIDOR))
                    .setRoom(sala(idConsulta))
                    .setSubject("Consulta Vida Plus")
                    .setUserInfo(usuario)
                    .setFeatureFlag("welcomepage.enabled", false)
                    .setFeatureFlag("prejoinpage.enabled", false)
                    .setFeatureFlag("invite.enabled", false)
                    .setFeatureFlag("add-people.enabled", false)
                    .setFeatureFlag("calendar.enabled", false)
                    .setFeatureFlag("call-integration.enabled", false)
                    .setFeatureFlag("live-streaming.enabled", false)
                    .setFeatureFlag("recording.enabled", false)
                    .setFeatureFlag("meeting-password.enabled", false)
                    .setFeatureFlag("unsaferoomwarning.enabled", false)
                    .build();

            JitsiMeetActivity.launch(activity, opcoes);
        } catch (Exception e) {
            Toast.makeText(activity, "Não foi possível abrir a videochamada.",
                    Toast.LENGTH_LONG).show();
        }
    }
}

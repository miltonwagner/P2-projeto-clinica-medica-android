package com.example.clinicavidaplus;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;

/**
 * Lê um documento da coleção "fila": paciente esperando atendimento ao
 * vivo com um médico, por ordem de chegada.
 */
public class EntradaFila {

    public static final String COLECAO = "fila";
    public static final String AGUARDANDO = "aguardando";
    public static final String EM_ATENDIMENTO = "em_atendimento";
    public static final String ATENDIDO = "atendido";
    public static final String CANCELADO = "cancelado";

    /** Duração média usada para estimar a espera. */
    public static final int MINUTOS_POR_PACIENTE = 20;

    public final String id;
    public final String medicoId;
    public final String medico;
    public final String pacienteId;
    public final String especialidade;
    public final String status;
    public final long entrouEm;

    public EntradaFila(DocumentSnapshot documento) {
        id = documento.getId();
        medicoId = texto(documento, "medicoId");
        medico = texto(documento, "medico");
        pacienteId = texto(documento, "pacienteId");
        especialidade = texto(documento, "especialidade");
        status = texto(documento, "status");
        Timestamp quando = documento.getTimestamp("entrouEm");
        // Ainda sem horário do servidor (acabou de entrar): vai para o fim.
        entrouEm = quando == null ? Long.MAX_VALUE : quando.toDate().getTime();
    }

    private static String texto(DocumentSnapshot documento, String campo) {
        String valor = documento.getString(campo);
        return valor == null ? "" : valor.trim();
    }

    public boolean aguardando() {
        return AGUARDANDO.equals(status);
    }

    public boolean emAtendimento() {
        return EM_ATENDIMENTO.equals(status);
    }

    public boolean ativa() {
        return aguardando() || emAtendimento();
    }
}

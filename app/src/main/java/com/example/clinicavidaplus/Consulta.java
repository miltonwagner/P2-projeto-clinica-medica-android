package com.example.clinicavidaplus;

import com.google.firebase.firestore.DocumentSnapshot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/** Leitura simplificada de um documento da coleção "agendamentos". */
public class Consulta {

    public static final String AGENDADO = "Agendado";
    public static final String CONFIRMADO = "Confirmado";
    public static final String CONCLUIDO = "Concluído";
    public static final String RECUSADO = "Recusado";
    public static final String CANCELADO = "Cancelado";
    public static final String EM_ATENDIMENTO = "Em atendimento";

    public final String id;
    public final String medico;
    public final String medicoId;
    public final String pacienteId;
    public final String especialidade;
    public final String data;
    public final String horario;
    public final String status;
    public final long momento;
    public final String modalidade;
    public final String motivo;
    public final String pacienteNome;
    public final String telefone;

    public Consulta(DocumentSnapshot documento) {
        id = documento.getId();
        medico = texto(documento, "medico");
        medicoId = texto(documento, "medicoId");
        pacienteId = texto(documento, "usuarioId");
        especialidade = texto(documento, "especialidade");
        data = texto(documento, "data");
        horario = texto(documento, "horario");
        String valorStatus = texto(documento, "status");
        status = valorStatus.isEmpty() ? AGENDADO : valorStatus;
        momento = converter(data, horario);
        modalidade = texto(documento, "modalidade");
        motivo = texto(documento, "motivo");
        pacienteNome = texto(documento, "pacienteNome");
        telefone = texto(documento, "telefone");
    }

    private static String texto(DocumentSnapshot documento, String campo) {
        String valor = documento.getString(campo);
        return valor == null ? "" : valor.trim();
    }

    private static long converter(String data, String horario) {
        try {
            SimpleDateFormat formato = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.ROOT);
            Date convertida = formato.parse(data + " " + (horario.isEmpty() ? "00:00" : horario));
            return convertida == null ? 0 : convertida.getTime();
        } catch (ParseException e) {
            return 0;
        }
    }

    public boolean eh(String valor) {
        return status.equalsIgnoreCase(valor);
    }

    public boolean pendente() {
        return eh(AGENDADO);
    }

    public boolean confirmada() {
        return eh(CONFIRMADO);
    }

    public boolean emAtendimento() {
        return eh(EM_ATENDIMENTO);
    }

    public boolean hoje() {
        if (momento == 0) {
            return false;
        }
        Calendar agora = Calendar.getInstance();
        Calendar quando = Calendar.getInstance();
        quando.setTimeInMillis(momento);
        return agora.get(Calendar.YEAR) == quando.get(Calendar.YEAR)
                && agora.get(Calendar.DAY_OF_YEAR) == quando.get(Calendar.DAY_OF_YEAR);
    }

    public boolean concluida() {
        return status.toLowerCase(Locale.ROOT).startsWith("conclu")
                || status.toLowerCase(Locale.ROOT).startsWith("execut");
    }

    public boolean encerradaSemAtendimento() {
        return eh(RECUSADO) || eh(CANCELADO);
    }

    public boolean noMesAtual() {
        if (momento == 0) {
            return false;
        }

        Calendar hoje = Calendar.getInstance();
        Calendar quando = Calendar.getInstance();
        quando.setTimeInMillis(momento);
        return hoje.get(Calendar.YEAR) == quando.get(Calendar.YEAR)
                && hoje.get(Calendar.MONTH) == quando.get(Calendar.MONTH);
    }

    /** Ex.: "Ter, 14 de mar · 15:00". */
    public String quando() {
        if (momento == 0) {
            return (data + " " + horario).trim();
        }

        String dia = new SimpleDateFormat("EEE, dd 'de' MMM", new Locale("pt", "BR"))
                .format(new Date(momento));
        dia = dia.substring(0, 1).toUpperCase(Locale.ROOT) + dia.substring(1);
        return dia.replace(".", "") + " · " + horario;
    }

    /** Texto do status como o paciente vê. */
    public String statusParaPaciente() {
        if (pendente()) {
            return "Aguardando confirmação";
        }

        if (emAtendimento()) {
            return "Em atendimento agora";
        }

        return status;
    }
}

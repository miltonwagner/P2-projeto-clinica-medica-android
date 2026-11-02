package com.example.clinicavidaplus;

import com.google.firebase.firestore.DocumentSnapshot;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Dados públicos de um médico exibidos no marketplace.
 * Os campos vêm da coleção "medicos"; quando algum não existir,
 * usa os valores padrão do catálogo da clínica.
 */
public class Medico implements Serializable {

    public String id;
    public String nome;
    public String especialidade;
    public String crm;
    public String bio;
    public int valor;
    public double avaliacao;
    public int avaliacoes;
    public boolean online;
    /** Como atende: "aovivo", "agendamento" ou "ambos". */
    public String atendimento = AGENDAMENTO;

    public static final String AO_VIVO = "aovivo";
    public static final String AGENDAMENTO = "agendamento";
    public static final String AMBOS = "ambos";

    private Medico(
            String id,
            String nome,
            String especialidade,
            String crm,
            int valor,
            double avaliacao,
            int avaliacoes,
            String bio
    ) {
        this.id = id;
        this.nome = nome;
        this.especialidade = especialidade;
        this.crm = crm;
        this.valor = valor;
        this.avaliacao = avaliacao;
        this.avaliacoes = avaliacoes;
        this.bio = bio;
    }

    /** Médicos cadastrados na clínica (usados também sem conexão). */
    public static List<Medico> padroes() {
        List<Medico> lista = new ArrayList<>();

        lista.add(new Medico(
                "ppBJexJ2FEUoijPoAhidZzuvBar2",
                "Dr. Arnaldo Silva",
                "Cardiologia",
                "CRM-SP 123456",
                220, 4.9, 38,
                "Cardiologista com 15 anos de experiência em prevenção, check-up "
                        + "cardiológico e acompanhamento de hipertensão."
        ));
        lista.add(new Medico(
                "CgzDk7lod9hkniRUQMHc4szYUMG2",
                "Dra. Juliana Costa",
                "Pediatria",
                "CRM-SP 234567",
                180, 5.0, 27,
                "Pediatra dedicada ao acompanhamento do crescimento e do "
                        + "desenvolvimento, do recém-nascido ao adolescente."
        ));
        lista.add(new Medico(
                "krdYyomS2GQEAw3WZTVYqqFO2X03",
                "Dr. Roberto Souza",
                "Ortopedia",
                "CRM-SP 345678",
                200, 4.8, 31,
                "Ortopedista especializado em coluna, joelho e lesões esportivas, "
                        + "com foco em tratamento conservador."
        ));

        return lista;
    }

    private static Medico padrao(String id) {
        for (Medico medico : padroes()) {
            if (medico.id.equals(id)) {
                return medico;
            }
        }

        return new Medico(id, "Médico", "Clínico geral", "", 150, 0, 0,
                "Profissional da Clínica Médica Vida Plus.");
    }

    public static Medico de(DocumentSnapshot documento) {
        Medico medico = padrao(documento.getId());

        medico.nome = texto(documento, "nome", medico.nome);
        medico.especialidade = texto(documento, "especialidade", medico.especialidade);
        medico.crm = texto(documento, "crm", medico.crm);
        medico.bio = texto(documento, "bio", medico.bio);

        Number valor = documento.getDouble("valor");
        if (valor != null) {
            medico.valor = valor.intValue();
        }

        Double avaliacao = documento.getDouble("avaliacao");
        if (avaliacao != null) {
            medico.avaliacao = avaliacao;
        }

        Long avaliacoes = documento.getLong("avaliacoes");
        if (avaliacoes != null) {
            medico.avaliacoes = avaliacoes.intValue();
        }

        medico.online = Boolean.TRUE.equals(documento.getBoolean("online"));
        String modo = documento.getString("atendimento");
        if (AO_VIVO.equals(modo) || AMBOS.equals(modo) || AGENDAMENTO.equals(modo)) {
            medico.atendimento = modo;
        }
        return medico;
    }

    private static String texto(DocumentSnapshot documento, String campo, String padrao) {
        String valor = documento.getString(campo);
        return valor == null || valor.trim().isEmpty() ? padrao : valor.trim();
    }

    public boolean aceitaAoVivo() {
        return AO_VIVO.equals(atendimento) || AMBOS.equals(atendimento);
    }

    public boolean aceitaAgendamento() {
        return AGENDAMENTO.equals(atendimento) || AMBOS.equals(atendimento);
    }

    /** Fila ao vivo aberta: o paciente pode entrar agora. */
    public boolean aoVivoAgora() {
        return online && aceitaAoVivo();
    }

    public String inicial() {
        String semTitulo = nome.replaceFirst("^(Dra?\\.?)\\s*", "");
        return semTitulo.isEmpty() ? "M" : semTitulo.substring(0, 1).toUpperCase(Locale.ROOT);
    }

    public String valorFormatado() {
        return moeda(valor);
    }

    public String avaliacaoTexto() {
        if (avaliacoes == 0) {
            return "Novo na clínica";
        }

        return String.format(Locale.US, "%.1f", avaliacao) + " (" + avaliacoes + ")";
    }

    public static String moeda(double valor) {
        return String.format(new Locale("pt", "BR"), "R$ %,.2f", valor)
                .replace(",00", "");
    }
}

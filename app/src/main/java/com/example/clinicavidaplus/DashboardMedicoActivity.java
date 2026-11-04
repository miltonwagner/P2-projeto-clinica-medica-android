package com.example.clinicavidaplus;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Tela inicial do médico: como ele atende (ao vivo, agendamento ou os
 * dois), a fila de hoje em tempo real (fila ao vivo + consultas
 * confirmadas do dia), pendências de confirmação e próximas consultas.
 */
public class DashboardMedicoActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private String uidMedico;
    private Medico medico;

    private LinearLayout containerEstatisticas;
    private LinearLayout containerEmAtendimento;
    private LinearLayout containerFila;
    private LinearLayout containerPendentes;
    private LinearLayout containerProximas;
    private MaterialSwitch switchOnline;

    private ListenerRegistration ouvinteAgendamentos;
    private ListenerRegistration ouvinteFila;

    private final List<Consulta> consultas = new ArrayList<>();
    private final List<EntradaFila> filaAoVivo = new ArrayList<>();
    private final Map<String, String> nomesPacientes = new HashMap<>();

    /** Item da fila do dia: entrada ao vivo ou consulta agendada. */
    private static final class ItemFila {
        final EntradaFila aoVivo;
        final Consulta agendada;
        final long ordem;

        ItemFila(EntradaFila aoVivo) {
            this.aoVivo = aoVivo;
            this.agendada = null;
            this.ordem = aoVivo.entrouEm;
        }

        ItemFila(Consulta agendada) {
            this.aoVivo = null;
            this.agendada = agendada;
            this.ordem = agendada.momento;
        }

        String pacienteId() {
            return aoVivo != null ? aoVivo.pacienteId : agendada.pacienteId;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard_medico);
        NavegacaoInferior.configurar(this, R.id.nav_inicio);

        db = FirebaseFirestore.getInstance();
        containerEstatisticas = findViewById(R.id.containerEstatisticas);
        containerEmAtendimento = findViewById(R.id.containerEmAtendimento);
        containerFila = findViewById(R.id.containerFila);
        containerPendentes = findViewById(R.id.containerPendentes);
        containerProximas = findViewById(R.id.containerProximas);
        switchOnline = findViewById(R.id.switchOnline);

        FirebaseUser usuario = FirebaseAuth.getInstance().getCurrentUser();
        if (usuario == null) {
            Sessao.sair(this);
            return;
        }

        uidMedico = usuario.getUid();
        carregarMedico(usuario);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (ouvinteAgendamentos != null) {
            ouvinteAgendamentos.remove();
        }
        if (ouvinteFila != null) {
            ouvinteFila.remove();
        }
    }

    private void carregarMedico(FirebaseUser usuario) {
        db.collection("medicos")
                .document(uidMedico)
                .get()
                .addOnSuccessListener(documento -> {
                    if (!documento.exists()
                            || !Boolean.TRUE.equals(documento.getBoolean("autorizado"))) {
                        Toast.makeText(this,
                                "Acesso negado. Seu cadastro médico não está autorizado.",
                                Toast.LENGTH_LONG).show();
                        Sessao.sair(this);
                        return;
                    }

                    medico = Medico.de(documento);
                    String nome = documento.getString("nome");
                    if (nome == null || nome.trim().isEmpty()) {
                        nome = usuario.getDisplayName() == null ? "Médico" : usuario.getDisplayName();
                    }
                    ((TextView) findViewById(R.id.txtBoasVindasMedico))
                            .setText("Bem-vindo(a), " + nome);

                    findViewById(R.id.pilulaAoVivo).setOnClickListener(v -> alterarModo(Medico.AO_VIVO));
                    findViewById(R.id.pilulaAgendamento).setOnClickListener(v -> alterarModo(Medico.AGENDAMENTO));
                    findViewById(R.id.pilulaAmbos).setOnClickListener(v -> alterarModo(Medico.AMBOS));

                    mostrarModo();
                    ouvirConsultas();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this,
                            "Não foi possível verificar sua autorização.",
                            Toast.LENGTH_LONG).show();
                    Sessao.sair(this);
                });
    }

    // ---- Modo de atendimento e status ao vivo ----

    private void alterarModo(String modo) {
        if (modo.equals(medico.atendimento)) {
            return;
        }

        String anterior = medico.atendimento;
        boolean onlineAnterior = medico.online;
        medico.atendimento = modo;
        if (!medico.aceitaAoVivo()) {
            medico.online = false;
        }
        mostrarModo();

        Map<String, Object> dados = new HashMap<>();
        dados.put("atendimento", modo);
        dados.put("online", medico.online);
        db.collection("medicos").document(uidMedico).update(dados)
                .addOnFailureListener(e -> {
                    medico.atendimento = anterior;
                    medico.online = onlineAnterior;
                    mostrarModo();
                    Toast.makeText(this, "Não foi possível alterar o modo.", Toast.LENGTH_SHORT).show();
                });
    }

    private void alterarOnline(boolean online) {
        medico.online = online;
        mostrarModo();

        db.collection("medicos").document(uidMedico).update("online", online)
                .addOnFailureListener(e -> {
                    medico.online = !online;
                    mostrarModo();
                    Toast.makeText(this, "Não foi possível alterar seu status.", Toast.LENGTH_SHORT).show();
                });
    }

    private void mostrarModo() {
        findViewById(R.id.pilulaAoVivo).setSelected(Medico.AO_VIVO.equals(medico.atendimento));
        findViewById(R.id.pilulaAgendamento).setSelected(Medico.AGENDAMENTO.equals(medico.atendimento));
        findViewById(R.id.pilulaAmbos).setSelected(Medico.AMBOS.equals(medico.atendimento));

        String detalhe;
        if (Medico.AO_VIVO.equals(medico.atendimento)) {
            detalhe = "Só fila ao vivo: pacientes entram na hora, sem agendar.";
        } else if (Medico.AGENDAMENTO.equals(medico.atendimento)) {
            detalhe = "Só com horário marcado: pacientes agendam pelo seu perfil.";
        } else {
            detalhe = "Agenda aberta e fila ao vivo quando você estiver online.";
        }
        ((TextView) findViewById(R.id.txtModoDetalhe)).setText(detalhe);

        View cardStatus = findViewById(R.id.cardStatusOnline);
        cardStatus.setVisibility(medico.aceitaAoVivo() ? View.VISIBLE : View.GONE);

        boolean online = medico.aoVivoAgora();
        cardStatus.setBackgroundResource(online ? R.drawable.bg_status_online : R.drawable.bg_status_offline);
        findViewById(R.id.indicadorOnline).setBackgroundResource(
                online ? R.drawable.bg_ponto_online : R.drawable.bg_ponto_offline);
        ((TextView) findViewById(R.id.txtStatusOnline)).setText(
                online ? "Você está ao vivo" : "Fila ao vivo fechada");
        ((TextView) findViewById(R.id.txtStatusOnlineDetalhe)).setText(online
                ? "Pacientes podem entrar na sua fila agora"
                : "Ative para receber pacientes na hora");

        switchOnline.setOnCheckedChangeListener(null);
        switchOnline.setChecked(online);
        switchOnline.setOnCheckedChangeListener((botao, ativo) -> alterarOnline(ativo));
    }

    // ---- Dados em tempo real ----

    private void ouvirConsultas() {
        ouvinteAgendamentos = db.collection("agendamentos")
                .whereEqualTo("medicoId", uidMedico)
                .addSnapshotListener((resultado, erro) -> {
                    if (resultado == null) {
                        if (erro != null) {
                            Toast.makeText(this, "Erro ao carregar suas consultas.", Toast.LENGTH_LONG).show();
                        }
                        return;
                    }
                    consultas.clear();
                    for (DocumentSnapshot documento : resultado.getDocuments()) {
                        consultas.add(new Consulta(documento));
                    }
                    consultas.sort((a, b) -> Long.compare(a.momento, b.momento));
                    mostrarTudo();
                });

        ouvinteFila = db.collection(EntradaFila.COLECAO)
                .whereEqualTo("medicoId", uidMedico)
                .whereIn("status", Arrays.asList(EntradaFila.AGUARDANDO, EntradaFila.EM_ATENDIMENTO))
                .addSnapshotListener((resultado, erro) -> {
                    if (resultado == null) {
                        return;
                    }
                    filaAoVivo.clear();
                    for (DocumentSnapshot documento : resultado.getDocuments()) {
                        filaAoVivo.add(new EntradaFila(documento));
                    }
                    filaAoVivo.sort((a, b) -> Long.compare(a.entrouEm, b.entrouEm));
                    mostrarTudo();
                });
    }

    private void mostrarTudo() {
        List<ItemFila> emAtendimento = new ArrayList<>();
        List<ItemFila> fila = new ArrayList<>();
        int pendentes = 0;
        int concluidasHoje = 0;

        containerPendentes.removeAllViews();
        containerProximas.removeAllViews();

        for (EntradaFila entrada : filaAoVivo) {
            (entrada.emAtendimento() ? emAtendimento : fila).add(new ItemFila(entrada));
        }

        for (Consulta consulta : consultas) {
            if (consulta.emAtendimento()) {
                emAtendimento.add(new ItemFila(consulta));
            } else if (consulta.confirmada() && consulta.hoje()) {
                fila.add(new ItemFila(consulta));
            } else if (consulta.pendente()) {
                pendentes++;
                containerPendentes.addView(cardPendente(consulta));
            } else if (consulta.confirmada() && consulta.momento > System.currentTimeMillis()) {
                containerProximas.addView(cardProxima(consulta));
            } else if (consulta.concluida() && consulta.hoje()) {
                concluidasHoje++;
            }
        }

        fila.sort((a, b) -> Long.compare(a.ordem, b.ordem));

        // Em atendimento
        containerEmAtendimento.removeAllViews();
        findViewById(R.id.secaoEmAtendimento).setVisibility(emAtendimento.isEmpty() ? View.GONE : View.VISIBLE);
        for (ItemFila item : emAtendimento) {
            containerEmAtendimento.addView(cardEmAtendimento(item));
        }

        // Fila
        containerFila.removeAllViews();
        int aoVivo = 0;
        for (ItemFila item : fila) {
            if (item.aoVivo != null) {
                aoVivo++;
            }
        }
        ((TextView) findViewById(R.id.txtResumoFila)).setText(fila.isEmpty()
                ? "Ninguém esperando agora."
                : fila.size() + (fila.size() == 1 ? " paciente" : " pacientes") + " · "
                + aoVivo + " ao vivo · " + (fila.size() - aoVivo) + " agendado(s) para hoje");

        if (fila.isEmpty()) {
            containerFila.addView(vazio(medico.aoVivoAgora()
                    ? "Sua fila ao vivo está aberta. Os pacientes aparecem aqui assim que entrarem."
                    : "Consultas confirmadas de hoje e pacientes da fila ao vivo aparecem aqui."));
        }
        for (int i = 0; i < fila.size(); i++) {
            containerFila.addView(cardFila(fila.get(i), i + 1, emAtendimento.isEmpty() && i == 0));
        }

        if (pendentes == 0) {
            containerPendentes.addView(vazio("Nenhuma consulta aguardando confirmação."));
        }
        if (containerProximas.getChildCount() == 0) {
            containerProximas.addView(vazio("Nenhuma consulta confirmada para os próximos dias."));
        }

        containerEstatisticas.removeAllViews();
        LinearLayout linha1 = Tela.linha(this);
        linha1.addView(estatistica("Na fila", fila.size(), R.color.primaria, false));
        linha1.addView(estatistica("A confirmar", pendentes, R.color.aviso, true));
        LinearLayout linha2 = Tela.linha(this);
        linha2.addView(estatistica("Em atendimento", emAtendimento.size(), R.color.sucesso, false));
        linha2.addView(estatistica("Atendidos hoje", concluidasHoje, R.color.texto_escuro, true));
        containerEstatisticas.addView(linha1);
        containerEstatisticas.addView(linha2);
    }

    // ---- Cards ----

    private View cardFila(ItemFila item, int posicao, boolean proximo) {
        LinearLayout card = Tela.card(this);

        LinearLayout topo = Tela.linha(this);
        topo.addView(Tela.avatar(this, String.valueOf(posicao), 40));
        LinearLayout info = Tela.coluna(this, 12);
        TextView nome = Tela.texto(this, "Carregando paciente...", R.style.Texto_CardTitulo, 0);
        info.addView(nome);
        info.addView(Tela.texto(this, origem(item), R.style.Texto_Pequeno, 2));
        topo.addView(info);
        card.addView(topo);
        preencherNome(item, nome);

        if (item.agendada != null && !item.agendada.motivo.isEmpty()) {
            card.addView(Tela.texto(this, "Motivo: " + item.agendada.motivo, R.style.Texto_Corpo, 10));
        }

        LinearLayout botoes = Tela.linha(this);
        botoes.setPadding(0, Tela.dp(this, 14), 0, 0);
        Button atender = Tela.botao(this, proximo ? "Chamar próximo" : "Atender", true);
        Button outro = Tela.botao(this, item.aoVivo != null ? "Remover" : "Faltou", false);
        ((LinearLayout.LayoutParams) outro.getLayoutParams()).setMarginStart(Tela.dp(this, 10));
        botoes.addView(atender);
        botoes.addView(outro);
        card.addView(botoes);

        atender.setOnClickListener(v -> {
            if (item.aoVivo != null) {
                mudarFila(item.aoVivo, EntradaFila.EM_ATENDIMENTO, "chamadoEm", v, outro);
            } else {
                mudarConsulta(item.agendada, Consulta.EM_ATENDIMENTO, v, outro);
            }
        });
        outro.setOnClickListener(v -> {
            if (item.aoVivo != null) {
                mudarFila(item.aoVivo, EntradaFila.CANCELADO, "finalizadoEm", v, atender);
            } else {
                mudarConsulta(item.agendada, Consulta.CANCELADO, v, atender);
            }
        });
        return card;
    }

    private View cardEmAtendimento(ItemFila item) {
        LinearLayout card = Tela.card(this);
        card.setBackgroundResource(R.drawable.bg_status_online);
        card.setPadding(Tela.dp(this, 20), Tela.dp(this, 18), Tela.dp(this, 20), Tela.dp(this, 18));

        TextView nome = Tela.texto(this, "Carregando paciente...", R.style.Texto_CardTitulo, 0);
        card.addView(nome);
        preencherNome(item, nome);
        card.addView(Tela.texto(this, origem(item), R.style.Texto_Pequeno, 2));

        if (item.agendada != null) {
            if (!item.agendada.motivo.isEmpty()) {
                card.addView(Tela.texto(this, "Motivo: " + item.agendada.motivo, R.style.Texto_Corpo, 10));
            }
            if (!item.agendada.telefone.isEmpty()) {
                card.addView(Tela.texto(this, "Telefone: " + item.agendada.telefone, R.style.Texto_Pequeno, 6));
            }
        }

        if (item.aoVivo != null || !"Presencial".equalsIgnoreCase(item.agendada.modalidade)) {
            Button video = Tela.botao(this, "Entrar na videochamada", true);
            video.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_video, 0, 0, 0);
            ((LinearLayout.LayoutParams) video.getLayoutParams()).topMargin = Tela.dp(this, 14);
            video.setOnClickListener(v -> Videochamada.entrar(this,
                    item.aoVivo != null ? item.aoVivo.id : item.agendada.id,
                    nomeMedico()));
            card.addView(video);
        }

        LinearLayout botoes = Tela.linha(this);
        botoes.setPadding(0, Tela.dp(this, 14), 0, 0);
        Button finalizar = Tela.botao(this, "Finalizar", false);
        Button prontuario = Tela.botao(this, "Prontuário", false);
        ((LinearLayout.LayoutParams) prontuario.getLayoutParams()).setMarginStart(Tela.dp(this, 10));
        botoes.addView(finalizar);
        botoes.addView(prontuario);
        card.addView(botoes);

        finalizar.setOnClickListener(v -> {
            if (item.aoVivo != null) {
                mudarFila(item.aoVivo, EntradaFila.ATENDIDO, "finalizadoEm", v, prontuario);
            } else {
                mudarConsulta(item.agendada, Consulta.CONCLUIDO, v, prontuario);
            }
        });
        prontuario.setOnClickListener(v -> abrirProntuario(item.pacienteId()));
        return card;
    }

    private View cardPendente(Consulta consulta) {
        LinearLayout card = Tela.card(this);
        card.addView(Tela.etiqueta(this, "Pendente"));

        TextView nome = Tela.texto(this, "Carregando paciente...", R.style.Texto_CardTitulo, 10);
        card.addView(nome);
        preencherNome(new ItemFila(consulta), nome);

        card.addView(Tela.texto(this, modalidade(consulta) + " · " + consulta.especialidade,
                R.style.Texto_Pequeno, 2));
        card.addView(Tela.textoComIcone(this, consulta.quando(), R.drawable.ic_relogio, 8));
        if (!consulta.motivo.isEmpty()) {
            card.addView(Tela.texto(this, "Motivo: " + consulta.motivo, R.style.Texto_Corpo, 8));
        }

        LinearLayout botoes = Tela.linha(this);
        botoes.setPadding(0, Tela.dp(this, 14), 0, 0);
        Button confirmar = Tela.botao(this, "Confirmar", true);
        Button recusar = Tela.botao(this, "Recusar", false);
        ((LinearLayout.LayoutParams) recusar.getLayoutParams()).setMarginStart(Tela.dp(this, 10));
        botoes.addView(confirmar);
        botoes.addView(recusar);
        card.addView(botoes);

        confirmar.setOnClickListener(v -> mudarConsulta(consulta, Consulta.CONFIRMADO, v, recusar));
        recusar.setOnClickListener(v -> mudarConsulta(consulta, Consulta.RECUSADO, v, confirmar));
        return card;
    }

    private View cardProxima(Consulta consulta) {
        LinearLayout card = Tela.card(this);

        TextView nome = Tela.texto(this, "Carregando paciente...", R.style.Texto_CardTitulo, 0);
        card.addView(nome);
        preencherNome(new ItemFila(consulta), nome);

        card.addView(Tela.texto(this, modalidade(consulta) + " · " + consulta.especialidade,
                R.style.Texto_Pequeno, 2));
        card.addView(Tela.textoComIcone(this, consulta.quando(), R.drawable.ic_relogio, 8));

        TextView prontuario = Tela.compacto(Tela.texto(this, "Ver prontuário", R.style.Texto_Link, 10));
        prontuario.setTextColor(ContextCompat.getColor(this, R.color.primaria));
        prontuario.setOnClickListener(v -> abrirProntuario(consulta.pacienteId));
        card.addView(prontuario);
        return card;
    }

    private String origem(ItemFila item) {
        if (item.aoVivo != null) {
            String hora = item.aoVivo.entrouEm == Long.MAX_VALUE ? "agora"
                    : new SimpleDateFormat("HH:mm", Locale.ROOT).format(new Date(item.aoVivo.entrouEm));
            return "Fila ao vivo · entrou às " + hora;
        }
        return "Agendado · " + item.agendada.horario + " · " + modalidade(item.agendada);
    }

    private static String modalidade(Consulta consulta) {
        return consulta.modalidade.isEmpty() ? "Presencial" : consulta.modalidade;
    }

    // ---- Ações ----

    private void mudarConsulta(Consulta consulta, String status, View botao, View outro) {
        botao.setEnabled(false);
        outro.setEnabled(false);

        db.collection("agendamentos").document(consulta.id)
                .update("status", status)
                .addOnSuccessListener(r -> {
                    Toast.makeText(this,
                            "Consulta: " + status.toLowerCase(Locale.ROOT) + ".", Toast.LENGTH_SHORT).show();
                    if (Consulta.CONFIRMADO.equals(status)) {
                        ConfirmacaoEmail.enviar(consulta.id);
                    }
                    // Consulta online: ao atender, o médico já entra na videochamada.
                    if (Consulta.EM_ATENDIMENTO.equals(status)
                            && !"Presencial".equalsIgnoreCase(consulta.modalidade)) {
                        Videochamada.entrar(this, consulta.id, nomeMedico());
                    }
                })
                .addOnFailureListener(e -> {
                    botao.setEnabled(true);
                    outro.setEnabled(true);
                    Toast.makeText(this, "Não foi possível atualizar a consulta.", Toast.LENGTH_LONG).show();
                });
    }

    private void mudarFila(EntradaFila entrada, String status, String campoData, View botao, View outro) {
        botao.setEnabled(false);
        outro.setEnabled(false);

        Map<String, Object> dados = new HashMap<>();
        dados.put("status", status);
        dados.put(campoData, FieldValue.serverTimestamp());

        db.collection(EntradaFila.COLECAO).document(entrada.id)
                .update(dados)
                .addOnSuccessListener(r -> {
                    // Fila ao vivo é sempre por vídeo: ao chamar, o médico já entra na sala.
                    if (EntradaFila.EM_ATENDIMENTO.equals(status)) {
                        Videochamada.entrar(this, entrada.id, nomeMedico());
                    }
                })
                .addOnFailureListener(e -> {
                    botao.setEnabled(true);
                    outro.setEnabled(true);
                    Toast.makeText(this, "Não foi possível atualizar a fila.", Toast.LENGTH_LONG).show();
                });
    }

    private String nomeMedico() {
        return medico != null && medico.nome != null ? medico.nome : "Médico";
    }

    private void abrirProntuario(String pacienteId) {
        Intent intent = new Intent(this, ProntuarioActivity.class);
        intent.putExtra(ProntuarioActivity.EXTRA_PACIENTE_ID, pacienteId);
        intent.putExtra(ProntuarioActivity.EXTRA_PACIENTE_NOME, nomesPacientes.get(pacienteId));
        startActivity(intent);
    }

    private void preencherNome(ItemFila item, TextView destino) {
        String pacienteId = item.pacienteId();

        if (item.agendada != null && !item.agendada.pacienteNome.isEmpty()) {
            nomesPacientes.put(pacienteId, item.agendada.pacienteNome);
        }

        if (pacienteId.isEmpty()) {
            destino.setText("Paciente não identificado");
            return;
        }

        String salvo = nomesPacientes.get(pacienteId);
        if (salvo != null) {
            destino.setText(salvo);
            return;
        }

        db.collection("pacientes").document(pacienteId).get()
                .addOnSuccessListener(documento -> {
                    String nome = documento.getString("nome");
                    if (nome == null || nome.trim().isEmpty()) {
                        nome = "Paciente";
                    }
                    nomesPacientes.put(pacienteId, nome.trim());
                    destino.setText(nome.trim());
                })
                .addOnFailureListener(e -> destino.setText("Paciente"));
    }

    private View estatistica(String rotulo, int valor, int cor, boolean direita) {
        LinearLayout card = Tela.card(this);
        LinearLayout.LayoutParams parametros = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        parametros.bottomMargin = Tela.dp(this, 12);
        if (direita) {
            parametros.setMarginStart(Tela.dp(this, 12));
        }
        card.setLayoutParams(parametros);

        card.addView(Tela.texto(this, rotulo, R.style.Texto_Pequeno, 0));
        TextView numero = Tela.texto(this, String.format(Locale.ROOT, "%02d", valor), R.style.Texto_Titulo, 4);
        numero.setTextSize(28);
        numero.setTextColor(ContextCompat.getColor(this, cor));
        card.addView(numero);
        return card;
    }

    private View vazio(String mensagem) {
        LinearLayout card = Tela.card(this);
        card.addView(Tela.texto(this, mensagem, R.style.Texto_Subtitulo, 0));
        return card;
    }
}

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
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Tela inicial do médico: visão geral, status de atendimento e pendências. */
public class DashboardMedicoActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private String uidMedico;

    private LinearLayout containerEstatisticas;
    private LinearLayout containerPendentes;
    private LinearLayout containerProximas;
    private MaterialSwitch switchOnline;

    private final Map<String, String> nomesPacientes = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard_medico);
        NavegacaoInferior.configurar(this, R.id.nav_inicio);

        db = FirebaseFirestore.getInstance();
        containerEstatisticas = findViewById(R.id.containerEstatisticas);
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
    protected void onRestart() {
        super.onRestart();
        if (uidMedico != null) {
            carregarConsultas();
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

                    Medico medico = Medico.de(documento);
                    String nome = documento.getString("nome");
                    if (nome == null || nome.trim().isEmpty()) {
                        nome = usuario.getDisplayName() == null ? "Médico" : usuario.getDisplayName();
                    }

                    ((TextView) findViewById(R.id.txtBoasVindasMedico))
                            .setText("Bem-vindo(a), " + nome);

                    mostrarStatus(medico.online);
                    switchOnline.setChecked(medico.online);
                    switchOnline.setOnCheckedChangeListener((botao, ativo) -> alterarStatus(ativo));

                    carregarConsultas();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this,
                            "Não foi possível verificar sua autorização.",
                            Toast.LENGTH_LONG).show();
                    Sessao.sair(this);
                });
    }

    private void alterarStatus(boolean online) {
        mostrarStatus(online);

        db.collection("medicos")
                .document(uidMedico)
                .update("online", online)
                .addOnFailureListener(e -> {
                    Toast.makeText(this,
                            "Não foi possível alterar seu status.",
                            Toast.LENGTH_SHORT).show();
                    switchOnline.setOnCheckedChangeListener(null);
                    switchOnline.setChecked(!online);
                    mostrarStatus(!online);
                    switchOnline.setOnCheckedChangeListener((botao, ativo) -> alterarStatus(ativo));
                });
    }

    private void mostrarStatus(boolean online) {
        findViewById(R.id.cardStatusOnline).setBackgroundResource(
                online ? R.drawable.bg_status_online : R.drawable.bg_status_offline);
        findViewById(R.id.indicadorOnline).setBackgroundResource(
                online ? R.drawable.bg_ponto_online : R.drawable.bg_ponto_offline);
        ((TextView) findViewById(R.id.txtStatusOnline)).setText(
                online ? "Você está atendendo" : "Você está offline");
        ((TextView) findViewById(R.id.txtStatusOnlineDetalhe)).setText(
                online
                        ? "Pacientes veem você como disponível agora"
                        : "Ative para aparecer como atendendo no app");
    }

    private void carregarConsultas() {
        db.collection("agendamentos")
                .whereEqualTo("medicoId", uidMedico)
                .get()
                .addOnSuccessListener(resultado -> {
                    List<Consulta> consultas = new ArrayList<>();

                    for (DocumentSnapshot documento : resultado.getDocuments()) {
                        consultas.add(new Consulta(documento));
                    }

                    consultas.sort((a, b) -> Long.compare(a.momento, b.momento));
                    mostrarConsultas(consultas);
                })
                .addOnFailureListener(e -> Toast.makeText(this,
                        "Erro ao carregar suas consultas.",
                        Toast.LENGTH_LONG).show());
    }

    private void mostrarConsultas(List<Consulta> consultas) {
        int pendentes = 0;
        int confirmadas = 0;
        int concluidas = 0;

        containerPendentes.removeAllViews();
        containerProximas.removeAllViews();

        for (Consulta consulta : consultas) {
            if (consulta.pendente()) {
                pendentes++;
                containerPendentes.addView(cardPendente(consulta));
            } else if (consulta.confirmada()) {
                confirmadas++;
                containerProximas.addView(cardProxima(consulta));
            } else if (consulta.concluida()) {
                concluidas++;
            }
        }

        if (pendentes == 0) {
            containerPendentes.addView(vazio("Nenhuma consulta aguardando confirmação."));
        }

        if (confirmadas == 0) {
            containerProximas.addView(vazio("Nenhuma consulta confirmada no momento."));
        }

        containerEstatisticas.removeAllViews();
        LinearLayout linha1 = Tela.linha(this);
        linha1.addView(estatistica("Pendentes", pendentes, R.color.aviso, false));
        linha1.addView(estatistica("Confirmadas", confirmadas, R.color.sucesso, true));
        LinearLayout linha2 = Tela.linha(this);
        linha2.addView(estatistica("Concluídas", concluidas, R.color.texto_escuro, false));
        linha2.addView(estatistica("Total", consultas.size(), R.color.texto_escuro, true));
        containerEstatisticas.addView(linha1);
        containerEstatisticas.addView(linha2);
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
        TextView numero = Tela.texto(this, String.format("%02d", valor), R.style.Texto_Titulo, 4);
        numero.setTextSize(28);
        numero.setTextColor(ContextCompat.getColor(this, cor));
        card.addView(numero);
        return card;
    }

    private View cardPendente(Consulta consulta) {
        LinearLayout card = Tela.card(this);
        card.addView(Tela.etiqueta(this, "Pendente"));

        TextView nome = Tela.texto(this, "Carregando paciente...", R.style.Texto_CardTitulo, 10);
        card.addView(nome);
        preencherNomePaciente(consulta.pacienteId, nome);

        card.addView(Tela.texto(this, consulta.especialidade, R.style.Texto_Pequeno, 2));
        card.addView(Tela.textoComIcone(this, consulta.quando(), R.drawable.ic_relogio, 8));

        LinearLayout botoes = Tela.linha(this);
        botoes.setPadding(0, Tela.dp(this, 14), 0, 0);
        Button confirmar = Tela.botao(this, "Confirmar", true);
        Button recusar = Tela.botao(this, "Recusar", false);
        ((LinearLayout.LayoutParams) recusar.getLayoutParams()).setMarginStart(Tela.dp(this, 10));
        botoes.addView(confirmar);
        botoes.addView(recusar);
        card.addView(botoes);

        confirmar.setOnClickListener(v -> alterarConsulta(consulta, Consulta.CONFIRMADO, v, recusar));
        recusar.setOnClickListener(v -> alterarConsulta(consulta, Consulta.RECUSADO, v, confirmar));
        return card;
    }

    private View cardProxima(Consulta consulta) {
        LinearLayout card = Tela.card(this);

        TextView nome = Tela.texto(this, "Carregando paciente...", R.style.Texto_CardTitulo, 0);
        card.addView(nome);
        preencherNomePaciente(consulta.pacienteId, nome);

        card.addView(Tela.texto(this, consulta.especialidade, R.style.Texto_Pequeno, 2));
        card.addView(Tela.textoComIcone(this, consulta.quando(), R.drawable.ic_relogio, 8));

        LinearLayout botoes = Tela.linha(this);
        botoes.setPadding(0, Tela.dp(this, 14), 0, 0);
        Button concluir = Tela.botao(this, "Concluir", true);
        Button anotacoes = Tela.botao(this, "Prontuário", false);
        ((LinearLayout.LayoutParams) anotacoes.getLayoutParams()).setMarginStart(Tela.dp(this, 10));
        botoes.addView(concluir);
        botoes.addView(anotacoes);
        card.addView(botoes);

        concluir.setOnClickListener(v -> alterarConsulta(consulta, Consulta.CONCLUIDO, v, anotacoes));
        anotacoes.setOnClickListener(v -> {
            Intent intent = new Intent(this, ProntuarioActivity.class);
            intent.putExtra(ProntuarioActivity.EXTRA_PACIENTE_ID, consulta.pacienteId);
            intent.putExtra(ProntuarioActivity.EXTRA_PACIENTE_NOME, nomesPacientes.get(consulta.pacienteId));
            startActivity(intent);
        });
        return card;
    }

    private void alterarConsulta(Consulta consulta, String novoStatus, View botao, View outro) {
        botao.setEnabled(false);
        outro.setEnabled(false);

        db.collection("agendamentos")
                .document(consulta.id)
                .update("status", novoStatus)
                .addOnSuccessListener(r -> {
                    Toast.makeText(this, "Consulta " + novoStatus.toLowerCase() + ".",
                            Toast.LENGTH_SHORT).show();
                    carregarConsultas();
                })
                .addOnFailureListener(e -> {
                    botao.setEnabled(true);
                    outro.setEnabled(true);
                    Toast.makeText(this, "Não foi possível atualizar a consulta.",
                            Toast.LENGTH_LONG).show();
                });
    }

    private void preencherNomePaciente(String pacienteId, TextView destino) {
        if (pacienteId.isEmpty()) {
            destino.setText("Paciente não identificado");
            return;
        }

        String salvo = nomesPacientes.get(pacienteId);
        if (salvo != null) {
            destino.setText(salvo);
            return;
        }

        db.collection("pacientes")
                .document(pacienteId)
                .get()
                .addOnSuccessListener(documento -> {
                    String nome = documento.getString("nome");
                    if (nome == null || nome.trim().isEmpty()) {
                        nome = "Paciente";
                    }
                    nomesPacientes.put(pacienteId, nome);
                    destino.setText(nome);
                })
                .addOnFailureListener(e -> destino.setText("Paciente"));
    }

    private View vazio(String mensagem) {
        LinearLayout card = Tela.card(this);
        card.addView(Tela.texto(this, mensagem, R.style.Texto_Subtitulo, 0));
        return card;
    }
}

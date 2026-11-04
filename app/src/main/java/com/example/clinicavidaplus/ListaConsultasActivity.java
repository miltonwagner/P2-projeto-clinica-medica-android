
package com.example.clinicavidaplus;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.util.HashSet;
import java.util.Set;

public class ListaConsultasActivity extends AppCompatActivity {

    private LinearLayout containerConsultas;
    private LinearLayout layoutSemConsultas;
    private FirebaseFirestore db;

    private final Set<String> cancelamentosEmAndamento =
            new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lista_consultas);

        db = FirebaseFirestore.getInstance();

        containerConsultas = findViewById(R.id.containerConsultas);
        layoutSemConsultas = findViewById(R.id.layoutSemConsultas);

        Button btnVoltar = findViewById(R.id.btnVoltarLista);
        Button btnNovo = findViewById(R.id.btnNovoAgendamento);

        btnVoltar.setOnClickListener(v -> irParaInicio());

        btnNovo.setOnClickListener(v ->
                startActivity(new Intent(
                        this,
                        ConsultasActivity.class
                ))
        );

        getOnBackPressedDispatcher().addCallback(this,
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        irParaInicio();
                    }
                });

        carregarConsultas();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (containerConsultas != null) {
            carregarConsultas();
        }
    }

    // =====================================================
    // CARREGAR CONSULTAS DO PACIENTE
    // =====================================================

    private void carregarConsultas() {

        FirebaseUser usuario =
                FirebaseAuth.getInstance().getCurrentUser();

        if (usuario == null) {

            mostrarSemConsultas();

            Toast.makeText(
                    this,
                    "Faça login para consultar seus agendamentos.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        String usuarioId = usuario.getUid();

        db.collection("agendamentos")
                .whereEqualTo("usuarioId", usuarioId)
                .get()
                .addOnSuccessListener(resultado -> {

                    containerConsultas.removeAllViews();

                    if (resultado.isEmpty()) {
                        mostrarSemConsultas();
                        return;
                    }

                    for (DocumentSnapshot documento
                            : resultado.getDocuments()) {

                        adicionarCartao(documento);
                    }

                    layoutSemConsultas.setVisibility(View.GONE);
                })
                .addOnFailureListener(e -> erroAoCarregar());
    }

    // =====================================================
    // MONTAR CARTÃO DA CONSULTA
    // =====================================================

    private void adicionarCartao(DocumentSnapshot documento) {

        String medico = valor(documento, "medico");
        String especialidade = valor(documento, "especialidade");
        String data = valor(documento, "data");
        String horario = valor(documento, "horario");
        String status = valor(documento, "status");

        CardView cartao = new CardView(this);

        LinearLayout.LayoutParams parametros =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        parametros.setMargins(0, 0, 0, 16);

        cartao.setLayoutParams(parametros);
        cartao.setRadius(20f);
        cartao.setCardElevation(3f);
        cartao.setCardBackgroundColor(Color.WHITE);
        cartao.setUseCompatPadding(true);

        LinearLayout conteudo = new LinearLayout(this);
        conteudo.setOrientation(LinearLayout.VERTICAL);
        conteudo.setPadding(24, 24, 24, 24);

        conteudo.addView(
                criarTexto("Consulta médica", 19, "#174D35", true)
        );

        TextView medicoTexto = criarTexto(
                "Médico: " + medico,
                16,
                "#263C30",
                true
        );

        adicionarMargemSuperior(medicoTexto, 12);
        conteudo.addView(medicoTexto);

        TextView especialidadeTexto = criarTexto(
                "Especialidade: " + especialidade,
                15,
                "#526657",
                false
        );

        adicionarMargemSuperior(especialidadeTexto, 8);
        conteudo.addView(especialidadeTexto);

        TextView dataTexto = criarTexto(
                "Data: " + data + " às " + horario,
                15,
                "#526657",
                false
        );

        adicionarMargemSuperior(dataTexto, 8);
        conteudo.addView(dataTexto);

        // Cores específicas para cada situação.
        String corStatus;

        if ("Executado".equalsIgnoreCase(status)) {
            corStatus = "#66766C";

        } else if ("Cancelado".equalsIgnoreCase(status)) {
            corStatus = "#B3261E";

        } else {
            corStatus = "#246B45";
        }

        TextView statusTexto = criarTexto(
                "Status: " + status,
                14,
                corStatus,
                true
        );

        adicionarMargemSuperior(statusTexto, 12);
        conteudo.addView(statusTexto);

        // O botão aparece somente para consultas agendadas.
        if ("Agendado".equalsIgnoreCase(status)) {

            Button btnCancelar = new Button(this);

            btnCancelar.setText("Cancelar consulta");
            btnCancelar.setAllCaps(false);
            btnCancelar.setTextColor(
                    Color.parseColor("#B3261E")
            );

            btnCancelar.setBackgroundTintList(
                    ColorStateList.valueOf(
                            Color.parseColor("#FDECEA")
                    )
            );

            LinearLayout.LayoutParams parametrosBotao =
                    new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    );

            parametrosBotao.topMargin = (int) (
                    16 * getResources()
                            .getDisplayMetrics().density
            );

            btnCancelar.setLayoutParams(parametrosBotao);

            btnCancelar.setOnClickListener(v ->
                    confirmarCancelamento(
                            documento.getId(),
                            btnCancelar
                    )
            );

            conteudo.addView(btnCancelar);
        }

        cartao.addView(conteudo);
        containerConsultas.addView(cartao);
    }

    // =====================================================
    // CONFIRMAR CANCELAMENTO
    // =====================================================

    private void confirmarCancelamento(
            String agendamentoId,
            Button botao
    ) {

        new AlertDialog.Builder(this)
                .setTitle("Cancelar consulta")
                .setMessage(
                        "Tem certeza de que deseja cancelar esta consulta?\n\n"
                                + "Essa ação liberará o horário para "
                                + "outro paciente."
                )
                .setNegativeButton("Não", (dialog, which) -> {
                    dialog.dismiss();
                })
                .setPositiveButton(
                        "Sim, cancelar",
                        (dialog, which) -> {

                            if (cancelamentosEmAndamento.contains(
                                    agendamentoId
                            )) {
                                return;
                            }

                            cancelarConsulta(
                                    agendamentoId,
                                    botao
                            );
                        }
                )
                .show();
    }

    // =====================================================
    // CANCELAR CONSULTA E LIBERAR HORÁRIO
    // =====================================================

    private void cancelarConsulta(
            String agendamentoId,
            Button botao
    ) {

        FirebaseUser usuario =
                FirebaseAuth.getInstance().getCurrentUser();

        if (usuario == null) {

            Toast.makeText(
                    this,
                    "Faça login para cancelar a consulta.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        String usuarioId = usuario.getUid();

        DocumentReference agendamentoRef =
                db.collection("agendamentos")
                        .document(agendamentoId);

        cancelamentosEmAndamento.add(agendamentoId);
        botao.setEnabled(false);

        db.runTransaction(transaction -> {

            // Primeiro, consultar o agendamento.
            DocumentSnapshot agendamento =
                    transaction.get(agendamentoRef);

            if (!agendamento.exists()) {
                throw new IllegalStateException(
                        "AGENDAMENTO_NAO_ENCONTRADO"
                );
            }

            // Verificar se pertence ao paciente autenticado.
            String donoAgendamento =
                    agendamento.getString("usuarioId");

            if (donoAgendamento == null
                    || !donoAgendamento.equals(usuarioId)) {

                throw new IllegalStateException(
                        "AGENDAMENTO_NAO_PERTENCE_AO_PACIENTE"
                );
            }

            // Somente consultas agendadas podem ser canceladas.
            String statusAtual =
                    agendamento.getString("status");

            if (!"Agendado".equalsIgnoreCase(statusAtual)) {

                throw new IllegalStateException(
                        "CONSULTA_NAO_PODE_SER_CANCELADA"
                );
            }

            // Recuperar os dados do horário reservado.
            String horarioId =
                    agendamento.getString("horarioId");

            DocumentReference horarioRef = null;
            DocumentSnapshot horarioDocumento = null;

            // Consultas antigas podem não possuir horárioId.
            // Nesse caso, somente o status será atualizado.
            if (horarioId != null
                    && !horarioId.trim().isEmpty()) {

                horarioRef = db.collection("horarios_ocupados")
                        .document(horarioId);

                // Ler o horário antes de executar qualquer gravação.
                horarioDocumento = transaction.get(horarioRef);
            }

            // Se o horário existe, confirmar que pertence
            // exatamente a este agendamento.
            if (horarioDocumento != null
                    && horarioDocumento.exists()) {

                String agendamentoDoHorario =
                        horarioDocumento.getString("agendamentoId");

                String medicoIdAgendamento =
                        agendamento.getString("medicoId");

                String medicoIdHorario =
                        horarioDocumento.getString("medicoId");

                String dataAgendamento =
                        agendamento.getString("data");

                String dataHorario =
                        horarioDocumento.getString("data");

                String horaAgendamento =
                        agendamento.getString("horario");

                String horaHorario =
                        horarioDocumento.getString("horario");

                Boolean ocupado =
                        horarioDocumento.getBoolean("ocupado");

                boolean horarioCorreto =
                        agendamentoId.equals(agendamentoDoHorario)
                                && medicoIdAgendamento != null
                                && medicoIdAgendamento.equals(medicoIdHorario)
                                && dataAgendamento != null
                                && dataAgendamento.equals(dataHorario)
                                && horaAgendamento != null
                                && horaAgendamento.equals(horaHorario)
                                && Boolean.TRUE.equals(ocupado);

                if (!horarioCorreto) {

                    throw new IllegalStateException(
                            "HORARIO_NAO_CORRESPONDE"
                    );
                }
            }

            // Atualizar o status do agendamento.
            transaction.update(
                    agendamentoRef,
                    "status",
                    "Cancelado"
            );

            // Liberar o horário somente se ele pertencer
            // ao agendamento que está sendo cancelado.
            if (horarioDocumento != null
                    && horarioDocumento.exists()) {

                transaction.delete(horarioRef);
            }

            return null;

        }).addOnSuccessListener(resultado -> {

            cancelamentosEmAndamento.remove(agendamentoId);

            Toast.makeText(
                    this,
                    "Consulta cancelada com sucesso. Horário liberado.",
                    Toast.LENGTH_LONG
            ).show();

            carregarConsultas();

        }).addOnFailureListener(erro -> {

            cancelamentosEmAndamento.remove(agendamentoId);

            if (!isFinishing()) {
                botao.setEnabled(true);
            }

            String mensagem = mensagemErroCancelamento(erro);

            Toast.makeText(
                    this,
                    mensagem,
                    Toast.LENGTH_LONG
            ).show();
        });
    }

    // =====================================================
    // TRATAR ERROS DE CANCELAMENTO
    // =====================================================

    private String mensagemErroCancelamento(Exception erro) {

        String detalhe = erro.getMessage();

        if (detalhe != null) {

            if (detalhe.contains("AGENDAMENTO_NAO_ENCONTRADO")) {
                return "O agendamento não foi encontrado.";
            }

            if (detalhe.contains(
                    "AGENDAMENTO_NAO_PERTENCE_AO_PACIENTE"
            )) {
                return "Você não pode cancelar esta consulta.";
            }

            if (detalhe.contains(
                    "CONSULTA_NAO_PODE_SER_CANCELADA"
            )) {
                return "Esta consulta não pode mais ser cancelada.";
            }

            if (detalhe.contains("HORARIO_NAO_CORRESPONDE")) {
                return "Não foi possível liberar o horário com segurança. "
                        + "Entre em contato com a clínica.";
            }
        }

        if (erro instanceof FirebaseFirestoreException) {

            FirebaseFirestoreException erroFirebase =
                    (FirebaseFirestoreException) erro;

            if (erroFirebase.getCode()
                    == FirebaseFirestoreException.Code.PERMISSION_DENIED) {

                return "O Firebase ainda não permite o cancelamento. "
                        + "As regras de segurança precisam ser atualizadas.";
            }
        }

        return "Não foi possível cancelar a consulta. "
                + "Verifique a conexão e tente novamente.";
    }

    // =====================================================
    // CRIAR TEXTOS
    // =====================================================

    private TextView criarTexto(
            String texto,
            int tamanho,
            String cor,
            boolean negrito
    ) {

        TextView view = new TextView(this);

        view.setText(texto);
        view.setTextSize(tamanho);
        view.setTextColor(Color.parseColor(cor));

        if (negrito) {
            view.setTypeface(null, Typeface.BOLD);
        }

        return view;
    }

    // =====================================================
    // MARGEM SUPERIOR
    // =====================================================

    private void adicionarMargemSuperior(View view, int dp) {

        LinearLayout.LayoutParams parametros =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        parametros.topMargin = (int) (
                dp * getResources().getDisplayMetrics().density
        );

        view.setLayoutParams(parametros);
    }

    // =====================================================
    // OBTER VALOR DO FIRESTORE
    // =====================================================

    private String valor(
            DocumentSnapshot documento,
            String campo
    ) {

        String valor = documento.getString(campo);

        return valor == null || valor.trim().isEmpty()
                ? "Não informado"
                : valor;
    }

    // =====================================================
    // TELA SEM CONSULTAS
    // =====================================================

    private void mostrarSemConsultas() {

        containerConsultas.removeAllViews();

        layoutSemConsultas.setVisibility(View.VISIBLE);
    }

    // =====================================================
    // ERRO AO CARREGAR CONSULTAS
    // =====================================================

    private void erroAoCarregar() {

        mostrarSemConsultas();

        Toast.makeText(
                this,
                "Erro ao carregar as consultas. "
                        + "Verifique a conexão e as permissões do Firebase.",
                Toast.LENGTH_LONG
        ).show();
    }

    // =====================================================
    // VOLTAR PARA O INÍCIO
    // =====================================================

    private void irParaInicio() {

        Intent intent = new Intent(
                this,
                IndexActivity.class
        );

        intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);
        finish();
    }
}

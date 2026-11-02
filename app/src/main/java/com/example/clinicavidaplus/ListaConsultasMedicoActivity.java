
package com.example.clinicavidaplus;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class ListaConsultasMedicoActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private LinearLayout containerConsultas;
    private LinearLayout layoutSemConsultas;
    private TextView txtIdentificacaoMedico;
    private Button btnSair;

    private String uidMedico;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lista_consultas_medico);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        containerConsultas =
                findViewById(R.id.containerConsultasMedico);

        layoutSemConsultas =
                findViewById(R.id.layoutSemConsultasMedico);

        txtIdentificacaoMedico =
                findViewById(R.id.txtIdentificacaoMedico);

        btnSair =
                findViewById(R.id.btnSairListaMedico);

        btnSair.setOnClickListener(v -> sairDaConta());

        verificarAutorizacaoMedico();
    }

    private void verificarAutorizacaoMedico() {

        FirebaseUser usuario = auth.getCurrentUser();

        if (usuario == null) {
            voltarParaLogin();
            return;
        }

        uidMedico = usuario.getUid();

        db.collection("medicos")
                .document(uidMedico)
                .get()
                .addOnSuccessListener(documento -> {

                    if (!documento.exists()) {
                        bloquearAcesso();
                        return;
                    }

                    Boolean autorizado =
                            documento.getBoolean("autorizado");

                    if (!Boolean.TRUE.equals(autorizado)) {
                        bloquearAcesso();
                        return;
                    }

                    String nomeMedico = documento.getString("nome");

                    if (nomeMedico == null || nomeMedico.trim().isEmpty()) {
                        nomeMedico = usuario.getDisplayName();
                    }

                    if (nomeMedico == null || nomeMedico.trim().isEmpty()) {
                        nomeMedico = "Médico";
                    }

                    txtIdentificacaoMedico.setText(
                            "Bem-vindo(a), " + nomeMedico
                                    + ". Acompanhe seus agendamentos."
                    );

                    carregarConsultasMedico();
                })
                .addOnFailureListener(e -> {

                    Toast.makeText(
                            this,
                            "Não foi possível verificar sua autorização.",
                            Toast.LENGTH_LONG
                    ).show();

                    voltarParaLogin();
                });
    }

    private void carregarConsultasMedico() {

        containerConsultas.removeAllViews();
        layoutSemConsultas.setVisibility(View.GONE);

        db.collection("agendamentos")
                .whereEqualTo("medicoId", uidMedico)
                .get()
                .addOnSuccessListener(resultado -> {

                    if (resultado.isEmpty()) {
                        layoutSemConsultas.setVisibility(View.VISIBLE);
                        return;
                    }

                    for (QueryDocumentSnapshot documento : resultado) {
                        criarCardConsulta(documento);
                    }
                })
                .addOnFailureListener(e -> {

                    Toast.makeText(
                            this,
                            "Erro ao carregar suas consultas. "
                                    + "Verifique as permissões do Firebase.",
                            Toast.LENGTH_LONG
                    ).show();

                    layoutSemConsultas.setVisibility(View.VISIBLE);
                });
    }

    private void criarCardConsulta(DocumentSnapshot documento) {

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(20, 18, 20, 18);

        GradientDrawable fundo = new GradientDrawable();
        fundo.setColor(Color.WHITE);
        fundo.setCornerRadius(dp(20));
        fundo.setStroke(1, Color.rgb(220, 232, 224));

        card.setBackground(fundo);
        card.setElevation(dp(3));

        LinearLayout.LayoutParams parametros =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        parametros.setMargins(0, 0, 0, dp(16));

        card.setLayoutParams(parametros);

        String especialidade = obterTexto(documento, "especialidade");
        String data = obterTexto(documento, "data");
        String horario = obterTexto(documento, "horario");
        String status = obterTexto(documento, "status");

        // Obtém o nome diretamente do documento do agendamento.
        String pacienteNome = obterTexto(documento, "pacienteNome");

        // Compatibilidade com os agendamentos antigos.
        if (pacienteNome.trim().isEmpty()) {
            pacienteNome = "Paciente de agendamento anterior";
        }

        adicionarTexto(
                card,
                "Consulta de " + especialidade,
                18,
                "#174D35",
                true
        );

        adicionarTexto(
                card,
                "Paciente: " + pacienteNome,
                15,
                "#334155",
                true
        );

        adicionarTexto(
                card,
                "Data: " + data,
                14,
                "#475569",
                false
        );

        adicionarTexto(
                card,
                "Horário: " + horario,
                14,
                "#475569",
                false
        );

        adicionarTexto(
                card,
                "Status: " + status,
                14,
                "#2E7D32",
                true
        );

        containerConsultas.addView(card);

        // Exibe o botão somente para consultas ainda agendadas.
        if ("Agendado".equalsIgnoreCase(status)) {
            adicionarBotaoExecutado(card, documento);
        }
    }

    private void adicionarBotaoExecutado(
            LinearLayout card,
            DocumentSnapshot documento) {

        Button botao = new Button(this);

        botao.setText("Marcar como Executado");
        botao.setTextColor(Color.WHITE);
        botao.setTextSize(14);
        botao.setAllCaps(false);
        botao.setTypeface(null, Typeface.BOLD);

        GradientDrawable fundoBotao = new GradientDrawable();
        fundoBotao.setColor(Color.parseColor("#246B45"));
        fundoBotao.setCornerRadius(dp(12));

        botao.setBackground(fundoBotao);

        LinearLayout.LayoutParams parametros =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(50)
                );

        parametros.topMargin = dp(12);

        botao.setLayoutParams(parametros);

        botao.setOnClickListener(v ->
                confirmarExecucao(documento, botao)
        );

        card.addView(botao);
    }

    private void confirmarExecucao(
            DocumentSnapshot documento,
            Button botao) {

        new AlertDialog.Builder(this)
                .setTitle("Confirmar realização")
                .setMessage(
                        "Deseja marcar esta consulta como Executado?"
                )
                .setNegativeButton("Cancelar", (dialog, which) ->
                        dialog.dismiss()
                )
                .setPositiveButton("Confirmar", (dialog, which) ->
                        atualizarStatusConsulta(documento, botao)
                )
                .show();
    }

    private void atualizarStatusConsulta(
            DocumentSnapshot documento,
            Button botao) {

        botao.setEnabled(false);
        botao.setText("Atualizando...");

        // Atualiza exclusivamente o campo status.
        documento.getReference()
                .update("status", "Executado")
                .addOnSuccessListener(unused -> {

                    Toast.makeText(
                            this,
                            "Consulta marcada como Executado!",
                            Toast.LENGTH_LONG
                    ).show();

                    carregarConsultasMedico();
                })
                .addOnFailureListener(e -> {

                    botao.setEnabled(true);
                    botao.setText("Marcar como Executado");

                    Toast.makeText(
                            this,
                            "Não foi possível atualizar a consulta. "
                                    + "Verifique as permissões do Firebase.",
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private TextView adicionarTexto(
            LinearLayout card,
            String texto,
            int tamanho,
            String cor,
            boolean negrito) {

        TextView textView = new TextView(this);

        textView.setText(texto);
        textView.setTextSize(tamanho);
        textView.setTextColor(Color.parseColor(cor));

        if (negrito) {
            textView.setTypeface(null, Typeface.BOLD);
        }

        LinearLayout.LayoutParams parametros =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        parametros.setMargins(0, 0, 0, dp(9));

        textView.setLayoutParams(parametros);

        card.addView(textView);

        return textView;
    }

    private String obterTexto(
            DocumentSnapshot documento,
            String campo) {

        String valor = documento.getString(campo);

        return valor == null ? "" : valor;
    }

    private int dp(int valor) {
        return (int) (
                valor * getResources().getDisplayMetrics().density
        );
    }

    private void bloquearAcesso() {

        Toast.makeText(
                this,
                "Acesso negado. Seu cadastro médico não está autorizado.",
                Toast.LENGTH_LONG
        ).show();

        auth.signOut();
        voltarParaLogin();
    }

    private void sairDaConta() {

        auth.signOut();

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

    private void voltarParaLogin() {

        Intent intent = new Intent(
                this,
                LoginActivity.class
        );

        intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);
        finish();
    }
}


package com.example.clinicavidaplus;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class ListaConsultasActivity extends AppCompatActivity {

    private LinearLayout containerConsultas;
    private LinearLayout layoutSemConsultas;
    private FirebaseFirestore db;

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

    private void adicionarCartao(DocumentSnapshot documento) {
        String medico = valor(documento, "medico");
        String especialidade = valor(documento, "especialidade");
        String data = valor(documento, "data");
        String horario = valor(documento, "horario");
        String status = valor(documento, "status");

        // O status não é alterado automaticamente quando a data passa.
        // A confirmação de realização será feita pelo médico responsável.

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

        String corStatus = "Executado".equalsIgnoreCase(status)
                ? "#66766C"
                : "#246B45";

        TextView statusTexto = criarTexto(
                "Status: " + status,
                14,
                corStatus,
                true
        );

        adicionarMargemSuperior(statusTexto, 12);
        conteudo.addView(statusTexto);

        cartao.addView(conteudo);
        containerConsultas.addView(cartao);
    }

    private TextView criarTexto(
            String texto,
            int tamanho,
            String cor,
            boolean negrito) {

        TextView view = new TextView(this);

        view.setText(texto);
        view.setTextSize(tamanho);
        view.setTextColor(Color.parseColor(cor));

        if (negrito) {
            view.setTypeface(null, Typeface.BOLD);
        }

        return view;
    }

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

    private String valor(DocumentSnapshot documento, String campo) {
        String valor = documento.getString(campo);

        return valor == null || valor.trim().isEmpty()
                ? "Não informado"
                : valor;
    }

    private void mostrarSemConsultas() {
        containerConsultas.removeAllViews();
        layoutSemConsultas.setVisibility(View.VISIBLE);
    }

    private void erroAoCarregar() {
        mostrarSemConsultas();

        Toast.makeText(
                this,
                "Erro ao carregar as consultas. Verifique a conexão e as permissões do Firebase.",
                Toast.LENGTH_LONG
        ).show();
    }

    private void irParaInicio() {
        Intent intent = new Intent(this, IndexActivity.class);

        intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);
        finish();
    }
}

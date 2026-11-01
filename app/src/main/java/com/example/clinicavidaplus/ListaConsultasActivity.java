
package com.example.clinicavidaplus;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

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

        NavegacaoInferior.configurar(this, R.id.nav_consultas);

        Button btnNovo = findViewById(R.id.btnNovoAgendamento);

        btnNovo.setOnClickListener(v ->
                startActivity(new Intent(
                        this,
                        ConsultasActivity.class
                ))
        );

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

        LinearLayout cartao = Tela.card(this);

        cartao.addView(Tela.etiqueta(this, status));
        cartao.addView(Tela.texto(this, medico, R.style.Texto_CardTitulo, 10));
        cartao.addView(Tela.texto(this, especialidade, R.style.Texto_Subtitulo, 2));
        cartao.addView(Tela.textoComIcone(
                this,
                data + " às " + horario,
                R.drawable.ic_nav_consultas,
                12
        ));

        containerConsultas.addView(cartao);
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
}

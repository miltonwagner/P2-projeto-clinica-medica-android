
package com.example.clinicavidaplus;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

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

        NavegacaoInferior.configurar(this, R.id.nav_agenda_medico);

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
        LinearLayout card = Tela.card(this);

        String especialidade = obterTexto(documento, "especialidade");
        String data = obterTexto(documento, "data");
        String horario = obterTexto(documento, "horario");
        String status = obterTexto(documento, "status");
        String pacienteId = obterTexto(documento, "usuarioId");

        card.addView(Tela.etiqueta(this, status));

        TextView txtPaciente = Tela.texto(
                this,
                "Paciente: carregando...",
                R.style.Texto_CardTitulo,
                10
        );
        card.addView(txtPaciente);
        card.addView(Tela.texto(this, especialidade, R.style.Texto_Subtitulo, 2));
        card.addView(Tela.textoComIcone(
                this,
                data + " às " + horario,
                R.drawable.ic_nav_consultas,
                12
        ));

        containerConsultas.addView(card);

        if (pacienteId.isEmpty()) {
            txtPaciente.setText("Paciente: identificação indisponível");
            return;
        }

        db.collection("pacientes")
                .document(pacienteId)
                .get()
                .addOnSuccessListener(documentoPaciente -> {

                    String nomePaciente =
                            documentoPaciente.getString("nome");

                    if (nomePaciente == null || nomePaciente.trim().isEmpty()) {
                        nomePaciente = "Cadastro não localizado";
                    }

                    txtPaciente.setText(nomePaciente);
                })
                .addOnFailureListener(e ->
                        txtPaciente.setText(
                                "Paciente: não foi possível consultar o cadastro"
                        )
                );
    }

    private String obterTexto(
            DocumentSnapshot documento,
            String campo
    ) {

        String valor = documento.getString(campo);

        return valor == null ? "" : valor;
    }

    private void bloquearAcesso() {
        Toast.makeText(
                this,
                "Acesso negado. Seu cadastro médico não está autorizado.",
                Toast.LENGTH_LONG
        ).show();

        Sessao.sair(this);
    }

    private void voltarParaLogin() {
        Sessao.sair(this);
    }
}

package com.example.clinicavidaplus;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RevisaoAgendamentoActivity extends AppCompatActivity {

    private String medico, especialidade, data, horario;
    private Button btnConfirmar;
    private boolean salvando = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_revisao_agendamento);

        medico = getIntent().getStringExtra("medico");
        especialidade = getIntent().getStringExtra("especialidade");
        data = getIntent().getStringExtra("data");
        horario = getIntent().getStringExtra("horario");

        if (medico == null || especialidade == null ||
                data == null || horario == null ||
                medico.trim().isEmpty() || especialidade.trim().isEmpty() ||
                data.trim().isEmpty() || horario.trim().isEmpty()) {
            Toast.makeText(this, "Dados incompletos. Selecione a consulta novamente.",
                    Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        ((TextView) findViewById(R.id.txtRevisaoEspecialidade)).setText(especialidade);
        ((TextView) findViewById(R.id.txtRevisaoMedico)).setText(medico);
        ((TextView) findViewById(R.id.txtRevisaoData)).setText(data);
        ((TextView) findViewById(R.id.txtRevisaoHorario)).setText(horario);

        btnConfirmar = findViewById(R.id.btnConfirmarRevisao);

        findViewById(R.id.btnVoltarRevisao).setOnClickListener(v -> finish());

        btnConfirmar.setOnClickListener(v -> confirmarAgendamento());
    }

    private void confirmarAgendamento() {
        if (salvando) return;

        FirebaseUser usuario = FirebaseAuth.getInstance().getCurrentUser();

        if (usuario == null) {
            Toast.makeText(this, "Faça login para agendar uma consulta.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        salvando = true;
        btnConfirmar.setEnabled(false);
        btnConfirmar.setText("SALVANDO...");

        Map<String, Object> consulta = new HashMap<>();
        consulta.put("medico", medico);
        consulta.put("especialidade", especialidade);
        consulta.put("data", data);
        consulta.put("horario", horario);
        consulta.put("status", "Agendado");
        consulta.put("usuarioId", usuario.getUid());

        FirebaseFirestore.getInstance()
                .collection("agendamentos")
                .add(consulta)
                .addOnSuccessListener(docRef -> {
                    Intent intent = new Intent(
                            RevisaoAgendamentoActivity.this,
                            AgendamentoSucessoActivity.class);
                    intent.putExtra("agendamentoId", docRef.getId());
                    intent.putExtra("medico", medico);
                    intent.putExtra("especialidade", especialidade);
                    intent.putExtra("data", data);
                    intent.putExtra("horario", horario);

                    startActivity(intent);

                    String email = usuario.getEmail();
                    if (email != null && !email.trim().isEmpty()) {
                        enviarEmailDeConfirmacao(usuario, email, docRef.getId());
                    }

                    finish();
                })
                .addOnFailureListener(e -> {
                    salvando = false;
                    btnConfirmar.setEnabled(true);
                    btnConfirmar.setText("CONFIRMAR CONSULTA");
                    Toast.makeText(this,
                            "Não foi possível salvar a consulta. Verifique sua conexão.",
                            Toast.LENGTH_LONG).show();
                });
    }

    private void enviarEmailDeConfirmacao(
            FirebaseUser usuario, String email, String agendamentoId) {

        String nomePadrao = usuario.getDisplayName() == null
                ? "Paciente" : usuario.getDisplayName();

        FirebaseFirestore.getInstance()
                .collection("usuarios")
                .document(usuario.getUid())
                .get()
                .addOnSuccessListener(documento -> {
                    String nome = documento.exists() &&
                            documento.getString("nome") != null
                            ? documento.getString("nome") : nomePadrao;
                    executarEnvioEmail(email, nome, agendamentoId);
                })
                .addOnFailureListener(e ->
                        executarEnvioEmail(email, nomePadrao, agendamentoId));
    }

    private void executarEnvioEmail(
            String email, String nome, String agendamentoId) {

        new EmailSender(
                email, nome, medico, especialidade, data, horario, agendamentoId,
                new EmailSender.OnEmailResult() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(getApplicationContext(),
                                "E-mail de confirmação enviado.",
                                Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onFailure(String erro) {
                        Toast.makeText(getApplicationContext(),
                                "Consulta salva, mas não foi possível enviar o e-mail.",
                                Toast.LENGTH_LONG).show();
                    }
                }).execute();
    }
}


package com.example.clinicavidaplus;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentReference;

import java.util.HashMap;
import java.util.Map;

public class RevisaoAgendamentoActivity extends AppCompatActivity {

    private String medico;
    private String medicoId;
    private String especialidade;
    private String data;
    private String horario;

    private Button btnConfirmar;
    private boolean salvando = false;

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_revisao_agendamento);

        db = FirebaseFirestore.getInstance();

        medico = getIntent().getStringExtra("medico");
        medicoId = getIntent().getStringExtra("medicoId");
        especialidade = getIntent().getStringExtra("especialidade");
        data = getIntent().getStringExtra("data");
        horario = getIntent().getStringExtra("horario");

        if (medico == null || medicoId == null
                || especialidade == null || data == null || horario == null
                || medico.trim().isEmpty() || medicoId.trim().isEmpty()
                || especialidade.trim().isEmpty()
                || data.trim().isEmpty() || horario.trim().isEmpty()) {

            Toast.makeText(
                    this,
                    "Dados incompletos. Selecione a consulta novamente.",
                    Toast.LENGTH_LONG
            ).show();

            finish();
            return;
        }

        ((TextView) findViewById(R.id.txtRevisaoEspecialidade))
                .setText(especialidade);

        ((TextView) findViewById(R.id.txtRevisaoMedico))
                .setText(medico);

        ((TextView) findViewById(R.id.txtRevisaoData))
                .setText(data);

        ((TextView) findViewById(R.id.txtRevisaoHorario))
                .setText(horario);

        btnConfirmar = findViewById(R.id.btnConfirmarRevisao);

        findViewById(R.id.btnVoltarRevisao)
                .setOnClickListener(v -> finish());

        btnConfirmar.setOnClickListener(v -> confirmarAgendamento());
    }

    private void confirmarAgendamento() {

        if (salvando) {
            return;
        }

        FirebaseUser usuario = FirebaseAuth.getInstance().getCurrentUser();

        if (usuario == null) {
            Toast.makeText(
                    this,
                    "Faça login para agendar uma consulta.",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        salvando = true;
        btnConfirmar.setEnabled(false);
        btnConfirmar.setText("SALVANDO...");

        String usuarioId = usuario.getUid();

        // Identificadores padronizados do horário.
        String dataChave = data.replace("/", "-");
        String horarioChave = horario.replace(":", "-");

        String horarioId = medicoId + "_"
                + dataChave + "_"
                + horarioChave;

        // Primeiro, buscamos o cadastro do paciente.
        db.collection("pacientes")
                .document(usuarioId)
                .get()
                .addOnSuccessListener(documentoPaciente -> {

                    if (!documentoPaciente.exists()) {
                        restaurarBotao();

                        Toast.makeText(
                                this,
                                "Cadastro do paciente não encontrado. Verifique seu cadastro.",
                                Toast.LENGTH_LONG
                        ).show();

                        return;
                    }

                    String pacienteNome =
                            documentoPaciente.getString("nome");

                    if (pacienteNome == null || pacienteNome.trim().isEmpty()) {
                        restaurarBotao();

                        Toast.makeText(
                                this,
                                "O nome do paciente não está preenchido no cadastro.",
                                Toast.LENGTH_LONG
                        ).show();

                        return;
                    }

                    salvarAgendamentoComTransacao(
                            usuario,
                            pacienteNome.trim(),
                            dataChave,
                            horarioChave,
                            horarioId
                    );
                })
                .addOnFailureListener(e -> {

                    restaurarBotao();

                    Toast.makeText(
                            this,
                            "Não foi possível consultar o cadastro do paciente.",
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private void salvarAgendamentoComTransacao(
            FirebaseUser usuario,
            String pacienteNome,
            String dataChave,
            String horarioChave,
            String horarioId
    ) {

        String agendamentoId;

        DocumentReference agendamentoRef =
                db.collection("agendamentos").document();

        agendamentoId = agendamentoRef.getId();

        DocumentReference horarioRef =
                db.collection("horarios_ocupados")
                        .document(horarioId);

        // Dados do agendamento.
        Map<String, Object> consulta = new HashMap<>();

        consulta.put("medico", medico);
        consulta.put("medicoId", medicoId);
        consulta.put("especialidade", especialidade);
        consulta.put("data", data);
        consulta.put("horario", horario);
        consulta.put("status", "Agendado");
        consulta.put("usuarioId", usuario.getUid());
        consulta.put("pacienteNome", pacienteNome);
        consulta.put("horarioId", horarioId);
        consulta.put("dataChave", dataChave);
        consulta.put("horarioChave", horarioChave);

        // Dados da reserva do horário.
        Map<String, Object> horarioOcupado = new HashMap<>();

        horarioOcupado.put("medicoId", medicoId);
        horarioOcupado.put("data", data);
        horarioOcupado.put("horario", horario);
        horarioOcupado.put("dataChave", dataChave);
        horarioOcupado.put("horarioChave", horarioChave);
        horarioOcupado.put("ocupado", true);
        horarioOcupado.put("agendamentoId", agendamentoId);

        // A transação grava os dois documentos juntos.
        db.runTransaction(transaction -> {

            DocumentSnapshot horarioExistente =
                    transaction.get(horarioRef);

            if (horarioExistente.exists()) {
                throw new IllegalStateException(
                        "HORARIO_JA_RESERVADO"
                );
            }

            transaction.set(agendamentoRef, consulta);
            transaction.set(horarioRef, horarioOcupado);

            return null;

        }).addOnSuccessListener(resultado -> {

            abrirTelaSucesso(usuario, agendamentoId);

        }).addOnFailureListener(e -> {

            restaurarBotao();

            String erro = e.getMessage();

            if (erro != null
                    && erro.contains("HORARIO_JA_RESERVADO")) {

                Toast.makeText(
                        RevisaoAgendamentoActivity.this,
                        "Este horário acabou de ser reservado por outra pessoa. Escolha outro horário.",
                        Toast.LENGTH_LONG
                ).show();

            } else {

                Toast.makeText(
                        RevisaoAgendamentoActivity.this,
                        "Não foi possível confirmar a consulta. Verifique sua conexão ou tente novamente.",
                        Toast.LENGTH_LONG
                ).show();
            }
        });
    }

    private void abrirTelaSucesso(
            FirebaseUser usuario,
            String agendamentoId
    ) {

        Intent intent = new Intent(
                RevisaoAgendamentoActivity.this,
                AgendamentoSucessoActivity.class
        );

        intent.putExtra("agendamentoId", agendamentoId);
        intent.putExtra("medico", medico);
        intent.putExtra("medicoId", medicoId);
        intent.putExtra("especialidade", especialidade);
        intent.putExtra("data", data);
        intent.putExtra("horario", horario);

        startActivity(intent);

        String email = usuario.getEmail();

        if (email != null && !email.trim().isEmpty()) {
            enviarEmailDeConfirmacao(
                    usuario,
                    email,
                    agendamentoId
            );
        }

        finish();
    }

    private void restaurarBotao() {

        salvando = false;

        if (btnConfirmar != null) {
            btnConfirmar.setEnabled(true);
            btnConfirmar.setText("CONFIRMAR CONSULTA");
        }
    }

    private void enviarEmailDeConfirmacao(
            FirebaseUser usuario,
            String email,
            String agendamentoId
    ) {

        String nomePadrao = usuario.getDisplayName() == null
                ? "Paciente"
                : usuario.getDisplayName();

        db.collection("usuarios")
                .document(usuario.getUid())
                .get()
                .addOnSuccessListener(documento -> {

                    String nome = documento.exists()
                            && documento.getString("nome") != null
                            ? documento.getString("nome")
                            : nomePadrao;

                    executarEnvioEmail(email, nome, agendamentoId);
                })
                .addOnFailureListener(e ->
                        executarEnvioEmail(
                                email,
                                nomePadrao,
                                agendamentoId
                        )
                );
    }

    private void executarEnvioEmail(
            String email,
            String nome,
            String agendamentoId
    ) {

        new EmailSender(
                email,
                nome,
                medico,
                especialidade,
                data,
                horario,
                agendamentoId,
                new EmailSender.OnEmailResult() {

                    @Override
                    public void onSuccess() {
                        Toast.makeText(
                                getApplicationContext(),
                                "E-mail de confirmação enviado.",
                                Toast.LENGTH_LONG
                        ).show();
                    }

                    @Override
                    public void onFailure(String erro) {
                        Toast.makeText(
                                getApplicationContext(),
                                "Consulta salva, mas não foi possível enviar o e-mail.",
                                Toast.LENGTH_LONG
                        ).show();
                    }
                }
        ).execute();
    }
}

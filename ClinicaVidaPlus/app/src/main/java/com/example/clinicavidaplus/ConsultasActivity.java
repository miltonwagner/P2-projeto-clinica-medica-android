package com.example.clinicavidaplus;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConsultasActivity extends AppCompatActivity {
    private Spinner spinnerMedicos, spinnerEspecialidades;
    private TextView txtData;
    private GridLayout grid;
    private String dataF = "", horaF = "";
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            setContentView(R.layout.activity_consultas);
            db = FirebaseFirestore.getInstance();

            spinnerMedicos = findViewById(R.id.spinnerMedicos);
            spinnerEspecialidades = findViewById(R.id.spinnerEspecialidades);
            txtData = findViewById(R.id.txtDataSelecionada);
            grid = findViewById(R.id.gridHorarios);

            String[] medicos = {"Selecione o Médico", "Dr. Arnaldo Silva", "Dra. Juliana Costa", "Dr. Roberto Souza"};
            ArrayAdapter<String> adapterMed = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, medicos);
            adapterMed.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerMedicos.setAdapter(adapterMed);

            findViewById(R.id.btnVoltar).setOnClickListener(v -> finish());

            findViewById(R.id.btnSelecionarData).setOnClickListener(v -> {
                Calendar c = Calendar.getInstance();
                new DatePickerDialog(this, (view, y, m, d) -> {
                    dataF = String.format("%02d/%02d/%d", d, (m + 1), y);
                    txtData.setText("Data: " + dataF);
                    carregarHorariosDisponiveis();
                }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
            });

            findViewById(R.id.btnConfirmarAgendamento).setOnClickListener(v -> salvarAgendamento());

        } catch (Exception e) {
            Log.e("CONSULTAS_ERRO", "Erro crítico: " + e.getMessage());
            Toast.makeText(this, "Erro ao carregar a tela de consultas", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void carregarHorariosDisponiveis() {
        if (grid == null) return;
        String medico = spinnerMedicos.getSelectedItem().toString();
        if (dataF.isEmpty() || medico.equals("Selecione o Médico")) return;

        grid.removeAllViews();
        String[] horas = {"08:00", "09:00", "10:00", "11:00", "13:00", "14:00", "15:00", "16:00", "17:00"};

        db.collection("agendamentos")
                .whereEqualTo("medico", medico)
                .whereEqualTo("data", dataF)
                .get()
                .addOnSuccessListener(snapshots -> {
                    List<String> ocupados = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshots) {
                        ocupados.add(doc.getString("horario"));
                    }

                    for (String h : horas) {
                        Button b = new Button(this);
                        b.setText(h);
                        if (ocupados.contains(h)) {
                            b.setEnabled(false);
                            b.setBackgroundTintList(ColorStateList.valueOf(Color.LTGRAY));
                        } else {
                            b.setOnClickListener(v -> {
                                horaF = h;
                                resetarCoresBotoes();
                                b.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#2E7D32")));
                                b.setTextColor(Color.WHITE);
                            });
                        }
                        grid.addView(b);
                    }
                });
    }

    private void resetarCoresBotoes() {
        for (int i = 0; i < grid.getChildCount(); i++) {
            if (grid.getChildAt(i).isEnabled()) {
                grid.getChildAt(i).setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E8F5E9")));
                ((Button) grid.getChildAt(i)).setTextColor(Color.BLACK);
            }
        }
    }

    private void salvarAgendamento() {
        String med = spinnerMedicos.getSelectedItem().toString();
        if (dataF.isEmpty() || horaF.isEmpty() || med.equals("Selecione o Médico")) {
            Toast.makeText(this, "Preencha médico, data e hora!", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> consulta = new HashMap<>();
        consulta.put("medico", med);
        consulta.put("especialidade", spinnerEspecialidades.getSelectedItem().toString());
        consulta.put("data", dataF);
        consulta.put("horario", horaF);
        consulta.put("status", "Agendado");

        db.collection("agendamentos").add(consulta).addOnSuccessListener(docRef -> {

            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

            if (user != null && user.getEmail() != null) {
                String emailUsuario = user.getEmail();
                String nomeUsuario = user.getDisplayName() != null ? user.getDisplayName() : "Paciente";

                db.collection("usuarios").document(user.getUid()).get()
                        .addOnSuccessListener(doc -> {
                            String nome = doc.exists() && doc.getString("nome") != null
                                    ? doc.getString("nome") : nomeUsuario;

                            new EmailSender(
                                    emailUsuario,
                                    nome,
                                    med,
                                    spinnerEspecialidades.getSelectedItem().toString(),
                                    dataF,
                                    horaF,
                                    docRef.getId(),
                                    new EmailSender.OnEmailResult() {
                                        @Override
                                        public void onSuccess() {
                                            Toast.makeText(ConsultasActivity.this,
                                                    "Consulta agendada! E-mail de confirmação enviado ✉️",
                                                    Toast.LENGTH_LONG).show();
                                        }
                                        @Override
                                        public void onFailure(String erro) {
                                            Toast.makeText(ConsultasActivity.this,
                                                    "Consulta agendada! (Falha no e-mail)",
                                                    Toast.LENGTH_SHORT).show();
                                        }
                                    }
                            ).execute();
                        })
                        .addOnFailureListener(e -> {
                            // Se falhar ao buscar nome, envia com nome padrão
                            new EmailSender(
                                    emailUsuario,
                                    nomeUsuario,
                                    med,
                                    spinnerEspecialidades.getSelectedItem().toString(),
                                    dataF,
                                    horaF,
                                    docRef.getId(),
                                    new EmailSender.OnEmailResult() {
                                        @Override
                                        public void onSuccess() {
                                            Toast.makeText(ConsultasActivity.this,
                                                    "Consulta agendada! E-mail enviado ✉️",
                                                    Toast.LENGTH_LONG).show();
                                        }
                                        @Override
                                        public void onFailure(String erro) {
                                            Toast.makeText(ConsultasActivity.this,
                                                    "Consulta agendada! (Falha no e-mail)",
                                                    Toast.LENGTH_SHORT).show();
                                        }
                                    }
                            ).execute();
                        });
            }

            Intent intent = new Intent(this, ListaConsultasActivity.class);
            intent.putExtra("agendamentoId", docRef.getId());
            startActivity(intent);

        }).addOnFailureListener(e ->
                Toast.makeText(this, "Erro ao agendar: " + e.getMessage(), Toast.LENGTH_SHORT).show()
        );
    }
}

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
    private String dataF = "";
    private String horaF = "";
    private FirebaseFirestore db;

    private static final String MEDICO_ARNALDO_ID =
            "ppBJexJ2FEUoijPoAhidZzuvBar2";
    private static final String MEDICA_JULIANA_ID =
            "CgzDk7lod9hkniRUQMHc4szYUMG2";
    private static final String MEDICO_ROBERTO_ID =
            "krdYyomS2GQEAw3WZTVYqqFO2X03";

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

            String[] medicos = {
                    "Selecione o Médico",
                    "Dr. Arnaldo Silva",
                    "Dra. Juliana Costa",
                    "Dr. Roberto Souza"
            };

            ArrayAdapter<String> adapterMed = new ArrayAdapter<>(
                    this,
                    android.R.layout.simple_spinner_item,
                    medicos
            );
            adapterMed.setDropDownViewResource(
                    android.R.layout.simple_spinner_dropdown_item
            );
            spinnerMedicos.setAdapter(adapterMed);

            findViewById(R.id.btnVoltar).setOnClickListener(v -> {
                Intent intent = new Intent(
                        ConsultasActivity.this,
                        IndexActivity.class
                );
                intent.addFlags(
                        Intent.FLAG_ACTIVITY_CLEAR_TOP
                                | Intent.FLAG_ACTIVITY_SINGLE_TOP
                );
                startActivity(intent);
                finish();
            });

            findViewById(R.id.btnSelecionarData).setOnClickListener(v -> {
                Calendar c = Calendar.getInstance();

                new DatePickerDialog(
                        this,
                        (view, y, m, d) -> {
                            dataF = String.format(
                                    "%02d/%02d/%d",
                                    d,
                                    m + 1,
                                    y
                            );
                            txtData.setText("Data: " + dataF);
                            horaF = "";
                            carregarHorariosDisponiveis();
                        },
                        c.get(Calendar.YEAR),
                        c.get(Calendar.MONTH),
                        c.get(Calendar.DAY_OF_MONTH)
                ).show();
            });

            spinnerMedicos.setOnItemSelectedListener(
                    new android.widget.AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(
                                android.widget.AdapterView<?> parent,
                                android.view.View view,
                                int position,
                                long id
                        ) {
                            horaF = "";
                            if (!dataF.isEmpty()) {
                                carregarHorariosDisponiveis();
                            } else if (grid != null) {
                                grid.removeAllViews();
                            }
                        }

                        @Override
                        public void onNothingSelected(
                                android.widget.AdapterView<?> parent
                        ) {
                            horaF = "";
                            if (grid != null) {
                                grid.removeAllViews();
                            }
                        }
                    }
            );

            findViewById(R.id.btnConfirmarAgendamento)
                    .setOnClickListener(v -> abrirRevisaoAgendamento());

        } catch (Exception e) {
            Log.e("CONSULTAS_ERRO", "Erro ao carregar a tela", e);
            Toast.makeText(
                    this,
                    "Erro ao carregar a tela de consultas.",
                    Toast.LENGTH_LONG
            ).show();
            finish();
        }
    }

    private String obterMedicoId(String medico) {
        if ("Dr. Arnaldo Silva".equals(medico)) {
            return MEDICO_ARNALDO_ID;
        }

        if ("Dra. Juliana Costa".equals(medico)) {
            return MEDICA_JULIANA_ID;
        }

        if ("Dr. Roberto Souza".equals(medico)) {
            return MEDICO_ROBERTO_ID;
        }

        return null;
    }

    private void carregarHorariosDisponiveis() {
        if (grid == null || spinnerMedicos.getSelectedItem() == null) {
            return;
        }

        String medico = spinnerMedicos.getSelectedItem().toString();

        if (dataF.isEmpty() || "Selecione o Médico".equals(medico)) {
            grid.removeAllViews();
            return;
        }

        grid.removeAllViews();

        String[] horas = {
                "08:00", "09:00", "10:00", "11:00",
                "13:00", "14:00", "15:00", "16:00", "17:00"
        };

        db.collection("agendamentos")
                .whereEqualTo("medico", medico)
                .whereEqualTo("data", dataF)
                .get()
                .addOnSuccessListener(snapshots -> {
                    List<String> ocupados = new ArrayList<>();

                    for (DocumentSnapshot doc : snapshots) {
                        String horario = doc.getString("horario");
                        if (horario != null) {
                            ocupados.add(horario);
                        }
                    }

                    for (String h : horas) {
                        Button botao = new Button(this);
                        botao.setText(h);

                        if (ocupados.contains(h)) {
                            botao.setEnabled(false);
                            botao.setBackgroundTintList(
                                    ColorStateList.valueOf(Color.LTGRAY)
                            );
                        } else {
                            botao.setBackgroundTintList(
                                    ColorStateList.valueOf(
                                            Color.parseColor("#E8F5E9")
                                    )
                            );
                            botao.setTextColor(Color.BLACK);

                            botao.setOnClickListener(v -> {
                                horaF = h;
                                resetarCoresBotoes();

                                botao.setBackgroundTintList(
                                        ColorStateList.valueOf(
                                                Color.parseColor("#2E7D32")
                                        )
                                );
                                botao.setTextColor(Color.WHITE);
                            });
                        }

                        grid.addView(botao);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(
                            "CONSULTAS_HORARIOS",
                            "Erro ao consultar horários",
                            e
                    );
                    Toast.makeText(
                            this,
                            "Não foi possível carregar os horários. Tente novamente.",
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private void resetarCoresBotoes() {
        if (grid == null) {
            return;
        }

        for (int i = 0; i < grid.getChildCount(); i++) {
            if (grid.getChildAt(i) instanceof Button) {
                Button botao = (Button) grid.getChildAt(i);

                if (botao.isEnabled()) {
                    botao.setBackgroundTintList(
                            ColorStateList.valueOf(
                                    Color.parseColor("#E8F5E9")
                            )
                    );
                    botao.setTextColor(Color.BLACK);
                }
            }
        }
    }

    /*
     * Método antigo mantido para preservar o código existente.
     * O fluxo principal de agendamento utiliza a tela de revisão.
     */
    private void salvarAgendamento() {
        String med = spinnerMedicos.getSelectedItem() == null
                ? ""
                : spinnerMedicos.getSelectedItem().toString();

        if (dataF.isEmpty() || horaF.isEmpty()
                || med.isEmpty()
                || "Selecione o Médico".equals(med)) {
            Toast.makeText(
                    this,
                    "Preencha médico, data e horário!",
                    Toast.LENGTH_SHORT
            ).show();
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

        String medicoId = obterMedicoId(med);

        if (medicoId == null) {
            Toast.makeText(
                    this,
                    "Médico inválido.",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        Map<String, Object> consulta = new HashMap<>();
        consulta.put("medico", med);
        consulta.put("medicoId", medicoId);
        consulta.put(
                "especialidade",
                spinnerEspecialidades.getSelectedItem().toString()
        );
        consulta.put("data", dataF);
        consulta.put("horario", horaF);
        consulta.put("status", "Agendado");
        consulta.put("usuarioId", usuario.getUid());

        db.collection("agendamentos")
                .add(consulta)
                .addOnSuccessListener(docRef -> {
                    Intent intent = new Intent(
                            this,
                            ListaConsultasActivity.class
                    );
                    intent.putExtra("agendamentoId", docRef.getId());
                    startActivity(intent);
                })
                .addOnFailureListener(e -> {
                    Log.e("CONSULTAS_SALVAR", "Erro ao agendar", e);
                    Toast.makeText(
                            this,
                            "Erro ao agendar: " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private void abrirRevisaoAgendamento() {
        String medico = spinnerMedicos.getSelectedItem() == null
                ? ""
                : spinnerMedicos.getSelectedItem().toString();

        String especialidade = spinnerEspecialidades.getSelectedItem() == null
                ? ""
                : spinnerEspecialidades.getSelectedItem().toString();

        if (medico.isEmpty()
                || "Selecione o Médico".equals(medico)
                || especialidade.isEmpty()
                || dataF.isEmpty()
                || horaF.isEmpty()) {
            Toast.makeText(
                    this,
                    "Selecione especialidade, médico, data e horário.",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        String medicoId = obterMedicoId(medico);

        if (medicoId == null) {
            Toast.makeText(
                    this,
                    "Não foi possível identificar o médico selecionado.",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        Intent intent = new Intent(
                this,
                RevisaoAgendamentoActivity.class
        );
        intent.putExtra("medico", medico);
        intent.putExtra("medicoId", medicoId);
        intent.putExtra("especialidade", especialidade);
        intent.putExtra("data", dataF);
        intent.putExtra("horario", horaF);

        startActivity(intent);
    }
}

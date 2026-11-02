
package com.example.clinicavidaplus;

import android.app.DatePickerDialog;
import android.content.Intent;
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

    /** Nome do médico escolhido no marketplace (pré-seleciona o formulário). */
    public static final String EXTRA_MEDICO_NOME = "medico_pre_selecionado";
    private static final String SELECIONE_MEDICO = "Selecione o Médico";

    private final List<String> nomesMedicos = new ArrayList<>();
    private final Map<String, Medico> medicosPorNome = new HashMap<>();

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

            configurarMedicos();

            NavegacaoInferior.configurar(this, R.id.nav_inicio);

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
                            selecionarEspecialidadeDoMedico();
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

    /** Lista os médicos da clínica (Firestore, com o catálogo como reserva). */
    private void configurarMedicos() {
        preencherMedicos(Medico.padroes());

        String preSelecionado = getIntent().getStringExtra(EXTRA_MEDICO_NOME);
        selecionarMedico(preSelecionado);

        db.collection("medicos")
                .get()
                .addOnSuccessListener(resultado -> {
                    if (resultado.isEmpty()) {
                        return;
                    }

                    Object atual = spinnerMedicos.getSelectedItem();
                    List<Medico> lista = new ArrayList<>();

                    for (DocumentSnapshot documento : resultado.getDocuments()) {
                        lista.add(Medico.de(documento));
                    }

                    preencherMedicos(lista);
                    selecionarMedico(atual == null ? preSelecionado : atual.toString());
                });
    }

    private void preencherMedicos(List<Medico> lista) {
        nomesMedicos.clear();
        medicosPorNome.clear();
        nomesMedicos.add(SELECIONE_MEDICO);

        for (Medico medico : lista) {
            nomesMedicos.add(medico.nome);
            medicosPorNome.put(medico.nome, medico);
        }

        ArrayAdapter<String> adapterMed = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                new ArrayList<>(nomesMedicos)
        );
        adapterMed.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );
        spinnerMedicos.setAdapter(adapterMed);
    }

    private void selecionarMedico(String nome) {
        int posicao = nome == null ? -1 : nomesMedicos.indexOf(nome);

        if (posicao > 0) {
            spinnerMedicos.setSelection(posicao);
            selecionarEspecialidadeDoMedico();
        }
    }

    /** A especialidade acompanha o médico escolhido. */
    private void selecionarEspecialidadeDoMedico() {
        Object selecionado = spinnerMedicos.getSelectedItem();
        Medico medico = selecionado == null ? null : medicosPorNome.get(selecionado.toString());

        if (medico == null) {
            return;
        }

        String[] especialidades = getResources().getStringArray(R.array.especialidades_array);

        for (int i = 0; i < especialidades.length; i++) {
            if (especialidades[i].equalsIgnoreCase(medico.especialidade)) {
                spinnerEspecialidades.setSelection(i);
                return;
            }
        }
    }

    private String obterMedicoId(String medico) {
        Medico encontrado = medicosPorNome.get(medico);

        if (encontrado != null) {
            return encontrado.id;
        }

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

        if (dataF.isEmpty() || SELECIONE_MEDICO.equals(medico)) {
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
                        Button botao = criarBotaoHorario(h);

                        if (ocupados.contains(h)) {
                            botao.setEnabled(false);
                            botao.setBackgroundResource(R.drawable.bg_chip);
                            botao.setTextColor(getColor(R.color.texto_claro));
                        } else {
                            botao.setBackgroundResource(R.drawable.bg_chip);
                            botao.setTextColor(getColor(R.color.texto_medio));

                            botao.setOnClickListener(v -> {
                                horaF = h;
                                resetarCoresBotoes();

                                botao.setBackgroundResource(R.drawable.bg_chip_selecionado);
                                botao.setTextColor(getColor(R.color.white));
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

    /** Botão de horário no padrão "chip" (3 por linha). */
    private Button criarBotaoHorario(String horario) {
        Button botao = new Button(this);
        botao.setText(horario);
        botao.setAllCaps(false);
        botao.setTextSize(14);
        botao.setStateListAnimator(null);
        botao.setMinWidth(0);
        botao.setMinimumWidth(0);

        GridLayout.LayoutParams parametros = new GridLayout.LayoutParams(
                GridLayout.spec(GridLayout.UNDEFINED),
                GridLayout.spec(GridLayout.UNDEFINED, 1f)
        );
        parametros.width = 0;
        parametros.height = Tela.dp(this, 44);
        int margem = Tela.dp(this, 4);
        parametros.setMargins(margem, margem, margem, margem);
        botao.setLayoutParams(parametros);

        return botao;
    }

    private void resetarCoresBotoes() {
        if (grid == null) {
            return;
        }

        for (int i = 0; i < grid.getChildCount(); i++) {
            if (grid.getChildAt(i) instanceof Button) {
                Button botao = (Button) grid.getChildAt(i);

                if (botao.isEnabled()) {
                    botao.setBackgroundResource(R.drawable.bg_chip);
                    botao.setTextColor(getColor(R.color.texto_medio));
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
                || SELECIONE_MEDICO.equals(medico)
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

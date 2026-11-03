
package com.example.clinicavidaplus;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

public class ConsultasActivity extends AppCompatActivity {

    private Spinner spinnerMedicos, spinnerEspecialidades;
    private TextView txtData;
    private GridLayout grid;

    private String dataF = "";
    private String horaF = "";

    private FirebaseFirestore db;

    private int requisicaoHorarios = 0;

    private final List<Medico> medicosPublicos = new ArrayList<>();

    private static final String TEXTO_SELECIONE_MEDICO =
            "Selecione o Médico";

    private static final String TEXTO_SELECIONE_ESPECIALIDADE =
            "Selecione a Especialidade";

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

            configurarSpinnersIniciais();

            configurarBotaoVoltar();
            configurarSelecaoData();
            configurarSelecaoEspecialidade();
            configurarSelecaoMedico();
            configurarBotaoConfirmar();

            carregarMedicosPublicos();

        } catch (Exception e) {

            Log.e(
                    "CONSULTAS_ERRO",
                    "Erro ao carregar a tela",
                    e
            );

            Toast.makeText(
                    this,
                    "Erro ao carregar a tela de consultas.",
                    Toast.LENGTH_LONG
            ).show();

            finish();
        }
    }

    // =====================================================
    // MODELO DO MÉDICO
    // =====================================================

    private static class Medico {

        private final String id;
        private final String nome;
        private final String especialidade;

        Medico(
                String id,
                String nome,
                String especialidade
        ) {
            this.id = id;
            this.nome = nome;
            this.especialidade = especialidade;
        }

        @Override
        public String toString() {
            return nome;
        }
    }

    // =====================================================
    // CONFIGURAÇÃO INICIAL DOS SPINNERS
    // =====================================================

    private void configurarSpinnersIniciais() {

        List<String> especialidades = new ArrayList<>();

        especialidades.add(TEXTO_SELECIONE_ESPECIALIDADE);

        ArrayAdapter<String> adapterEspecialidades =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_item,
                        especialidades
                );

        adapterEspecialidades.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spinnerEspecialidades.setAdapter(adapterEspecialidades);

        List<Medico> medicos = new ArrayList<>();

        medicos.add(
                new Medico(
                        null,
                        TEXTO_SELECIONE_MEDICO,
                        null
                )
        );

        ArrayAdapter<Medico> adapterMedicos =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_item,
                        medicos
                );

        adapterMedicos.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spinnerMedicos.setAdapter(adapterMedicos);
    }

    // =====================================================
    // CARREGAR MÉDICOS PUBLICADOS NO FIRESTORE
    // =====================================================

    private void carregarMedicosPublicos() {

        Toast.makeText(
                this,
                "Carregando médicos disponíveis...",
                Toast.LENGTH_SHORT
        ).show();

        db.collection("medicos_publicos")
                .get()
                .addOnSuccessListener(resultado -> {

                    medicosPublicos.clear();

                    List<String> especialidades =
                            new ArrayList<>();

                    for (DocumentSnapshot documento
                            : resultado.getDocuments()) {

                        String nome =
                                documento.getString("nome");

                        String especialidade =
                                documento.getString("especialidade");

                        if (nome == null
                                || especialidade == null
                                || nome.trim().isEmpty()
                                || especialidade.trim().isEmpty()) {

                            Log.w(
                                    "CONSULTAS_MEDICOS",
                                    "Documento ignorado: "
                                            + documento.getId()
                            );

                            continue;
                        }

                        Medico medico = new Medico(
                                documento.getId(),
                                nome.trim(),
                                especialidade.trim()
                        );

                        medicosPublicos.add(medico);

                        if (!especialidades.contains(
                                medico.especialidade
                        )) {
                            especialidades.add(
                                    medico.especialidade
                            );
                        }
                    }

                    Collections.sort(
                            medicosPublicos,
                            (a, b) -> a.nome.compareToIgnoreCase(b.nome)
                    );

                    Collections.sort(
                            especialidades,
                            String.CASE_INSENSITIVE_ORDER
                    );

                    List<String> opcoesEspecialidades =
                            new ArrayList<>();

                    opcoesEspecialidades.add(
                            TEXTO_SELECIONE_ESPECIALIDADE
                    );

                    opcoesEspecialidades.addAll(especialidades);

                    ArrayAdapter<String> adapter =
                            new ArrayAdapter<>(
                                    this,
                                    android.R.layout.simple_spinner_item,
                                    opcoesEspecialidades
                            );

                    adapter.setDropDownViewResource(
                            android.R.layout.simple_spinner_dropdown_item
                    );

                    spinnerEspecialidades.setAdapter(adapter);

                    atualizarListaMedicos(
                            TEXTO_SELECIONE_ESPECIALIDADE
                    );

                    if (medicosPublicos.isEmpty()) {

                        Toast.makeText(
                                this,
                                "Nenhum médico está publicado no momento.",
                                Toast.LENGTH_LONG
                        ).show();

                    } else {

                        Log.d(
                                "CONSULTAS_MEDICOS",
                                "Médicos carregados: "
                                        + medicosPublicos.size()
                        );
                    }
                })
                .addOnFailureListener(erro -> {

                    Log.e(
                            "CONSULTAS_MEDICOS",
                            "Erro ao carregar médicos públicos",
                            erro
                    );

                    Toast.makeText(
                            this,
                            "Não foi possível carregar os médicos. "
                                    + "Verifique as permissões do Firebase.",
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    // =====================================================
    // ATUALIZAR MÉDICOS CONFORME A ESPECIALIDADE
    // =====================================================

    private void atualizarListaMedicos(String especialidade) {

        List<Medico> opcoes = new ArrayList<>();

        opcoes.add(
                new Medico(
                        null,
                        TEXTO_SELECIONE_MEDICO,
                        null
                )
        );

        if (!TEXTO_SELECIONE_ESPECIALIDADE.equals(especialidade)) {

            for (Medico medico : medicosPublicos) {

                if (medico.especialidade.equalsIgnoreCase(
                        especialidade
                )) {
                    opcoes.add(medico);
                }
            }
        }

        ArrayAdapter<Medico> adapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_item,
                        opcoes
                );

        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spinnerMedicos.setAdapter(adapter);

        horaF = "";

        if (grid != null) {
            grid.removeAllViews();
        }
    }

    // =====================================================
    // BOTÃO VOLTAR
    // =====================================================

    private void configurarBotaoVoltar() {

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
    }

    // =====================================================
    // SELEÇÃO DE DATA
    // =====================================================

    private void configurarSelecaoData() {

        findViewById(R.id.btnSelecionarData)
                .setOnClickListener(v -> {

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
    }

    // =====================================================
    // SELEÇÃO DE ESPECIALIDADE
    // =====================================================

    private void configurarSelecaoEspecialidade() {

        spinnerEspecialidades.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {

                    @Override
                    public void onItemSelected(
                            AdapterView<?> parent,
                            android.view.View view,
                            int position,
                            long id
                    ) {

                        String especialidade =
                                parent.getItemAtPosition(position).toString();

                        atualizarListaMedicos(especialidade);
                    }

                    @Override
                    public void onNothingSelected(
                            AdapterView<?> parent
                    ) {

                        atualizarListaMedicos(
                                TEXTO_SELECIONE_ESPECIALIDADE
                        );
                    }
                }
        );
    }

    // =====================================================
    // SELEÇÃO DE MÉDICO
    // =====================================================

    private void configurarSelecaoMedico() {

        spinnerMedicos.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {

                    @Override
                    public void onItemSelected(
                            AdapterView<?> parent,
                            android.view.View view,
                            int position,
                            long id
                    ) {

                        horaF = "";

                        Medico medico = obterMedicoSelecionado();

                        if (medico != null
                                && !dataF.isEmpty()) {

                            carregarHorariosDisponiveis();

                        } else if (grid != null) {

                            grid.removeAllViews();
                        }
                    }

                    @Override
                    public void onNothingSelected(
                            AdapterView<?> parent
                    ) {

                        horaF = "";

                        if (grid != null) {
                            grid.removeAllViews();
                        }
                    }
                }
        );
    }

    private Medico obterMedicoSelecionado() {

        Object selecionado = spinnerMedicos.getSelectedItem();

        if (selecionado instanceof Medico) {

            Medico medico = (Medico) selecionado;

            if (medico.id != null) {
                return medico;
            }
        }

        return null;
    }

    // =====================================================
    // BOTÃO CONFIRMAR
    // =====================================================

    private void configurarBotaoConfirmar() {

        findViewById(R.id.btnConfirmarAgendamento)
                .setOnClickListener(
                        v -> abrirRevisaoAgendamento()
                );
    }

    // =====================================================
    // CONSULTAR HORÁRIOS DISPONÍVEIS
    // =====================================================

    private void carregarHorariosDisponiveis() {

        if (grid == null) {
            return;
        }

        Medico medico = obterMedicoSelecionado();

        if (dataF.isEmpty() || medico == null) {

            grid.removeAllViews();
            horaF = "";

            return;
        }

        final int requisicaoAtual = ++requisicaoHorarios;

        grid.removeAllViews();
        horaF = "";

        String[] horas = {
                "08:00", "09:00", "10:00", "11:00",
                "13:00", "14:00", "15:00", "16:00", "17:00"
        };

        db.collection("horarios_ocupados")
                .whereEqualTo("medicoId", medico.id)
                .whereEqualTo("data", dataF)
                .whereEqualTo("ocupado", true)
                .get()
                .addOnSuccessListener(snapshots -> {

                    if (requisicaoAtual != requisicaoHorarios) {
                        return;
                    }

                    List<String> ocupados = new ArrayList<>();

                    for (DocumentSnapshot doc : snapshots) {

                        String horario = doc.getString("horario");

                        if (horario != null) {
                            ocupados.add(horario);
                        }
                    }

                    exibirHorarios(horas, ocupados);
                })
                .addOnFailureListener(e -> {

                    if (requisicaoAtual != requisicaoHorarios) {
                        return;
                    }

                    Log.e(
                            "CONSULTAS_HORARIOS",
                            "Erro ao consultar horários ocupados",
                            e
                    );

                    grid.removeAllViews();
                    horaF = "";

                    Toast.makeText(
                            this,
                            "Não foi possível carregar os horários. "
                                    + "Verifique a conexão e as permissões do Firebase.",
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    // =====================================================
    // EXIBIR HORÁRIOS
    // =====================================================

    private void exibirHorarios(
            String[] horas,
            List<String> ocupados
    ) {

        grid.removeAllViews();

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
    }

    // =====================================================
    // RESETAR CORES DOS HORÁRIOS
    // =====================================================

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

    // =====================================================
    // ABRIR REVISÃO DO AGENDAMENTO
    // =====================================================

    private void abrirRevisaoAgendamento() {

        Medico medico = obterMedicoSelecionado();

        String especialidade =
                spinnerEspecialidades.getSelectedItem() == null
                        ? ""
                        : spinnerEspecialidades.getSelectedItem().toString();

        if (medico == null
                || especialidade.isEmpty()
                || TEXTO_SELECIONE_ESPECIALIDADE.equals(especialidade)
                || !medico.especialidade.equalsIgnoreCase(especialidade)
                || dataF.isEmpty()
                || horaF.isEmpty()) {

            Toast.makeText(
                    this,
                    "Selecione especialidade, médico, data e horário.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {

            Toast.makeText(
                    this,
                    "Faça login para agendar uma consulta.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        String dataChave = dataF.replace("/", "-");

        String horarioChave = horaF.replace(":", "-");

        String horarioId = medico.id
                + "_"
                + dataChave
                + "_"
                + horarioChave;

        Intent intent = new Intent(
                this,
                RevisaoAgendamentoActivity.class
        );

        intent.putExtra("medico", medico.nome);
        intent.putExtra("medicoId", medico.id);
        intent.putExtra("especialidade", especialidade);
        intent.putExtra("data", dataF);
        intent.putExtra("horario", horaF);

        intent.putExtra("dataChave", dataChave);
        intent.putExtra("horarioChave", horarioChave);
        intent.putExtra("horarioId", horarioId);

        startActivity(intent);
    }
}

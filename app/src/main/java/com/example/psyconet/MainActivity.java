package com.example.psyconet;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Filter;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointForward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;

// Imports do Firebase Firestore
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class MainActivity extends AppCompatActivity {

    // ---------- Views ----------
    private View root;
    private View header;
    private View bottomBar;
    private ImageButton btnVoltar;
    private TextInputLayout tilMedico;
    private AutoCompleteTextView medicoDropdown;
    private MaterialButton btnEscolherData;
    private TextView tvDataSelecionada;
    private TextView tvHorariosVazio;
    private ChipGroup chipGroupHorarios;
    private MaterialButton btnConfirmar;

    // Instância do Firebase Firestore
    private FirebaseFirestore db;

    // ---------- Estado ----------
    private String dataSelecionada = null;

    // ---------- Dados de exemplo ----------
    private final List<String> listaMedicos = new ArrayList<>();

    private final List<String> horariosDisponiveis = Arrays.asList(
            "08:00", "09:00", "10:00", "11:00", "14:00", "15:00", "16:00", "17:00");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this, SystemBarStyle.dark(Color.TRANSPARENT));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inicializa o Firestore
        db = FirebaseFirestore.getInstance();

        carregarDadosDeExemplo();
        vincularViews();
        ajustarInsets();
        configurarMedicos();
        configurarAcoes();
    }

    private void carregarDadosDeExemplo() {
        listaMedicos.addAll(Arrays.asList(
                "Dra. Helena Duarte",
                "Dr. Marcos Vieira",
                "Dr. Rafael Nogueira",
                "Dra. Beatriz Amaral",
                "Dra. Camila Prado",
                "Dr. Henrique Sales",
                "Dra. Sofia Martins",
                "Dr. André Lacerda"
        ));
    }

    private void vincularViews() {
        root = findViewById(R.id.main);
        header = findViewById(R.id.header);
        bottomBar = findViewById(R.id.bottomBar);
        btnVoltar = findViewById(R.id.btnVoltar);
        tilMedico = findViewById(R.id.tilMedico);
        medicoDropdown = findViewById(R.id.spinnerMedico);
        btnEscolherData = findViewById(R.id.btnEscolherData);
        tvDataSelecionada = findViewById(R.id.tvDataSelecionada);
        tvHorariosVazio = findViewById(R.id.tvHorariosVazio);
        chipGroupHorarios = findViewById(R.id.chipGroupHorarios);
        btnConfirmar = findViewById(R.id.btnConfirmar);
    }

    private void ajustarInsets() {
        final int topBase = header.getPaddingTop();
        ViewCompat.setOnApplyWindowInsetsListener(header, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), topBase + bars.top,
                    v.getPaddingRight(), v.getPaddingBottom());
            return insets;
        });

        final int bottomBase = bottomBar.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(bottomBar, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(),
                    v.getPaddingRight(), bottomBase + bars.bottom);
            return insets;
        });
    }

    private void configurarMedicos() {
        medicoDropdown.setAdapter(new DropdownAdapter(this, listaMedicos));
        tilMedico.setEnabled(true);
    }

    private void configurarAcoes() {
        btnVoltar.setOnClickListener(v -> finish());
        btnEscolherData.setOnClickListener(v -> abrirCalendario());
        btnConfirmar.setOnClickListener(v -> confirmarAgendamento());
    }

    private void abrirCalendario() {
        CalendarConstraints restricoes = new CalendarConstraints.Builder()
                .setValidator(DateValidatorPointForward.now())
                .build();

        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(R.string.escolher_data_titulo)
                .setCalendarConstraints(restricoes)
                .build();

        picker.addOnPositiveButtonClickListener(millisUtc -> {
            Locale ptBr = Locale.forLanguageTag("pt-BR");
            SimpleDateFormat formato =
                    new SimpleDateFormat("EEEE, dd 'de' MMMM 'de' yyyy", ptBr);
            formato.setTimeZone(TimeZone.getTimeZone("UTC"));

            String texto = formato.format(millisUtc);
            texto = texto.substring(0, 1).toUpperCase(ptBr) + texto.substring(1);

            dataSelecionada = texto;
            tvDataSelecionada.setText(texto);
            tvDataSelecionada.setTextColor(ContextCompat.getColor(this, R.color.brand_700));
            montarHorarios();
        });
        picker.show(getSupportFragmentManager(), "date_picker");
    }

    private void montarHorarios() {
        chipGroupHorarios.removeAllViews();
        float dp = getResources().getDisplayMetrics().density;

        int corMarca = ContextCompat.getColor(this, R.color.brand_700);
        int corSuperficie = ContextCompat.getColor(this, R.color.surface);
        int corBorda = ContextCompat.getColor(this, R.color.stroke);
        int corTexto = ContextCompat.getColor(this, R.color.text_primary);
        int corTextoSobreMarca = ContextCompat.getColor(this, R.color.text_on_brand);

        for (String hora : horariosDisponiveis) {
            Chip chip = new Chip(this);
            chip.setId(View.generateViewId());
            chip.setText(hora);
            chip.setCheckable(true);
            chip.setCheckedIconVisible(false);
            chip.setChipBackgroundColor(seletor(corMarca, corSuperficie));
            chip.setTextColor(seletor(corTextoSobreMarca, corTexto));
            chip.setChipStrokeColor(seletor(corMarca, corBorda));
            chip.setChipStrokeWidth(dp);
            chip.setChipCornerRadius(12f * dp);

            chipGroupHorarios.addView(chip);
        }
        tvHorariosVazio.setVisibility(View.GONE);
    }

    private ColorStateList seletor(int corSelecionado, int corNormal) {
        return new ColorStateList(
                new int[][]{
                        new int[]{android.R.attr.state_checked},
                        new int[]{}
                },
                new int[]{corSelecionado, corNormal});
    }

    // ---------- Confirmação e Integração com Firebase ----------

    private void confirmarAgendamento() {
        String medico = medicoDropdown.getText().toString();
        String data = dataSelecionada;

        String horario;
        int chipId = chipGroupHorarios.getCheckedChipId();
        if (chipId != View.NO_ID) {
            Chip chip = chipGroupHorarios.findViewById(chipId);
            if (chip != null) {
                horario = chip.getText().toString();
            } else {
                horario = null;
            }
        } else {
            horario = null;
        }

        int erro = 0;
        if (medico.trim().isEmpty()) {
            erro = R.string.erro_medico;
        } else if (data == null) {
            erro = R.string.erro_data;
        } else if (horario == null) {
            erro = R.string.erro_horario;
        }

        if (erro != 0) {
            Snackbar.make(root, erro, Snackbar.LENGTH_SHORT)
                    .setAnchorView(bottomBar)
                    .show();
            return;
        }

        // Criando o objeto com os dados da consulta para o Firebase
        Map<String, Object> consulta = new HashMap<>();
        consulta.put("psicologo", medico);
        consulta.put("data", data + " às " + horario);
        consulta.put("emailUsuario", "usuario@email.com"); // Substitua pelo e-mail real se tiver autenticação

        // Salvando na coleção "agendamentos" do Firestore
        db.collection("agendamentos")
                .add(consulta)
                .addOnSuccessListener(documentReference -> {
                    // Exibe o diálogo de sucesso e ao clicar em OK abre a tela de próximas consultas
                    new MaterialAlertDialogBuilder(this)
                            .setTitle(R.string.sucesso_titulo)
                            .setMessage(getString(R.string.sucesso_mensagem, medico, data, horario))
                            .setPositiveButton(R.string.ok, (dialog, which) -> {
                                limparFormulario();
                                // Abre a tela de Próximas Consultas
                                Intent intent = new Intent(MainActivity.this, UpcomingAppointmentsActivity.class);
                                startActivity(intent);
                            })
                            .setCancelable(false)
                            .show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Erro ao salvar agendamento: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void limparFormulario() {
        medicoDropdown.setText("", false);
        dataSelecionada = null;
        tvDataSelecionada.setText(R.string.nenhuma_data);
        tvDataSelecionada.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        chipGroupHorarios.removeAllViews();
        tvHorariosVazio.setVisibility(View.VISIBLE);
    }

    private static class DropdownAdapter extends ArrayAdapter<String> {
        private final List<String> itens;

        DropdownAdapter(Context context, List<String> itens) {
            super(context, android.R.layout.simple_list_item_1, itens);
            this.itens = itens;
        }

        @Override
        public Filter getFilter() {
            return new Filter() {
                @Override
                protected FilterResults performFiltering(CharSequence constraint) {
                    FilterResults results = new FilterResults();
                    results.values = itens;
                    results.count = itens.size();
                    return results;
                }

                @Override
                protected void publishResults(CharSequence constraint, FilterResults results) {
                    notifyDataSetChanged();
                }
            };
        }
    }
}
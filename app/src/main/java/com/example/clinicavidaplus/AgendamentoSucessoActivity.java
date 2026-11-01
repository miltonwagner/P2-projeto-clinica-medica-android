package com.example.clinicavidaplus;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

public class AgendamentoSucessoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_agendamento_sucesso);

        String medico = getIntent().getStringExtra("medico");
        String especialidade = getIntent().getStringExtra("especialidade");
        String data = getIntent().getStringExtra("data");
        String horario = getIntent().getStringExtra("horario");

        ((TextView) findViewById(R.id.txtSucessoResumo)).setText(
                "Especialidade: " + texto(especialidade) +
                        "\nMédico: " + texto(medico) +
                        "\nData: " + texto(data) +
                        "\nHorário: " + texto(horario)
        );

        // Botão: Ver Minhas Consultas
        findViewById(R.id.btnVerMinhasConsultas).setOnClickListener(v ->
                NavegacaoInferior.irParaInicio(this));

        // Botão: Novo Agendamento
        findViewById(R.id.btnNovoAgendamentoSucesso).setOnClickListener(v -> {
            Intent intent = new Intent(
                    AgendamentoSucessoActivity.this,
                    ConsultasActivity.class
            );
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            startActivity(intent);
            finish();
        });

        // Botão: Voltar
        // Voltar (botão ou gesto) leva à lista de consultas, não ao formulário.
        findViewById(R.id.btnVoltarSucesso).setOnClickListener(v ->
                NavegacaoInferior.irParaInicio(this));

        getOnBackPressedDispatcher().addCallback(this,
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        NavegacaoInferior.irParaInicio(AgendamentoSucessoActivity.this);
                    }
                });
    }

    private String texto(String valor) {
        return valor == null || valor.trim().isEmpty()
                ? "Não informado"
                : valor;
    }
}

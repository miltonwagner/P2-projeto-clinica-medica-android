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
                NavegacaoInferior.abrirAba(this, ListaConsultasActivity.class));

        // Botão: Novo Agendamento
        findViewById(R.id.btnNovoAgendamentoSucesso).setOnClickListener(v ->
                NavegacaoInferior.irParaInicio(this));

        // Botão: Voltar
        // Voltar (botão ou gesto) leva às consultas, não ao formulário.
        findViewById(R.id.btnVoltarSucesso).setOnClickListener(v ->
                NavegacaoInferior.abrirAba(this, ListaConsultasActivity.class));

        getOnBackPressedDispatcher().addCallback(this,
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        NavegacaoInferior.abrirAba(
                                AgendamentoSucessoActivity.this,
                                ListaConsultasActivity.class
                        );
                    }
                });
    }

    private String texto(String valor) {
        return valor == null || valor.trim().isEmpty()
                ? "Não informado"
                : valor;
    }
}

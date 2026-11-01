package com.example.clinicavidaplus;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

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
        String agendamentoId = getIntent().getStringExtra("agendamentoId");

        ((TextView) findViewById(R.id.txtSucessoResumo)).setText(
                "Especialidade: " + texto(especialidade) +
                        "\nMédico: " + texto(medico) +
                        "\nData: " + texto(data) +
                        "\nHorário: " + texto(horario)
        );

        // Botão: Ver Minhas Consultas
        findViewById(R.id.btnVerMinhasConsultas).setOnClickListener(v -> {
            Intent intent = new Intent(
                    AgendamentoSucessoActivity.this,
                    ListaConsultasActivity.class
            );
            intent.putExtra("agendamentoId", agendamentoId);
            startActivity(intent);
            finish();
        });

        // Botão: Novo Agendamento
        findViewById(R.id.btnNovoAgendamentoSucesso).setOnClickListener(v -> {
            Intent intent = new Intent(
                    AgendamentoSucessoActivity.this,
                    ConsultasActivity.class
            );
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        });

        // Botão: Voltar
        findViewById(R.id.btnVoltarSucesso).setOnClickListener(v -> {
            Intent intent = new Intent(
                    AgendamentoSucessoActivity.this,
                    ListaConsultasActivity.class
            );
            intent.putExtra("agendamentoId", agendamentoId);
            startActivity(intent);
            finish();
        });
    }

    private String texto(String valor) {
        return valor == null || valor.trim().isEmpty()
                ? "Não informado"
                : valor;
    }
}

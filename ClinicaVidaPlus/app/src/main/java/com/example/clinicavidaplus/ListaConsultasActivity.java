package com.example.clinicavidaplus;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.FirebaseFirestore;

public class ListaConsultasActivity extends AppCompatActivity {

    private TextView txtMed, txtEsp, txtData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lista_consultas);

        txtMed  = findViewById(R.id.txtResumoMedico);
        txtEsp  = findViewById(R.id.txtResumoEspecialidade);
        txtData = findViewById(R.id.txtResumoDataHora);
        Button btnVoltar = findViewById(R.id.btnVoltarLista);

        // Voltar tela inicial
        btnVoltar.setOnClickListener(v -> irParaInicio());

        // Pega o ID do agendamento passado pelo ConsultasActivity
        String agendamentoId = getIntent().getStringExtra("agendamentoId");

        if (agendamentoId != null) {
            FirebaseFirestore.getInstance()
                    .collection("agendamentos")
                    .document(agendamentoId)
                    .get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            String medico = doc.getString("medico");
                            String espec  = doc.getString("especialidade");
                            String data   = doc.getString("data");
                            String hora   = doc.getString("horario");

                            txtMed.setText("Médico: "        + (medico != null ? medico : "Não informado"));
                            txtEsp.setText("Especialidade: " + (espec  != null ? espec  : "Não informado"));
                            txtData.setText("Data: "         + data    + " às " + (hora != null ? hora : "---"));
                        }
                    });
        } else {
            txtMed.setText("Erro ao carregar agendamento");
        }
    }

    // Ao apertar Voltar do celular também vai para a tela inicial
    @Override
    public void onBackPressed() {
        irParaInicio();
    }

    private void irParaInicio() {
        Intent intent = new Intent(this, IndexActivity.class);
        // Abre o Index como raiz
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}

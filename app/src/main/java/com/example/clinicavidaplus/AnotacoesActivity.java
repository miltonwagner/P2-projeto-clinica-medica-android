package com.example.clinicavidaplus;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Área do médico: pacientes atendidos, com acesso ao prontuário de cada um. */
public class AnotacoesActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private LinearLayout container;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lista_simples);
        NavegacaoInferior.configurar(this, R.id.nav_anotacoes);

        ((TextView) findViewById(R.id.txtTituloListaSimples)).setText("Pacientes");
        ((TextView) findViewById(R.id.txtSubtituloListaSimples))
                .setText("Prontuário e anotações de cada paciente");

        db = FirebaseFirestore.getInstance();
        container = findViewById(R.id.containerListaSimples);

        FirebaseUser usuario = FirebaseAuth.getInstance().getCurrentUser();
        if (usuario == null) {
            Sessao.sair(this);
            return;
        }

        carregar(usuario.getUid());
    }

    private void carregar(String uidMedico) {
        db.collection("agendamentos")
                .whereEqualTo("medicoId", uidMedico)
                .get()
                .addOnSuccessListener(resultado -> {
                    List<Consulta> consultas = new ArrayList<>();
                    for (DocumentSnapshot documento : resultado.getDocuments()) {
                        Consulta consulta = new Consulta(documento);
                        if (!consulta.pacienteId.isEmpty() && !consulta.encerradaSemAtendimento()) {
                            consultas.add(consulta);
                        }
                    }

                    // Mais recentes primeiro; um card por paciente.
                    consultas.sort((a, b) -> Long.compare(b.momento, a.momento));
                    Map<String, List<Consulta>> porPaciente = new LinkedHashMap<>();
                    for (Consulta consulta : consultas) {
                        List<Consulta> lista = porPaciente.get(consulta.pacienteId);
                        if (lista == null) {
                            lista = new ArrayList<>();
                            porPaciente.put(consulta.pacienteId, lista);
                        }
                        lista.add(consulta);
                    }

                    mostrar(porPaciente);
                })
                .addOnFailureListener(e -> Toast.makeText(this,
                        "Erro ao carregar seus pacientes.", Toast.LENGTH_LONG).show());
    }

    private void mostrar(Map<String, List<Consulta>> porPaciente) {
        container.removeAllViews();

        if (porPaciente.isEmpty()) {
            LinearLayout card = Tela.card(this);
            card.addView(Tela.texto(this,
                    "Seus pacientes aparecerão aqui após o primeiro agendamento.",
                    R.style.Texto_Subtitulo, 0));
            container.addView(card);
            return;
        }

        for (Map.Entry<String, List<Consulta>> item : porPaciente.entrySet()) {
            container.addView(cardPaciente(item.getKey(), item.getValue()));
        }
    }

    private View cardPaciente(String pacienteId, List<Consulta> consultas) {
        LinearLayout card = Tela.card(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);

        TextView avatar = Tela.avatar(this, "P", 48);
        card.addView(avatar);

        LinearLayout info = Tela.coluna(this, 14);
        TextView nome = Tela.texto(this, "Carregando...", R.style.Texto_CardTitulo, 0);
        info.addView(nome);
        int total = consultas.size();
        info.addView(Tela.texto(this,
                (total == 1 ? "1 consulta" : total + " consultas")
                        + " · última: " + consultas.get(0).data,
                R.style.Texto_Pequeno, 2));
        card.addView(info);

        TextView abrir = Tela.compacto(Tela.texto(this, "Abrir", R.style.Texto_Link, 0));
        abrir.setTextColor(ContextCompat.getColor(this, R.color.primaria));
        card.addView(abrir);

        final String[] nomePaciente = {"Paciente"};
        db.collection("pacientes")
                .document(pacienteId)
                .get()
                .addOnSuccessListener(documento -> {
                    String valor = documento.getString("nome");
                    if (valor != null && !valor.trim().isEmpty()) {
                        nomePaciente[0] = valor.trim();
                    }
                    nome.setText(nomePaciente[0]);
                    avatar.setText(nomePaciente[0].substring(0, 1).toUpperCase());
                })
                .addOnFailureListener(e -> nome.setText(nomePaciente[0]));

        card.setOnClickListener(v -> {
            Intent intent = new Intent(this, ProntuarioActivity.class);
            intent.putExtra(ProntuarioActivity.EXTRA_PACIENTE_ID, pacienteId);
            intent.putExtra(ProntuarioActivity.EXTRA_PACIENTE_NOME, nomePaciente[0]);
            startActivity(intent);
        });

        return card;
    }
}

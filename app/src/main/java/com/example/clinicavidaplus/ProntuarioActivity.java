package com.example.clinicavidaplus;

import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Prontuário de um paciente: anotações de sessão feitas pelo médico. */
public class ProntuarioActivity extends AppCompatActivity {

    public static final String EXTRA_PACIENTE_ID = "prontuario_paciente_id";
    public static final String EXTRA_PACIENTE_NOME = "prontuario_paciente_nome";

    private FirebaseFirestore db;
    private LinearLayout container;
    private String uidMedico;
    private String pacienteId;
    private String pacienteNome;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lista_simples);
        NavegacaoInferior.configurar(this, R.id.nav_anotacoes);

        FirebaseUser usuario = FirebaseAuth.getInstance().getCurrentUser();
        pacienteId = getIntent().getStringExtra(EXTRA_PACIENTE_ID);
        pacienteNome = getIntent().getStringExtra(EXTRA_PACIENTE_NOME);

        if (usuario == null || pacienteId == null || pacienteId.isEmpty()) {
            finish();
            return;
        }

        if (pacienteNome == null || pacienteNome.trim().isEmpty()) {
            pacienteNome = "Paciente";
        }

        uidMedico = usuario.getUid();
        db = FirebaseFirestore.getInstance();
        container = findViewById(R.id.containerListaSimples);

        View voltar = findViewById(R.id.btnVoltarListaSimples);
        voltar.setVisibility(View.VISIBLE);
        voltar.setOnClickListener(v -> finish());

        ((TextView) findViewById(R.id.txtTituloListaSimples)).setText(pacienteNome);
        ((TextView) findViewById(R.id.txtSubtituloListaSimples)).setText("Prontuário · anotações de sessão");

        TextView acao = findViewById(R.id.btnAcaoListaSimples);
        acao.setText("+ Nova anotação");
        acao.setVisibility(View.VISIBLE);
        acao.setOnClickListener(v -> novaAnotacao());

        carregarResumo();
        carregarAnotacoes();
    }

    private void carregarResumo() {
        db.collection("agendamentos")
                .whereEqualTo("medicoId", uidMedico)
                .whereEqualTo("usuarioId", pacienteId)
                .get()
                .addOnSuccessListener(resultado -> {
                    int concluidas = 0;
                    String especialidade = "";
                    for (DocumentSnapshot documento : resultado.getDocuments()) {
                        Consulta consulta = new Consulta(documento);
                        if (consulta.concluida()) {
                            concluidas++;
                        }
                        especialidade = consulta.especialidade;
                    }

                    LinearLayout resumo = findViewById(R.id.containerResumoListaSimples);
                    resumo.removeAllViews();
                    LinearLayout card = Tela.card(this);
                    LinearLayout linha = Tela.linha(this);
                    linha.addView(item(String.valueOf(resultado.size()), "Consultas"));
                    linha.addView(item(String.valueOf(concluidas), "Concluídas"));
                    linha.addView(item(especialidade.isEmpty() ? "-" : especialidade, "Especialidade"));
                    card.addView(linha);
                    resumo.addView(card);
                });
    }

    private View item(String valor, String rotulo) {
        LinearLayout coluna = Tela.coluna(this, 0);
        coluna.setGravity(android.view.Gravity.CENTER_HORIZONTAL);
        TextView txtValor = Tela.compacto(Tela.texto(this, valor, R.style.Texto_CardTitulo, 0));
        TextView txtRotulo = Tela.compacto(Tela.texto(this, rotulo, R.style.Texto_Pequeno, 2));
        coluna.addView(txtValor);
        coluna.addView(txtRotulo);
        return coluna;
    }

    private void carregarAnotacoes() {
        db.collection("anotacoes")
                .whereEqualTo("medicoId", uidMedico)
                .whereEqualTo("pacienteId", pacienteId)
                .get()
                .addOnSuccessListener(resultado -> {
                    List<DocumentSnapshot> anotacoes = new ArrayList<>(resultado.getDocuments());
                    anotacoes.sort((a, b) -> Long.compare(momento(b), momento(a)));
                    mostrar(anotacoes);
                })
                .addOnFailureListener(e -> Toast.makeText(this,
                        "Não foi possível carregar o prontuário.", Toast.LENGTH_LONG).show());
    }

    private static long momento(DocumentSnapshot documento) {
        Timestamp criadoEm = documento.getTimestamp("criadoEm");
        return criadoEm == null ? Long.MAX_VALUE : criadoEm.toDate().getTime();
    }

    private void mostrar(List<DocumentSnapshot> anotacoes) {
        container.removeAllViews();

        TextView titulo = Tela.texto(this, "Anotações de sessão", R.style.Texto_Secao, 8);
        container.addView(titulo);
        ((LinearLayout.LayoutParams) titulo.getLayoutParams()).bottomMargin = Tela.dp(this, 12);

        if (anotacoes.isEmpty()) {
            LinearLayout card = Tela.card(this);
            card.addView(Tela.texto(this,
                    "Nenhuma anotação ainda. Registre a evolução do paciente após cada consulta.",
                    R.style.Texto_Subtitulo, 0));
            container.addView(card);
            return;
        }

        SimpleDateFormat formato = new SimpleDateFormat("dd MMM yyyy · HH:mm", new Locale("pt", "BR"));

        for (DocumentSnapshot documento : anotacoes) {
            LinearLayout card = Tela.card(this);
            Timestamp criadoEm = documento.getTimestamp("criadoEm");
            String data = criadoEm == null ? "Agora" : formato.format(criadoEm.toDate()).replace(".", "");

            card.addView(Tela.textoComIcone(this, "Sessão · " + data, R.drawable.ic_relogio, 0));

            String tag = documento.getString("tag");
            if (tag != null && !tag.trim().isEmpty()) {
                TextView etiqueta = Tela.etiqueta(this, tag.trim());
                ((LinearLayout.LayoutParams) etiqueta.getLayoutParams()).topMargin = Tela.dp(this, 10);
                card.addView(etiqueta);
            }

            String texto = documento.getString("texto");
            TextView corpo = Tela.texto(this, texto == null ? "" : texto, R.style.Texto_Corpo, 10);
            corpo.setLineSpacing(Tela.dp(this, 3), 1f);
            card.addView(corpo);
            container.addView(card);
        }
    }

    private void novaAnotacao() {
        LinearLayout formulario = new LinearLayout(this);
        formulario.setOrientation(LinearLayout.VERTICAL);
        int espaco = Tela.dp(this, 20);
        formulario.setPadding(espaco, Tela.dp(this, 8), espaco, 0);

        EditText edtTag = new EditText(this);
        edtTag.setHint("Diagnóstico ou tema (ex.: Hipertensão)");
        edtTag.setSingleLine(true);
        formulario.addView(edtTag);

        EditText edtTexto = new EditText(this);
        edtTexto.setHint("Evolução, conduta, prescrição...");
        edtTexto.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        edtTexto.setMinLines(4);
        edtTexto.setGravity(android.view.Gravity.TOP);
        formulario.addView(edtTexto);

        AlertDialog dialogo = new AlertDialog.Builder(this)
                .setTitle("Nova anotação")
                .setView(formulario)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Salvar", null)
                .create();

        dialogo.setOnShowListener(d -> dialogo.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    String texto = edtTexto.getText().toString().trim();
                    if (texto.isEmpty()) {
                        edtTexto.setError("Escreva a anotação");
                        return;
                    }
                    salvar(edtTag.getText().toString().trim(), texto);
                    dialogo.dismiss();
                }));

        dialogo.show();
    }

    private void salvar(String tag, String texto) {
        Map<String, Object> anotacao = new HashMap<>();
        anotacao.put("medicoId", uidMedico);
        anotacao.put("pacienteId", pacienteId);
        anotacao.put("pacienteNome", pacienteNome);
        anotacao.put("tag", tag);
        anotacao.put("texto", texto);
        anotacao.put("criadoEm", FieldValue.serverTimestamp());

        db.collection("anotacoes")
                .add(anotacao)
                .addOnSuccessListener(r -> {
                    Toast.makeText(this, "Anotação salva.", Toast.LENGTH_SHORT).show();
                    carregarAnotacoes();
                })
                .addOnFailureListener(e -> Toast.makeText(this,
                        "Não foi possível salvar a anotação.", Toast.LENGTH_LONG).show());
    }
}

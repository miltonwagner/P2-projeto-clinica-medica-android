package com.example.clinicavidaplus;

import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class AdminMedicosActivity extends AppCompatActivity {

    private static final String UID_ADMIN =
            "0cjzlxWzOZM0DI6HNpTWqgENdA93";

    private LinearLayout listaMedicos;
    private FirebaseFirestore db;
    private TextView mensagem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FirebaseUser usuario =
                FirebaseAuth.getInstance().getCurrentUser();

        if (usuario == null
                || !UID_ADMIN.equals(usuario.getUid())) {
            Toast.makeText(
                    this,
                    "Acesso restrito ao administrador.",
                    Toast.LENGTH_LONG
            ).show();
            Sessao.sair(this);
            return;
        }

        setContentView(R.layout.activity_admin_medicos);
        NavegacaoInferior.configurar(this, R.id.nav_medicos);

        db = FirebaseFirestore.getInstance();
        listaMedicos = findViewById(R.id.listaMedicos);
        mensagem = findViewById(R.id.txtMensagemAdmin);

        findViewById(R.id.btnAtualizarMedicos)
                .setOnClickListener(v -> carregarMedicos());

        carregarMedicos();
    }

    private void carregarMedicos() {
        mensagem.setText("Carregando médicos...");
        listaMedicos.removeAllViews();

        db.collection("medicos")
                .get()
                .addOnSuccessListener(resultado -> {
                    listaMedicos.removeAllViews();

                    if (resultado.isEmpty()) {
                        mensagem.setText("Nenhum médico cadastrado.");
                        return;
                    }

                    mensagem.setText("Médicos cadastrados: " + resultado.size());

                    for (DocumentSnapshot documento : resultado.getDocuments()) {
                        adicionarMedico(documento);
                    }
                })
                .addOnFailureListener(erro -> {
                    mensagem.setText("Não foi possível carregar os médicos.");
                    Toast.makeText(
                            this,
                            "Verifique as regras do Firestore.",
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private void adicionarMedico(DocumentSnapshot documento) {
        String nome = documento.getString("nome");
        String email = documento.getString("email");
        boolean autorizado = Boolean.TRUE.equals(documento.getBoolean("autorizado"));

        if (nome == null || nome.trim().isEmpty()) {
            nome = "Médico sem nome";
        }

        if (email == null || email.trim().isEmpty()) {
            email = "E-mail não informado";
        }

        LinearLayout cartao = Tela.card(this);

        cartao.addView(Tela.etiqueta(
                this,
                autorizado ? "Autorizado" : "Aguardando autorização"
        ));
        cartao.addView(Tela.texto(this, nome, R.style.Texto_CardTitulo, 10));
        cartao.addView(Tela.texto(this, email, R.style.Texto_Subtitulo, 2));

        Button btnAlterar = new Button(this);
        btnAlterar.setText(autorizado ? "Bloquear acesso" : "Autorizar médico");
        btnAlterar.setAllCaps(false);
        btnAlterar.setTextSize(14);
        btnAlterar.setStateListAnimator(null);
        btnAlterar.setBackgroundResource(
                autorizado ? R.drawable.bg_botao_perigo : R.drawable.bg_botao_primario
        );
        btnAlterar.setTextColor(getColor(autorizado ? R.color.perigo : R.color.white));

        LinearLayout.LayoutParams margemBotao = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                Tela.dp(this, 44)
        );
        margemBotao.topMargin = Tela.dp(this, 14);
        cartao.addView(btnAlterar, margemBotao);

        btnAlterar.setOnClickListener(v -> {
            btnAlterar.setEnabled(false);
            boolean novoEstado = !autorizado;

            db.collection("medicos")
                    .document(documento.getId())
                    .update("autorizado", novoEstado)
                    .addOnSuccessListener(resultado -> {
                        Toast.makeText(
                                this,
                                novoEstado
                                        ? "Médico autorizado."
                                        : "Acesso do médico bloqueado.",
                                Toast.LENGTH_SHORT
                        ).show();
                        carregarMedicos();
                    })
                    .addOnFailureListener(erro -> {
                        btnAlterar.setEnabled(true);
                        Toast.makeText(
                                this,
                                "Não foi possível alterar a autorização. "
                                        + "Confira as regras do Firebase.",
                                Toast.LENGTH_LONG
                        ).show();
                    });
        });

        listaMedicos.addView(cartao);
    }
}

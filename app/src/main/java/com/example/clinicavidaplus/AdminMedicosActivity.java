
package com.example.clinicavidaplus;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
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
            finish();
            return;
        }

        db = FirebaseFirestore.getInstance();

        LinearLayout raiz = new LinearLayout(this);
        raiz.setOrientation(LinearLayout.VERTICAL);
        raiz.setPadding(dp(20), dp(20), dp(20), dp(20));
        raiz.setBackgroundColor(Color.rgb(242, 248, 244));

        TextView titulo = criarTexto(
                "Administração de médicos",
                24,
                "#174D35",
                true
        );
        raiz.addView(titulo);

        TextView descricao = criarTexto(
                "Autorize ou bloqueie o acesso à área médica.",
                15,
                "#526657",
                false
        );

        LinearLayout.LayoutParams margemDescricao =
                new LinearLayout.LayoutParams(
                        -1, -2
                );
        margemDescricao.topMargin = dp(8);
        raiz.addView(descricao, margemDescricao);

        mensagem = criarTexto(
                "Carregando médicos...",
                15,
                "#526657",
                false
        );

        LinearLayout.LayoutParams margemMensagem =
                new LinearLayout.LayoutParams(-1, -2);
        margemMensagem.topMargin = dp(18);
        raiz.addView(mensagem, margemMensagem);

        ScrollView rolagem = new ScrollView(this);
        listaMedicos = new LinearLayout(this);
        listaMedicos.setOrientation(LinearLayout.VERTICAL);

        LinearLayout.LayoutParams margemLista =
                new LinearLayout.LayoutParams(-1, -2);
        margemLista.topMargin = dp(12);

        rolagem.addView(listaMedicos);
        raiz.addView(rolagem, new LinearLayout.LayoutParams(
                -1, 0, 1
        ));

        Button btnAtualizar = new Button(this);
        btnAtualizar.setText("Atualizar lista");
        btnAtualizar.setOnClickListener(v -> carregarMedicos());
        raiz.addView(btnAtualizar);

        Button btnSair = new Button(this);
        btnSair.setText("Sair da administração");
        btnSair.setOnClickListener(v -> finish());

        LinearLayout.LayoutParams margemSair =
                new LinearLayout.LayoutParams(-1, -2);
        margemSair.topMargin = dp(8);
        raiz.addView(btnSair, margemSair);

        setContentView(raiz);

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
                        mensagem.setText(
                                "Nenhum médico cadastrado."
                        );
                        return;
                    }

                    mensagem.setText(
                            "Médicos cadastrados: "
                                    + resultado.size()
                    );

                    for (DocumentSnapshot documento
                            : resultado.getDocuments()) {
                        adicionarMedico(documento);
                    }
                })
                .addOnFailureListener(erro -> {
                    mensagem.setText(
                            "Não foi possível carregar os médicos."
                    );

                    Toast.makeText(
                            this,
                            "Verifique as regras do Firestore.",
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private void adicionarMedico(
            DocumentSnapshot documento) {

        String nome = documento.getString("nome");
        String email = documento.getString("email");
        Boolean autorizadoCampo =
                documento.getBoolean("autorizado");

        boolean autorizado =
                Boolean.TRUE.equals(autorizadoCampo);

        if (nome == null || nome.trim().isEmpty()) {
            nome = "Médico sem nome";
        }

        if (email == null || email.trim().isEmpty()) {
            email = "E-mail não informado";
        }

        LinearLayout cartao = new LinearLayout(this);
        cartao.setOrientation(LinearLayout.VERTICAL);
        cartao.setPadding(dp(16), dp(16), dp(16), dp(16));
        cartao.setBackgroundColor(Color.WHITE);

        LinearLayout.LayoutParams parametrosCartao =
                new LinearLayout.LayoutParams(-1, -2);
        parametrosCartao.bottomMargin = dp(12);
        cartao.setLayoutParams(parametrosCartao);

        TextView textoNome = criarTexto(
                nome,
                18,
                "#174D35",
                true
        );
        cartao.addView(textoNome);

        TextView textoEmail = criarTexto(
                email,
                14,
                "#526657",
                false
        );

        LinearLayout.LayoutParams margemEmail =
                new LinearLayout.LayoutParams(-1, -2);
        margemEmail.topMargin = dp(6);
        cartao.addView(textoEmail, margemEmail);

        TextView textoStatus = criarTexto(
                autorizado
                        ? "Status: Autorizado"
                        : "Status: Aguardando autorização",
                15,
                autorizado ? "#246B45" : "#996515",
                true
        );

        LinearLayout.LayoutParams margemStatus =
                new LinearLayout.LayoutParams(-1, -2);
        margemStatus.topMargin = dp(10);
        cartao.addView(textoStatus, margemStatus);

        Button btnAlterar = new Button(this);
        btnAlterar.setText(
                autorizado ? "Bloquear acesso" : "Autorizar médico"
        );

        LinearLayout.LayoutParams margemBotao =
                new LinearLayout.LayoutParams(-1, -2);
        margemBotao.topMargin = dp(8);
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

    private TextView criarTexto(
            String texto,
            int tamanho,
            String cor,
            boolean negrito) {

        TextView view = new TextView(this);
        view.setText(texto);
        view.setTextSize(tamanho);
        view.setTextColor(Color.parseColor(cor));

        if (negrito) {
            view.setTypeface(null, Typeface.BOLD);
        }

        return view;
    }

    private int dp(int valor) {
        return (int) (
                valor * getResources()
                        .getDisplayMetrics().density
        );
    }
}

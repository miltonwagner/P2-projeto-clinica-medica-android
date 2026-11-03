
package com.example.clinicavidaplus;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
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
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.util.HashMap;
import java.util.Map;

public class AdminMedicosActivity extends AppCompatActivity {

    private static final String UID_ADMIN =
            "0cjzlxWzOZM0DI6HNpTWqgENdA93";

    private static final String COLECAO_PRIVADA = "medicos";
    private static final String COLECAO_PUBLICA = "medicos_publicos";

    private static final String[] CAMPOS_PUBLICOS = {
            "nome",
            "especialidade",
            "bio",
            "avaliacao",
            "avaliacoes",
            "valor",
            "atendimento",
            "online",
            "fotoUrl"
    };

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
                new LinearLayout.LayoutParams(-1, -2);

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

        rolagem.addView(listaMedicos);

        raiz.addView(
                rolagem,
                new LinearLayout.LayoutParams(-1, 0, 1)
        );

        Button btnSincronizar = new Button(this);
        btnSincronizar.setText("Sincronizar médicos autorizados");

        btnSincronizar.setOnClickListener(v -> {
            btnSincronizar.setEnabled(false);
            sincronizarMedicosAutorizados(btnSincronizar);
        });

        LinearLayout.LayoutParams margemSincronizar =
                new LinearLayout.LayoutParams(-1, -2);

        margemSincronizar.topMargin = dp(8);
        raiz.addView(btnSincronizar, margemSincronizar);

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

    // =====================================================
    // CARREGAMENTO DOS MÉDICOS PRIVADOS
    // =====================================================

    private void carregarMedicos() {

        mensagem.setText("Carregando médicos...");
        listaMedicos.removeAllViews();

        db.collection(COLECAO_PRIVADA)
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
                            "Erro ao consultar médicos: "
                                    + erro.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    // =====================================================
    // EXIBIÇÃO DOS MÉDICOS NO PAINEL
    // =====================================================

    private void adicionarMedico(DocumentSnapshot documento) {

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
                autorizado
                        ? "Bloquear acesso"
                        : "Autorizar médico"
        );

        LinearLayout.LayoutParams margemBotao =
                new LinearLayout.LayoutParams(-1, -2);

        margemBotao.topMargin = dp(8);
        cartao.addView(btnAlterar, margemBotao);

        btnAlterar.setOnClickListener(v -> {

            btnAlterar.setEnabled(false);

            boolean novoEstado = !autorizado;

            alterarAutorizacao(
                    documento,
                    novoEstado,
                    btnAlterar
            );
        });

        listaMedicos.addView(cartao);
    }

    // =====================================================
    // SINCRONIZAÇÃO INICIAL DOS MÉDICOS AUTORIZADOS
    // =====================================================

    private void sincronizarMedicosAutorizados(
            Button btnSincronizar) {

        mensagem.setText(
                "Verificando médicos autorizados..."
        );

        db.collection(COLECAO_PRIVADA)
                .get()
                .addOnSuccessListener(resultado -> {

                    WriteBatch lote = db.batch();

                    int quantidadeSincronizada = 0;

                    for (DocumentSnapshot documento
                            : resultado.getDocuments()) {

                        Boolean autorizadoCampo =
                                documento.getBoolean("autorizado");

                        boolean autorizado =
                                Boolean.TRUE.equals(autorizadoCampo);

                        // Ignora médicos que não estão autorizados.
                        if (!autorizado) {
                            continue;
                        }

                        Map<String, Object> dadosPublicos =
                                new HashMap<>();

                        // Copia somente os campos permitidos.
                        for (String campo : CAMPOS_PUBLICOS) {

                            Object valor = documento.get(campo);

                            if (valor != null) {
                                dadosPublicos.put(campo, valor);
                            }
                        }

                        Object nome = dadosPublicos.get("nome");

                        Object especialidade =
                                dadosPublicos.get("especialidade");

                        // Valida os campos obrigatórios.
                        if (!(nome instanceof String)
                                || ((String) nome).trim().isEmpty()
                                || !(especialidade instanceof String)
                                || ((String) especialidade).trim().isEmpty()) {

                            btnSincronizar.setEnabled(true);

                            mensagem.setText(
                                    "Sincronização interrompida."
                            );

                            Toast.makeText(
                                    this,
                                    "O médico "
                                            + documento.getId()
                                            + " não possui nome ou especialidade válidos.",
                                    Toast.LENGTH_LONG
                            ).show();

                            return;
                        }

                        // Usa o mesmo UID do documento privado.
                        // Se já existir, atualiza sem duplicar.
                        lote.set(
                                db.collection(COLECAO_PUBLICA)
                                        .document(documento.getId()),
                                dadosPublicos,
                                SetOptions.merge()
                        );

                        quantidadeSincronizada++;
                    }

                    if (quantidadeSincronizada == 0) {

                        btnSincronizar.setEnabled(true);

                        mensagem.setText(
                                "Nenhum médico autorizado encontrado."
                        );

                        Toast.makeText(
                                this,
                                "Não há médicos autorizados para sincronizar.",
                                Toast.LENGTH_LONG
                        ).show();

                        return;
                    }

                    final int total = quantidadeSincronizada;

                    lote.commit()
                            .addOnSuccessListener(resultadoCommit -> {

                                mensagem.setText(
                                        "Médicos públicos sincronizados: "
                                                + total
                                );

                                Toast.makeText(
                                        this,
                                        total
                                                + " médico(s) sincronizado(s) com sucesso.",
                                        Toast.LENGTH_LONG
                                ).show();

                                btnSincronizar.setEnabled(true);

                                carregarMedicos();
                            })
                            .addOnFailureListener(erro -> {

                                btnSincronizar.setEnabled(true);

                                mensagem.setText(
                                        "Falha na sincronização."
                                );

                                Toast.makeText(
                                        this,
                                        "Erro ao publicar médicos: "
                                                + erro.getMessage(),
                                        Toast.LENGTH_LONG
                                ).show();
                            });
                })
                .addOnFailureListener(erro -> {

                    btnSincronizar.setEnabled(true);

                    mensagem.setText(
                            "Não foi possível consultar os médicos."
                    );

                    Toast.makeText(
                            this,
                            "Erro ao consultar médicos: "
                                    + erro.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    // =====================================================
    // ALTERAÇÃO DA AUTORIZAÇÃO
    // =====================================================

    private void alterarAutorizacao(
            DocumentSnapshot documento,
            boolean novoEstado,
            Button btnAlterar) {

        String medicoId = documento.getId();

        Map<String, Object> dadosPublicos =
                new HashMap<>();

        if (novoEstado) {

            for (String campo : CAMPOS_PUBLICOS) {

                Object valor = documento.get(campo);

                if (valor != null) {
                    dadosPublicos.put(campo, valor);
                }
            }

            Object nome = dadosPublicos.get("nome");

            Object especialidade =
                    dadosPublicos.get("especialidade");

            if (!(nome instanceof String)
                    || ((String) nome).trim().isEmpty()
                    || !(especialidade instanceof String)
                    || ((String) especialidade).trim().isEmpty()) {

                btnAlterar.setEnabled(true);

                Toast.makeText(
                        this,
                        "O médico precisa ter nome e especialidade cadastrados.",
                        Toast.LENGTH_LONG
                ).show();

                return;
            }
        }

        WriteBatch lote = db.batch();

        lote.update(
                db.collection(COLECAO_PRIVADA)
                        .document(medicoId),
                "autorizado",
                novoEstado
        );

        if (novoEstado) {

            lote.set(
                    db.collection(COLECAO_PUBLICA)
                            .document(medicoId),
                    dadosPublicos,
                    SetOptions.merge()
            );

        } else {

            lote.delete(
                    db.collection(COLECAO_PUBLICA)
                            .document(medicoId)
            );
        }

        lote.commit()
                .addOnSuccessListener(resultado -> {

                    Toast.makeText(
                            this,
                            novoEstado
                                    ? "Médico autorizado e publicado."
                                    : "Acesso bloqueado e perfil retirado da publicação.",
                            Toast.LENGTH_LONG
                    ).show();

                    carregarMedicos();
                })
                .addOnFailureListener(erro -> {

                    btnAlterar.setEnabled(true);

                    Toast.makeText(
                            this,
                            "Não foi possível concluir a operação: "
                                    + erro.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    // =====================================================
    // COMPONENTES VISUAIS
    // =====================================================

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

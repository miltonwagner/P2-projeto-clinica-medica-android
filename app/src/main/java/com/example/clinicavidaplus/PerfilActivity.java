package com.example.clinicavidaplus;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

/** Dados do usuário logado e logout, comum aos três perfis. */
public class PerfilActivity extends AppCompatActivity {

    private TextView txtAvatar;
    private TextView txtNome;
    private LinearLayout containerInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_perfil);

        FirebaseUser usuario = FirebaseAuth.getInstance().getCurrentUser();

        if (usuario == null) {
            Sessao.sair(this);
            return;
        }

        NavegacaoInferior.configurar(this, R.id.nav_perfil);

        txtAvatar = findViewById(R.id.txtAvatar);
        txtNome = findViewById(R.id.txtPerfilNome);
        containerInfo = findViewById(R.id.containerPerfilInfo);
        TextView txtEmail = findViewById(R.id.txtPerfilEmail);
        TextView txtTipo = findViewById(R.id.txtPerfilTipo);

        txtEmail.setText(usuario.getEmail());
        mostrarNome(usuario.getDisplayName());

        String perfil = Sessao.perfil(this);

        if (Sessao.PERFIL_ADMIN.equals(perfil)) {
            txtTipo.setText("Administrador");
        } else if (Sessao.PERFIL_MEDICO.equals(perfil)) {
            txtTipo.setText("Médico");
            carregarDados("medicos", usuario.getUid());
        } else {
            txtTipo.setText("Paciente");
            carregarDados("pacientes", usuario.getUid());
        }

        findViewById(R.id.btnVerClinica).setOnClickListener(v ->
                startActivity(new android.content.Intent(this, EnderecoActivity.class)));
        findViewById(R.id.btnSairConta).setOnClickListener(v -> Sessao.sair(this));
    }

    private void carregarDados(String colecao, String uid) {
        boolean paciente = "pacientes".equals(colecao);
        if (paciente) {
            // 1º o banco local (ROOM): aparece na hora, mesmo sem internet.
            PacienteRepository.buscar(this, uid, local -> {
                if (local != null && containerInfo.getChildCount() == 0) {
                    mostrarNome(local.getNome());
                    adicionarInfo("Telefone", local.getTelefone());
                    adicionarInfo("Data de nascimento", local.getDataNascimento());
                }
            });
        }

        // Depois o Firestore (fonte oficial), que atualiza a cópia local.
        FirebaseFirestore.getInstance()
                .collection(colecao)
                .document(uid)
                .get()
                .addOnSuccessListener(documento -> {
                    if (paciente) {
                        PacienteRepository.salvarDoFirestore(this, documento);
                    }
                    preencher(documento);
                })
                .addOnFailureListener(e -> {
                    if (paciente && containerInfo.getChildCount() > 0) {
                        adicionarInfo("Modo offline", "Dados salvos no aparelho (ROOM)");
                    }
                });
    }

    private void preencher(DocumentSnapshot documento) {
        if (!documento.exists()) {
            return;
        }
        containerInfo.removeAllViews();

        String nome = documento.getString("nome");
        if (nome != null && !nome.trim().isEmpty()) {
            mostrarNome(nome);
        }

        adicionarInfo("Telefone", documento.getString("telefone"));
        adicionarInfo("Data de nascimento", documento.getString("nascimento"));
        adicionarInfo("Especialidade", documento.getString("especialidade"));
        adicionarInfo("CRM", documento.getString("crm"));
    }

    private void mostrarNome(String nome) {
        String valor = nome == null || nome.trim().isEmpty() ? "Usuário" : nome.trim();
        txtNome.setText(valor);
        txtAvatar.setText(valor.substring(0, 1).toUpperCase());
    }

    private void adicionarInfo(String rotulo, String valor) {
        if (valor == null || valor.trim().isEmpty()) {
            return;
        }

        boolean primeiro = containerInfo.getChildCount() == 0;
        containerInfo.setVisibility(View.VISIBLE);
        containerInfo.addView(Tela.texto(this, rotulo, R.style.Texto_Pequeno, primeiro ? 0 : 14));
        containerInfo.addView(Tela.texto(this, valor, R.style.Texto_CardTitulo, 2));
    }
}

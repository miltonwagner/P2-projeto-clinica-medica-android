package com.example.clinicavidaplus;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

/**
 * Tela de entrada do app.
 * Visitante vai direto ao marketplace de médicos (o login só é pedido ao
 * agendar). Logado, vai para a tela inicial do perfil.
 */
public class IndexActivity extends AppCompatActivity {

    private View botoes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_index);

        findViewById(R.id.btnIrLogin).setOnClickListener(v ->
                startActivity(new Intent(this, LoginActivity.class)));

        findViewById(R.id.btnIrCadastro).setOnClickListener(v ->
                startActivity(new Intent(this, MainActivity.class)));

        findViewById(R.id.btnVerEndereco).setOnClickListener(v ->
                startActivity(new Intent(this, EnderecoActivity.class)));

        botoes = findViewById(R.id.btnIrLogin);

        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            habilitarBotoes(false);
            Sessao.encaminhar(this, () -> habilitarBotoes(true));
        } else {
            startActivity(new Intent(this, MarketplaceActivity.class));
            finish();
        }
    }

    private void habilitarBotoes(boolean habilitado) {
        botoes.setEnabled(habilitado);
        findViewById(R.id.btnIrCadastro).setEnabled(habilitado);
    }
}

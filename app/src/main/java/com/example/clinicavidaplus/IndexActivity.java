package com.example.clinicavidaplus;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;

public class IndexActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_index);

        // 1. Configuração do Botão Sair
        Button btnSair = findViewById(R.id.btnSairApp);
        btnSair.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            finish(); // Fecha o app ou volta para a tela de autenticação
        });

        // 2. Botão Login
        findViewById(R.id.btnIrLogin).setOnClickListener(v ->
                startActivity(new Intent(this, LoginActivity.class)));

        // 3. Botão Cadastro
        findViewById(R.id.btnIrCadastro).setOnClickListener(v ->
                startActivity(new Intent(this, MainActivity.class)));

        // 4. Botão Endereço
        findViewById(R.id.btnVerEndereco).setOnClickListener(v ->
                startActivity(new Intent(this, EnderecoActivity.class)));
    }
}

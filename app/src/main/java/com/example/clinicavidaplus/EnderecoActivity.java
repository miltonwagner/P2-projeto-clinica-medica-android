package com.example.clinicavidaplus;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class EnderecoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_endereco);

        // Botão para voltar
        NavegacaoInferior.configurar(this, R.id.nav_clinica);

        // Com o menu inferior visível (usuário logado), o "Voltar" do topo é dispensável.
        View btnVoltar = findViewById(R.id.btnVoltarEnd);
        View nav = findViewById(R.id.navInferior);
        btnVoltar.setVisibility(nav.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
        btnVoltar.setOnClickListener(v -> finish());

        // Botão que abre o Google Maps com o endereço de Indaiatuba
        Button btnMaps = findViewById(R.id.btnAbrirMaps);
        btnMaps.setOnClickListener(v -> {
            String endereco = "Rua Bernardino de Campos, 933, Centro, Indaiatuba, SP";
            Uri gmmIntentUri = Uri.parse("geo:0,0?q=" + Uri.encode(endereco));

            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
            mapIntent.setPackage("com.google.android.apps.maps");

            // Verifica se o usuário tem o Maps instalado
            if (mapIntent.resolveActivity(getPackageManager()) != null) {
                startActivity(mapIntent);
            } else {
                // Caso não tenha o Maps, abre no navegador
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=" + Uri.encode(endereco))));
            }
        });
    }
}

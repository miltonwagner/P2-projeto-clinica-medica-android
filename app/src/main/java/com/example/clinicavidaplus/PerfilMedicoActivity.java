package com.example.clinicavidaplus;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

/** Perfil público do médico, aberto a partir do marketplace. */
public class PerfilMedicoActivity extends AppCompatActivity {

    public static final String EXTRA_MEDICO = "medico_marketplace";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_perfil_medico);
        NavegacaoInferior.configurar(this, R.id.nav_inicio);

        Medico medico = (Medico) getIntent().getSerializableExtra(EXTRA_MEDICO);

        if (medico == null) {
            finish();
            return;
        }

        ((TextView) findViewById(R.id.txtAvatarMedico)).setText(medico.inicial());
        ((TextView) findViewById(R.id.txtNomeMedico)).setText(medico.nome);
        ((TextView) findViewById(R.id.txtCrmMedico)).setText(
                medico.crm.isEmpty() ? medico.especialidade : medico.crm + " · " + medico.especialidade);
        ((TextView) findViewById(R.id.txtAvaliacaoMedico)).setText(
                medico.avaliacoes == 0
                        ? medico.avaliacaoTexto()
                        : medico.avaliacaoTexto().replace("(", "· ").replace(")", " avaliações"));
        ((TextView) findViewById(R.id.txtEspecialidadeTag)).setText(
                medico.online ? medico.especialidade + "  ·  ● Atendendo agora" : medico.especialidade);
        ((TextView) findViewById(R.id.txtBioMedico)).setText(medico.bio);

        LinearLayout planos = findViewById(R.id.containerPlanosMedico);
        planos.addView(plano("Consulta presencial", medico.valorFormatado(),
                "por consulta · 30 min", true));
        planos.addView(plano("Retorno", "Grátis",
                "em até 30 dias após a consulta", false));

        findViewById(R.id.btnVoltarPerfilMedico).setOnClickListener(v -> finish());

        findViewById(R.id.btnAgendarComMedico).setOnClickListener(v -> {
            Intent intent = new Intent(this, ConsultasActivity.class);
            intent.putExtra(ConsultasActivity.EXTRA_MEDICO_NOME, medico.nome);
            startActivity(intent);
        });
    }

    private LinearLayout plano(String titulo, String valor, String detalhe, boolean destaque) {
        LinearLayout card = Tela.card(this);
        card.setBackgroundResource(destaque ? R.drawable.bg_botao_primario : R.drawable.bg_card);
        card.setPadding(Tela.dp(this, 18), Tela.dp(this, 14), Tela.dp(this, 18), Tela.dp(this, 14));

        int corTexto = ContextCompat.getColor(this, destaque ? R.color.white : R.color.texto_escuro);
        int corSecundaria = ContextCompat.getColor(this, destaque ? R.color.primaria_clara : R.color.texto_cinza);

        TextView txtTitulo = Tela.texto(this, titulo, R.style.Texto_Pequeno, 0);
        txtTitulo.setTextColor(corSecundaria);
        card.addView(txtTitulo);

        TextView txtValor = Tela.texto(this, valor, R.style.Texto_Titulo, 2);
        txtValor.setTextColor(corTexto);
        card.addView(txtValor);

        TextView txtDetalhe = Tela.texto(this, detalhe, R.style.Texto_Pequeno, 2);
        txtDetalhe.setTextColor(corSecundaria);
        card.addView(txtDetalhe);

        card.setGravity(Gravity.START);
        return card;
    }
}

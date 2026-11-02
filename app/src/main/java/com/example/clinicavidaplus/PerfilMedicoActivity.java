package com.example.clinicavidaplus;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

/** Perfil público do médico, aberto a partir do marketplace. */
public class PerfilMedicoActivity extends AppCompatActivity {

    private com.google.firebase.firestore.ListenerRegistration ouvinte;

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
        mostrarDisponibilidade(medico);
        ouvinte = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("medicos").document(medico.id)
                .addSnapshotListener((documento, erro) -> {
                    if (documento != null && documento.exists()) {
                        Medico atual = Medico.de(documento);
                        medico.online = atual.online;
                        medico.atendimento = atual.atendimento;
                        mostrarDisponibilidade(medico);
                    }
                });
        ((TextView) findViewById(R.id.txtBioMedico)).setText(medico.bio);

        LinearLayout planos = findViewById(R.id.containerPlanosMedico);
        planos.addView(plano("Consulta presencial", medico.valorFormatado(),
                "por consulta · 30 min", true));
        planos.addView(plano("Retorno", "Grátis",
                "em até 30 dias após a consulta", false));

        findViewById(R.id.btnVoltarPerfilMedico).setOnClickListener(v -> finish());

        findViewById(R.id.btnFilaAoVivo).setOnClickListener(v -> {
            if (com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() == null) {
                Sessao.guardarAgendamentoPendente(this, Sessao.PREFIXO_FILA + medico.id);
                android.widget.Toast.makeText(this,
                        "Entre ou crie sua conta para entrar na fila.",
                        android.widget.Toast.LENGTH_LONG).show();
                startActivity(new Intent(this, LoginActivity.class));
                return;
            }

            Intent intent = new Intent(this, FilaActivity.class);
            intent.putExtra(FilaActivity.EXTRA_MEDICO, medico);
            startActivity(intent);
        });

        findViewById(R.id.btnAgendarComMedico).setOnClickListener(v -> {
            if (com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() == null) {
                // Visitante: entra ou se cadastra e volta direto para o agendamento.
                Sessao.guardarAgendamentoPendente(this, medico.id);
                android.widget.Toast.makeText(this,
                        "Entre ou crie sua conta para concluir o agendamento.",
                        android.widget.Toast.LENGTH_LONG).show();
                startActivity(new Intent(this, LoginActivity.class));
                return;
            }

            Intent intent = new Intent(this, AgendarActivity.class);
            intent.putExtra(AgendarActivity.EXTRA_MEDICO, medico);
            startActivity(intent);
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (ouvinte != null) {
            ouvinte.remove();
        }
    }

    private void mostrarDisponibilidade(Medico medico) {
        boolean aoVivo = medico.aoVivoAgora();
        ((TextView) findViewById(R.id.txtEspecialidadeTag)).setText(
                aoVivo ? medico.especialidade + "  ·  ● Ao vivo agora" : medico.especialidade);

        String texto;
        if (aoVivo && medico.aceitaAgendamento()) {
            texto = "Atendendo ao vivo agora · também aceita agendamento";
        } else if (aoVivo) {
            texto = "Atende só ao vivo, por ordem de chegada";
        } else if (medico.aceitaAoVivo() && medico.aceitaAgendamento()) {
            texto = "Fila ao vivo fechada no momento · agende um horário";
        } else if (medico.aceitaAoVivo()) {
            texto = "Atende só ao vivo · fila fechada no momento";
        } else {
            texto = "Atendimento somente com horário marcado";
        }
        ((TextView) findViewById(R.id.txtDisponibilidadeMedico)).setText(texto);

        findViewById(R.id.btnFilaAoVivo).setVisibility(aoVivo ? View.VISIBLE : View.GONE);
        findViewById(R.id.btnAgendarComMedico).setVisibility(
                medico.aceitaAgendamento() ? View.VISIBLE : View.GONE);
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

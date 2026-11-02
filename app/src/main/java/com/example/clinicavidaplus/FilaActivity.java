package com.example.clinicavidaplus;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Sala de espera do paciente na fila ao vivo de um médico: mostra a
 * posição em tempo real e avisa quando chega a vez dele.
 */
public class FilaActivity extends AppCompatActivity {

    public static final String EXTRA_MEDICO = "fila_medico";
    public static final String EXTRA_MEDICO_ID = "fila_medico_id";

    private FirebaseFirestore db;
    private String uid;
    private Medico medico;
    private EntradaFila minhaEntrada;

    private ListenerRegistration ouvinteEntrada;
    private ListenerRegistration ouvinteFila;

    private LinearLayout resumo;
    private LinearLayout container;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lista_simples);
        NavegacaoInferior.configurar(this, R.id.nav_inicio);

        FirebaseUser usuario = FirebaseAuth.getInstance().getCurrentUser();
        if (usuario == null) {
            finish();
            return;
        }

        uid = usuario.getUid();
        db = FirebaseFirestore.getInstance();
        resumo = findViewById(R.id.containerResumoListaSimples);
        container = findViewById(R.id.containerListaSimples);

        View voltar = findViewById(R.id.btnVoltarListaSimples);
        voltar.setVisibility(View.VISIBLE);
        voltar.setOnClickListener(v -> finish());
        ((TextView) findViewById(R.id.txtTituloListaSimples)).setText("Fila ao vivo");
        ((TextView) findViewById(R.id.txtSubtituloListaSimples)).setText("Carregando...");

        Medico recebido = (Medico) getIntent().getSerializableExtra(EXTRA_MEDICO);
        String medicoId = recebido != null ? recebido.id : getIntent().getStringExtra(EXTRA_MEDICO_ID);
        procurarEntradaAtiva(medicoId);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (ouvinteEntrada != null) {
            ouvinteEntrada.remove();
        }
        if (ouvinteFila != null) {
            ouvinteFila.remove();
        }
    }

    /** O paciente fica em uma fila por vez: retoma a que já estiver ativa. */
    private void procurarEntradaAtiva(String medicoId) {
        db.collection(EntradaFila.COLECAO)
                .whereEqualTo("pacienteId", uid)
                .whereIn("status", Arrays.asList(EntradaFila.AGUARDANDO, EntradaFila.EM_ATENDIMENTO))
                .get()
                .addOnSuccessListener(resultado -> {
                    if (!resultado.isEmpty()) {
                        EntradaFila ativa = new EntradaFila(resultado.getDocuments().get(0));
                        if (medicoId != null && !medicoId.equals(ativa.medicoId)) {
                            Toast.makeText(this, "Você já está na fila de " + ativa.medico
                                    + ". Saia dela para entrar em outra.", Toast.LENGTH_LONG).show();
                        }
                        carregarMedico(ativa.medicoId, false);
                        return;
                    }

                    if (medicoId == null) {
                        finish();
                        return;
                    }
                    carregarMedico(medicoId, true);
                })
                .addOnFailureListener(e -> erro());
    }

    private void carregarMedico(String medicoId, boolean entrar) {
        db.collection("medicos").document(medicoId).get()
                .addOnSuccessListener(documento -> {
                    medico = Medico.de(documento);
                    ((TextView) findViewById(R.id.txtSubtituloListaSimples))
                            .setText(medico.nome + " · " + medico.especialidade);

                    if (!entrar) {
                        ouvirFila();
                    } else if (medico.aoVivoAgora()) {
                        entrarNaFila();
                    } else {
                        mostrarFechada();
                    }
                })
                .addOnFailureListener(e -> erro());
    }

    private void entrarNaFila() {
        Map<String, Object> entrada = new HashMap<>();
        entrada.put("medicoId", medico.id);
        entrada.put("medico", medico.nome);
        entrada.put("especialidade", medico.especialidade);
        entrada.put("pacienteId", uid);
        entrada.put("status", EntradaFila.AGUARDANDO);
        entrada.put("entrouEm", FieldValue.serverTimestamp());

        db.collection(EntradaFila.COLECAO).add(entrada)
                .addOnSuccessListener(r -> ouvirFila())
                .addOnFailureListener(e -> erro());
    }

    private void ouvirFila() {
        ouvinteEntrada = db.collection(EntradaFila.COLECAO)
                .whereEqualTo("pacienteId", uid)
                .whereEqualTo("medicoId", medico.id)
                .addSnapshotListener((resultado, erro) -> {
                    if (resultado == null) {
                        return;
                    }
                    EntradaFila maisRecente = null;
                    for (DocumentSnapshot documento : resultado.getDocuments()) {
                        EntradaFila entrada = new EntradaFila(documento);
                        if (maisRecente == null || entrada.ativa()
                                || (!maisRecente.ativa() && entrada.entrouEm > maisRecente.entrouEm)) {
                            maisRecente = entrada;
                            if (entrada.ativa()) {
                                break;
                            }
                        }
                    }
                    minhaEntrada = maisRecente;
                    atualizar(-1);
                });

        ouvinteFila = db.collection(EntradaFila.COLECAO)
                .whereEqualTo("medicoId", medico.id)
                .whereEqualTo("status", EntradaFila.AGUARDANDO)
                .addSnapshotListener((resultado, erro) -> {
                    if (resultado == null || minhaEntrada == null) {
                        return;
                    }
                    int aFrente = 0;
                    for (DocumentSnapshot documento : resultado.getDocuments()) {
                        EntradaFila outra = new EntradaFila(documento);
                        if (!outra.id.equals(minhaEntrada.id) && outra.entrouEm < minhaEntrada.entrouEm) {
                            aFrente++;
                        }
                    }
                    atualizar(aFrente);
                });
    }

    private int ultimaPosicao = 0;

    private void atualizar(int aFrente) {
        if (aFrente >= 0) {
            ultimaPosicao = aFrente + 1;
        }
        if (minhaEntrada == null) {
            return;
        }

        resumo.removeAllViews();
        container.removeAllViews();

        if (minhaEntrada.aguardando()) {
            int posicao = Math.max(ultimaPosicao, 1);
            resumo.addView(destaque(String.valueOf(posicao) + "º",
                    posicao == 1 ? "Você é o próximo!" : "Sua posição na fila",
                    posicao == 1
                            ? "Fique por perto: o médico vai te chamar em instantes."
                            : "Tempo estimado: ~" + ((posicao - 1) * EntradaFila.MINUTOS_POR_PACIENTE)
                            + " min · atualiza sozinho"));
            container.addView(info("Pode sair desta tela: sua vaga continua guardada. "
                    + "Volte pelo aviso no início para acompanhar."));
            container.addView(botaoSair());
        } else if (minhaEntrada.emAtendimento()) {
            resumo.addView(destaque("●", "É a sua vez!",
                    medico.nome + " iniciou o seu atendimento agora."));
            Button video = Tela.botao(this, "Entrar na videochamada", true);
            video.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_video, 0, 0, 0);
            video.setOnClickListener(v -> Videochamada.entrar(this, minhaEntrada.id, nomePaciente()));
            container.addView(video);
            container.addView(info("Permita câmera e microfone quando o app pedir."));
        } else if (EntradaFila.ATENDIDO.equals(minhaEntrada.status)) {
            resumo.addView(destaque("✓", "Atendimento finalizado",
                    "Obrigado por usar a Vida Plus."));
            container.addView(botaoInicio());
        } else {
            resumo.addView(destaque("–", "Você saiu da fila",
                    "Entre de novo pelo perfil do médico quando quiser."));
            container.addView(botaoInicio());
        }
    }

    private String nomePaciente() {
        FirebaseUser usuario = FirebaseAuth.getInstance().getCurrentUser();
        if (usuario != null && usuario.getDisplayName() != null && !usuario.getDisplayName().isEmpty()) {
            return usuario.getDisplayName();
        }
        return "Paciente";
    }

    private void mostrarFechada() {
        resumo.removeAllViews();
        container.removeAllViews();
        resumo.addView(destaque("–", "Fila ao vivo fechada",
                medico.nome + " não está atendendo ao vivo agora."));

        if (medico.aceitaAgendamento()) {
            Button agendar = Tela.botao(this, "Agendar um horário", true);
            agendar.setOnClickListener(v -> {
                Intent intent = new Intent(this, AgendarActivity.class);
                intent.putExtra(AgendarActivity.EXTRA_MEDICO, medico);
                startActivity(intent);
                finish();
            });
            container.addView(agendar);
        } else {
            container.addView(botaoInicio());
        }
    }

    private View destaque(String numero, String titulo, String detalhe) {
        LinearLayout card = Tela.card(this);
        card.setGravity(Gravity.CENTER_HORIZONTAL);

        TextView circulo = Tela.avatar(this, numero, 96);
        circulo.setTextSize(34);
        ((LinearLayout.LayoutParams) circulo.getLayoutParams()).gravity = Gravity.CENTER_HORIZONTAL;
        card.addView(circulo);

        TextView txtTitulo = Tela.texto(this, titulo, R.style.Texto_Titulo, 14);
        txtTitulo.setGravity(Gravity.CENTER);
        card.addView(txtTitulo);

        TextView txtDetalhe = Tela.texto(this, detalhe, R.style.Texto_Subtitulo, 6);
        txtDetalhe.setGravity(Gravity.CENTER);
        card.addView(txtDetalhe);
        return card;
    }

    private View info(String mensagem) {
        LinearLayout card = Tela.card(this);
        TextView texto = Tela.texto(this, mensagem, R.style.Texto_Corpo, 0);
        card.addView(texto);
        return card;
    }

    private View botaoSair() {
        Button sair = Tela.botao(this, "Sair da fila", false);
        sair.setOnClickListener(v -> {
            sair.setEnabled(false);
            db.collection(EntradaFila.COLECAO).document(minhaEntrada.id)
                    .update("status", EntradaFila.CANCELADO)
                    .addOnFailureListener(e -> {
                        sair.setEnabled(true);
                        erro();
                    });
        });
        sair.setTextColor(ContextCompat.getColor(this, R.color.perigo));
        sair.setTypeface(null, Typeface.BOLD);
        return sair;
    }

    private View botaoInicio() {
        Button inicio = Tela.botao(this, "Voltar ao início", true);
        inicio.setOnClickListener(v -> NavegacaoInferior.irParaInicio(this));
        return inicio;
    }

    private void erro() {
        Toast.makeText(this, "Não foi possível acessar a fila. Verifique a conexão.",
                Toast.LENGTH_LONG).show();
    }
}

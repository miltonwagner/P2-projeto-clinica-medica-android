package com.example.clinicavidaplus;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.EditText;
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

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Agendar consulta com o médico escolhido no marketplace: modo de
 * atendimento, dia no calendário, horário livre e dados do paciente.
 */
public class AgendarActivity extends AppCompatActivity {

    public static final String EXTRA_MEDICO = "agendar_medico";
    public static final String EXTRA_MEDICO_ID = "agendar_medico_id";

    private static final String[] HORARIOS = {
            "09:00", "10:00", "11:00",
            "14:00", "15:00", "16:00",
            "17:00", "18:00", "19:00"
    };
    private static final String[] DIAS_SEMANA = {"D", "S", "T", "Q", "Q", "S", "S"};
    private static final int DIAS_A_FRENTE = 60;

    private final Locale ptBr = new Locale("pt", "BR");
    private final SimpleDateFormat formatoData = new SimpleDateFormat("dd/MM/yyyy", ptBr);

    private FirebaseFirestore db;
    private FirebaseUser usuario;
    private Medico medico;

    private final Calendar mesVisivel = Calendar.getInstance();
    private Calendar diaSelecionado;
    private String horarioSelecionado;
    private boolean online = true;
    private final Set<String> ocupados = new HashSet<>();

    private LinearLayout containerCalendario;
    private LinearLayout containerHorarios;
    private TextView txtAvisoHorarios;
    private EditText edtNome;
    private EditText edtEmail;
    private EditText edtTelefone;
    private EditText edtMotivo;
    private TextView btnConfirmar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_agendar);
        NavegacaoInferior.configurar(this, R.id.nav_inicio);

        usuario = FirebaseAuth.getInstance().getCurrentUser();
        if (usuario == null) {
            finish();
            return;
        }

        db = FirebaseFirestore.getInstance();
        containerCalendario = findViewById(R.id.containerCalendario);
        containerHorarios = findViewById(R.id.containerHorarios);
        txtAvisoHorarios = findViewById(R.id.txtAvisoHorarios);
        edtNome = findViewById(R.id.edtNomeAgendar);
        edtEmail = findViewById(R.id.edtEmailAgendar);
        edtTelefone = findViewById(R.id.edtTelefoneAgendar);
        edtMotivo = findViewById(R.id.edtMotivoAgendar);
        btnConfirmar = findViewById(R.id.btnConfirmarAgendar);

        findViewById(R.id.btnVoltarAgendar).setOnClickListener(v -> finish());
        findViewById(R.id.btnModoOnline).setOnClickListener(v -> selecionarModo(true));
        findViewById(R.id.btnModoPresencial).setOnClickListener(v -> selecionarModo(false));
        findViewById(R.id.btnMesAnterior).setOnClickListener(v -> mudarMes(-1));
        findViewById(R.id.btnMesSeguinte).setOnClickListener(v -> mudarMes(1));
        btnConfirmar.setOnClickListener(v -> confirmar());

        ColorStateList corPilula = ContextCompat.getColorStateList(this, R.color.pilula_texto);
        ((TextView) findViewById(R.id.btnModoOnline)).setCompoundDrawableTintList(corPilula);
        ((TextView) findViewById(R.id.btnModoPresencial)).setCompoundDrawableTintList(corPilula);

        mesVisivel.set(Calendar.DAY_OF_MONTH, 1);
        selecionarModo(true);
        desenharCalendario();
        desenharHorarios();
        preencherDados();

        Medico recebido = (Medico) getIntent().getSerializableExtra(EXTRA_MEDICO);
        if (recebido != null) {
            definirMedico(recebido);
        } else {
            carregarMedico(getIntent().getStringExtra(EXTRA_MEDICO_ID));
        }
    }

    private void carregarMedico(String id) {
        if (id == null || id.isEmpty()) {
            finish();
            return;
        }

        for (Medico padrao : Medico.padroes()) {
            if (padrao.id.equals(id)) {
                definirMedico(padrao);
            }
        }

        db.collection("medicos").document(id).get()
                .addOnSuccessListener(documento -> {
                    if (documento.exists()) {
                        definirMedico(Medico.de(documento));
                    } else if (medico == null) {
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    if (medico == null) {
                        Toast.makeText(this, "Não foi possível carregar o médico.", Toast.LENGTH_LONG).show();
                        finish();
                    }
                });
    }

    private void definirMedico(Medico escolhido) {
        medico = escolhido;
        ((TextView) findViewById(R.id.txtMedicoAgendar)).setText(
                "com " + medico.nome + " · " + medico.especialidade);
        ((TextView) findViewById(R.id.txtRodapeAgendar)).setText(
                medico.valorFormatado() + " · pagamento feito na clínica, no dia da consulta");
        carregarOcupados();
    }

    private void preencherDados() {
        edtEmail.setText(usuario.getEmail());
        edtNome.setText(usuario.getDisplayName());

        db.collection("pacientes").document(usuario.getUid()).get()
                .addOnSuccessListener(documento -> {
                    preencher(edtNome, documento, "nome");
                    preencher(edtEmail, documento, "email");
                    preencher(edtTelefone, documento, "telefone");
                });
    }

    private static void preencher(EditText campo, DocumentSnapshot documento, String chave) {
        String valor = documento.getString(chave);
        if (valor != null && !valor.trim().isEmpty()) {
            campo.setText(valor.trim());
        }
    }

    // ---- Modo de atendimento ----

    private void selecionarModo(boolean ehOnline) {
        online = ehOnline;
        findViewById(R.id.btnModoOnline).setSelected(ehOnline);
        findViewById(R.id.btnModoPresencial).setSelected(!ehOnline);
        ((TextView) findViewById(R.id.txtDetalheModo)).setText(ehOnline
                ? "Consulta por videochamada, sem sair de casa."
                : "Na clínica: " + getString(R.string.endereco_clinica));
    }

    // ---- Calendário ----

    private void mudarMes(int delta) {
        Calendar candidato = (Calendar) mesVisivel.clone();
        candidato.add(Calendar.MONTH, delta);

        Calendar inicio = hoje();
        inicio.set(Calendar.DAY_OF_MONTH, 1);
        Calendar limite = ultimoDia();

        if (candidato.before(inicio) || candidato.after(limite)) {
            return;
        }

        mesVisivel.setTimeInMillis(candidato.getTimeInMillis());
        desenharCalendario();
    }

    private static Calendar hoje() {
        Calendar hoje = Calendar.getInstance();
        hoje.set(Calendar.HOUR_OF_DAY, 0);
        hoje.set(Calendar.MINUTE, 0);
        hoje.set(Calendar.SECOND, 0);
        hoje.set(Calendar.MILLISECOND, 0);
        return hoje;
    }

    private static Calendar ultimoDia() {
        Calendar limite = hoje();
        limite.add(Calendar.DAY_OF_MONTH, DIAS_A_FRENTE);
        return limite;
    }

    private boolean disponivel(Calendar dia) {
        return !dia.before(hoje())
                && !dia.after(ultimoDia())
                && dia.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY;
    }

    private void desenharCalendario() {
        String mes = new SimpleDateFormat("MMMM yyyy", ptBr).format(mesVisivel.getTime());
        ((TextView) findViewById(R.id.txtMesAgendar)).setText(
                mes.substring(0, 1).toUpperCase(ptBr) + mes.substring(1));

        containerCalendario.removeAllViews();

        LinearLayout cabecalho = Tela.linha(this);
        for (String dia : DIAS_SEMANA) {
            TextView letra = celula(dia);
            letra.setTextColor(ContextCompat.getColor(this, R.color.texto_cinza));
            letra.setTextSize(13);
            cabecalho.addView(letra);
        }
        containerCalendario.addView(cabecalho);

        Calendar cursor = (Calendar) mesVisivel.clone();
        int deslocamento = cursor.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY;
        int diasNoMes = cursor.getActualMaximum(Calendar.DAY_OF_MONTH);

        LinearLayout semana = Tela.linha(this);
        for (int i = 0; i < deslocamento; i++) {
            semana.addView(celula(""));
        }

        for (int dia = 1; dia <= diasNoMes; dia++) {
            Calendar data = (Calendar) mesVisivel.clone();
            data.set(Calendar.DAY_OF_MONTH, dia);
            semana.addView(celulaDia(data));

            if (semana.getChildCount() == 7) {
                containerCalendario.addView(semana);
                semana = Tela.linha(this);
            }
        }

        if (semana.getChildCount() > 0) {
            while (semana.getChildCount() < 7) {
                semana.addView(celula(""));
            }
            containerCalendario.addView(semana);
        }
    }

    private TextView celula(String texto) {
        TextView celula = new TextView(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, Tela.dp(this, 42), 1f);
        celula.setLayoutParams(params);
        celula.setGravity(Gravity.CENTER);
        celula.setText(texto);
        celula.setTextSize(15);
        celula.setTextColor(ContextCompat.getColor(this, R.color.texto_escuro));
        return celula;
    }

    private LinearLayout celulaDia(Calendar data) {
        LinearLayout moldura = new LinearLayout(this);
        moldura.setLayoutParams(new LinearLayout.LayoutParams(0, Tela.dp(this, 42), 1f));
        moldura.setGravity(Gravity.CENTER);

        TextView numero = new TextView(this);
        int tamanho = Tela.dp(this, 38);
        numero.setLayoutParams(new LinearLayout.LayoutParams(tamanho, tamanho));
        numero.setGravity(Gravity.CENTER);
        numero.setText(String.valueOf(data.get(Calendar.DAY_OF_MONTH)));
        numero.setTextSize(15);
        moldura.addView(numero);

        boolean selecionado = diaSelecionado != null && mesmoDia(data, diaSelecionado);

        if (selecionado) {
            numero.setBackgroundResource(R.drawable.bg_dia_selecionado);
            numero.setTextColor(ContextCompat.getColor(this, R.color.white));
            numero.setTypeface(numero.getTypeface(), android.graphics.Typeface.BOLD);
        } else if (disponivel(data)) {
            numero.setTextColor(ContextCompat.getColor(this, R.color.texto_escuro));
            numero.setTypeface(numero.getTypeface(), android.graphics.Typeface.BOLD);
        } else {
            numero.setTextColor(ContextCompat.getColor(this, R.color.texto_claro));
        }

        if (disponivel(data)) {
            moldura.setOnClickListener(v -> {
                diaSelecionado = data;
                horarioSelecionado = null;
                desenharCalendario();
                carregarOcupados();
            });
        }

        return moldura;
    }

    private static boolean mesmoDia(Calendar a, Calendar b) {
        return a.get(Calendar.YEAR) == b.get(Calendar.YEAR)
                && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR);
    }

    // ---- Horários ----

    private void carregarOcupados() {
        ocupados.clear();

        if (diaSelecionado == null || medico == null) {
            desenharHorarios();
            return;
        }

        txtAvisoHorarios.setText("Carregando horários...");
        String data = formatoData.format(diaSelecionado.getTime());

        db.collection("agendamentos")
                .whereEqualTo("medicoId", medico.id)
                .whereEqualTo("data", data)
                .get()
                .addOnSuccessListener(resultado -> {
                    for (DocumentSnapshot documento : resultado.getDocuments()) {
                        Consulta consulta = new Consulta(documento);
                        if (!consulta.encerradaSemAtendimento()) {
                            ocupados.add(consulta.horario);
                        }
                    }
                    desenharHorarios();
                })
                .addOnFailureListener(e -> desenharHorarios());
    }

    private boolean horarioLivre(String horario) {
        if (diaSelecionado == null || ocupados.contains(horario)) {
            return false;
        }

        if (mesmoDia(diaSelecionado, Calendar.getInstance())) {
            int hora = Integer.parseInt(horario.substring(0, 2));
            return hora > Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        }

        return true;
    }

    private void desenharHorarios() {
        containerHorarios.removeAllViews();
        int livres = 0;
        LinearLayout linha = null;

        for (int i = 0; i < HORARIOS.length; i++) {
            if (i % 3 == 0) {
                linha = Tela.linha(this);
                if (i > 0) {
                    ((LinearLayout.LayoutParams) linha.getLayoutParams()).topMargin = Tela.dp(this, 10);
                }
                containerHorarios.addView(linha);
            }

            String horario = HORARIOS[i];
            boolean livre = horarioLivre(horario);
            if (livre) {
                livres++;
            }

            TextView pilula = new TextView(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, Tela.dp(this, 44), 1f);
            params.setMarginStart(i % 3 == 0 ? 0 : Tela.dp(this, 5));
            params.setMarginEnd(i % 3 == 2 ? 0 : Tela.dp(this, 5));
            pilula.setLayoutParams(params);
            pilula.setGravity(Gravity.CENTER);
            pilula.setText(horario);
            pilula.setTextSize(15);
            pilula.setTypeface(pilula.getTypeface(), android.graphics.Typeface.BOLD);
            pilula.setBackgroundResource(R.drawable.bg_pilula);
            pilula.setTextColor(ContextCompat.getColorStateList(this, R.color.pilula_texto));
            pilula.setSelected(horario.equals(horarioSelecionado));
            pilula.setEnabled(livre);
            pilula.setAlpha(livre ? 1f : 0.35f);
            pilula.setOnClickListener(v -> {
                horarioSelecionado = horario;
                desenharHorarios();
            });
            linha.addView(pilula);
        }

        if (diaSelecionado == null) {
            txtAvisoHorarios.setText("Escolha um dia para ver os horários livres.");
        } else if (livres == 0) {
            txtAvisoHorarios.setText("Sem horários livres neste dia. Escolha outra data.");
        } else {
            txtAvisoHorarios.setText(livres + (livres == 1 ? " horário livre" : " horários livres")
                    + " em " + formatoData.format(diaSelecionado.getTime()));
        }
    }

    // ---- Confirmação ----

    private void confirmar() {
        String nome = edtNome.getText().toString().trim();
        String email = edtEmail.getText().toString().trim();
        String telefone = edtTelefone.getText().toString().trim();

        if (medico == null) {
            return;
        }
        if (diaSelecionado == null) {
            Toast.makeText(this, "Escolha o dia da consulta.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (horarioSelecionado == null) {
            Toast.makeText(this, "Escolha um horário.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (nome.isEmpty()) {
            edtNome.setError("Informe seu nome");
            edtNome.requestFocus();
            return;
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            edtEmail.setError("E-mail inválido");
            edtEmail.requestFocus();
            return;
        }
        if (telefone.replaceAll("\\D", "").length() < 10) {
            edtTelefone.setError("Telefone com DDD");
            edtTelefone.requestFocus();
            return;
        }

        btnConfirmar.setEnabled(false);
        String data = formatoData.format(diaSelecionado.getTime());
        String horario = horarioSelecionado;

        // Confere de novo se o horário continua livre antes de gravar.
        db.collection("agendamentos")
                .whereEqualTo("medicoId", medico.id)
                .whereEqualTo("data", data)
                .whereEqualTo("horario", horario)
                .get()
                .addOnSuccessListener(resultado -> {
                    for (DocumentSnapshot documento : resultado.getDocuments()) {
                        if (!new Consulta(documento).encerradaSemAtendimento()) {
                            Toast.makeText(this, "Esse horário acabou de ser reservado. Escolha outro.",
                                    Toast.LENGTH_LONG).show();
                            btnConfirmar.setEnabled(true);
                            carregarOcupados();
                            return;
                        }
                    }
                    salvar(nome, email, telefone, data, horario);
                })
                .addOnFailureListener(e -> {
                    btnConfirmar.setEnabled(true);
                    Toast.makeText(this, "Sem conexão. Tente novamente.", Toast.LENGTH_LONG).show();
                });
    }

    private void salvar(String nome, String email, String telefone, String data, String horario) {
        Map<String, Object> consulta = new HashMap<>();
        consulta.put("medico", medico.nome);
        consulta.put("medicoId", medico.id);
        consulta.put("especialidade", medico.especialidade);
        consulta.put("data", data);
        consulta.put("horario", horario);
        consulta.put("status", "Agendado");
        consulta.put("usuarioId", usuario.getUid());
        consulta.put("pacienteNome", nome);
        consulta.put("pacienteEmail", email);
        consulta.put("telefone", telefone);
        consulta.put("modalidade", online ? "Online" : "Presencial");
        consulta.put("motivo", edtMotivo.getText().toString().trim());
        consulta.put("valor", medico.valor);
        consulta.put("criadoEm", FieldValue.serverTimestamp());

        db.collection("agendamentos")
                .add(consulta)
                .addOnSuccessListener(referencia -> {
                    Intent intent = new Intent(this, AgendamentoSucessoActivity.class);
                    intent.putExtra("agendamentoId", referencia.getId());
                    intent.putExtra("medico", medico.nome);
                    intent.putExtra("medicoId", medico.id);
                    intent.putExtra("especialidade", medico.especialidade);
                    intent.putExtra("data", data);
                    intent.putExtra("horario", horario);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnConfirmar.setEnabled(true);
                    Toast.makeText(this, "Não foi possível agendar. Tente novamente.",
                            Toast.LENGTH_LONG).show();
                });
    }
}

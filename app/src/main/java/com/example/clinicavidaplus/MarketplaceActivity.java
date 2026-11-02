package com.example.clinicavidaplus;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/** Tela inicial do paciente: marketplace de médicos da clínica. */
public class MarketplaceActivity extends AppCompatActivity {

    private static final String TODAS = "Todas";

    private final List<Medico> medicos = new ArrayList<>();

    private LinearLayout containerMedicos;
    private LinearLayout containerEspecialidades;
    private TextView txtResultado;
    private EditText edtBusca;

    private String especialidadeSelecionada = TODAS;
    private int ordem = R.id.ordemRelevancia;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_marketplace);
        NavegacaoInferior.configurar(this, R.id.nav_inicio);

        containerMedicos = findViewById(R.id.containerMedicos);
        containerEspecialidades = findViewById(R.id.containerEspecialidades);
        txtResultado = findViewById(R.id.txtResultadoMedicos);
        edtBusca = findViewById(R.id.edtBuscaMedico);

        edtBusca.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mostrarMedicos();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        findViewById(R.id.btnFiltroMedicos).setOnClickListener(v -> abrirFiltros());

        carregarSaudacao();
        montarEspecialidades();
        carregarMedicos();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            // Voltou ao início sem logar: esquece o agendamento iniciado.
            Sessao.descartarAgendamentoPendente(this);
        }
        carregarMedicos();
    }

    private void carregarSaudacao() {
        FirebaseUser usuario = FirebaseAuth.getInstance().getCurrentUser();
        View entrar = findViewById(R.id.btnEntrarMarketplace);

        if (usuario == null) {
            entrar.setVisibility(View.VISIBLE);
            entrar.setOnClickListener(v -> {
                Sessao.descartarAgendamentoPendente(this);
                startActivity(new Intent(this, LoginActivity.class));
            });
            return;
        }

        entrar.setVisibility(View.GONE);

        FirebaseFirestore.getInstance()
                .collection("pacientes")
                .document(usuario.getUid())
                .get()
                .addOnSuccessListener(documento -> {
                    String nome = documento.getString("nome");

                    if (nome == null || nome.trim().isEmpty()) {
                        nome = usuario.getDisplayName();
                    }

                    if (nome != null && !nome.trim().isEmpty()) {
                        String primeiroNome = nome.trim().split("\\s+")[0];
                        ((TextView) findViewById(R.id.txtSaudacaoMarketplace)).setText(
                                "Olá, " + primeiroNome + "! Escolha o profissional ideal para você"
                        );
                    }
                });
    }

    private void carregarMedicos() {
        FirebaseFirestore.getInstance()
                .collection("medicos")
                .get()
                .addOnSuccessListener(resultado -> {
                    medicos.clear();

                    for (DocumentSnapshot documento : resultado.getDocuments()) {
                        medicos.add(Medico.de(documento));
                    }

                    if (medicos.isEmpty()) {
                        medicos.addAll(Medico.padroes());
                    }

                    mostrarMedicos();
                })
                .addOnFailureListener(erro -> {
                    medicos.clear();
                    medicos.addAll(Medico.padroes());
                    mostrarMedicos();
                });
    }

    private void montarEspecialidades() {
        containerEspecialidades.removeAllViews();

        List<String> opcoes = new ArrayList<>();
        opcoes.add(TODAS);
        List<String> especialidades = Arrays.asList(
                getResources().getStringArray(R.array.especialidades_array));
        opcoes.addAll(especialidades.subList(1, especialidades.size()));

        for (String opcao : opcoes) {
            TextView chip = new TextView(this);
            chip.setText(opcao);
            chip.setTextSize(13);
            chip.setTypeface(null, android.graphics.Typeface.BOLD);
            chip.setGravity(Gravity.CENTER);
            chip.setBackgroundResource(R.drawable.bg_pilula);
            chip.setTextColor(ContextCompat.getColorStateList(this, R.color.pilula_texto));
            chip.setPadding(Tela.dp(this, 16), 0, Tela.dp(this, 16), 0);
            chip.setSelected(opcao.equals(especialidadeSelecionada));

            LinearLayout.LayoutParams parametros = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, Tela.dp(this, 36));
            parametros.setMarginEnd(Tela.dp(this, 8));
            chip.setLayoutParams(parametros);

            chip.setOnClickListener(v -> {
                especialidadeSelecionada = opcao;

                for (int i = 0; i < containerEspecialidades.getChildCount(); i++) {
                    View filho = containerEspecialidades.getChildAt(i);
                    filho.setSelected(filho == v);
                }

                mostrarMedicos();
            });

            containerEspecialidades.addView(chip);
        }
    }

    private void mostrarMedicos() {
        containerMedicos.removeAllViews();

        String busca = normalizar(edtBusca.getText().toString());
        List<Medico> filtrados = new ArrayList<>();

        for (Medico medico : medicos) {
            boolean especialidadeOk = TODAS.equals(especialidadeSelecionada)
                    || especialidadeSelecionada.equalsIgnoreCase(medico.especialidade);
            boolean buscaOk = busca.isEmpty()
                    || normalizar(medico.nome).contains(busca)
                    || normalizar(medico.especialidade).contains(busca);

            if (especialidadeOk && buscaOk) {
                filtrados.add(medico);
            }
        }

        if (ordem == R.id.ordemMenorPreco) {
            filtrados.sort((a, b) -> Integer.compare(a.valor, b.valor));
        } else if (ordem == R.id.ordemAvaliacao) {
            filtrados.sort((a, b) -> Double.compare(b.avaliacao, a.avaliacao));
        } else {
            filtrados.sort((a, b) -> {
                if (a.online != b.online) {
                    return a.online ? -1 : 1;
                }
                return Double.compare(b.avaliacao, a.avaliacao);
            });
        }

        int total = filtrados.size();
        txtResultado.setText(total == 1
                ? "1 médico encontrado"
                : total + " médicos encontrados");

        if (filtrados.isEmpty()) {
            LinearLayout vazio = Tela.card(this);
            vazio.setGravity(Gravity.CENTER);
            TextView texto = Tela.texto(this,
                    "Nenhum médico encontrado com esses filtros.",
                    R.style.Texto_Subtitulo, 0);
            texto.setGravity(Gravity.CENTER);
            vazio.addView(texto);
            containerMedicos.addView(vazio);
            return;
        }

        for (Medico medico : filtrados) {
            containerMedicos.addView(criarCard(medico));
        }
    }

    private View criarCard(Medico medico) {
        LinearLayout card = Tela.card(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setClickable(true);
        card.setFocusable(true);

        android.util.TypedValue fundo = new android.util.TypedValue();
        getTheme().resolveAttribute(android.R.attr.selectableItemBackground, fundo, true);
        card.setForeground(ContextCompat.getDrawable(this, fundo.resourceId));

        card.addView(Tela.avatar(this, medico.inicial(), 56));

        LinearLayout info = Tela.coluna(this, 14);
        info.addView(Tela.texto(this, medico.nome, R.style.Texto_CardTitulo, 0));
        info.addView(Tela.texto(this, medico.especialidade, R.style.Texto_Pequeno, 2));

        TextView avaliacao = Tela.texto(this, medico.avaliacaoTexto(), R.style.Texto_Pequeno, 6);
        avaliacao.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_estrela, 0, 0, 0);
        avaliacao.setCompoundDrawablePadding(Tela.dp(this, 4));
        avaliacao.setTextColor(ContextCompat.getColor(this, R.color.texto_medio));
        info.addView(avaliacao);
        card.addView(info);

        LinearLayout preco = new LinearLayout(this);
        preco.setOrientation(LinearLayout.VERTICAL);
        preco.setGravity(Gravity.END);
        preco.setPadding(Tela.dp(this, 8), 0, 0, 0);

        TextView valor = Tela.compacto(Tela.texto(this, medico.valorFormatado(), R.style.Texto_CardTitulo, 0));
        valor.setTextSize(16);
        preco.addView(valor);
        preco.addView(Tela.compacto(Tela.texto(this, "por consulta", R.style.Texto_Pequeno, 0)));

        if (medico.online) {
            TextView online = Tela.compacto(Tela.texto(this, "● Atendendo", R.style.Texto_Pequeno, 6));
            online.setTextColor(ContextCompat.getColor(this, R.color.sucesso));
            online.setTypeface(null, android.graphics.Typeface.BOLD);
            preco.addView(online);
        }

        card.addView(preco);

        card.setOnClickListener(v -> {
            Intent intent = new Intent(this, PerfilMedicoActivity.class);
            intent.putExtra(PerfilMedicoActivity.EXTRA_MEDICO, medico);
            startActivity(intent);
        });

        return card;
    }

    private void abrirFiltros() {
        BottomSheetDialog dialogo = new BottomSheetDialog(this);
        View conteudo = getLayoutInflater().inflate(R.layout.sheet_filtro_medicos, null);
        dialogo.setContentView(conteudo);

        RadioGroup grupo = conteudo.findViewById(R.id.grupoOrdenacao);
        grupo.check(ordem);

        conteudo.findViewById(R.id.btnAplicarFiltro).setOnClickListener(v -> {
            ordem = grupo.getCheckedRadioButtonId();
            mostrarMedicos();
            dialogo.dismiss();
        });

        conteudo.findViewById(R.id.btnLimparFiltro).setOnClickListener(v -> {
            ordem = R.id.ordemRelevancia;
            especialidadeSelecionada = TODAS;
            edtBusca.setText("");
            montarEspecialidades();
            mostrarMedicos();
            dialogo.dismiss();
        });

        dialogo.setOnShowListener(d -> {
            View pai = (View) conteudo.getParent();
            if (pai != null) {
                pai.setBackgroundColor(Color.TRANSPARENT);
            }
        });

        dialogo.show();
    }

    private static String normalizar(String texto) {
        if (texto == null) {
            return "";
        }

        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .trim();
    }
}

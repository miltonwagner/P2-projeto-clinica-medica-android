package com.example.clinicavidaplus;

import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Carteira: para o paciente, os valores das consultas; para o médico,
 * os ganhos com as consultas concluídas. O pagamento é feito na clínica.
 */
public class CarteiraActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private String uid;
    private boolean medico;

    private final Map<String, Medico> medicosPorId = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_carteira);
        NavegacaoInferior.configurar(this, R.id.nav_carteira);

        FirebaseUser usuario = FirebaseAuth.getInstance().getCurrentUser();
        if (usuario == null) {
            Sessao.sair(this);
            return;
        }

        uid = usuario.getUid();
        medico = Sessao.PERFIL_MEDICO.equals(Sessao.perfil(this));
        db = FirebaseFirestore.getInstance();

        texto(R.id.txtSubtituloCarteira, medico
                ? "Seus ganhos e histórico de recebimentos"
                : "Seus pagamentos e consultas");
        texto(R.id.txtHeroRotulo, medico ? "Total recebido" : "Total em consultas");
        texto(R.id.txtSecaoCarteira, medico ? "Histórico de recebimentos" : "Histórico de pagamentos");

        for (Medico padrao : Medico.padroes()) {
            medicosPorId.put(padrao.id, padrao);
        }

        db.collection("medicos")
                .get()
                .addOnSuccessListener(resultado -> {
                    for (DocumentSnapshot documento : resultado.getDocuments()) {
                        medicosPorId.put(documento.getId(), Medico.de(documento));
                    }
                    carregarConsultas();
                })
                .addOnFailureListener(e -> carregarConsultas());
    }

    private void carregarConsultas() {
        db.collection("agendamentos")
                .whereEqualTo(medico ? "medicoId" : "usuarioId", uid)
                .get()
                .addOnSuccessListener(resultado -> {
                    List<Consulta> consultas = new ArrayList<>();
                    for (DocumentSnapshot documento : resultado.getDocuments()) {
                        consultas.add(new Consulta(documento));
                    }
                    consultas.sort((a, b) -> Long.compare(b.momento, a.momento));

                    if (medico) {
                        mostrarMedico(consultas);
                    } else {
                        mostrarPaciente(consultas);
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this,
                        "Erro ao carregar a carteira.", Toast.LENGTH_LONG).show());
    }

    private int valor(Consulta consulta) {
        Medico dono = medicosPorId.get(consulta.medicoId);
        return dono == null ? 150 : dono.valor;
    }

    private void mostrarPaciente(List<Consulta> consultas) {
        double total = 0;
        double pago = 0;
        int validas = 0;
        int concluidas = 0;

        for (Consulta consulta : consultas) {
            if (consulta.encerradaSemAtendimento()) {
                continue;
            }
            validas++;
            total += valor(consulta);
            if (consulta.concluida()) {
                concluidas++;
                pago += valor(consulta);
            }
        }

        texto(R.id.txtHeroValor, Medico.moeda(total));
        texto(R.id.txtHeroDetalhe, (validas == 1 ? "1 consulta" : validas + " consultas")
                + " · " + concluidas + " concluída" + (concluidas == 1 ? "" : "s"));

        LinearLayout resumo = findViewById(R.id.containerResumoCarteira);
        resumo.removeAllViews();

        TextView secao = Tela.texto(this, "Forma de pagamento", R.style.Texto_Secao, 4);
        resumo.addView(secao);

        LinearLayout metodo = Tela.card(this);
        ((LinearLayout.LayoutParams) metodo.getLayoutParams()).topMargin = Tela.dp(this, 12);
        metodo.setOrientation(LinearLayout.HORIZONTAL);
        metodo.setGravity(Gravity.CENTER_VERTICAL);
        metodo.addView(Tela.avatar(this, "R$", 44));
        LinearLayout info = Tela.coluna(this, 14);
        info.addView(Tela.texto(this, "Pagamento na clínica", R.style.Texto_CardTitulo, 0));
        info.addView(Tela.texto(this, "Pix, cartão ou dinheiro no dia da consulta",
                R.style.Texto_Pequeno, 2));
        metodo.addView(info);
        resumo.addView(metodo);

        resumo.addView(linhaResumo(
                new String[]{Medico.moeda(pago), Medico.moeda(total - pago), String.valueOf(concluidas)},
                new String[]{"Pago", "A pagar", "Concluídas"}));

        LinearLayout lista = findViewById(R.id.containerCarteira);
        lista.removeAllViews();

        if (consultas.isEmpty()) {
            lista.addView(vazio("Seus pagamentos aparecerão aqui após o primeiro agendamento."));
            return;
        }

        for (Consulta consulta : consultas) {
            lista.addView(movimento(
                    consulta.especialidade + " · " + consulta.medico,
                    consulta.data + " · " + consulta.statusParaPaciente(),
                    consulta.encerradaSemAtendimento() ? "—" : Medico.moeda(valor(consulta)),
                    consulta.concluida() ? R.color.sucesso : R.color.texto_escuro,
                    consulta.medico.isEmpty() ? "C" : consulta.medico
                            .replaceFirst("^(Dra?\\.?)\\s*", "").substring(0, 1)));
        }
    }

    private void mostrarMedico(List<Consulta> consultas) {
        double recebido = 0;
        double mes = 0;
        double aReceber = 0;
        int pagas = 0;
        List<Consulta> concluidas = new ArrayList<>();

        for (Consulta consulta : consultas) {
            int preco = valor(consulta);
            if (consulta.concluida()) {
                recebido += preco;
                pagas++;
                concluidas.add(consulta);
                if (consulta.noMesAtual()) {
                    mes += preco;
                }
            } else if (consulta.confirmada() || consulta.pendente()) {
                aReceber += preco;
            }
        }

        texto(R.id.txtHeroValor, Medico.moeda(recebido));
        texto(R.id.txtHeroDetalhe, "Valores pagos pelos pacientes na clínica");

        LinearLayout resumo = findViewById(R.id.containerResumoCarteira);
        resumo.removeAllViews();
        resumo.addView(linhaResumo(
                new String[]{Medico.moeda(mes), Medico.moeda(aReceber), String.valueOf(pagas)},
                new String[]{"Este mês", "A receber", "Consultas pagas"}));

        LinearLayout lista = findViewById(R.id.containerCarteira);
        lista.removeAllViews();

        if (concluidas.isEmpty()) {
            lista.addView(vazio("Conclua uma consulta na Visão geral para registrar o recebimento."));
            return;
        }

        for (Consulta consulta : concluidas) {
            View item = movimento("Carregando paciente...",
                    consulta.especialidade + " · " + consulta.data,
                    "+ " + Medico.moeda(valor(consulta)), R.color.sucesso, "P");
            lista.addView(item);
            preencherPaciente(consulta.pacienteId, item);
        }
    }

    private void preencherPaciente(String pacienteId, View item) {
        if (pacienteId.isEmpty()) {
            return;
        }

        TextView titulo = item.findViewWithTag("titulo");
        TextView avatar = item.findViewWithTag("avatar");

        db.collection("pacientes")
                .document(pacienteId)
                .get()
                .addOnSuccessListener(documento -> {
                    String nome = documento.getString("nome");
                    if (nome == null || nome.trim().isEmpty()) {
                        nome = "Paciente";
                    }
                    titulo.setText(nome.trim());
                    avatar.setText(nome.trim().substring(0, 1).toUpperCase());
                })
                .addOnFailureListener(e -> titulo.setText("Paciente"));
    }

    private View linhaResumo(String[] valores, String[] rotulos) {
        LinearLayout card = Tela.card(this);
        ((LinearLayout.LayoutParams) card.getLayoutParams()).topMargin = Tela.dp(this, 4);
        LinearLayout linha = Tela.linha(this);

        for (int i = 0; i < valores.length; i++) {
            LinearLayout coluna = Tela.coluna(this, 0);
            coluna.setGravity(Gravity.CENTER_HORIZONTAL);
            coluna.addView(Tela.compacto(Tela.texto(this, valores[i], R.style.Texto_CardTitulo, 0)));
            coluna.addView(Tela.compacto(Tela.texto(this, rotulos[i], R.style.Texto_Pequeno, 2)));
            linha.addView(coluna);
        }

        card.addView(linha);
        return card;
    }

    private View movimento(String titulo, String detalhe, String valor, int corValor, String letra) {
        LinearLayout card = Tela.card(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);

        TextView avatar = Tela.avatar(this, letra.toUpperCase(), 44);
        avatar.setTag("avatar");
        card.addView(avatar);

        LinearLayout info = Tela.coluna(this, 14);
        TextView txtTitulo = Tela.texto(this, titulo, R.style.Texto_CardTitulo, 0);
        txtTitulo.setTag("titulo");
        info.addView(txtTitulo);
        info.addView(Tela.texto(this, detalhe, R.style.Texto_Pequeno, 2));
        card.addView(info);

        TextView txtValor = Tela.compacto(Tela.texto(this, valor, R.style.Texto_CardTitulo, 0));
        txtValor.setTextColor(ContextCompat.getColor(this, corValor));
        txtValor.setPadding(Tela.dp(this, 8), 0, 0, 0);
        card.addView(txtValor);
        return card;
    }

    private View vazio(String mensagem) {
        LinearLayout card = Tela.card(this);
        card.addView(Tela.texto(this, mensagem, R.style.Texto_Subtitulo, 0));
        return card;
    }

    private void texto(int id, String valor) {
        ((TextView) findViewById(id)).setText(valor);
    }
}

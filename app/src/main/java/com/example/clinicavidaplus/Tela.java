package com.example.clinicavidaplus;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.TextViewCompat;

/**
 * Componentes visuais padronizados criados por código
 * (cards, textos e etiquetas de status), no mesmo estilo dos layouts.
 */
public final class Tela {

    private Tela() {
    }

    public static int dp(Context contexto, int valor) {
        return Math.round(valor * contexto.getResources().getDisplayMetrics().density);
    }

    /**
     * Respeita a barra de status e a barra de navegação do sistema
     * (Android 15+ desenha o app atrás delas).
     */
    public static void ajustarBordas(Activity activity) {
        ViewGroup conteudo = activity.findViewById(android.R.id.content);

        if (conteudo == null || conteudo.getChildCount() == 0) {
            return;
        }

        View raiz = conteudo.getChildAt(0);

        if (raiz.getTag(R.id.tag_bordas_ajustadas) != null) {
            return;
        }

        raiz.setTag(R.id.tag_bordas_ajustadas, Boolean.TRUE);

        int esquerda = raiz.getPaddingLeft();
        int topo = raiz.getPaddingTop();
        int direita = raiz.getPaddingRight();
        int base = raiz.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(raiz, (v, insets) -> {
            int tipos = WindowInsetsCompat.Type.systemBars();

            if (Build.VERSION.SDK_INT >= 35) {
                tipos |= WindowInsetsCompat.Type.ime();
            }

            Insets barras = insets.getInsets(tipos);
            v.setPadding(
                    esquerda + barras.left,
                    topo + barras.top,
                    direita + barras.right,
                    base + barras.bottom
            );
            return WindowInsetsCompat.CONSUMED;
        });

        ViewCompat.requestApplyInsets(raiz);
    }

    /** Card branco com borda suave, igual ao estilo "Card". */
    public static LinearLayout card(Context contexto) {
        LinearLayout card = new LinearLayout(contexto);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_card);

        int espaco = dp(contexto, 16);
        card.setPadding(espaco, espaco, espaco, espaco);

        LinearLayout.LayoutParams parametros = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        parametros.bottomMargin = dp(contexto, 12);
        card.setLayoutParams(parametros);

        return card;
    }

    public static TextView texto(Context contexto, String valor, int estilo, int margemTopoDp) {
        TextView texto = new TextView(contexto);
        TextViewCompat.setTextAppearance(texto, estilo);
        texto.setText(valor);

        LinearLayout.LayoutParams parametros = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        parametros.topMargin = dp(contexto, margemTopoDp);
        texto.setLayoutParams(parametros);

        return texto;
    }

    /** Texto com ícone à esquerda (ex.: data e horário). */
    public static TextView textoComIcone(
            Context contexto,
            String valor,
            int icone,
            int margemTopoDp
    ) {
        TextView texto = texto(contexto, valor, R.style.Texto_Corpo, margemTopoDp);
        texto.setCompoundDrawablesRelativeWithIntrinsicBounds(icone, 0, 0, 0);
        texto.setCompoundDrawablePadding(dp(contexto, 8));
        texto.setGravity(android.view.Gravity.CENTER_VERTICAL);
        return texto;
    }

    /** Etiqueta colorida de status (agendado, confirmado, cancelado...). */
    public static TextView etiqueta(Context contexto, String status) {
        String valor = status == null ? "" : status.trim();
        String chave = valor.toLowerCase(java.util.Locale.ROOT);

        int fundo = R.drawable.bg_badge_info;
        int cor = R.color.primaria_escura;

        if (chave.contains("execut") || chave.contains("conclu")
                || chave.contains("confirm") || chave.contains("autoriz")) {
            fundo = R.drawable.bg_badge_sucesso;
            cor = R.color.sucesso;
        } else if (chave.contains("cancel") || chave.contains("recus")
                || chave.contains("bloque")) {
            fundo = R.drawable.bg_badge_perigo;
            cor = R.color.perigo;
        } else if (chave.contains("pend") || chave.contains("aguard")
                || chave.contains("agendad")) {
            fundo = R.drawable.bg_badge_aviso;
            cor = R.color.aviso;
        }

        TextView etiqueta = new TextView(contexto);
        etiqueta.setText(valor.isEmpty() ? "Sem status" : valor);
        etiqueta.setTextSize(12);
        etiqueta.setTypeface(null, android.graphics.Typeface.BOLD);
        etiqueta.setTextColor(ContextCompat.getColor(contexto, cor));
        etiqueta.setBackgroundResource(fundo);
        etiqueta.setPadding(dp(contexto, 10), dp(contexto, 4), dp(contexto, 10), dp(contexto, 4));

        LinearLayout.LayoutParams parametros = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        etiqueta.setLayoutParams(parametros);

        return etiqueta;
    }

    /** Círculo com a inicial do nome (avatar). */
    public static TextView avatar(Context contexto, String letra, int tamanhoDp) {
        TextView avatar = new TextView(contexto);
        avatar.setText(letra);
        avatar.setGravity(android.view.Gravity.CENTER);
        avatar.setBackgroundResource(R.drawable.bg_avatar);
        avatar.setTextColor(ContextCompat.getColor(contexto, R.color.primaria_escura));
        avatar.setTypeface(null, android.graphics.Typeface.BOLD);
        avatar.setTextSize(tamanhoDp * 0.4f);
        avatar.setLayoutParams(new LinearLayout.LayoutParams(dp(contexto, tamanhoDp), dp(contexto, tamanhoDp)));
        return avatar;
    }

    /** Linha horizontal centralizada verticalmente. */
    public static LinearLayout linha(Context contexto) {
        LinearLayout linha = new LinearLayout(contexto);
        linha.setOrientation(LinearLayout.HORIZONTAL);
        linha.setGravity(android.view.Gravity.CENTER_VERTICAL);
        linha.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        return linha;
    }

    /** Coluna vertical que ocupa o espaço restante de uma linha. */
    public static LinearLayout coluna(Context contexto, int margemInicioDp) {
        LinearLayout coluna = new LinearLayout(contexto);
        coluna.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams parametros = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        parametros.setMarginStart(dp(contexto, margemInicioDp));
        coluna.setLayoutParams(parametros);
        return coluna;
    }

    /** Botão compacto (40dp) para ações dentro de cards. */
    public static android.widget.Button botao(Context contexto, String texto, boolean primario) {
        android.widget.Button botao = new android.widget.Button(contexto);
        botao.setText(texto);
        botao.setAllCaps(false);
        botao.setTextSize(14);
        botao.setTypeface(null, android.graphics.Typeface.BOLD);
        botao.setStateListAnimator(null);
        botao.setMinHeight(0);
        botao.setMinimumHeight(0);
        botao.setBackgroundResource(primario ? R.drawable.bg_botao_primario : R.drawable.bg_botao_secundario);
        botao.setTextColor(ContextCompat.getColor(contexto, primario ? R.color.white : R.color.primaria_escura));
        LinearLayout.LayoutParams parametros = new LinearLayout.LayoutParams(
                0, dp(contexto, 40), 1f);
        botao.setLayoutParams(parametros);
        return botao;
    }

    /** Ajusta um texto para ocupar só o tamanho do conteúdo. */
    public static TextView compacto(TextView texto) {
        texto.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        return texto;
    }
}

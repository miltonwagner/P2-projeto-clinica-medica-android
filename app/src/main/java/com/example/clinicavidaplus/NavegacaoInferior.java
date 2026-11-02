package com.example.clinicavidaplus;

import android.content.Intent;
import android.view.View;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;

/**
 * Menu fixo inferior, igual em todas as telas principais.
 *
 * Regras de navegação:
 * - A tela inicial do perfil é a raiz. Voltar nela minimiza o app
 *   (o usuário continua logado), em vez de cair no login.
 * - As demais abas abrem por cima da raiz. Voltar nelas retorna à raiz.
 */
public final class NavegacaoInferior {

    private NavegacaoInferior() {
    }

    public static void configurar(AppCompatActivity activity, int itemAtual) {
        BottomNavigationView nav = activity.findViewById(R.id.navInferior);
        String perfil = Sessao.perfil(activity);
        boolean logado = FirebaseAuth.getInstance().getCurrentUser() != null;

        if (nav == null) {
            return;
        }

        if (!logado || perfil == null) {
            // Ex.: tela da clínica aberta antes do login.
            nav.setVisibility(View.GONE);
            View divisor = activity.findViewById(R.id.divisorNavInferior);
            if (divisor != null) {
                divisor.setVisibility(View.GONE);
            }
            return;
        }

        nav.getMenu().clear();
        nav.inflateMenu(menuDoPerfil(perfil));

        if (nav.getMenu().findItem(itemAtual) != null) {
            nav.setSelectedItemId(itemAtual);
        }

        nav.setOnItemReselectedListener(item -> voltarParaRaizDaAba(activity, perfil, item.getItemId()));

        nav.setOnItemSelectedListener(item -> {
            if (item.getItemId() == itemAtual) {
                voltarParaRaizDaAba(activity, perfil, item.getItemId());
                return true;
            }

            abrir(activity, perfil, item.getItemId());
            return false;
        });

        activity.getOnBackPressedDispatcher().addCallback(activity,
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        if (ehRaiz(activity, perfil)) {
                            activity.moveTaskToBack(true);
                        } else {
                            activity.finish();
                        }
                    }
                });
    }

    /** Volta para a tela inicial do perfil, limpando as telas acima dela. */
    public static void irParaInicio(AppCompatActivity activity) {
        String perfil = Sessao.perfil(activity);
        Intent intent = new Intent(activity, Sessao.telaInicial(perfil));
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        activity.startActivity(intent);
        activity.finish();
    }

    /**
     * Tocar na aba atual estando numa tela interna dela (ex.: perfil do
     * médico dentro de "Buscar") volta para o início da aba.
     */
    private static void voltarParaRaizDaAba(AppCompatActivity activity, String perfil, int itemId) {
        if (destinoDoItem(itemId, perfil) == Sessao.telaInicial(perfil) && !ehRaiz(activity, perfil)) {
            irParaInicio(activity);
        }
    }

    /**
     * Abre uma aba secundária logo acima da tela inicial, descartando as
     * telas intermediárias (voltar nela retorna ao início).
     */
    public static void abrirAba(AppCompatActivity activity, Class<?> destino) {
        String perfil = Sessao.perfil(activity);
        Intent inicio = new Intent(activity, Sessao.telaInicial(perfil));
        inicio.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        Intent aba = new Intent(activity, destino);
        aba.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);

        activity.startActivities(new Intent[]{inicio, aba});
        activity.finish();
        activity.overridePendingTransition(0, 0);
    }

    private static boolean ehRaiz(AppCompatActivity activity, String perfil) {
        return activity.getClass() == Sessao.telaInicial(perfil);
    }

    private static void abrir(AppCompatActivity activity, String perfil, int itemId) {
        Class<?> destino = destinoDoItem(itemId, perfil);

        if (destino == null) {
            return;
        }

        if (destino == Sessao.telaInicial(perfil)) {
            irParaInicio(activity);
            activity.overridePendingTransition(0, 0);
            return;
        }

        if (!ehRaiz(activity, perfil)) {
            // A raiz fica sempre na pilha; telas internas e abas são substituídas.
            abrirAba(activity, destino);
            return;
        }

        Intent intent = new Intent(activity, destino);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        activity.startActivity(intent);
        activity.overridePendingTransition(0, 0);
    }

    private static int menuDoPerfil(String perfil) {
        if (Sessao.PERFIL_ADMIN.equals(perfil)) {
            return R.menu.menu_nav_admin;
        }

        if (Sessao.PERFIL_MEDICO.equals(perfil)) {
            return R.menu.menu_nav_medico;
        }

        return R.menu.menu_nav_paciente;
    }

    private static Class<?> destinoDoItem(int itemId, String perfil) {
        if (itemId == R.id.nav_inicio || itemId == R.id.nav_medicos) {
            return Sessao.telaInicial(perfil);
        }

        if (itemId == R.id.nav_consultas) {
            return ListaConsultasActivity.class;
        }

        if (itemId == R.id.nav_agenda_medico) {
            return ListaConsultasMedicoActivity.class;
        }

        if (itemId == R.id.nav_anotacoes) {
            return AnotacoesActivity.class;
        }

        if (itemId == R.id.nav_carteira) {
            return CarteiraActivity.class;
        }

        if (itemId == R.id.nav_clinica) {
            return EnderecoActivity.class;
        }

        if (itemId == R.id.nav_perfil) {
            return PerfilActivity.class;
        }

        return null;
    }
}

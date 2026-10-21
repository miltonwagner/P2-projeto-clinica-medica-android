
package com.example.clinicavidaplus;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class EsqueceuSenhaActivity extends AppCompatActivity {

    private EditText edtEmailRecuperacao;
    private Button btnEnviarRecuperacao;
    private TextView txtVoltarLogin;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_esqueceu_senha);

        auth = FirebaseAuth.getInstance();

        edtEmailRecuperacao =
                findViewById(R.id.edtEmailRecuperacao);

        btnEnviarRecuperacao =
                findViewById(R.id.btnEnviarRecuperacao);

        txtVoltarLogin =
                findViewById(R.id.txtVoltarLogin);

        btnEnviarRecuperacao.setOnClickListener(
                v -> enviarRecuperacao()
        );

        txtVoltarLogin.setOnClickListener(
                v -> finish()
        );
    }

    private void enviarRecuperacao() {
        String email = edtEmailRecuperacao
                .getText()
                .toString()
                .trim();

        if (TextUtils.isEmpty(email)) {
            edtEmailRecuperacao.setError(
                    "Digite seu e-mail"
            );
            edtEmailRecuperacao.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            edtEmailRecuperacao.setError(
                    "Digite um e-mail válido"
            );
            edtEmailRecuperacao.requestFocus();
            return;
        }

        btnEnviarRecuperacao.setEnabled(false);

        auth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    btnEnviarRecuperacao.setEnabled(true);

                    if (task.isSuccessful()) {
                        Toast.makeText(
                                this,
                                "Se o e-mail estiver cadastrado, você receberá as instruções de recuperação.",
                                Toast.LENGTH_LONG
                        ).show();

                        finish();
                    } else {
                        Toast.makeText(
                                this,
                                "Não foi possível enviar o e-mail. Verifique sua conexão e tente novamente.",
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
    }
}

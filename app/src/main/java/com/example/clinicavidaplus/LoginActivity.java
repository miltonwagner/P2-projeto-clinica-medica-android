package com.example.clinicavidaplus;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LOGIN_DEBUG";

    private GoogleSignInClient mGoogleSignInClient;
    private FirebaseAuth mAuth;
    private EditText editEmail;
    private EditText editSenha;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

        editEmail = findViewById(R.id.edtEmail);
        editSenha = findViewById(R.id.edtSenha);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(
                GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(
                        getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        findViewById(R.id.txtEsqueceuSenha).setOnClickListener(v -> {
            Intent intent = new Intent(
                    LoginActivity.this,
                    EsqueceuSenhaActivity.class
            );
            startActivity(intent);
        });

        findViewById(R.id.txtCadastro).setOnClickListener(v -> {
            Intent intent = new Intent(
                    LoginActivity.this,
                    MainActivity.class
            );
            startActivity(intent);
        });

        findViewById(R.id.btnEntrar).setOnClickListener(v -> {

            String email = editEmail.getText().toString().trim();
            String senha = editSenha.getText().toString().trim();

            if (email.isEmpty()) {
                editEmail.setError("Informe o e-mail");
                editEmail.requestFocus();
                return;
            }

            if (senha.isEmpty()) {
                editSenha.setError("Informe a senha");
                editSenha.requestFocus();
                return;
            }

            mAuth.signInWithEmailAndPassword(email, senha)
                    .addOnCompleteListener(this, task -> {

                        if (task.isSuccessful()) {
                            Log.d(TAG, "Login por e-mail realizado.");
                            irParaConsultas();
                        } else {
                            String erro = task.getException() != null
                                    ? task.getException().getMessage()
                                    : "Erro desconhecido";

                            Toast.makeText(
                                    this,
                                    "Não foi possível entrar: " + erro,
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                    });
        });

        findViewById(R.id.btnGoogle).setOnClickListener(v -> {
            Log.d(TAG, "Iniciando login com Google.");
            googleLauncher.launch(
                    mGoogleSignInClient.getSignInIntent()
            );
        });
    }

    private void irParaConsultas() {
        Intent intent = new Intent(this, ConsultasActivity.class);
        intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );
        startActivity(intent);
        finish();
    }

    private final ActivityResultLauncher<Intent> googleLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {

                        Log.d(TAG, "Retorno do login Google: "
                                + result.getResultCode());

                        if (result.getData() == null) {
                            Toast.makeText(
                                    this,
                                    "Não foi possível obter a resposta do Google.",
                                    Toast.LENGTH_LONG
                            ).show();
                            return;
                        }

                        Task<GoogleSignInAccount> task =
                                GoogleSignIn.getSignedInAccountFromIntent(
                                        result.getData()
                                );

                        try {
                            GoogleSignInAccount account =
                                    task.getResult(ApiException.class);

                            String idToken = account.getIdToken();

                            if (idToken == null) {
                                Toast.makeText(
                                        this,
                                        "Não foi possível obter o token do Google.",
                                        Toast.LENGTH_LONG
                                ).show();
                                return;
                            }

                            AuthCredential credential =
                                    GoogleAuthProvider.getCredential(idToken, null);

                            mAuth.signInWithCredential(credential)
                                    .addOnCompleteListener(this, authTask -> {

                                        if (authTask.isSuccessful()) {
                                            Log.d(TAG, "Login Google/Firebase OK.");
                                            irParaConsultas();
                                        } else {
                                            String erro =
                                                    authTask.getException() != null
                                                            ? authTask.getException().getMessage()
                                                            : "Erro desconhecido";

                                            Log.e(TAG, "Erro Firebase: " + erro);

                                            Toast.makeText(
                                                    this,
                                                    "Falha no login Google: " + erro,
                                                    Toast.LENGTH_LONG
                                            ).show();
                                        }
                                    });

                        } catch (ApiException e) {
                            Log.e(TAG, "Erro no login Google: "
                                    + e.getStatusCode(), e);

                            Toast.makeText(
                                    this,
                                    "Falha no login Google. Código: "
                                            + e.getStatusCode(),
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                    }
            );
}

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
    private EditText editEmail, editSenha;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        editEmail = findViewById(R.id.editEmailLogin);
        editSenha = findViewById(R.id.editSenhaLogin);

        GoogleSignInOptions gso = new GoogleSignInOptions
                .Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        findViewById(R.id.btnVoltarLogin).setOnClickListener(v -> finish());

        findViewById(R.id.btnEntrar).setOnClickListener(v -> {
            String email = editEmail.getText().toString().trim();
            String senha = editSenha.getText().toString().trim();

            if (email.isEmpty()) { editEmail.setError("Informe o e-mail"); return; }
            if (senha.isEmpty()) { editSenha.setError("Informe a senha"); return; }

            mAuth.signInWithEmailAndPassword(email, senha)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "Login e-mail OK");
                            irParaConsultas();
                        } else {
                            String erro = task.getException() != null
                                    ? task.getException().getMessage() : "Erro desconhecido";
                            Toast.makeText(this, "Erro: " + erro, Toast.LENGTH_LONG).show();
                        }
                    });
        });

        findViewById(R.id.btnGoogle).setOnClickListener(v -> {
            Log.d(TAG, "Clicou Google - lançando intent");
            googleLauncher.launch(mGoogleSignInClient.getSignInIntent());
        });
    }


    private void irParaConsultas() {
        Intent intent = new Intent(this, ConsultasActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private final ActivityResultLauncher<Intent> googleLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                Log.d(TAG, "Retorno Google - resultCode: " + result.getResultCode());

                if (result.getData() == null) {
                    Log.e(TAG, "getData() nulo");
                    Toast.makeText(this, "Erro: resposta nula do Google", Toast.LENGTH_LONG).show();
                    return;
                }

                Task<GoogleSignInAccount> task =
                        GoogleSignIn.getSignedInAccountFromIntent(result.getData());

                try {
                    GoogleSignInAccount account = task.getResult(ApiException.class);
                    String idToken = account.getIdToken();
                    Log.d(TAG, "Token obtido, autenticando no Firebase...");

                    AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);

                    mAuth.signInWithCredential(credential)
                            .addOnCompleteListener(this, authTask -> {
                                if (authTask.isSuccessful()) {
                                    Log.d(TAG, "Firebase auth OK!");
                                    irParaConsultas();
                                } else {
                                    String erro = authTask.getException() != null
                                            ? authTask.getException().getMessage() : "Erro desconhecido";
                                    Log.e(TAG, "Erro Firebase: " + erro);
                                    Toast.makeText(this, "Erro Firebase: " + erro, Toast.LENGTH_LONG).show();
                                }
                            });

                } catch (ApiException e) {
                    Log.e(TAG, "ApiException código: " + e.getStatusCode());
                    Toast.makeText(this, "Erro Google código " + e.getStatusCode(), Toast.LENGTH_LONG).show();
                }
            });
}

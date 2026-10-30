
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
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LOGIN_DEBUG";

    // Deve ser exatamente a mesma chave utilizada na MainActivity.
    private static final String EXTRA_COMPLETAR_CADASTRO_GOOGLE =
            "com.example.clinicavidaplus.COMPLETAR_CADASTRO_GOOGLE";

    private GoogleSignInClient mGoogleSignInClient;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private EditText editEmail;
    private EditText editSenha;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        editEmail = findViewById(R.id.edtEmail);
        editSenha = findViewById(R.id.edtSenha);

        configurarLoginGoogle();
        configurarBotoes();

    }

    private void configurarLoginGoogle() {

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(
                GoogleSignInOptions.DEFAULT_SIGN_IN
        )
                .requestIdToken(
                        getString(R.string.default_web_client_id)
                )
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
    }

    private void configurarBotoes() {

        findViewById(R.id.btnVoltarLogin).setOnClickListener(v -> {
            Intent intent = new Intent(
                    LoginActivity.this,
                    IndexActivity.class
            );

            startActivity(intent);
            finish();
        });

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

        // Login com e-mail e senha.
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

                            verificarPerfilEEncaminhar();

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

        // Login com Google.
        findViewById(R.id.btnGoogle).setOnClickListener(v -> {

            Log.d(TAG, "Iniciando login com Google.");

            googleLauncher.launch(
                    mGoogleSignInClient.getSignInIntent()
            );
        });
    }

    /**
     * Verifica a credencial administrativa e depois consulta
     * os documentos de médico e paciente.
     */
    private void verificarPerfilEEncaminhar() {

        FirebaseUser usuario = mAuth.getCurrentUser();

        if (usuario == null) {
            Toast.makeText(
                    this,
                    "Não foi possível identificar o usuário.",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        // Atualiza o token para consultar as credenciais mais recentes.
        usuario.getIdToken(true)
                .addOnSuccessListener(resultado -> {

                    Object permissaoAdmin =
                            resultado.getClaims().get("admin");

                    if (Boolean.TRUE.equals(permissaoAdmin)) {

                        Log.d(TAG, "Perfil administrativo identificado.");

                        abrirTela(AdminMedicosActivity.class);
                        return;
                    }

                    verificarPerfilMedico(usuario);

                })
                .addOnFailureListener(e -> {

                    Log.e(TAG, "Erro ao verificar credenciais.", e);

                    Toast.makeText(
                            this,
                            "Não foi possível verificar seu perfil.",
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private void verificarPerfilMedico(FirebaseUser usuario) {

        String uid = usuario.getUid();

        db.collection("medicos")
                .document(uid)
                .get()
                .addOnSuccessListener(documentoMedico -> {

                    if (documentoMedico.exists()) {

                        Boolean autorizado =
                                documentoMedico.getBoolean("autorizado");

                        if (Boolean.TRUE.equals(autorizado)) {

                            Log.d(TAG, "Médico autorizado.");

                            abrirTela(ListaConsultasMedicoActivity.class);

                        } else {

                            Toast.makeText(
                                    this,
                                    "Seu acesso como médico ainda não foi autorizado.",
                                    Toast.LENGTH_LONG
                            ).show();
                        }

                        return;
                    }

                    verificarPerfilPaciente(usuario);
                })
                .addOnFailureListener(e -> {

                    Log.e(TAG, "Erro ao consultar perfil médico.", e);

                    Toast.makeText(
                            this,
                            "Não foi possível consultar seu perfil médico. "
                                    + "Verifique a conexão e as permissões do Firebase.",
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private void verificarPerfilPaciente(FirebaseUser usuario) {

        String uid = usuario.getUid();

        db.collection("pacientes")
                .document(uid)
                .get()
                .addOnSuccessListener(documentoPaciente -> {

                    if (documentoPaciente.exists()) {

                        Log.d(TAG, "Perfil de paciente identificado.");

                        abrirTela(ListaConsultasActivity.class);

                        return;
                    }

                    if (usuarioEntrouComGoogle(usuario)) {

                        Log.d(
                                TAG,
                                "Usuário Google precisa completar o cadastro."
                        );

                        abrirCadastroGoogle();

                    } else {

                        Toast.makeText(
                                this,
                                "Cadastro de paciente não encontrado. "
                                        + "Faça seu cadastro para continuar.",
                                Toast.LENGTH_LONG
                        ).show();

                        mAuth.signOut();
                    }
                })
                .addOnFailureListener(e -> {

                    Log.e(TAG, "Erro ao consultar perfil do paciente.", e);

                    Toast.makeText(
                            this,
                            "Não foi possível consultar seu cadastro.",
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private boolean usuarioEntrouComGoogle(FirebaseUser usuario) {

        for (com.google.firebase.auth.UserInfo informacao
                : usuario.getProviderData()) {

            if ("google.com".equals(informacao.getProviderId())) {
                return true;
            }
        }

        return false;
    }

    private void abrirCadastroGoogle() {

        Intent intent = new Intent(
                LoginActivity.this,
                MainActivity.class
        );

        intent.putExtra(
                EXTRA_COMPLETAR_CADASTRO_GOOGLE,
                true
        );

        intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);
        finish();
    }

    private void abrirTela(Class<?> destino) {

        Intent intent = new Intent(
                LoginActivity.this,
                destino
        );

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

                        Log.d(
                                TAG,
                                "Retorno do login Google: "
                                        + result.getResultCode()
                        );

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
                                    GoogleAuthProvider.getCredential(
                                            idToken,
                                            null
                                    );

                            mAuth.signInWithCredential(credential)
                                    .addOnCompleteListener(this, authTask -> {

                                        if (authTask.isSuccessful()) {

                                            Log.d(
                                                    TAG,
                                                    "Login Google/Firebase OK."
                                            );

                                            verificarPerfilEEncaminhar();

                                        } else {

                                            String erro =
                                                    authTask.getException() != null
                                                            ? authTask.getException()
                                                            .getMessage()
                                                            : "Erro desconhecido";

                                            Log.e(
                                                    TAG,
                                                    "Erro Firebase: " + erro
                                            );

                                            Toast.makeText(
                                                    this,
                                                    "Falha no login Google: " + erro,
                                                    Toast.LENGTH_LONG
                                            ).show();
                                        }
                                    });

                        } catch (ApiException e) {

                            Log.e(
                                    TAG,
                                    "Erro no login Google: " + e.getStatusCode(),
                                    e
                            );

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

package com.example.clinicavidaplus;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private EditText editNome;
    private EditText editEmail;
    private EditText editTelefone;
    private EditText editNascimento;
    private EditText editSenha;

    private Button btnCadastrar;
    private Button btnVoltar;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inicializa os serviços do Firebase.
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Localiza os componentes da tela.
        inicializarComponentes();

        // Encerra uma sessão anterior para iniciar um novo cadastro.
        auth.signOut();

        // Retorna à tela anterior.
        btnVoltar.setOnClickListener(v -> finish());

        // Abre o calendário para selecionar a data de nascimento.
        editNascimento.setFocusable(false);
        editNascimento.setClickable(true);

        editNascimento.setOnClickListener(v -> abrirCalendario());

        // Executa o cadastro do paciente.
        btnCadastrar.setOnClickListener(v -> {
            String nome = editNome.getText().toString().trim();
            String email = editEmail.getText().toString().trim();
            String telefone = editTelefone.getText().toString().trim();
            String nascimento = editNascimento.getText().toString().trim();
            String senha = editSenha.getText().toString();

            if (validarCampos(nome, email, telefone, nascimento, senha)) {
                btnCadastrar.setEnabled(false);

                cadastrarNovoPaciente(
                        nome,
                        email,
                        telefone,
                        nascimento,
                        senha
                );
            }
        });
    }

    private void inicializarComponentes() {
        editNome = findViewById(R.id.editNomeCadastro);
        editEmail = findViewById(R.id.editEmailCadastro);
        editTelefone = findViewById(R.id.editTelefoneCadastro);
        editNascimento = findViewById(R.id.editNascimentoCadastro);
        editSenha = findViewById(R.id.editSenhaCadastro);

        btnCadastrar = findViewById(R.id.btnCadastrar);
        btnVoltar = findViewById(R.id.btnVoltarCadastro);
    }

    private void abrirCalendario() {
        Calendar calendario = Calendar.getInstance();

        DatePickerDialog seletorData = new DatePickerDialog(
                this,
                (view, ano, mes, dia) -> {
                    String data = String.format(
                            Locale.getDefault(),
                            "%02d/%02d/%04d",
                            dia,
                            mes + 1,
                            ano
                    );

                    editNascimento.setText(data);
                    editNascimento.setError(null);
                },
                calendario.get(Calendar.YEAR),
                calendario.get(Calendar.MONTH),
                calendario.get(Calendar.DAY_OF_MONTH)
        );

        // Impede a seleção de uma data futura.
        seletorData.getDatePicker().setMaxDate(
                System.currentTimeMillis()
        );

        seletorData.show();
    }

    private boolean validarCampos(
            String nome,
            String email,
            String telefone,
            String nascimento,
            String senha
    ) {
        if (nome.isEmpty()) {
            editNome.setError("O nome é obrigatório");
            editNome.requestFocus();
            return false;
        }

        if (email.isEmpty()) {
            editEmail.setError("O e-mail é obrigatório");
            editEmail.requestFocus();
            return false;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            editEmail.setError("Informe um e-mail válido");
            editEmail.requestFocus();
            return false;
        }

        if (telefone.isEmpty()) {
            editTelefone.setError("O telefone é obrigatório");
            editTelefone.requestFocus();
            return false;
        }

        if (nascimento.isEmpty()) {
            editNascimento.setError("A data de nascimento é obrigatória");
            editNascimento.requestFocus();
            return false;
        }

        if (senha.isEmpty()) {
            editSenha.setError("A senha é obrigatória");
            editSenha.requestFocus();
            return false;
        }

        if (senha.length() < 6) {
            editSenha.setError(
                    "A senha deve ter no mínimo 6 caracteres"
            );
            editSenha.requestFocus();
            return false;
        }

        return true;
    }

    private void cadastrarNovoPaciente(
            String nome,
            String email,
            String telefone,
            String nascimento,
            String senha
    ) {
        auth.createUserWithEmailAndPassword(email, senha)
                .addOnCompleteListener(this, tarefa -> {

                    if (tarefa.isSuccessful()) {

                        if (auth.getCurrentUser() == null) {
                            btnCadastrar.setEnabled(true);

                            Toast.makeText(
                                    MainActivity.this,
                                    "Usuário criado, mas não foi possível obter sua sessão.",
                                    Toast.LENGTH_LONG
                            ).show();

                            return;
                        }

                        String uid = auth.getCurrentUser().getUid();

                        Map<String, Object> paciente = new HashMap<>();
                        paciente.put("nome", nome);
                        paciente.put("email", email);
                        paciente.put("telefone", telefone);
                        paciente.put("nascimento", nascimento);

                        db.collection("pacientes")
                                .document(uid)
                                .set(paciente)
                                .addOnSuccessListener(resultado -> {

                                    Toast.makeText(
                                            MainActivity.this,
                                            "Paciente cadastrado com sucesso!",
                                            Toast.LENGTH_SHORT
                                    ).show();

                                    abrirLogin();
                                })
                                .addOnFailureListener(erro -> {

                                    btnCadastrar.setEnabled(true);

                                    Toast.makeText(
                                            MainActivity.this,
                                            "A conta foi criada, mas houve falha ao salvar os dados no Firestore: "
                                                    + erro.getLocalizedMessage(),
                                            Toast.LENGTH_LONG
                                    ).show();
                                });

                    } else {
                        btnCadastrar.setEnabled(true);

                        String mensagem = tarefa.getException() != null
                                ? tarefa.getException().getLocalizedMessage()
                                : "Erro desconhecido";

                        Toast.makeText(
                                MainActivity.this,
                                "Erro ao cadastrar: " + mensagem,
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
    }

    private void abrirLogin() {
        Intent intent = new Intent(
                MainActivity.this,
                LoginActivity.class
        );

        startActivity(intent);
        finish();
    }
}

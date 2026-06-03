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

public class MainActivity extends AppCompatActivity {

    private EditText editNome, editEmail, editTelefone, editNascimento, editSenha;
    private Button btnCadastrar, btnVoltar;
    private FirebaseAuth auth = FirebaseAuth.getInstance();
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        inicializarComponentes();

        // Encerra qualquer sessão anterior
        auth.signOut();

        // Botão Voltar
        btnVoltar.setOnClickListener(v -> finish());

        // Abre DatePickerDialog ao clicar no campo de nascimento
        editNascimento.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                String data = String.format("%02d/%02d/%04d", dayOfMonth, (month + 1), year);
                editNascimento.setText(data);
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
        });

        // Botão Cadastrar
        btnCadastrar.setOnClickListener(v -> {
            String nome      = editNome.getText().toString().trim();
            String email     = editEmail.getText().toString().trim();
            String telefone  = editTelefone.getText().toString().trim();
            String nascimento = editNascimento.getText().toString().trim();
            String senha     = editSenha.getText().toString().trim();

            if (validarCampos(nome, email, telefone, nascimento, senha)) {
                cadastrarNovoPaciente(nome, email, telefone, nascimento, senha);
            }
        });
    }

    private void cadastrarNovoPaciente(String nome, String email, String telefone,
                                       String nascimento, String senha) {
        auth.createUserWithEmailAndPassword(email, senha)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Salva os dados extras no Firestore
                        String uid = auth.getCurrentUser().getUid();
                        Map<String, Object> paciente = new HashMap<>();
                        paciente.put("nome", nome);
                        paciente.put("email", email);
                        paciente.put("telefone", telefone);
                        paciente.put("nascimento", nascimento);

                        db.collection("pacientes").document(uid).set(paciente)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this, "Paciente cadastrado com sucesso!",
                                            Toast.LENGTH_SHORT).show();
                                    startActivity(new Intent(MainActivity.this, LoginActivity.class));
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    // Auth criado, mas falhou ao salvar no Firestore
                                    Toast.makeText(this,
                                            "Cadastrado, mas erro ao salvar dados: " + e.getMessage(),
                                            Toast.LENGTH_LONG).show();
                                    startActivity(new Intent(MainActivity.this, LoginActivity.class));
                                    finish();
                                });
                    } else {
                        String erro = task.getException() != null
                                ? task.getException().getMessage() : "Erro desconhecido";
                        Toast.makeText(this, "Erro ao cadastrar: " + erro, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private boolean validarCampos(String nome, String email, String telefone,
                                  String nascimento, String senha) {
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
            editSenha.setError("A senha deve ter no mínimo 6 caracteres");
            editSenha.requestFocus();
            return false;
        }
        return true;
    }

    private void inicializarComponentes() {
        editNome       = findViewById(R.id.editNomeCadastro);
        editEmail      = findViewById(R.id.editEmailCadastro);
        editTelefone   = findViewById(R.id.editTelefoneCadastro);
        editNascimento = findViewById(R.id.editNascimentoCadastro);
        editSenha      = findViewById(R.id.editSenhaCadastro);
        btnCadastrar   = findViewById(R.id.btnCadastrar);
        btnVoltar      = findViewById(R.id.btnVoltarCadastro);
    }
}

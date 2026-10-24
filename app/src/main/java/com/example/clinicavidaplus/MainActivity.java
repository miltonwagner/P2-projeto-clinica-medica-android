
package com.example.clinicavidaplus;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private EditText editNome;
    private EditText editEmail;
    private EditText editTelefone;
    private EditText editNascimento;
    private ImageButton btnCalendarioNascimento;
    private EditText editSenha;

    private Button btnCadastrar;
    private Button btnVoltar;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private boolean alterandoDataNascimento = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        inicializarComponentes();
        configurarMascaraDataNascimento();

        auth.signOut();

        btnVoltar.setOnClickListener(v -> finish());

        btnCalendarioNascimento.setOnClickListener(
                v -> abrirCalendario()
        );

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
        btnCalendarioNascimento = findViewById(R.id.btnCalendarioNascimento);
        editSenha = findViewById(R.id.editSenhaCadastro);

        btnCadastrar = findViewById(R.id.btnCadastrar);
        btnVoltar = findViewById(R.id.btnVoltarCadastro);
    }

    private void configurarMascaraDataNascimento() {
        editNascimento.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(
                    CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(
                    CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (alterandoDataNascimento) {
                    return;
                }

                alterandoDataNascimento = true;

                String numeros = s.toString().replaceAll("[^0-9]", "");

                if (numeros.length() > 8) {
                    numeros = numeros.substring(0, 8);
                }

                StringBuilder dataFormatada = new StringBuilder();

                if (numeros.length() > 0) {
                    dataFormatada.append(
                            numeros.substring(0, Math.min(2, numeros.length()))
                    );
                }

                if (numeros.length() > 2) {
                    dataFormatada.append("/");
                    dataFormatada.append(
                            numeros.substring(2, Math.min(4, numeros.length()))
                    );
                }

                if (numeros.length() > 4) {
                    dataFormatada.append("/");
                    dataFormatada.append(
                            numeros.substring(4, Math.min(8, numeros.length()))
                    );
                }

                String resultado = dataFormatada.toString();

                if (!resultado.equals(s.toString())) {
                    editNascimento.setText(resultado);
                    editNascimento.setSelection(resultado.length());
                }

                alterandoDataNascimento = false;
            }
        });
    }

    private void abrirCalendario() {
        Calendar calendario = Calendar.getInstance();

        DatePickerDialog seletorData = new DatePickerDialog(
                this,
                (view, ano, mes, dia) -> {
                    String data = String.format(
                            Locale.ROOT,
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
            editNome.setError("O nome e obrigatorio");
            editNome.requestFocus();
            return false;
        }

        if (email.isEmpty()) {
            editEmail.setError("O e-mail e obrigatorio");
            editEmail.requestFocus();
            return false;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            editEmail.setError("Informe um e-mail valido");
            editEmail.requestFocus();
            return false;
        }

        if (telefone.isEmpty()) {
            editTelefone.setError("O telefone e obrigatorio");
            editTelefone.requestFocus();
            return false;
        }

        if (nascimento.isEmpty()) {
            editNascimento.setError("A data de nascimento e obrigatoria");
            editNascimento.requestFocus();
            return false;
        }

        if (!validarDataNascimento(nascimento)) {
            editNascimento.setError(
                    "Informe uma data valida, anterior ou igual a hoje"
            );
            editNascimento.requestFocus();
            return false;
        }

        if (senha.isEmpty()) {
            editSenha.setError("A senha e obrigatoria");
            editSenha.requestFocus();
            return false;
        }

        if (senha.length() < 6) {
            editSenha.setError("A senha deve ter no minimo 6 caracteres");
            editSenha.requestFocus();
            return false;
        }

        return true;
    }

    private boolean validarDataNascimento(String nascimento) {
        if (!nascimento.matches("\\d{2}/\\d{2}/\\d{4}")) {
            return false;
        }

        SimpleDateFormat formato =
                new SimpleDateFormat("dd/MM/yyyy", Locale.ROOT);
        formato.setLenient(false);

        ParsePosition posicao = new ParsePosition(0);
        Date data = formato.parse(nascimento, posicao);

        if (data == null || posicao.getIndex() != nascimento.length()) {
            return false;
        }

        Calendar dataInformada = Calendar.getInstance();
        dataInformada.setTime(data);
        zerarHorario(dataInformada);

        Calendar hoje = Calendar.getInstance();
        zerarHorario(hoje);

        return !dataInformada.after(hoje);
    }

    private void zerarHorario(Calendar calendario) {
        calendario.set(Calendar.HOUR_OF_DAY, 0);
        calendario.set(Calendar.MINUTE, 0);
        calendario.set(Calendar.SECOND, 0);
        calendario.set(Calendar.MILLISECOND, 0);
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
                                    "Usuario criado, mas nao foi possivel obter a sessao.",
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
                                            "Conta criada, mas houve falha ao salvar os dados: "
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

package com.example.clinicavidaplus;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    public static final String EXTRA_COMPLETAR_CADASTRO_GOOGLE =
            "com.example.clinicavidaplus.COMPLETAR_CADASTRO_GOOGLE";

    private EditText editNome;
    private EditText editEmail;
    private EditText editTelefone;
    private EditText editNascimento;
    private EditText editSenha;

    private ImageButton btnCalendarioNascimento;
    private Button btnCadastrar;
    private Button btnVoltar;
    private TextView txtLabelSenha;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private boolean alterandoDataNascimento = false;
    private boolean modoGoogle = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        modoGoogle = getIntent().getBooleanExtra(
                EXTRA_COMPLETAR_CADASTRO_GOOGLE,
                false
        );

        inicializarComponentes();
        configurarMascaraDataNascimento();

        if (modoGoogle) {
            configurarCadastroGoogle();
        } else {
            // No cadastro tradicional, não manter uma sessão anterior.
            auth.signOut();
        }

        btnVoltar.setOnClickListener(v -> finish());

        btnCalendarioNascimento.setOnClickListener(
                v -> abrirCalendario()
        );

        btnCadastrar.setOnClickListener(v -> processarCadastro());
    }

    private void inicializarComponentes() {
        editNome = findViewById(R.id.editNomeCadastro);
        editEmail = findViewById(R.id.editEmailCadastro);
        editTelefone = findViewById(R.id.editTelefoneCadastro);
        editNascimento = findViewById(R.id.editNascimentoCadastro);
        editSenha = findViewById(R.id.editSenhaCadastro);

        btnCalendarioNascimento =
                findViewById(R.id.btnCalendarioNascimento);

        btnCadastrar = findViewById(R.id.btnCadastrar);
        btnVoltar = findViewById(R.id.btnVoltarCadastro);

        txtLabelSenha = findViewById(R.id.txtLabelSenhaCadastro);
    }

    private void configurarCadastroGoogle() {
        FirebaseUser usuario = auth.getCurrentUser();

        if (usuario == null) {
            Toast.makeText(
                    this,
                    "Sua sessão Google não foi encontrada. Entre novamente.",
                    Toast.LENGTH_LONG
            ).show();

            finish();
            return;
        }

        // Preenche os dados disponíveis na conta Google.
        if (usuario.getDisplayName() != null) {
            editNome.setText(usuario.getDisplayName());
        }

        if (usuario.getEmail() != null) {
            editEmail.setText(usuario.getEmail());
        }

        // O e-mail pertence à conta autenticada.
        editEmail.setEnabled(false);
        editEmail.setFocusable(false);
        editEmail.setClickable(false);

        // Não solicitar senha no cadastro complementar.
        editSenha.setVisibility(View.GONE);
        txtLabelSenha.setVisibility(View.GONE);

        btnCadastrar.setText("FINALIZAR CADASTRO");

        Toast.makeText(
                this,
                "Complete seus dados para continuar.",
                Toast.LENGTH_SHORT
        ).show();
    }

    private void processarCadastro() {
        String nome = editNome.getText().toString().trim();
        String email = editEmail.getText().toString().trim();
        String telefone = editTelefone.getText().toString().trim();
        String nascimento = editNascimento.getText().toString().trim();
        String senha = editSenha.getText().toString();

        boolean camposValidos;

        if (modoGoogle) {
            camposValidos = validarCamposComGoogle(
                    nome,
                    email,
                    telefone,
                    nascimento
            );
        } else {
            camposValidos = validarCampos(
                    nome,
                    email,
                    telefone,
                    nascimento,
                    senha
            );
        }

        if (!camposValidos) {
            return;
        }

        btnCadastrar.setEnabled(false);

        if (modoGoogle) {
            salvarComplementoGoogle(
                    nome,
                    email,
                    telefone,
                    nascimento
            );
        } else {
            cadastrarNovoPaciente(
                    nome,
                    email,
                    telefone,
                    nascimento,
                    senha
            );
        }
    }

    private void configurarMascaraDataNascimento() {
        editNascimento.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(
                    CharSequence s,
                    int start,
                    int count,
                    int after
            ) {
            }

            @Override
            public void onTextChanged(
                    CharSequence s,
                    int start,
                    int before,
                    int count
            ) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (alterandoDataNascimento) {
                    return;
                }

                alterandoDataNascimento = true;

                String numeros = s.toString()
                        .replaceAll("[^0-9]", "");

                if (numeros.length() > 8) {
                    numeros = numeros.substring(0, 8);
                }

                StringBuilder dataFormatada = new StringBuilder();

                if (numeros.length() > 0) {
                    dataFormatada.append(
                            numeros.substring(
                                    0,
                                    Math.min(2, numeros.length())
                            )
                    );
                }

                if (numeros.length() > 2) {
                    dataFormatada.append("/");
                    dataFormatada.append(
                            numeros.substring(
                                    2,
                                    Math.min(4, numeros.length())
                            )
                    );
                }

                if (numeros.length() > 4) {
                    dataFormatada.append("/");
                    dataFormatada.append(
                            numeros.substring(4, 8)
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

    private boolean validarCamposComGoogle(
            String nome,
            String email,
            String telefone,
            String nascimento
    ) {
        if (!validarDadosBasicos(
                nome,
                email,
                telefone,
                nascimento
        )) {
            return false;
        }

        return true;
    }

    private boolean validarCampos(
            String nome,
            String email,
            String telefone,
            String nascimento,
            String senha
    ) {
        if (!validarDadosBasicos(
                nome,
                email,
                telefone,
                nascimento
        )) {
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

    private boolean validarDadosBasicos(
            String nome,
            String email,
            String telefone,
            String nascimento
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

        if (!android.util.Patterns.EMAIL_ADDRESS
                .matcher(email).matches()) {
            editEmail.setError("Informe um e-mail válido");
            editEmail.requestFocus();
            return false;
        }

        if (telefone.isEmpty()) {
            editTelefone.setError("O telefone é obrigatório");
            editTelefone.requestFocus();
            return false;
        }

        String numerosTelefone = telefone.replaceAll("[^0-9]", "");

        if (numerosTelefone.length() < 10
                || numerosTelefone.length() > 13) {
            editTelefone.setError("Informe um telefone válido");
            editTelefone.requestFocus();
            return false;
        }

        if (nascimento.isEmpty()) {
            editNascimento.setError(
                    "A data de nascimento é obrigatória"
            );
            editNascimento.requestFocus();
            return false;
        }

        if (!validarDataNascimento(nascimento)) {
            editNascimento.setError(
                    "Informe uma data válida, anterior ou igual a hoje"
            );
            editNascimento.requestFocus();
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

        if (data == null
                || posicao.getIndex() != nascimento.length()) {
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

    private void salvarComplementoGoogle(
            String nome,
            String email,
            String telefone,
            String nascimento
    ) {
        FirebaseUser usuario = auth.getCurrentUser();

        if (usuario == null) {
            btnCadastrar.setEnabled(true);

            Toast.makeText(
                    this,
                    "Sessão expirada. Entre novamente com o Google.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        String uid = usuario.getUid();

        Map<String, Object> paciente = new HashMap<>();
        paciente.put("nome", nome);
        paciente.put("email", email);
        paciente.put("telefone", telefone);
        paciente.put("nascimento", nascimento);

        db.collection("pacientes")
                .document(uid)
                .get()
                .addOnSuccessListener(documento -> {

                    if (documento.exists()) {
                        btnCadastrar.setEnabled(true);

                        Toast.makeText(
                                this,
                                "Este cadastro já está completo.",
                                Toast.LENGTH_LONG
                        ).show();

                        abrirListaConsultas();
                        return;
                    }

                    db.collection("pacientes")
                            .document(uid)
                            .set(paciente)
                            .addOnSuccessListener(resultado -> {
                                Toast.makeText(
                                        this,
                                        "Cadastro Google concluído com sucesso!",
                                        Toast.LENGTH_LONG
                                ).show();

                                abrirListaConsultas();
                            })
                            .addOnFailureListener(erro -> {
                                btnCadastrar.setEnabled(true);

                                Toast.makeText(
                                        this,
                                        "Erro ao salvar os dados: "
                                                + erro.getLocalizedMessage(),
                                        Toast.LENGTH_LONG
                                ).show();
                            });
                })
                .addOnFailureListener(erro -> {
                    btnCadastrar.setEnabled(true);

                    Toast.makeText(
                            this,
                            "Não foi possível verificar seu cadastro: "
                                    + erro.getLocalizedMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
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

                        FirebaseUser usuario = auth.getCurrentUser();

                        if (usuario == null) {
                            btnCadastrar.setEnabled(true);

                            Toast.makeText(
                                    this,
                                    "Usuário criado, mas não foi possível obter a sessão.",
                                    Toast.LENGTH_LONG
                            ).show();

                            return;
                        }

                        String uid = usuario.getUid();

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
                                            this,
                                            "Paciente cadastrado com sucesso!",
                                            Toast.LENGTH_SHORT
                                    ).show();

                                    abrirLogin();
                                })
                                .addOnFailureListener(erro -> {
                                    btnCadastrar.setEnabled(true);

                                    Toast.makeText(
                                            this,
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
                                this,
                                "Erro ao cadastrar: " + mensagem,
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
    }

    private void abrirListaConsultas() {
        Intent intent = new Intent(
                MainActivity.this,
                ListaConsultasActivity.class
        );

        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);
        finish();
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

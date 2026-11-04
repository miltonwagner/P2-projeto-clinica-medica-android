package com.example.clinicavidaplus;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

/**
 * Entity do ROOM: tabela "pacientes" do banco local clinica_db.
 * Guarda no aparelho uma cópia do cadastro do paciente (o original fica no Firestore),
 * para o perfil abrir na hora e funcionar mesmo sem internet.
 */
@Entity(tableName = "pacientes")
public class Paciente {

    /** Mesmo UID do Firebase Authentication. */
    @PrimaryKey
    @NonNull
    private String uid = "";
    private String nome;
    private String email;
    private String telefone;
    private String dataNascimento;
    /** Momento (ms) da última sincronização com o Firestore. */
    private long atualizadoEm;

    public Paciente() { }

    @Ignore
    public Paciente(@NonNull String uid, String nome, String email, String telefone, String dataNascimento) {
        this.uid = uid;
        this.nome = nome;
        this.email = email;
        this.telefone = telefone;
        this.dataNascimento = dataNascimento;
        this.atualizadoEm = System.currentTimeMillis();
    }

    @NonNull
    public String getUid() { return uid; }
    public void setUid(@NonNull String uid) { this.uid = uid; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }
    public String getDataNascimento() { return dataNascimento; }
    public void setDataNascimento(String dataNascimento) { this.dataNascimento = dataNascimento; }
    public long getAtualizadoEm() { return atualizadoEm; }
    public void setAtualizadoEm(long atualizadoEm) { this.atualizadoEm = atualizadoEm; }
}

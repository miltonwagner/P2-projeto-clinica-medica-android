package com.example.clinicavidaplus;

public class Paciente {
    private String nome;
    private String email;
    private String telefone;

    // Construtor vazio obrigatório para o Firebase
    public Paciente() { }

    public Paciente(String nome, String email, String telefone) {
        this.nome = nome;
        this.email = email;
        this.telefone = telefone;
    }

    // Getters e Setters (para o Firebase conseguir ler e gravar os dados)
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }
}

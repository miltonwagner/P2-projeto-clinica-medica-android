package com.example.clinicavidaplus;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

/** DAO do ROOM: operações SQL na tabela "pacientes" do banco local. */
@Dao
public interface PacienteDao {

    /** Insere ou atualiza (mesmo UID substitui). */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void salvar(Paciente paciente);

    @Query("SELECT * FROM pacientes WHERE uid = :uid LIMIT 1")
    Paciente buscar(String uid);

    @Query("SELECT * FROM pacientes ORDER BY atualizadoEm DESC")
    List<Paciente> listar();

    @Query("DELETE FROM pacientes WHERE uid = :uid")
    void apagar(String uid);
}

package com.example.clinicavidaplus;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

/** Banco local ROOM (SQLite) "clinica_db" — instância única (Singleton). */
@Database(entities = {Paciente.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase instancia;

    public abstract PacienteDao pacienteDao();

    public static AppDatabase get(Context contexto) {
        if (instancia == null) {
            synchronized (AppDatabase.class) {
                if (instancia == null) {
                    instancia = Room.databaseBuilder(contexto.getApplicationContext(),
                                    AppDatabase.class, "clinica_db")
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return instancia;
    }
}

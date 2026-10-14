package com.example.psyconet;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class UpcomingAppointmentsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upcoming_appointments);

        // Configuração da RecyclerView
        recyclerView = findViewById(R.id.recyclerAppointments);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Botão de voltar no cabeçalho
        View btnVoltar = findViewById(R.id.btnVoltar);
        if (btnVoltar != null) {
            btnVoltar.setOnClickListener(v -> finish());
        }

        // Inicialização do Firebase Firestore
        db = FirebaseFirestore.getInstance();
        loadAppointments();

        // Configuração da navegação do rodapé
        setupBottomNavigation();
    }

    private void loadAppointments() {
        db.collection("agendamentos")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Appointment> appointmentList = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String data = document.getString("data");
                        String psicologo = document.getString("psicologo");

                        if (data != null && psicologo != null) {
                            appointmentList.add(new Appointment(psicologo, data));
                        }
                    }

                    AppointmentAdapter adapter = new AppointmentAdapter(appointmentList);
                    recyclerView.setAdapter(adapter);
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Erro ao carregar dados.", Toast.LENGTH_SHORT).show());
    }

    private void setupBottomNavigation() {
        View navHome = findViewById(R.id.navHome);
        View navAppointments = findViewById(R.id.navAppointments);
        View navProfile = findViewById(R.id.navProfile);

        if (navHome != null) {
            navHome.setOnClickListener(v -> {
                // Ir para a tela principal (MainActivity)
                Intent intent = new Intent(UpcomingAppointmentsActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            });
        }

        if (navAppointments != null) {
            navAppointments.setOnClickListener(v -> {
                // Já estamos na tela de consultas, opcionalmente pode recarregar
                Toast.makeText(this, "Você já está nas Próximas Consultas", Toast.LENGTH_SHORT).show();
            });
        }

        if (navProfile != null) {
            navProfile.setOnClickListener(v -> {
                // Substitua ProfileActivity.class pelo nome real da sua Activity de perfil se houver
                // Intent intent = new Intent(UpcomingAppointmentsActivity.this, ProfileActivity.class);
                // startActivity(intent);
                Toast.makeText(this, "Tela de Perfil em breve", Toast.LENGTH_SHORT).show();
            });
        }
    }

    // 1. Classe Modelo para representar cada agendamento
    public static class Appointment {
        private final String psicologo;
        private final String data;

        public Appointment(String psicologo, String data) {
            this.psicologo = psicologo;
            this.data = data;
        }

        public String getPsicologo() {
            return psicologo;
        }

        public String getData() {
            return data;
        }
    }

    // 2. Adapter configurado para usar o item_appointment_card.xml
    private static class AppointmentAdapter extends RecyclerView.Adapter<AppointmentAdapter.ViewHolder> {
        private final List<Appointment> items;

        public AppointmentAdapter(List<Appointment> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_appointment_card, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Appointment item = items.get(position);
            holder.tvPsicologo.setText(item.getPsicologo());
            holder.tvDataHora.setText(item.getData());
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        // 3. ViewHolder mapeando os componentes do XML item_appointment_card.xml
        public static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvPsicologo;
            TextView tvDataHora;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvPsicologo = itemView.findViewById(R.id.tvPsicologo);
                tvDataHora = itemView.findViewById(R.id.tvDataHora);
            }
        }
    }
}
package com.example.clinicavidaplus;

import android.os.AsyncTask;

public class EmailSender extends AsyncTask<Void, Void, Boolean> {

    private final OnEmailResult callback;

    public interface OnEmailResult {
        void onSuccess();
        void onFailure(String erro);
    }

    public EmailSender(String destinatario, String nomeDestinatario,
                       String medico, String especialidade,
                       String data, String horario,
                       String agendamentoId, OnEmailResult callback) {
        this.callback = callback;
    }

    @Override
    protected Boolean doInBackground(Void... voids) {
        // Envio temporariamente desativado.
        // Nenhuma conexão SMTP será realizada.
        return false;
    }

    @Override
    protected void onPostExecute(Boolean sucesso) {
        if (callback == null) {
            return;
        }

        if (sucesso) {
            callback.onSuccess();
        } else {
            callback.onFailure(
                "Envio de e-mail temporariamente desativado."
            );
        }
    }
}

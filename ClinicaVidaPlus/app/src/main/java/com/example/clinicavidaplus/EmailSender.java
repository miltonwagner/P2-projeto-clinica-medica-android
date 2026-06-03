package com.example.clinicavidaplus;

import android.os.AsyncTask;
import java.util.Properties;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class EmailSender extends AsyncTask<Void, Void, Boolean> {

    private final String destinatario;
    private final String nomeDestinatario;
    private final String medico;
    private final String especialidade;
    private final String data;
    private final String horario;
    private final String agendamentoId;
    private final OnEmailResult callback;

    // ⚠️  As credenciais são lidas via BuildConfig (definidas em local.properties).
    //     NUNCA coloque e-mail ou senha diretamente neste arquivo.
    private static final String EMAIL_REMETENTE = BuildConfig.EMAIL_REMETENTE;
    private static final String SENHA_APP       = BuildConfig.SENHA_APP;

    public interface OnEmailResult {
        void onSuccess();
        void onFailure(String erro);
    }

    public EmailSender(String destinatario, String nomeDestinatario,
                       String medico, String especialidade,
                       String data, String horario,
                       String agendamentoId, OnEmailResult callback) {
        this.destinatario     = destinatario;
        this.nomeDestinatario = nomeDestinatario;
        this.medico           = medico;
        this.especialidade    = especialidade;
        this.data             = data;
        this.horario          = horario;
        this.agendamentoId    = agendamentoId;
        this.callback         = callback;
    }

    @Override
    protected Boolean doInBackground(Void... voids) {
        try {
            Properties props = new Properties();
            props.put("mail.smtp.host", "smtp.gmail.com");
            props.put("mail.smtp.port", "587");
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.ssl.trust", "smtp.gmail.com");

            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(EMAIL_REMETENTE, SENHA_APP);
                }
            });

            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(EMAIL_REMETENTE, "Clínica Vida Plus"));
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(destinatario));
            message.setSubject("✅ Consulta confirmada — " + data + " às " + horario, "UTF-8");
            message.setContent(montarHtmlEmail(), "text/html; charset=utf-8");

            Transport.send(message);
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    protected void onPostExecute(Boolean sucesso) {
        if (sucesso) {
            callback.onSuccess();
        } else {
            callback.onFailure("Falha ao enviar e-mail");
        }
    }

    private String montarHtmlEmail() {
        return "<!DOCTYPE html>" +
                "<html lang='pt-BR'><head><meta charset='UTF-8'></head>" +
                "<body style='margin:0;padding:0;background:#f4f7f9;font-family:Arial,sans-serif;'>" +
                "<table width='100%' cellpadding='0' cellspacing='0' style='background:#f4f7f9;padding:30px 0;'>" +
                "<tr><td align='center'>" +
                "<table width='600' cellpadding='0' cellspacing='0' style='background:#fff;border-radius:10px;overflow:hidden;'>" +
                "<tr><td style='background:#1a8c7f;padding:30px;text-align:center;'>" +
                "<h1 style='color:#fff;margin:0;'>✅ Consulta Confirmada!</h1>" +
                "<p style='color:#b2dfdb;margin:8px 0 0;'>Clínica Vida Plus</p>" +
                "</td></tr>" +
                "<tr><td style='padding:30px;'>" +
                "<p>Olá, <strong>" + nomeDestinatario + "</strong>!</p>" +
                "<p>Seu agendamento foi confirmado. Detalhes:</p>" +
                "<table width='100%' style='background:#e8f5e9;border-radius:8px;border-left:4px solid #1a8c7f;'>" +
                "<tr><td style='padding:20px;'>" +
                "<p>🩺 <strong>Médico:</strong> " + medico + "</p>" +
                "<p>📋 <strong>Especialidade:</strong> " + especialidade + "</p>" +
                "<p>📅 <strong>Data:</strong> " + data + " às " + horario + "</p>" +
                "<p>🔖 <strong>ID:</strong> " + agendamentoId + "</p>" +
                "</td></tr></table>" +
                "</td></tr>" +
                "</table></td></tr></table>" +
                "</body></html>";
    }
}

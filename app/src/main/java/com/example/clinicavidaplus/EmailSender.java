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

    private static final String EMAIL_REMETENTE = "milton.wagner2013@gmail.com";
    private static final String SENHA_APP       = "grlfqohvbnfonqel";

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
            message.setSubject(" Consulta confirmada — " + data + " às " + horario, "UTF-8");
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
                "<table width='600' cellpadding='0' cellspacing='0' style='background:#fff;border-radius:10px;overflow:hidden;box-shadow:0 2px 8px rgba(0,0,0,0.1);'>" +
                "<tr><td style='background:#1a8c7f;padding:30px;text-align:center;'>" +
                "<h1 style='color:#fff;margin:0;font-size:24px;'>✅ Consulta Confirmada!</h1>" +
                "<p style='color:#b2dfdb;margin:8px 0 0;'>Clínica Vida Plus</p>" +
                "</td></tr>" +
                "<tr><td style='padding:30px;'>" +
                "<p style='font-size:16px;color:#333;margin:0 0 20px;'>Olá, <strong>" + nomeDestinatario + "</strong>!</p>" +
                "<p style='font-size:15px;color:#555;margin:0 0 25px;'>Seu agendamento foi confirmado. Confira os detalhes:</p>" +
                "<table width='100%' cellpadding='0' cellspacing='0' style='background:#e8f5e9;border-radius:8px;border-left:4px solid #1a8c7f;'>" +
                "<tr><td style='padding:20px;'>" +
                "<table width='100%' cellpadding='6' cellspacing='0'>" +
                "<tr><td style='color:#777;font-size:13px;width:40%;'>🩺 Médico</td><td style='color:#333;font-size:14px;font-weight:bold;'>" + medico + "</td></tr>" +
                "<tr><td style='color:#777;font-size:13px;'>📋 Especialidade</td><td style='color:#333;font-size:14px;font-weight:bold;'>" + especialidade + "</td></tr>" +
                "<tr><td style='color:#777;font-size:13px;'>📅 Data</td><td style='color:#333;font-size:14px;font-weight:bold;'>" + data + "</td></tr>" +
                "<tr><td style='color:#777;font-size:13px;'>🕐 Horário</td><td style='color:#333;font-size:14px;font-weight:bold;'>" + horario + "</td></tr>" +
                "<tr><td style='color:#777;font-size:13px;'>📌 Status</td><td style='color:#1a8c7f;font-size:14px;font-weight:bold;'>Agendado</td></tr>" +
                "<tr><td style='color:#777;font-size:13px;'>🔖 ID</td><td style='color:#999;font-size:12px;'>" + agendamentoId + "</td></tr>" +
                "</table></td></tr></table>" +
                "<p style='font-size:14px;color:#777;margin:25px 0 0;'>📍 <strong>Endereço:</strong> Rua Bernardino de Campos, 933 — Centro, Indaiatuba/SP</p>" +
                "<p style='font-size:13px;color:#aaa;margin:5px 0 0;'>Em caso de dúvidas ou cancelamento, entre em contato com a clínica.</p>" +
                "</td></tr>" +
                "<tr><td style='background:#f4f7f9;padding:20px;text-align:center;border-top:1px solid #eee;'>" +
                "<p style='font-size:12px;color:#aaa;margin:0;'>Este e-mail foi enviado automaticamente pela Clínica Vida Plus.<br>Por favor, não responda este e-mail.</p>" +
                "</td></tr>" +
                "</table></td></tr></table></body></html>";
    }
}

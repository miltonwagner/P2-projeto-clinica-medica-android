const functions = require("firebase-functions");
const admin = require("firebase-admin");
const nodemailer = require("nodemailer");

admin.initializeApp();

// ⚠️  As credenciais OAuth2 são lidas de variáveis de ambiente do Firebase Functions.
//     Configure-as com:
//       firebase functions:secrets:set CLIENT_ID
//       firebase functions:secrets:set CLIENT_SECRET
//       firebase functions:secrets:set REFRESH_TOKEN
//       firebase functions:secrets:set EMAIL_CLINICA
//     NUNCA coloque credenciais diretamente neste arquivo.

const CLIENT_ID     = process.env.CLIENT_ID;
const CLIENT_SECRET = process.env.CLIENT_SECRET;
const REFRESH_TOKEN = process.env.REFRESH_TOKEN;
const EMAIL_CLINICA = process.env.EMAIL_CLINICA;

const transporter = nodemailer.createTransport({
    service: "gmail",
    auth: {
        type: "OAuth2",
        user: EMAIL_CLINICA,
        clientId: CLIENT_ID,
        clientSecret: CLIENT_SECRET,
        refreshToken: REFRESH_TOKEN,
    },
});

exports.enviarEmailConfirmacaoAgendamento = functions.firestore
    .document("agendamentos/{agendamentoId}")
    .onCreate(async (snap, context) => {

        const dados = snap.data();
        const agendamentoId = context.params.agendamentoId;

        let emailPaciente = null;
        let nomePaciente = "Paciente";

        try {
            if (dados.pacienteId) {
                const usuarioDoc = await admin
                    .firestore()
                    .collection("usuarios")
                    .doc(dados.pacienteId)
                    .get();

                if (usuarioDoc.exists) {
                    emailPaciente = usuarioDoc.data().email;
                    nomePaciente = usuarioDoc.data().nome || "Paciente";
                }
            }

            if (!emailPaciente && dados.emailPaciente) {
                emailPaciente = dados.emailPaciente;
                nomePaciente = dados.nomePaciente || "Paciente";
            }

            if (!emailPaciente) {
                console.log("E-mail do paciente não encontrado:", agendamentoId);
                return null;
            }

            const mailOptions = {
                from: `"Clínica Vida Plus" <${EMAIL_CLINICA}>`,
                to: emailPaciente,
                subject: `✅ Consulta confirmada — ${dados.data} às ${dados.horario}`,
                html: `
                  <h2>Olá, ${nomePaciente}!</h2>
                  <p>Sua consulta foi confirmada:</p>
                  <ul>
                    <li><strong>Médico:</strong> ${dados.medico || "Não informado"}</li>
                    <li><strong>Especialidade:</strong> ${dados.especialidade || "Não informada"}</li>
                    <li><strong>Data:</strong> ${dados.data} às ${dados.horario}</li>
                  </ul>
                  <p>Clínica Vida Plus</p>
                `
            };

            await transporter.sendMail(mailOptions);
            console.log("E-mail enviado com sucesso para:", emailPaciente);
            return null;

        } catch (error) {
            console.error("Erro ao enviar e-mail:", error);
            return null;
        }
    });

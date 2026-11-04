// POST /api/confirmar  { agendamentoId }
// Header: Authorization: Bearer <ID token do Firebase do usuário logado>
//
// - Paciente que acabou de agendar (status Agendado)  -> e-mail "Consulta agendada"
// - Médico que confirmou a consulta (status Confirmado) -> e-mail "Consulta confirmada"
// O documento é lido no Firestore COM o token do próprio usuário (as regras valem),
// e cada e-mail sai só uma vez (marca emailAgendadoEm / emailConfirmadoEm no doc).
const nodemailer = require('nodemailer');

const PROJETO = process.env.FIREBASE_PROJECT || 'clinicavidaplus-5b9ce';
const API_KEY = process.env.FIREBASE_API_KEY;
const FS = `https://firestore.googleapis.com/v1/projects/${PROJETO}/databases/(default)/documents`;

const valor = (campo) => (campo ? Object.values(campo)[0] : undefined);
const escapar = (t) => String(t ?? '').replace(/[&<>"]/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' }[c]));

async function usuarioDoToken(idToken) {
  const r = await fetch(`https://identitytoolkit.googleapis.com/v1/accounts:lookup?key=${API_KEY}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ idToken }),
  });
  if (!r.ok) return null;
  const u = (await r.json()).users?.[0];
  return u ? { uid: u.localId, email: u.email } : null;
}

function montarEmail(tipo, c) {
  const online = (c.modalidade || '').toLowerCase() === 'online';
  const titulo = tipo === 'agendada' ? 'Consulta agendada' : 'Consulta confirmada pelo médico';
  const intro = tipo === 'agendada'
    ? 'Recebemos o seu agendamento. Você receberá outro e-mail assim que o médico confirmar.'
    : 'Boa notícia: o médico confirmou a sua consulta.';
  const onde = online
    ? 'Online — no horário, abra o app Vida Plus em <b>Consultas</b> e toque em <b>Entrar na videochamada</b>.'
    : 'Presencial — Rua Bernardino de Campos, 933, Centro, Indaiatuba – SP. Chegue 10 minutos antes.';
  const linha = (rotulo, texto) => texto
    ? `<tr><td style="padding:6px 0;color:#64748b;width:120px">${rotulo}</td><td style="padding:6px 0;color:#0f172a;font-weight:600">${escapar(texto)}</td></tr>`
    : '';

  const html = `<!doctype html><html><body style="margin:0;background:#f1f5f9;font-family:Arial,Helvetica,sans-serif">
<table width="100%" cellpadding="0" cellspacing="0" style="padding:24px 12px"><tr><td align="center">
<table width="100%" cellpadding="0" cellspacing="0" style="max-width:520px;background:#ffffff;border-radius:14px;overflow:hidden">
<tr><td style="background:#0e7490;padding:22px 24px;color:#ffffff">
  <div style="font-size:13px;opacity:.85">Clínica Médica Vida Plus</div>
  <div style="font-size:22px;font-weight:700;margin-top:4px">${titulo}</div>
</td></tr>
<tr><td style="padding:22px 24px;color:#334155;font-size:15px;line-height:1.5">
  <p style="margin:0 0 14px">Olá, ${escapar(c.pacienteNome || 'paciente')}!</p>
  <p style="margin:0 0 16px">${intro}</p>
  <table cellpadding="0" cellspacing="0" style="width:100%;font-size:15px;border-top:1px solid #e2e8f0;border-bottom:1px solid #e2e8f0;margin-bottom:16px">
    ${linha('Médico', c.medico)}${linha('Especialidade', c.especialidade)}${linha('Data', c.data)}${linha('Horário', c.horario)}${linha('Modalidade', c.modalidade || 'Presencial')}
    ${c.valor ? linha('Valor', 'R$ ' + c.valor) : ''}
  </table>
  <p style="margin:0 0 16px">${onde}</p>
  <p style="margin:0;color:#64748b;font-size:13px">Precisa cancelar ou remarcar? Abra o app em <b>Consultas</b>.</p>
</td></tr>
<tr><td style="padding:14px 24px;background:#f8fafc;color:#94a3b8;font-size:12px">
  Este é um e-mail automático, não responda. © 2026 Clínica Médica Vida Plus
</td></tr></table></td></tr></table></body></html>`;

  const texto = `${titulo}\n\nOlá, ${c.pacienteNome || 'paciente'}!\n${intro}\n\nMédico: ${c.medico}\nEspecialidade: ${c.especialidade}\nData: ${c.data}\nHorário: ${c.horario}\nModalidade: ${c.modalidade || 'Presencial'}\n\n${onde.replace(/<[^>]+>/g, '')}\n\nClínica Médica Vida Plus`;
  return { assunto: `${titulo}: ${c.data} às ${c.horario} — Vida Plus`, html, texto };
}

module.exports = async (req, res) => {
  if (req.method !== 'POST') return res.status(405).json({ erro: 'use POST' });

  const idToken = (req.headers.authorization || '').replace(/^Bearer\s+/i, '');
  const { agendamentoId } = req.body || {};
  if (!idToken || !agendamentoId || !/^[A-Za-z0-9_-]{1,64}$/.test(agendamentoId)) {
    return res.status(400).json({ erro: 'token e agendamentoId obrigatórios' });
  }

  const usuario = await usuarioDoToken(idToken);
  if (!usuario) return res.status(401).json({ erro: 'token inválido' });

  const auth = { Authorization: `Bearer ${idToken}` };
  const r = await fetch(`${FS}/agendamentos/${agendamentoId}`, { headers: auth });
  if (!r.ok) return res.status(r.status === 404 ? 404 : 403).json({ erro: 'consulta não encontrada' });
  const doc = await r.json();
  const c = Object.fromEntries(Object.entries(doc.fields || {}).map(([k, v]) => [k, valor(v)]));

  let tipo, marca;
  if (c.status === 'Agendado' && c.usuarioId === usuario.uid) {
    tipo = 'agendada'; marca = 'emailAgendadoEm';
  } else if (c.status === 'Confirmado' && c.medicoId === usuario.uid) {
    tipo = 'confirmada'; marca = 'emailConfirmadoEm';
  } else {
    return res.status(409).json({ erro: 'nada a enviar para este status/usuário' });
  }
  if (c[marca]) return res.json({ ok: true, jaEnviado: true });

  const para = c.pacienteEmail || (tipo === 'agendada' ? usuario.email : null);
  if (!para) return res.status(422).json({ erro: 'consulta sem e-mail do paciente' });

  const { assunto, html, texto } = montarEmail(tipo, c);
  const transporte = nodemailer.createTransport({
    host: process.env.SMTP_HOST,
    port: Number(process.env.SMTP_PORT || 587),
    secure: Number(process.env.SMTP_PORT) === 465,
    auth: { user: process.env.SMTP_USER, pass: process.env.SMTP_PASS },
  });
  await transporte.sendMail({ from: process.env.SMTP_FROM, to: para, subject: assunto, html, text: texto });

  // Marca como enviado (com o token do usuário; as regras deixam paciente/médico da consulta atualizar).
  await fetch(`${FS}/agendamentos/${agendamentoId}?updateMask.fieldPaths=${marca}`, {
    method: 'PATCH',
    headers: { ...auth, 'Content-Type': 'application/json' },
    body: JSON.stringify({ fields: { [marca]: { timestampValue: new Date().toISOString() } } }),
  });

  return res.json({ ok: true, tipo, para: para.replace(/(^.).*(@.*$)/, '$1***$2') });
};

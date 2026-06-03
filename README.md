#  Clínica Médica Vida Plus — Aplicativo Android

> Aplicativo mobile para gestão de consultas médicas, desenvolvido em Java com Android Studio e integração ao Firebase.
>
> Desenvolvimento de Software Multiplataforma — 4º Semestre  
> Ano: 1/2026

---

##  Sumário

- [Sobre o Projeto]
- [Funcionalidades]
- [Tecnologias Utilizadas]
- [Arquitetura]
- [Estrutura de Arquivos]
- [Pré-requisitos]
- [Como Executar]
- [Telas do Aplicativo]
- [Funcionalidades Futuras]
- [Referências]

---

## Sobre o Projeto

**Clínica Médica Vida Plus** é um aplicativo Android que permite ao paciente realizar cadastro, autenticação segura, agendamento de consultas com médicos especializados, visualização do histórico de atendimentos e localização da clínica — tudo a partir de um único app.

O projeto surgiu da necessidade de digitalizar o processo de agendamento médico, eliminando filas telefônicas e formulários físicos, e foi desenvolvido como trabalho acadêmico do curso de Desenvolvimento de Software Multiplataforma.

---

## Funcionalidades

### Implementadas

| Funcionalidade | Descrição |
|---|---|
| Cadastro de Paciente | Nome, e-mail, telefone, data de nascimento e senha |
| Login com E-mail/Senha | Autenticação via Firebase Auth |
| Login com Google | Google Sign-In integrado ao Firebase |
| Agendamento de Consultas | Seleção de médico, especialidade, data e horário |
| Verificação de Disponibilidade | Horários ocupados exibidos em cinza em tempo real |
| Confirmação de Agendamento | Resumo da consulta salvo no Firestore |
| E-mail de Confirmação | Envio automático ao paciente após o agendamento |
| Localização da Clínica | Endereço e integração com Google Maps |

### Funcionalidades Futuras

- Listagem e cancelamento de agendamentos pelo paciente
- Perfil editável (nome, telefone, data de nascimento)
- Recuperação de senha por e-mail
- Notificações push 24h antes da consulta (Firebase Cloud Messaging)
- Histórico médico completo com filtros
- Chat com a recepção da clínica
- Avaliação de consultas com estrelas e comentários
- Painel administrativo web para a clínica

---

## Tecnologias Utilizadas

| Categoria | Tecnologia | Finalidade |
|---|---|---|
| IDE | Android Studio | Desenvolvimento do app Android |
| Linguagem | Java | Lógica do aplicativo |
| Autenticação | Firebase Authentication | Login com e-mail/senha e Google |
| Banco de Dados | Firebase Firestore | Armazenamento de pacientes e agendamentos |
| Localização | Google Maps SDK | Mapa da clínica |
| Login Social | Google Sign-In | Autenticação simplificada |
| E-mail | JavaMail (SMTP Gmail) | Confirmação de agendamento |
| Backend Serverless | Firebase Cloud Functions | Gatilho de e-mail via Firestore |
| Versionamento | Git / GitHub | Controle de versão |

---

## Arquitetura

O aplicativo segue a arquitetura de camadas típica de apps Android modernos:

```
┌─────────────────────────────────────────┐
│         Camada de Apresentação          │
│  (Activities + Layouts XML)             │
│  IndexActivity, LoginActivity,          │
│  MainActivity (Cadastro),               │
│  ConsultasActivity, ListaConsultasActivity│
│  EnderecoActivity                       │
└────────────────┬────────────────────────┘
                 │
┌────────────────▼────────────────────────┐
│         Camada de Lógica                │
│  (Classes Java — Controllers)           │
│  Validações, eventos, fluxo de dados    │
│  EmailSender (AsyncTask SMTP)           │
└────────────────┬────────────────────────┘
                 │
┌────────────────▼────────────────────────┐
│         Camada de Dados                 │
│  Firebase Firestore (NoSQL em nuvem)    │
│  Coleções: pacientes / agendamentos     │
└────────────────┬────────────────────────┘
                 │
┌────────────────▼────────────────────────┐
│         Serviços Externos               │
│  Firebase Auth · Google Sign-In         │
│  Google Maps SDK                        │
│  Firebase Cloud Functions (e-mail)      │
└─────────────────────────────────────────┘
```

A comunicação com o Firebase é **assíncrona**, feita via listeners e callbacks, garantindo que a UI nunca fique bloqueada durante operações de rede.

---

## Estrutura de Arquivos

```
clinicavidaplus/
├── app/
│   ├── src/main/
│   │   ├── java/com/example/clinicavidaplus/
│   │   │   ├── IndexActivity.java          # Tela inicial (menu principal)
│   │   │   ├── MainActivity.java           # Cadastro de paciente
│   │   │   ├── LoginActivity.java          # Login e-mail + Google Sign-In
│   │   │   ├── ConsultasActivity.java      # Agendamento de consultas
│   │   │   ├── ListaConsultasActivity.java # Confirmação do agendamento
│   │   │   ├── EnderecoActivity.java       # Localização da clínica
│   │   │   ├── EmailSender.java            # Envio de e-mail SMTP (AsyncTask)
│   │   │   └── Paciente.java              # Modelo de dados do paciente
│   │   ├── res/
│   │   │   ├── layout/
│   │   │   │   ├── activity_index.xml
│   │   │   │   ├── activity_main.xml
│   │   │   │   ├── activity_login.xml
│   │   │   │   ├── activity_consultas.xml
│   │   │   │   ├── activity_lista_consultas.xml
│   │   │   │   └── activity_endereco.xml
│   │   │   ├── values/
│   │   │   │   ├── strings.xml             # Strings e arrays de médicos/especialidades
│   │   │   │   ├── colors.xml
│   │   │   │   └── themes.xml
│   │   │   └── drawable/
│   │   │       ├── logomed.png
│   │   │       └── foto_clinica_1.png
│   │   └── AndroidManifest.xml
│   ├── build.gradle.kts                    # Dependências do módulo app
│   └── google-services.json               # ⚠️ NÃO versionar — ver seção abaixo
├── build.gradle.kts                        # Build raiz do projeto
├── functions/
│   └── index.js                           # Firebase Cloud Functions (e-mail OAuth2)
└── README.md
```

---

## Pré-requisitos

- **Android Studio** Hedgehog ou superior
- **JDK 11** ou superior
- **Conta no Firebase** com projeto configurado
- **Conta Google** com acesso ao Google Cloud Console
- **Git** instalado na máquina

---

## Telas do Aplicativo

| Tela | Activity | Descrição |
|---|---|---|
| Inicial | `IndexActivity` | Menu com Login, Cadastro e Localização |
| Cadastro | `MainActivity` | Formulário de registro do paciente |
| Login | `LoginActivity` | Autenticação e-mail/senha + Google |
| Agendamento | `ConsultasActivity` | Seleção de médico, data e horário |
| Confirmação | `ListaConsultasActivity` | Resumo do agendamento realizado |
| Localização | `EnderecoActivity` | Endereço da clínica + botão Google Maps |

---

## Funcionalidades Futuras

- [ ] Meus Agendamentos — listar, visualizar e cancelar consultas
- [ ] Perfil do Paciente — edição de dados cadastrais
- [ ] Recuperação de Senha — e-mail de redefinição via Firebase
- [ ] Logout — botão de sair na tela inicial logada
- [ ] Notificações Push — lembrete 24h antes via FCM
- [ ] Histórico Médico — consultas anteriores com diagnóstico
- [ ] Chat com Clínica — mensagens via Firestore Realtime
- [ ] Avaliação de Consultas — estrelas e comentários
- [ ] Painel Administrativo Web — gerenciamento de médicos e horários
- [ ] Modo Offline — cache local de agendamentos

---

## Referências

- CFM – Conselho Federal de Medicina. *Pesquisa sobre uso de tecnologia em saúde no Brasil*. Brasília: CFM, 2022.
- FGV – Fundação Getulio Vargas. *Pesquisa sobre uso de smartphones no Brasil*. São Paulo: FGV, 2023.
- Firebase Documentation. Google LLC. Disponível em: https://firebase.google.com/docs
- Android Developers Documentation. Disponível em: https://developer.android.com/docs
- Google Maps Platform Documentation. Disponível em: https://developers.google.com/maps/documentation
- Google Sign-In for Android. Disponível em: https://developers.google.com/identity/sign-in/android

---

> **Clínica Médica Vida Plus** — Rua Bernardino de Campos, 933, Centro, Indaiatuba – SP  
> 📞 (19) 99348-4567 | ✉️ clinica@gmail.com  
> © 2026 Clínica Médica Vida Plus
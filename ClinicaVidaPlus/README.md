# 🏥 Clínica Vida Plus — Aplicativo Android

> Aplicativo mobile para gestão de consultas médicas, desenvolvido em Java com Android Studio e integração ao Firebase.
>
> **Curso:** Desenvolvimento de Software Multiplataforma — 4º Semestre  
> **Ano:** 1/2026

---

## 📋 Sumário

- [Sobre o Projeto](#sobre-o-projeto)
- [Funcionalidades](#funcionalidades)
- [Tecnologias Utilizadas](#tecnologias-utilizadas)
- [Arquitetura](#arquitetura)
- [Estrutura de Arquivos](#estrutura-de-arquivos)
- [Pré-requisitos](#pré-requisitos)
- [Como Executar](#como-executar)
- [Configuração do Firebase](#configuração-do-firebase)
- [Configuração do Google Sign-In](#configuração-do-google-sign-in)
- [Configuração do E-mail (SMTP / OAuth2)](#configuração-do-e-mail)
- [Telas do Aplicativo](#telas-do-aplicativo)
- [Funcionalidades Futuras](#funcionalidades-futuras)
- [Referências](#referências)

---

## Sobre o Projeto

O **Clínica Vida Plus** é um aplicativo Android que permite ao paciente realizar cadastro, autenticação segura, agendamento de consultas com médicos especializados, visualização do histórico de atendimentos e localização da clínica — tudo a partir de um único app.

O projeto surgiu da necessidade de digitalizar o processo de agendamento médico, eliminando filas telefônicas e formulários físicos, e foi desenvolvido como trabalho acadêmico do curso de Desenvolvimento de Software Multiplataforma.

---

## Funcionalidades

### ✅ Implementadas

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

### 🔜 Funcionalidades Futuras

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

## Como Executar

### 1. Clone o repositório

```bash
git clone https://github.com/miltonwagner/projeto-clinica-medica-android.git
cd projeto-clinica-medica-android
```

### 2. Adicione o arquivo `google-services.json`

> ⚠️ **Este arquivo NÃO está no repositório por conter credenciais sensíveis.**

Acesse o [Firebase Console](https://console.firebase.google.com/), selecione o projeto `clinicavidaplus-5b9ce`, vá em **Configurações do Projeto → Seus aplicativos → Android** e faça o download do `google-services.json`. Coloque-o em:

```
app/google-services.json
```

### 3. Crie o arquivo `local.properties` (se não existir)

```
sdk.dir=/caminho/para/seu/Android/sdk
```

### 4. Abra no Android Studio

- **File → Open** → selecione a pasta do projeto
- Aguarde o Gradle sincronizar
- Clique em **Run ▶** ou pressione `Shift + F10`

---

## Configuração do Firebase

No [Firebase Console](https://console.firebase.google.com/), com o projeto `clinicavidaplus-5b9ce`:

### Firebase Authentication
- Habilite os provedores: **E-mail/Senha** e **Google**

### Firestore Database
- Crie as coleções:
  - `pacientes` — campos: `nome`, `email`, `telefone`, `nascimento`
  - `agendamentos` — campos: `medico`, `especialidade`, `data`, `horario`, `status`
- Regras de segurança recomendadas:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /pacientes/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
    match /agendamentos/{agendamentoId} {
      allow read, write: if request.auth != null;
    }
  }
}
```

---

## Configuração do Google Sign-In

1. Obtenha o SHA-1 do seu keystore de desenvolvimento:
   ```bash
   # Windows
   keytool -list -v -keystore "%USERPROFILE%\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android

   # Linux / macOS
   keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
   ```
2. Adicione o SHA-1 no Firebase Console em **Configurações do Projeto → Seus aplicativos → Android → Adicionar impressão digital**
3. Baixe novamente o `google-services.json` atualizado

---

## Configuração do E-mail

O envio de e-mail de confirmação é feito diretamente no app via **JavaMail SMTP** (classe `EmailSender`).

> ⚠️ **IMPORTANTE — Segurança das Credenciais**
>
> As credenciais SMTP (e-mail e senha de app do Gmail) **não devem ser salvas diretamente no código-fonte**. Essa é a razão pela qual o GitHub pode ter bloqueado o push.
>
> **Como resolver:**
>
> Mova as credenciais para o arquivo `local.properties` (que está no `.gitignore`):
>
> ```properties
> # local.properties
> EMAIL_REMETENTE=seu_email@gmail.com
> SENHA_APP=sua_senha_de_app
> ```
>
> E leia no código via `BuildConfig`:
>
> No `build.gradle.kts` (app), dentro de `defaultConfig { }`:
> ```kotlin
> val localProps = java.util.Properties()
> localProps.load(java.io.FileInputStream(rootProject.file("local.properties")))
> buildConfigField("String", "EMAIL_REMETENTE", "\"${localProps["EMAIL_REMETENTE"]}\"")
> buildConfigField("String", "SENHA_APP",       "\"${localProps["SENHA_APP"]}\"")
> ```
>
> No `EmailSender.java`, substitua as constantes por:
> ```java
> private static final String EMAIL_REMETENTE = BuildConfig.EMAIL_REMETENTE;
> private static final String SENHA_APP       = BuildConfig.SENHA_APP;
> ```

### Por que o GitHub bloqueou o push?

O GitHub possui um sistema de **Secret Scanning** que detecta automaticamente credenciais expostas (tokens OAuth2, client secrets, chaves de API, senhas). Quando encontra, bloqueia o push para proteger sua conta.

**Arquivos que causaram o bloqueio:**
- `EmailSender.java` — senha de app do Gmail em texto puro
- `functions/index.js` — `CLIENT_SECRET` e `REFRESH_TOKEN` do OAuth2
- `google-services.json` — chave de API do Firebase (este arquivo **nunca deve ir para o GitHub**)

**Solução passo a passo:**

```bash
# 1. Adicione ao .gitignore ANTES de qualquer commit
echo "google-services.json" >> .gitignore
echo "local.properties"     >> .gitignore

# 2. Se os arquivos já foram commitados, remova do histórico do Git
git rm --cached app/google-services.json
git rm --cached local.properties

# 3. Commit da limpeza
git add .gitignore
git commit -m "chore: remove credenciais sensíveis do versionamento"

# 4. Force push (necessário quando o histórico foi alterado)
git push origin main --force
```

> Após o force push, **gere novas credenciais** no Firebase Console e no Google Cloud Console, pois as antigas foram expostas e devem ser consideradas comprometidas.

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

> 📍 **Clínica Médica Vida Plus** — Rua Bernardino de Campos, 933, Centro, Indaiatuba – SP  
> 📞 (19) 99348-4567 | ✉️ clinica@gmail.com  
> © 2026 Clínica Médica Vida Plus

#  ClÃ­nica MÃ©dica Vida Plus â€” Aplicativo Android

> Aplicativo mobile para gestÃ£o de consultas mÃ©dicas, desenvolvido em Java com Android Studio.
>
> Curso: Desenvolvimento de Software Multiplataforma â€” 4Âº Semestre
> Aluno: Milton Wagner Filho
> Professora: Simone Mendes
> Ano: 1/2026

---

## SumÃ¡rio

- [Sobre o Projeto](#sobre-o-projeto)
- [Funcionalidades](#funcionalidades)
- [Tecnologias Utilizadas](#tecnologias-utilizadas)
- [Arquitetura](#arquitetura)
- [Estrutura de Arquivos](#estrutura-de-arquivos)
- [PrÃ©-requisitos](#prÃ©-requisitos)
- [Como Executar](#como-executar)
- [Telas do Aplicativo](#telas-do-aplicativo)
- [Conceitos Implementados](#conceitos-implementados)
- [ReferÃªncias](#referÃªncias)

---

## Sobre o Projeto

**ClÃ­nica MÃ©dica Vida Plus** Ã© um aplicativo Android que permite ao paciente realizar cadastro, autenticaÃ§Ã£o segura, agendamento de consultas com mÃ©dicos especializados, visualizaÃ§Ã£o do histÃ³rico de atendimentos e localizaÃ§Ã£o da clÃ­nica em mapa interativo â€” tudo a partir de um Ãºnico app.

O projeto foi desenvolvido como trabalho acadÃªmico do curso de Desenvolvimento de Software Multiplataforma, aplicando na prÃ¡tica os quatro conceitos principais vistos em aula: banco de dados local (ROOM), banco de dados remoto (Firebase Firestore), autenticaÃ§Ã£o com Google e geolocalizaÃ§Ã£o com OpenStreetMap.

---

## Funcionalidades

### Implementadas

| Funcionalidade | DescriÃ§Ã£o |
|---|---|
| Cadastro de Paciente | Nome, e-mail, telefone, data de nascimento e senha |
| Login com E-mail/Senha | AutenticaÃ§Ã£o via Firebase Auth |
| Login com Google | Google Sign-In integrado ao Firebase |
| Agendamento de Consultas | SeleÃ§Ã£o de mÃ©dico, especialidade, data e horÃ¡rio |
| VerificaÃ§Ã£o de Disponibilidade | HorÃ¡rios ocupados exibidos em cinza em tempo real |
| ConfirmaÃ§Ã£o de Agendamento | Resumo da consulta salvo no Firestore |
| E-mail de ConfirmaÃ§Ã£o | Envio automÃ¡tico ao paciente apÃ³s o agendamento |
| Armazenamento Local | Dados do paciente salvos localmente com ROOM |
| LocalizaÃ§Ã£o da ClÃ­nica | Mapa interativo com OpenStreetMap + GPS em tempo real |

### Funcionalidades Futuras

- Listagem e cancelamento de agendamentos pelo paciente
- Perfil editÃ¡vel (nome, telefone, data de nascimento)
- RecuperaÃ§Ã£o de senha por e-mail
- NotificaÃ§Ãµes push 24h antes da consulta (Firebase Cloud Messaging)
- HistÃ³rico mÃ©dico completo com filtros
- AvaliaÃ§Ã£o de consultas com estrelas e comentÃ¡rios
- Painel administrativo web para a clÃ­nica

---

## Tecnologias Utilizadas

| Categoria | Tecnologia | Finalidade |
|---|---|---|
| IDE | Android Studio | Desenvolvimento do app Android |
| Linguagem | Java | LÃ³gica do aplicativo |
| Banco Local | ROOM (SQLite) | Armazenamento offline dos dados do paciente |
| Banco Remoto | Firebase Firestore | Armazenamento de pacientes e agendamentos na nuvem |
| AutenticaÃ§Ã£o | Firebase Authentication | Login com e-mail/senha e Google |
| Login Social | Google Sign-In | AutenticaÃ§Ã£o simplificada com conta Google |
| GeolocalizaÃ§Ã£o | FusedLocationProviderClient | ObtenÃ§Ã£o das coordenadas GPS do usuÃ¡rio |
| Mapa | OpenStreetMap + osmdroid | ExibiÃ§Ã£o do mapa interativo sem API Key |
| E-mail | JavaMail (SMTP Gmail) | ConfirmaÃ§Ã£o de agendamento por e-mail |
| Backend Serverless | Firebase Cloud Functions | Gatilho de e-mail via Firestore |
| Versionamento | Git / GitHub | Controle de versÃ£o |

---

## Arquitetura

O aplicativo segue arquitetura de camadas com padrÃ£o Repository:

```
â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”
â”‚           Camada de ApresentaÃ§Ã£o            â”‚
â”‚  (Activities + Layouts XML)                 â”‚
â”‚  IndexActivity, LoginActivity,              â”‚
â”‚  MainActivity, ConsultasActivity,           â”‚
â”‚  ListaConsultasActivity, EnderecoActivity   â”‚
â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”¬â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜
                   â”‚
â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â–¼â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”
â”‚           Camada de LÃ³gica                  â”‚
â”‚  PacienteRepository â€” coordena ROOM e       â”‚
â”‚  Firebase Â· EmailSender (AsyncTask SMTP)    â”‚
â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”¬â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”¬â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜
           â”‚                   â”‚
â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â–¼â”€â”€â”€â”€â”€â”€â”   â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â–¼â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”
â”‚  Banco LOCAL    â”‚   â”‚  Banco REMOTO          â”‚
â”‚  ROOM (SQLite)  â”‚   â”‚  Firebase Firestore    â”‚
â”‚  clinica_db     â”‚   â”‚  /pacientes            â”‚
â”‚  tabela:        â”‚   â”‚  /agendamentos         â”‚
â”‚  pacientes      â”‚   â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜
â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜
```

---

## Estrutura de Arquivos

```
clinicavidaplus/
â”œâ”€â”€ assets/
â”‚   â”œâ”€â”€ tela-home.png
â”‚   â”œâ”€â”€ tela-login.png
â”‚   â”œâ”€â”€ tela-cadastro.png
â”‚   â”œâ”€â”€ tela-agendamento.png
â”‚   â”œâ”€â”€ tela-email.png
â”‚   â””â”€â”€ tela-localizacao.png
â”œâ”€â”€ app/
â”‚   â”œâ”€â”€ src/main/
â”‚   â”‚   â”œâ”€â”€ java/com/example/clinicavidaplus/
â”‚   â”‚   â”‚   â”œâ”€â”€ IndexActivity.java              # Tela inicial
â”‚   â”‚   â”‚   â”œâ”€â”€ MainActivity.java               # Cadastro de paciente
â”‚   â”‚   â”‚   â”œâ”€â”€ LoginActivity.java              # Login e-mail + Google
â”‚   â”‚   â”‚   â”œâ”€â”€ ConsultasActivity.java          # Agendamento de consultas
â”‚   â”‚   â”‚   â”œâ”€â”€ ListaConsultasActivity.java     # ConfirmaÃ§Ã£o do agendamento
â”‚   â”‚   â”‚   â”œâ”€â”€ EnderecoActivity.java           # Mapa com localizaÃ§Ã£o da clÃ­nica
â”‚   â”‚   â”‚   â”œâ”€â”€ EmailSender.java                # Envio de e-mail SMTP
â”‚   â”‚   â”‚   â”œâ”€â”€ Paciente.java                   # Entity ROOM + modelo Firebase
â”‚   â”‚   â”‚   â”œâ”€â”€ PacienteDao.java                # DAO â€” operaÃ§Ãµes no banco local
â”‚   â”‚   â”‚   â”œâ”€â”€ AppDatabase.java                # ConfiguraÃ§Ã£o do banco ROOM
â”‚   â”‚   â”‚   â””â”€â”€ PacienteRepository.java         # Repository â€” ROOM + Firebase
â”‚   â”‚   â”œâ”€â”€ res/layout/
â”‚   â”‚   â”‚   â”œâ”€â”€ activity_index.xml
â”‚   â”‚   â”‚   â”œâ”€â”€ activity_main.xml
â”‚   â”‚   â”‚   â”œâ”€â”€ activity_login.xml
â”‚   â”‚   â”‚   â”œâ”€â”€ activity_consultas.xml
â”‚   â”‚   â”‚   â”œâ”€â”€ activity_lista_consultas.xml
â”‚   â”‚   â”‚   â””â”€â”€ activity_endereco.xml
â”‚   â”‚   â””â”€â”€ AndroidManifest.xml
â”‚   â”œâ”€â”€ build.gradle.kts
â”‚   â””â”€â”€ google-services.json
â”œâ”€â”€ build.gradle.kts
â””â”€â”€ README.md
```

---

## PrÃ©-requisitos

- Android Studio Hedgehog ou superior
- JDK 11 ou superior
- Conta no Firebase com projeto configurado
- Dispositivo fÃ­sico ou emulador com Android 7.0 (API 24) ou superior

---

## Como Executar

1. Clone o repositÃ³rio
2. Abra o projeto no Android Studio
3. Aguarde o Gradle sincronizar as dependÃªncias
4. Conecte um dispositivo fÃ­sico (com depuraÃ§Ã£o USB ativada) ou inicie um emulador
5. Clique em **Run â–¶**

---

## Telas do Aplicativo

| Tela | Activity | DescriÃ§Ã£o |
|---|---|---|
| Inicial | `IndexActivity` | Menu com Login, Cadastro e LocalizaÃ§Ã£o |
| Cadastro | `MainActivity` | FormulÃ¡rio de registro do paciente |
| Login | `LoginActivity` | AutenticaÃ§Ã£o e-mail/senha + Google |
| Agendamento | `ConsultasActivity` | SeleÃ§Ã£o de mÃ©dico, data e horÃ¡rio |
| ConfirmaÃ§Ã£o | `ListaConsultasActivity` | Resumo do agendamento realizado |
| LocalizaÃ§Ã£o | `EnderecoActivity` | Mapa OSM com pin da clÃ­nica e GPS do usuÃ¡rio |

<p align="center">
  <img src="assets/tela-home.jpg" width="200" alt="Home"/>
  <img src="assets/tela-login.jpg" width="200" alt="Login"/>
  <img src="assets/tela-cadastro.jpg" width="200" alt="Cadastro"/>
</p>
<p align="center">
  <img src="assets/tela-agendamento.jpg" width="200" alt="Agendamento"/>
  <img src="assets/tela-email.jpg" width="200" alt="E-mail"/>
  <img src="assets/tela-localizacao.jpg" width="200" alt="LocalizaÃ§Ã£o"/>
</p>

---

## Conceitos Implementados

### 1. ConexÃ£o Local â€” ROOM
Biblioteca Android de persistÃªncia local baseada em SQLite. Ao cadastrar, os dados do paciente sÃ£o salvos no banco local `clinica_db` (tabela `pacientes`) via `PacienteRepository`, permitindo acesso offline.

### 2. ConexÃ£o Remota â€” Firebase Firestore
Banco de dados NoSQL em nuvem. Agendamentos e dados de pacientes sÃ£o sincronizados remotamente em tempo real, permitindo que mÃºltiplos dispositivos visualizem horÃ¡rios ocupados instantaneamente.

### 3. AutenticaÃ§Ã£o com Google
Implementada via Firebase Authentication com Google Sign-In. O paciente pode criar conta e fazer login usando sua conta Google, sem precisar digitar senha.

### 4. GeolocalizaÃ§Ã£o
Utiliza `FusedLocationProviderClient` para obter as coordenadas GPS do usuÃ¡rio e a biblioteca `osmdroid` para renderizar o mapa OpenStreetMap com um marcador fixo na clÃ­nica e outro marcador dinÃ¢mico na posiÃ§Ã£o atual do usuÃ¡rio.

---

## Referências

- Firebase Documentation. Google LLC. DisponÃ­vel em: https://firebase.google.com/docs
- Android Developers â€” ROOM. DisponÃ­vel em: https://developer.android.com/training/data-storage/room
- osmdroid â€” OpenStreetMap Tools for Android. DisponÃ­vel em: https://github.com/osmdroid/osmdroid
- Google Sign-In for Android. DisponÃ­vel em: https://developers.google.com/identity/sign-in/android
- Android Developers Documentation. DisponÃ­vel em: https://developer.android.com/docs

---

> **ClÃ­nica MÃ©dica Vida Plus** â€” Rua Bernardino de Campos, 933, Centro, Indaiatuba â€“ SP
> Telefone: (19) 99447.4377 | E-mail: milton.wagner2013@gmail.com
> Â© 2026 ClÃ­nica MÃ©dica Vida Plus

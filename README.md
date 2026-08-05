# servidor-chatcorporativo-sd202603

Solução de comunicação corporativa para órgãos públicos, pensada como alternativa nacional a plataformas internacionais e implementada como PoC com sockets TCP e protocolo próprio.

## Documentação principal

- [Documento de arquitetura do MVP](docs/arquitetura-mvp.md)
- [Versão HTML do documento de arquitetura](docs/arquitetura-mvp.html)

## Visão geral

O repositório implementa um servidor de chat em Java com:

- autenticação e auto-registro de usuários;
- envio de mensagens diretas;
- criação e uso de grupos;
- transferência de arquivos via socket;
- auditoria em arquivo;
- controle de restrições de comunicação entre órgãos.

O servidor atual é uma prova de conceito centralizada. O documento de arquitetura descreve tanto o estado atual quanto a evolução recomendada para um MVP distribuído.

## Estrutura do projeto

```text
src/main/java/br/ufmt/chat
├── ServerMain.java
├── exception/
├── model/
├── protocol/
├── server/
├── service/
└── util/
```

## Pré-requisitos

- JDK 17 ou superior
- terminal com suporte a UTF-8

## Compilação

Como o repositório ainda não possui `pom.xml` ou `build.gradle`, a compilação pode ser feita diretamente com `javac`:

```bash
mkdir -p out
find src/main/java -name "*.java" > sources.txt
javac -encoding UTF-8 -d out @sources.txt
```

## Execução do servidor

```bash
java -cp out br.ufmt.chat.ServerMain
```

O servidor sobe na porta `5000`.

## Teste manual do protocolo

Como ainda não existe um cliente CLI oficial no repositório, o teste básico pode ser feito com `nc` em terminais separados.

Terminal 1:

```bash
nc 127.0.0.1 5000
```

Comandos de exemplo:

```text
LOGIN alice 123 orgao-a
LIST
GCREATE equipe
GJOIN equipe
MSG bob Ola Bob
GMSG equipe Bom dia
QUIT
```

Terminal 2:

```bash
nc 127.0.0.1 5000
```

Comandos de exemplo:

```text
LOGIN bob 123 orgao-b
LIST
QUIT
```

## Protocolo de aplicação

### Comandos do cliente

| Comando | Sintaxe |
| --- | --- |
| `LOGIN` | `LOGIN <usuario> <senha> <orgao>` |
| `MSG` | `MSG <destinatario> <texto...>` |
| `GMSG` | `GMSG <grupo> <texto...>` |
| `LIST` | `LIST` |
| `GLIST` | `GLIST` |
| `GCREATE` | `GCREATE <nomeGrupo>` |
| `GJOIN` | `GJOIN <grupo>` |
| `GLEAVE` | `GLEAVE <grupo>` |
| `FILE` | `FILE <destinatario> <nomeArquivo> <tamanhoBytes>` seguido pelos bytes do arquivo |
| `QUIT` | `QUIT` |

### Respostas do servidor

| Resposta | Significado |
| --- | --- |
| `OK` | sucesso |
| `ERR` | erro |
| `RECV` | mensagem direta recebida |
| `GRECV` | mensagem de grupo recebida |
| `FRECV` | cabeçalho de recebimento de arquivo, seguido pelos bytes |

## Limitações atuais da PoC

- não há cliente oficial no repositório;
- não há persistência de usuários, mensagens e grupos;
- não há criptografia de tráfego;
- o histórico não garante ordem causal;
- a busca de usuários cadastrados ainda não está exposta no protocolo;
- arquivos só são entregues imediatamente para destinatários online.

## Auditoria

As ações registradas pelo servidor são persistidas em `audit.log` no diretório de execução do processo.


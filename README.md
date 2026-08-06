# Chat Corporativo - Servidor de Comunicação Corporativa Federada

Uma solução de comunicação corporativa segura, escalável e compatível com as determinações da legislação local de segurança de dados (LGPD) e soberania digital.

---

## 🏛️ Arquitetura Híbrida

O sistema adota uma **Arquitetura Híbrida**:
1. **Cliente-Servidor na Borda**: Os dispositivos dos usuários conectam-se a brokers regionais locais (por exemplo, um broker por estado da federação ou por grande órgão).
2. **Federação Peer-to-Peer**: Os brokers regionais conectam-se de forma P2P entre si, permitindo o roteamento de mensagens e transferência de arquivos interestaduais/inter-órgãos de forma transparente.

---

## Requisitos

- Java 25;
- Apache Maven (foi usada a versão 3.9.16 neste projeto).

---

## 🛠️ Como Compilar

O projeto é baseado em Java puro (sem frameworks pesados) para maximizar o controle de sockets e a eficiência de rede. 

Para compilar o código fonte, execute o seguinte comando no diretório raiz do projeto:

```bash
mvn clean compile
```

Agora gere o jar:
```bash
mvn clean package
```

---

## 🚀 Como Executar (Ambiente Federado)

Para testar a federação, você pode iniciar dois brokers regionais simulando diferentes estados (por exemplo, Mato Grosso `MT` e Mato Grosso do Sul `MS`).

### 1. Iniciar o Broker de Mato Grosso (MT) na porta 5000
Este broker conhece o peer `MT` que roda no `localhost:5000`:
```bash
java -jar target/servidor-chatcorporativo-1.0.0.jar 5000 MT MS=localhost:5001
```

### 2. Iniciar o Broker de Mato Grosso do Sul (MS) na porta 5001
Este broker conhece o peer `MS` que roda no `localhost:5001`:
```bash
java -jar target/servidor-chatcorporativo-1.0.0.jar 5001 MS MT=localhost:5000
```

---

## 💬 Exemplo de Uso e Comunicação Federada

Os clientes podem se conectar usando qualquer utilitário de socket TCP (como `telnet` ou `nc`) ou por meio do cliente do chat corporativo.

### Passo 1: Conectar os usuários
1. **Cristina** conecta no broker de `MT` (porta 5000):
   ```bash
   telnet localhost 5000
   ```
   E realiza o login:
   ```text
   LOGIN cristina senha_segura SEFAZ
   ```

2. **Carlos** conecta no broker de `MS` (porta 5001):
   ```bash
   telnet localhost 5001
   ```
   E realiza o login:
   ```text
   LOGIN carlos senha_segura PGE
   ```

### Passo 2: Enviar mensagem federada
Para enviar uma mensagem para um usuário em outro estado, adicione o sufixo `@<estado>` no destinatário.

No terminal da **Cristina** (conectada em `MT`):
```text
MSG carlos@MS Olá Carlos! Esta mensagem cruzou as fronteiras via federação P2P!
```

No terminal do **Carlos** (conectado em `MS`), ele receberá de forma assíncrona:
```text
RECV cristina@MT Olá Cristina! Esta mensagem cruzou as fronteiras via federação P2P!
```

### Passo 3: Enviar arquivo federado
Para enviar um arquivo federado, o cliente remetente envia o cabeçalho `FILE <destinatario> <nomeArquivo> <tamanhoBytes>\n` seguido dos bytes binários do arquivo. 
O destinatário recebe a notificação `+FRECV <remetente> <nomeArquivo> <tamanhoBytes>\n` seguida dos bytes binários do arquivo no stream.

Como o envio/recebimento envolve bytes brutos binários, o cliente precisa ler exatamente a quantidade de bytes informada em `tamanhoBytes` logo após o cabeçalho `+FRECV`.

#### Exemplo em PowerShell (Windows):

##### 1. No Terminal do Destinatário (Maria em MS - Porta 5001):
Execute o arquivo .\teste-terminal1.ps1

##### 2. No Terminal do Remetente (João em MT - Porta 5000):
Execute o arquivo .\teste-terminal2.ps1

### Exemplo no Linux (Netcat)

Destinatário em seu terminal
```bash
nc localhost 5001
LOGIN maria 123 SESP
```

Remetente:
```bash
(echo -e "LOGIN joao 123 SESP\nFILE maria@MS teste.txt 12"; echo -n "Hello World!") | nc localhost 5000
```

Destinatário vê:
```text
+FRECV joao@MT teste.txt 12
Hello World!
```

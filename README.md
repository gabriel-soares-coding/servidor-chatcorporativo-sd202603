# Chat Corporativo - Servidor de Comunicação Corporativa Federada

Uma solução de comunicação corporativa segura, escalável e compatível com as determinações da legislação local de segurança de dados (LGPD) e soberania digital.

---

## 🏛️ Arquitetura Híbrida

O sistema adota uma **Arquitetura Híbrida**:
1. **Cliente-Servidor na Borda**: Os dispositivos dos usuários conectam-se a brokers regionais locais (por exemplo, um broker por estado da federação ou por grande órgão).
2. **Federação Peer-to-Peer**: Os brokers regionais conectam-se de forma P2P entre si, permitindo o roteamento de mensagens e transferência de arquivos interestaduais/inter-órgãos de forma transparente.

---

## 🛠️ Como Compilar

O projeto é baseado em Java puro (sem frameworks pesados) para maximizar o controle de sockets e a eficiência de rede. 

Para compilar o código fonte, execute o seguinte comando no diretório raiz do projeto:

```bash
javac -d target/classes src/main/java/br/ufmt/chat/**/*.java src/main/java/br/ufmt/chat/*.java
```

*(Certifique-se de que o diretório `target/classes` existe ou crie-o antes de executar).*

---

## 🚀 Como Executar (Ambiente Federado)

Para testar a federação, você pode iniciar dois brokers regionais simulando diferentes estados (por exemplo, Mato Grosso `MT` e Mato Grosso do Sul `MS`).

### 1. Iniciar o Broker de Mato Grosso (MT) na porta 5000
Este broker conhece o peer `MS` que roda no `localhost:5000`:
```bash
java -cp target/classes br.ufmt.chat.ServerMain 5000 MT MS=localhost:5000
```

### 2. Iniciar o Broker de Mato Grosso do Sul (MS) na porta 5001
Este broker conhece o peer `MT` que roda no `localhost:5000`:
```bash
java -cp target/classes br.ufmt.chat.ServerMain 5001 MS MT=localhost:5000
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
Para enviar um arquivo federado, a Cristina envia:
```text
FILE carlos@MS arquivo.txt 12
```
Seguido pelo conteúdo do arquivo de 12 bytes: `Ola_Mundo_SD` no stream.

Carlos receberá a notificação:
```text
FRECV cristina@MT arquivo.txt 12
```
Seguido pelos 12 bytes correspondentes.

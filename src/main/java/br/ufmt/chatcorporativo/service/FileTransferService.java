package br.ufmt.chatcorporativo.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import br.ufmt.chatcorporativo.exception.ChatException;
import br.ufmt.chatcorporativo.protocol.ProtocolConstants;
import br.ufmt.chatcorporativo.util.Logger;

/**
 * Serviço responsável pela transferência de arquivos via socket (R12).
 *
 * Protocolo de transferência:
 * 1. Remetente envia: FILE <destinatario> <nomeArquivo> <tamanhoBytes>
 * 2. Servidor lê exatamente <tamanhoBytes> bytes do InputStream do remetente
 * 3. Servidor envia ao destinatário: FRECV <remetente> <nomeArquivo> <tamanhoBytes>
 * 4. Servidor escreve os bytes no OutputStream do destinatário
 */
public class FileTransferService {

    private static final Logger log = new Logger("FileTransferService");

    /**
     * Lê os bytes do arquivo do InputStream do remetente.
     *
     * @param input      InputStream do socket do remetente
     * @param fileSize   tamanho em bytes a ser lido
     * @return os bytes do arquivo
     * @throws ChatException se houver erro de leitura ou tamanho inválido
     */
    public byte[] receiveFileData(InputStream input, long fileSize) throws ChatException {
        if (fileSize <= 0 || fileSize > 10_000_000) { // Limite de 10 MB para PoC
            throw new ChatException(ProtocolConstants.ERR_INVALID_ARGS,
                    "Tamanho de arquivo inválido: " + fileSize + " (máx 10MB)");
        }

        try {
            byte[] data = new byte[(int) fileSize];
            int totalRead = 0;
            while (totalRead < fileSize) {
                int bytesRead = input.read(data, totalRead, (int) (fileSize - totalRead));
                if (bytesRead == -1) {
                    throw new ChatException(ProtocolConstants.ERR_INTERNAL,
                            "Conexão encerrada durante transferência do arquivo");
                }
                totalRead += bytesRead;
            }
            log.info("Arquivo recebido: " + fileSize + " bytes");
            return data;
        } catch (IOException e) {
            throw new ChatException(ProtocolConstants.ERR_INTERNAL,
                    "Erro ao ler arquivo: " + e.getMessage());
        }
    }

    /**
     * Envia os bytes do arquivo para o OutputStream do destinatário.
     *
     * @param output     OutputStream do socket do destinatário
     * @param data       bytes do arquivo
     * @throws ChatException se houver erro de escrita
     */
    public void sendFileData(OutputStream output, byte[] data) throws ChatException {
        try {
            output.write(data);
            output.flush();
            log.info("Arquivo enviado: " + data.length + " bytes");
        } catch (IOException e) {
            throw new ChatException(ProtocolConstants.ERR_INTERNAL,
                    "Erro ao enviar arquivo: " + e.getMessage());
        }
    }
}

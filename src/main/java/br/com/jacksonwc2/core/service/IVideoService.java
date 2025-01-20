package br.com.jacksonwc2.core.service;

import br.com.jacksonwc2.core.domain.Video;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.event.Observes;

/**
 * Interface com definição dos métodos que devem ser executados para o processamento do vídeo.
 */
public interface IVideoService {

    /**
     * Inicia thread que serve como listeners/ouvinte das mensagens publicadas na fila de mensagearia.
     * @param event
     */
    void iniciarListener(@Observes StartupEvent event);

    /**
     * Recebe os dados do vídeo e realiza o processamento com FFMPeg para a geração dos frames.
     * @param video
     */
    void processarVideo(Video video);
    
    /**
     * Comprime os frames gerados pelo processamento de vídeos.
     * @param video
     */
    void comprimirFrames(Video video);

    /**
     * Exclui o vídeo já processado.
     * @param video
     */
    void excluirVideoProcessado(Video video);

    /**
     * Exclui os frames do vídeo já processado.
     * @param video
     */
    void excluirFramesProcessado(Video video);

    /**
     * Notifica o encerramento do processamento do vídeo.
     * @param video
     * @param originalMessage
     */
    void notificarVideoProcessado(Video video, Object originalMessage);

    /**
     * Verifica se os diretórios de arquivos configurados existem
     * @param path
     */
    void verificarDiretorioArquivos(String path);
    
}

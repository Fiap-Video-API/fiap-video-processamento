package br.com.jacksonwc2.core.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.jacksonwc2.adapter.message.IMessageConnect;
import br.com.jacksonwc2.adapter.so.ISistemaOperacionalConnect;
import br.com.jacksonwc2.core.domain.ItemFila;
import br.com.jacksonwc2.core.domain.Video;
import br.com.jacksonwc2.core.domain.VideoStatus;
import br.com.jacksonwc2.core.exception.VideoException;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

@ApplicationScoped
public class VideoServiceImpl implements IVideoService {

    private static final Logger LOGGER = Logger.getLogger(VideoServiceImpl.class);
    private final ExecutorService executor = Executors.newFixedThreadPool(3);

    @ConfigProperty(name = "queue.processados")
    public String queueProcessadosUrl;

    @ConfigProperty(name = "path.processados")
    public String pathProcessados;

    @ConfigProperty(name = "path.processar")
    public String pathProcessar;

    @Inject
    IMessageConnect messageConnect;

    @Inject
    ISistemaOperacionalConnect sistemaOperacionalConnect;

    @Inject
    ObjectMapper objectMapper;

    @Override
    public void iniciarListener(@Observes StartupEvent event) {
        LOGGER.info("VideoServiceImpl.iniciarListener: Iniciando Listeners de mensagens da fila.");

        try {
            verificarDiretorioArquivos(pathProcessar);
            verificarDiretorioArquivos(pathProcessados);
        } catch (VideoException e) {
            LOGGER.error("VideoServiceImpl.iniciarListener: Erro ao verificar diretórios.", e);
        }
        
        executor.submit(() -> {
            while (true) {
                LOGGER.info("VideoServiceImpl.iniciarListener: Buscando mensagem na fila.");
                
                ItemFila itemFila = messageConnect.adquirirMensagemFila();
                
                if (itemFila == null) {
                    Thread.sleep(1000);
                    continue;
                }

                Video video = itemFila.getVideo();

                try {    
                    processarVideo(video);
                    comprimirFrames(video);
                    excluirVideoProcessado(video);
                    excluirFramesProcessado(video);
                    
                    video.setStatus(VideoStatus.FINALIZADO.getDescricao());
                    notificarVideoProcessado(video, itemFila.getOriginalMessage());

                } catch (VideoException e) {
                    LOGGER.error("VideoServiceImpl.iniciarListener: Erro ao consumir mensagem da fila", e);
                    
                    // NOTIFICANDO FALHA NO PROCESSAMENTO PARA ENVIO DE E-MAIL
                    video.setStatus(VideoStatus.FALHA.getDescricao());
                    notificarVideoProcessado(video, itemFila.getOriginalMessage());
                }
            }
        });
    }

	@Override
	public void processarVideo(Video video) throws VideoException {
        LOGGER.info("VideoServiceImpl.processarVideo: Processamento iniciado.");
		try {
            var pathFile = pathProcessar + video.getPathVideo();
            var pathFrames = pathProcessados + video.getId();

            verificarDiretorioArquivos(pathFrames);

            String[] command = {
                "/usr/bin/ffmpeg",
                "-i", pathFile,
                "-vf", "fps=1",
                pathFrames + "/frame_%04d.png"
            };

            sistemaOperacionalConnect.executarComandoSO(command);         
            LOGGER.info("VideoServiceImpl.processarVideo: Processamento finalizado com sucesso.");
        } catch (Exception e) {
            throw new VideoException("VideoServiceImpl.processarVideo: Erro ao processar vídeo");
        }
	}

	@Override
	public void comprimirFrames(Video video) throws VideoException {
	    LOGGER.info("VideoServiceImpl.comprimirFrames: Comprimindo os arquivos.");
        
        try {
            var pathFrames = pathProcessados + video.getId();
            String[] command = {
                "/bin/bash",
                "-c",
                String.format("/usr/bin/zip -j %s %s/*.png", pathFrames + ".zip", pathFrames)
            };

            sistemaOperacionalConnect.executarComandoSO(command);         
            video.setPathZip(video.getId() + ".zip");
            
            LOGGER.info("VideoServiceImpl.comprimirFrames: Zip finalizado com sucesso.");
        } catch (Exception e) {
            throw new VideoException("VideoServiceImpl.comprimirFrames: Erro ao comprimir vídeo");
        }
	}

	@Override
	public void excluirVideoProcessado(Video video) throws VideoException {
        LOGGER.info("VideoServiceImpl.excluirVideoProcessado: Excluindo vídeo.");
		
        try {
            var pathFile = pathProcessar + video.getPathVideo();
            
            String[] command = { "rm", "-f", pathFile };
            sistemaOperacionalConnect.executarComandoSO(command);            
            LOGGER.info("VideoServiceImpl.excluirVideoProcessado: Excluido com sucesso.");
        } catch (Exception e) {
            throw new VideoException("VideoServiceImpl.excluirVideoProcessado: Erro ao excluir vídeo");
        }
	}

    @Override
	public void excluirFramesProcessado(Video video) throws VideoException {
        LOGGER.info("VideoServiceImpl.excluirFramesProcessado: Excluindo Frames.");

		try {
            var pathFrames = pathProcessados + video.getId();
            String[] command = { "rm", "-rf", pathFrames };
            
            sistemaOperacionalConnect.executarComandoSO(command);            
            LOGGER.info("VideoServiceImpl.excluirFramesProcessado: Excluido com sucesso.");
        } catch (Exception e) {
            throw new VideoException("VideoServiceImpl.excluirFramesProcessado: Erro ao excluir frames");
        }
	}

	@Override
	public void notificarVideoProcessado(Video video, Object originalMessage) {
        LOGGER.info("VideoServiceImpl.notificarVideoProcessado: Notificar vídeo processado.");

		try {
            var json = objectMapper.writeValueAsString(video);
            
            messageConnect.deletarMensagemFila(originalMessage);
			messageConnect.enviarMensagemFila(queueProcessadosUrl, json);
		} catch (JsonProcessingException e) {
			LOGGER.error("VideoServiceImpl.notificarVideoProcessado: Falha ao notificar vído processado", e);
		}
	}

	@Override
	public void verificarDiretorioArquivos(String path) throws VideoException {
        LOGGER.info("VideoServiceImpl.verificarDiretorioArquivos: Verificando se os diretórios existem.");
		
        Path diretorio = Paths.get(path);

        // Verificar se o diretório existe
        if (Files.exists(diretorio)) {
            LOGGER.infof("VideoServiceImpl.verificarDiretorioArquivos: O caminho já existe: %s", diretorio.toAbsolutePath());
        } else {
            try {
                // Criar o diretório e subdiretórios (se necessário)
                Files.createDirectories(diretorio);
                LOGGER.infof("VideoServiceImpl.verificarDiretorioArquivos: diretório criado: %s", diretorio.toAbsolutePath());
            } catch (IOException e) {
                throw new VideoException("VideoServiceImpl.verificarDiretorioArquivos: Erro ao criar diretório");
            }
        }
	}

    public void stopListeners(){
        this.executor.shutdown();
    }

    
}

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
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import software.amazon.awssdk.services.sqs.SqsClient;

@ApplicationScoped
public class VideoServiceImpl implements IVideoService {

    private static final Logger LOGGER = Logger.getLogger(VideoServiceImpl.class);
    private final ExecutorService executor = Executors.newFixedThreadPool(3);

    @Inject
    SqsClient sqs;

    @ConfigProperty(name = "queue.processados")
    String queueProcessadosUrl;

    @ConfigProperty(name = "path.processados")
    String pathProcessados;

    @ConfigProperty(name = "path.processar")
    String pathProcessar;

    @Inject
    IMessageConnect messageConnect;

    @Inject
    ISistemaOperacionalConnect sistemaOperacionalConnect;

    @Override
    public void iniciarListener(@Observes StartupEvent event) {
        LOGGER.info("VideoServiceImpl.iniciarListener: Iniciando Listeners de mensagens da fila.");

        verificarDiretorioArquivos(pathProcessar);
        verificarDiretorioArquivos(pathProcessados);
        
        executor.submit(() -> {
            while (true) {
                try {
                    LOGGER.info("VideoServiceImpl.iniciarListener: Buscando mensagem na fila.");
                    
                    ItemFila itemFila = messageConnect.adquirirMensagemFila();
                    
                    if (itemFila == null) {
                        continue;
                    }

                    Video video = itemFila.getVideo();
                    
                    processarVideo(video);
                    comprimirFrames(video);
                    excluirVideoProcessado(video);
                    excluirFramesProcessado(video);
                    notificarVideoProcessado(video, itemFila.getOriginalMessage());

                } catch (Exception e) {
                    LOGGER.error("VideoServiceImpl.iniciarListener: Erro ao consumir mensagem da fila", e);
                }
            }
        });
    }

	@Override
	public void processarVideo(Video video) {
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
            LOGGER.error("VideoServiceImpl.processarVideo: Erro ao processar vídeo", e);
        }
	}

	@Override
	public void comprimirFrames(Video video) {
	    LOGGER.info("VideoServiceImpl.comprimirFrames: Comprimindo os arquivos.");
        
        try {
            var pathFrames = pathProcessados + video.getId();
            String[] command = {
                "/bin/bash",
                "-c",
                String.format("/usr/bin/zip -j %s %s/*.png", pathFrames + ".zip", pathFrames)
            };

            sistemaOperacionalConnect.executarComandoSO(command);         
            LOGGER.info("VideoServiceImpl.comprimirFrames: Zip finalizado com sucesso.");
        } catch (Exception e) {
            LOGGER.error("VideoServiceImpl.comprimirFrames: Erro ao comprimir vídeo", e);
        }
	}

	@Override
	public void excluirVideoProcessado(Video video) {
        LOGGER.info("VideoServiceImpl.excluirVideoProcessado: Excluindo vídeo.");
		
        try {
            var pathFile = pathProcessar + video.getPathVideo();
            
            String[] command = { "rm", "-f", pathFile };
            sistemaOperacionalConnect.executarComandoSO(command);            
            LOGGER.info("VideoServiceImpl.excluirVideoProcessado: Excluido com sucesso.");
        } catch (Exception e) {
            LOGGER.error("VideoServiceImpl.excluirVideoProcessado: Erro ao excluir vídeo", e);
        }
	}

    @Override
	public void excluirFramesProcessado(Video video) {
        LOGGER.info("VideoServiceImpl.excluirFramesProcessado: Excluindo Frames.");

		try {
            var pathFrames = pathProcessados + video.getId();
            String[] command = { "rm", "-rf", pathFrames };
            
            sistemaOperacionalConnect.executarComandoSO(command);            
            LOGGER.info("VideoServiceImpl.excluirFramesProcessado: Excluido com sucesso.");
        } catch (Exception e) {
            LOGGER.error("VideoServiceImpl.excluirFramesProcessado: Erro ao excluir frames", e);
        }
	}

	@Override
	public void notificarVideoProcessado(Video video, Object originalMessage) {
        LOGGER.info("VideoServiceImpl.notificarVideoProcessado: Notificar vídeo processado.");

		try {
            messageConnect.deletarMensagemFila(originalMessage);
			messageConnect.enviarMensagemFila(queueProcessadosUrl, new ObjectMapper().writeValueAsString(video));
		} catch (JsonProcessingException e) {
			LOGGER.error("VideoServiceImpl.notificarVideoProcessado: Falha ao notificar vído processado", e);
		}
	}

	@Override
	public void verificarDiretorioArquivos(String path) {
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
                LOGGER.error("VideoServiceImpl.verificarDiretorioArquivos: Erro ao criar diretório", e);
            }
        }
	}

    
}

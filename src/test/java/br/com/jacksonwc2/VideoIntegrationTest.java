package br.com.jacksonwc2;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Files;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.jacksonwc2.adapter.message.IMessageConnect;
import br.com.jacksonwc2.adapter.so.ISistemaOperacionalConnect;
import br.com.jacksonwc2.core.domain.ItemFila;
import br.com.jacksonwc2.core.domain.Video;
import br.com.jacksonwc2.core.service.VideoServiceImpl;
import io.quarkus.runtime.StartupEvent;

@ExtendWith(MockitoExtension.class)
public class VideoIntegrationTest {

    @InjectMocks
    private VideoServiceImpl videoService;

    @Mock
    private IMessageConnect messageConnect;

    @Mock
    private ISistemaOperacionalConnect sistemaOperacionalConnect;

    @Mock
    private ObjectMapper objectMapper;


    @BeforeEach
    public void setup() throws JsonProcessingException {

        videoService.queueProcessadosUrl = "test-queue-url";
        videoService.pathProcessados = "./";
        videoService.pathProcessar = "./";
        videoService.numeroThreadsProcessamento = 1;
        
        Video video = new Video();
        video.setId("12345");
        video.setPathVideo("video.mp4");

        ItemFila itemFila = new ItemFila();
        itemFila.setVideo(video);
        itemFila.setOriginalMessage(new Object());
        
        // Simula leitura do SQS
        when(messageConnect.adquirirMensagemFila()).thenReturn(itemFila).thenReturn(null);
        
        doNothing().when(sistemaOperacionalConnect).executarComandoSO(any());
        when(objectMapper.writeValueAsString(any())).thenReturn("json");
    }

    @Test
    public void testIniciarListener() throws Exception {
        try(MockedStatic<Files> mockedFiles = mockStatic(Files.class)){
            mockedFiles.when(() -> Files.exists(any())).thenReturn(true);

            // Inicia a execução da Thread
            videoService.iniciarListener(mock(StartupEvent.class));
            
            // Aguarda a execução da thread por 100ms
            Thread.sleep(1000);
    
            // Finaliza a execução da thread
            videoService.stopListeners();
    
            // verifica se foi executado 1x a notificação de video processado e mensagem apagada
            verify(messageConnect, times(1)).enviarMensagemFila(any(), any());
            verify(messageConnect, times(1)).deletarMensagemFila(any());
        }
    }
   
}

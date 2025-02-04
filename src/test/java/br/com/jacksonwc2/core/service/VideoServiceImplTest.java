package br.com.jacksonwc2.core.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.jacksonwc2.adapter.message.IMessageConnect;
import br.com.jacksonwc2.adapter.so.ISistemaOperacionalConnect;
import br.com.jacksonwc2.core.domain.Video;
import br.com.jacksonwc2.core.exception.VideoException;

@ExtendWith(MockitoExtension.class)
public class VideoServiceImplTest {

    @InjectMocks
    private VideoServiceImpl videoService;

    @Mock
    private IMessageConnect messageConnect;

    @Mock
    private ISistemaOperacionalConnect sistemaOperacionalConnect;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private Logger logger;

    @ConfigProperty(name = "path.processados")
    String pathProcessados;

    @ConfigProperty(name = "path.processar")
    String pathProcessar;

    @ConfigProperty(name = "queue.processados")
    String queueProcessadosUrl;

    @Test
    void testVerificarDiretorioArquivos_DiretorioExiste() throws VideoException {
        String path = "/tmp/test";
        Path mockPath = Paths.get(path);

        try (MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
            mockedFiles.when(() -> Files.exists(mockPath)).thenReturn(true);

            videoService.verificarDiretorioArquivos(path);

            mockedFiles.verify(() -> Files.exists(mockPath), times(1));
            mockedFiles.verify(() -> Files.createDirectories(mockPath), never());
        }
    }

    @Test
    void testVerificarDiretorioArquivos_DiretorioNaoExiste() throws IOException, VideoException {
        String path = "/tmp/test";
        Path mockPath = Paths.get(path);

        try (MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
            mockedFiles.when(() -> Files.exists(mockPath)).thenReturn(false);
            mockedFiles.when(() -> Files.createDirectories(mockPath)).thenReturn(mockPath);

            videoService.verificarDiretorioArquivos(path);

            mockedFiles.verify(() -> Files.exists(mockPath), times(1));
            mockedFiles.verify(() -> Files.createDirectories(mockPath), times(1));
        }
    }

    @Test
    void testVerificarDiretorioArquivos_ErroAoCriarDiretorio() throws IOException, VideoException {
        String path = "/tmp/test";
        Path mockPath = Paths.get(path);

        try (MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
            mockedFiles.when(() -> Files.exists(mockPath)).thenReturn(false);
            mockedFiles.when(() -> Files.createDirectories(mockPath)).thenThrow(new IOException("Erro ao criar diretório"));

            VideoException exception = assertThrows(VideoException.class, () -> videoService.verificarDiretorioArquivos(path));

            assertEquals("VideoServiceImpl.verificarDiretorioArquivos: Erro ao criar diretório", exception.getMessage());
            mockedFiles.verify(() -> Files.exists(mockPath), times(1));
            mockedFiles.verify(() -> Files.createDirectories(mockPath), times(1));
        }
    }

     @Test
    public void testNotificarVideoProcessado_Sucesso() throws Exception {
        
        var video = new Video();
        var originalMessage = new Object();

        Mockito.when(objectMapper.writeValueAsString(any())).thenReturn("json");

        videoService.notificarVideoProcessado(video, originalMessage);

        Mockito.verify(messageConnect, times(1)).deletarMensagemFila(originalMessage);
        Mockito.verify(messageConnect, times(1)).enviarMensagemFila(any(), any());
        Mockito.verify(objectMapper).writeValueAsString(any());
    }

    @Test
    public void testNotificarVideoProcessado_FalhaSerializacao() throws Exception {
        
        var video = new Video();
        var originalMessage = new Object();

        Mockito.when(objectMapper.writeValueAsString(video)).thenThrow(new JsonProcessingException("Erro de serialização") {});

        videoService.notificarVideoProcessado(video, originalMessage);

        Mockito.verify(messageConnect, never()).deletarMensagemFila(any());
        Mockito.verify(messageConnect, never()).enviarMensagemFila(any(), any());
    }

    @Test
    public void testExcluirFramesProcessado_Sucesso() throws Exception {
        Video video = new Video();
        video.setId("123");
        
        var expectedCommand = new String[]{"rm", "-rf", pathProcessados + video.getId()};
        videoService.excluirFramesProcessado(video);

        Mockito.verify(sistemaOperacionalConnect).executarComandoSO(expectedCommand);
    }

    @Test
    public void testExcluirFramesProcessado_FalhaComando() throws Exception {
         Video video = new Video();
        video.setId("123");

        var expectedCommand = new String[]{"rm", "-rf", pathProcessados + video.getId()};
        Mockito.doThrow(new RuntimeException("Erro ao executar comando")).when(sistemaOperacionalConnect).executarComandoSO(expectedCommand);

        VideoException exception = Assertions.assertThrows(VideoException.class, () -> {
            videoService.excluirFramesProcessado(video);
        });

        Assertions.assertEquals("VideoServiceImpl.excluirFramesProcessado: Erro ao excluir frames", exception.getMessage());
        Mockito.verify(sistemaOperacionalConnect).executarComandoSO(expectedCommand);
    }

    @Test
    void testExcluirVideoProcessado_Sucesso() throws Exception {
        Video video = new Video();
        video.setPathVideo("video.mp4");
        String[] expectedCommand = { "rm", "-f", pathProcessar + video.getPathVideo() };

        assertDoesNotThrow(() -> videoService.excluirVideoProcessado(video));

        verify(sistemaOperacionalConnect, times(1)).executarComandoSO(expectedCommand);
    }

    @Test
    void testExcluirVideoProcessado_Falha() throws Exception {
        Video video = new Video();
        video.setPathVideo("video.mp4");
        String[] expectedCommand = { "rm", "-f", pathProcessar + video.getPathVideo() };

        doThrow(new RuntimeException("Erro ao executar comando")).when(sistemaOperacionalConnect).executarComandoSO(expectedCommand);

        VideoException exception = assertThrows(VideoException.class, () -> videoService.excluirVideoProcessado(video));

        assertEquals("VideoServiceImpl.excluirVideoProcessado: Erro ao excluir vídeo", exception.getMessage());
        verify(sistemaOperacionalConnect, times(1)).executarComandoSO(expectedCommand);
    }

    @Test
    void testComprimirFrames_Sucesso() throws Exception {
        Video video = new Video();
        video.setId("12345");
        String pathFrames = pathProcessados + video.getId();
        String[] expectedCommand = {
            "/bin/bash",
            "-c",
            String.format("/usr/bin/zip -j %s %s/*.png", pathFrames + ".zip", pathFrames)
        };

        assertDoesNotThrow(() -> videoService.comprimirFrames(video));

        verify(sistemaOperacionalConnect, times(1)).executarComandoSO(expectedCommand);
        assertEquals("12345.zip", video.getPathZip());
    }

    @Test
    void testComprimirFrames_Falha() throws Exception {
        Video video = new Video();
        video.setId("12345");
        String pathFrames = pathProcessados + video.getId();
        String[] expectedCommand = {
            "/bin/bash",
            "-c",
            String.format("/usr/bin/zip -j %s %s/*.png", pathFrames + ".zip", pathFrames)
        };

        doThrow(new RuntimeException("Erro ao executar comando")).when(sistemaOperacionalConnect).executarComandoSO(expectedCommand);

        VideoException exception = assertThrows(VideoException.class, () -> videoService.comprimirFrames(video));
        
        assertEquals("VideoServiceImpl.comprimirFrames: Erro ao comprimir vídeo", exception.getMessage());
        verify(sistemaOperacionalConnect, times(1)).executarComandoSO(expectedCommand);
    }

    @Test
    void testProcessarVideo_Sucesso() throws Exception {
        Video video = new Video();
        video.setId("12345");
        video.setPathVideo("video.mp4");
        String pathFile = pathProcessar + video.getPathVideo();
        String pathFrames = pathProcessados + video.getId();

        String[] expectedCommand = {
            "/usr/bin/ffmpeg",
            "-i", pathFile,
            "-vf", "fps=1",
            pathFrames + "/frame_%04d.png"
        };

        doNothing().when(sistemaOperacionalConnect).executarComandoSO(expectedCommand);

        assertDoesNotThrow(() -> videoService.processarVideo(video));

        verify(sistemaOperacionalConnect, times(1)).executarComandoSO(expectedCommand);
    }

    @Test
    void testProcessarVideo_Falha() throws Exception {
        Video video = new Video();
        video.setId("12345");
        video.setPathVideo("video.mp4");
        String pathFile = pathProcessar + video.getPathVideo();
        String pathFrames = pathProcessados + video.getId();

        String[] expectedCommand = {
            "/usr/bin/ffmpeg",
            "-i", pathFile,
            "-vf", "fps=1",
            pathFrames + "/frame_%04d.png"
        };

        doThrow(new RuntimeException("Erro ao executar comando")).when(sistemaOperacionalConnect).executarComandoSO(expectedCommand);

        VideoException exception = assertThrows(VideoException.class, () -> videoService.processarVideo(video));
        assertEquals("VideoServiceImpl.processarVideo: Erro ao processar vídeo", exception.getMessage());

        verify(sistemaOperacionalConnect, times(1)).executarComandoSO(expectedCommand);
    }
}

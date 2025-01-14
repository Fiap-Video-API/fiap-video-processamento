package br.com.jacksonwc2;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

@ApplicationScoped
public class ProcessamentoVideos {

    private static final Logger LOGGER = Logger.getLogger(ProcessamentoVideos.class);
    private final ExecutorService executor = Executors.newFixedThreadPool(3);

    @Inject
    SqsClient sqs;

    @ConfigProperty(name = "queue.processar")
    String queueUrl;

    void onStart(@Observes StartupEvent event) {
        LOGGER.info("Iniciando processamento de mensagens da fila...");
        processMessages();
    }

    public void processMessages() {
        executor.submit(() -> {
            
            // Configura o ReceiveMessageRequest com o visibility timeout
            ReceiveMessageRequest request = ReceiveMessageRequest.builder()
                .queueUrl(queueUrl)
                .maxNumberOfMessages(1) // Consome apenas 1 mensagem por vez
                .visibilityTimeout(300) // Mensagem fica invisível por 5 minutos
                .waitTimeSeconds(0) // Long polling, para evitar custos
                .build();

            while (true) {
                try {
                    LOGGER.info("Iniciando leitura");
                    // Busca mensagens na fila
                    List<Message> messages = sqs.receiveMessage(request).messages();
                    LOGGER.info("Finalizando leitura");
                    
                    if(messages.isEmpty()){
                        Thread.sleep(1000);
                        continue;
                    }

                    // Processa a primeira mensagem
                    Message message = messages.get(0);
                    LOGGER.infof("Mensagem processada recebida: %s", message.body());
                    processarMessage(message);

                    deleteMessage(message);
                    LOGGER.info("Mensagem processada e excluída com sucesso!");

                } catch (Exception e) {
                    LOGGER.error("Erro ao consumir mensagem da fila SQS", e);
                    return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Erro ao consumir mensagem").build();
                }
            }
        });
            
        
    }

    private void processarMessage(Message message) {
        try {
            var pathFile = message.body();

            ProcessBuilder processBuilder = new ProcessBuilder();
            processBuilder.redirectErrorStream(true);

            String ffmpegPath = "/usr/bin/ffmpeg";
            String[] command = {
                ffmpegPath,
                "-i", "/home/jackson/git/fiap-videos/fiap-video-processamento/src/main/resources/arquivos/processar/input.mp4",
                "-vf", "fps=1",
                "/home/jackson/git/fiap-videos/fiap-video-processamento/src/main/resources/arquivos/processados/frame_%04d.png"
            };
            processBuilder.command(command);

            Process process = processBuilder.start();

             // Lê a saída do processo
             BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
             String line;
             while ((line = reader.readLine()) != null) {
                 System.out.println(line);
             } 

            int exitCode = process.waitFor();
            
            System.out.println("Processo finalizado com código de saída: " + exitCode);
        } catch (Exception e) {
            LOGGER.error("Erro ao deletar mensagem", e);
        }
    }

    private void deleteMessage(Message message) {
        try {
            DeleteMessageRequest deleteRequest = DeleteMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .receiptHandle(message.receiptHandle())
                    .build();

            sqs.deleteMessage(deleteRequest);
            LOGGER.infof("Mensagem deletada: %s", message.messageId());
        } catch (Exception e) {
            LOGGER.error("Erro ao deletar mensagem", e);
        }
    }

    
}

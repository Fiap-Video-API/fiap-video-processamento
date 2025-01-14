package br.com.jacksonwc2;

import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import org.jboss.logging.Logger;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

@Path("/testar-processamento")
public class ProcessamentoVideosAPI {

    private static final Logger LOGGER = Logger.getLogger(ProcessamentoVideosAPI.class);

    @Inject
    SqsClient sqs;

    @ConfigProperty(name = "queue.processar")
    String queueUrl;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response sendMessage() throws Exception {
        String message = "/home/jackson/git/fiap-videos/fiap-video-processamento/src/main/resources/arquivos/processar/input.mp4";
        SendMessageResponse response = sqs.sendMessage(m -> m.queueUrl(queueUrl).messageBody(message));
        
        LOGGER.info("Mensagem enviada");

        return Response.ok().entity(response.messageId()).build();
    }

    @GET
    public List<String> receive() {
        List<Message> messages = sqs.receiveMessage(m -> m.maxNumberOfMessages(10).queueUrl(queueUrl)).messages();

        return messages.stream()
            .map(Message::body)
            .collect(Collectors.toList());
    }
}

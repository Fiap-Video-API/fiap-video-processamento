package br.com.jacksonwc2.adapter.resource;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.jacksonwc2.core.domain.Video;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;
import software.amazon.awssdk.thirdparty.jackson.core.JsonProcessingException;

@Path("/sqs")
public class VideoResource {

    private static final Logger LOGGER = Logger.getLogger(VideoResource.class);

    @Inject
    SqsClient sqs;

    @ConfigProperty(name = "queue.processar")
    String queueUrl;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response enviarMensagem(Video video) throws Exception, JsonProcessingException {
        LOGGER.info("VideoResource.enviarMensagem: Enviando mensagem");
        
        var message = new ObjectMapper().writeValueAsString(video);
        SendMessageResponse response = sqs.sendMessage(m -> m.queueUrl(queueUrl).messageBody(message));
        
        return Response.ok().entity(response.messageId()).build();
    }
}

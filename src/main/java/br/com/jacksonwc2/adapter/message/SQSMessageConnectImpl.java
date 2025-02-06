package br.com.jacksonwc2.adapter.message;

import java.util.List;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.jacksonwc2.core.domain.ItemFila;
import br.com.jacksonwc2.core.domain.Video;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

@ApplicationScoped
public class SQSMessageConnectImpl implements IMessageConnect {

    private static final Logger LOGGER = Logger.getLogger(SQSMessageConnectImpl.class);
    
    @Inject
    SqsClient sqs;

    @ConfigProperty(name = "queue.processar")
    String queueUrl;

    ReceiveMessageRequest request;

    void configurarReceiveMessageRequest() {
        LOGGER.info("SQSMessageConnect.onStart: Iniciando configuração ReceiveMessageRequest para conexões com SQS");
        
        // Configura o ReceiveMessageRequest com o visibility timeout
        this.request = ReceiveMessageRequest.builder()
            .queueUrl(queueUrl)
            .maxNumberOfMessages(1) // Consome apenas 1 mensagem por vez
            .visibilityTimeout(300) // Mensagem fica invisível por 5 minutos
            .waitTimeSeconds(10) // Long polling, para evitar custos
            .build();
    }

    @Override
    public ItemFila adquirirMensagemFila() {
        LOGGER.info("SQSMessageConnect.adquirirMensagemFila: Buscando mensagens");

        if (request == null) {
            configurarReceiveMessageRequest();
        }

        List<Message> messages = sqs.receiveMessage(request).messages();
                
        if (messages.isEmpty()) {
            LOGGER.info("SQSMessageConnect.adquirirMensagemFila: Nenhuma mensagem na fila");
            return null;
        }

        try {
            LOGGER.info("Mensagem recebida: " + messages.get(0).body());
            ItemFila ret = new ItemFila();
            ret.setOriginalMessage(messages.get(0));
            ret.setVideo(new ObjectMapper().readValue(messages.get(0).body(), Video.class));

			return ret;
		} catch (JsonProcessingException e) {
			e.printStackTrace();
            return null;
		}
    }

    @Override
    public void deletarMensagemFila(Object originalMessage) {
        LOGGER.info("SQSMessageConnect.deletarMensagemFila: Deletando mensagem SQS");

        Message message = (Message) originalMessage;

        try {
            DeleteMessageRequest deleteRequest = DeleteMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .receiptHandle(message.receiptHandle())
                    .build();

            sqs.deleteMessage(deleteRequest);
            LOGGER.infof("SQSMessageConnect.deletarMensagemFila: Mensagem deletada: %s", message.messageId());
        } catch (Exception e) {
            LOGGER.error("SQSMessageConnect.deletarMensagemFila: Erro ao deletar mensagem", e);
        }
    }

	@Override
	public void enviarMensagemFila(String queue, String message) {
        LOGGER.infof("SQSMessageConnect.enviarMensagemFila: %s", message);
		sqs.sendMessage(m -> m.queueUrl(queue).messageBody(message));
	}
        
}

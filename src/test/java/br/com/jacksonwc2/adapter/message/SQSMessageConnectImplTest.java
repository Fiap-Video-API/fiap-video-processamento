package br.com.jacksonwc2.adapter.message;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.core.JsonProcessingException;

import br.com.jacksonwc2.core.domain.ItemFila;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

@ExtendWith(MockitoExtension.class)
public class SQSMessageConnectImplTest {

    @InjectMocks
    private SQSMessageConnectImpl sqsMessageConnect;

    @Mock
    private SqsClient sqs;

    @BeforeEach
    void setUp() {
        sqsMessageConnect.queueUrl = "https://sqs.mock.queue";
    }

    @Test
    void testConfigurarReceiveMessageRequest() {
        sqsMessageConnect.configurarReceiveMessageRequest();

        ReceiveMessageRequest request = sqsMessageConnect.request;
        assertEquals("https://sqs.mock.queue", request.queueUrl());
        assertEquals(1, request.maxNumberOfMessages());
        assertEquals(300, request.visibilityTimeout());
        assertEquals(10, request.waitTimeSeconds());
    }

    @Test
    void testAdquirirMensagemFila_ComMensagem() throws JsonProcessingException {
        Message mockMessage = Message.builder()
                .body("{\"id\":\"12345\",\"pathVideo\":\"video.mp4\"}")
                .receiptHandle("mockHandle")
                .messageId("mockId")
                .build();

        when(sqs.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(ReceiveMessageResponse.builder().messages(mockMessage).build());

        ItemFila itemFila = sqsMessageConnect.adquirirMensagemFila();

        assertEquals("12345", itemFila.getVideo().getId());
        assertEquals("video.mp4", itemFila.getVideo().getPathVideo());
        assertEquals(mockMessage, itemFila.getOriginalMessage());
    }

    @Test
    void testAdquirirMensagemFila_SemMensagem() {
        when(sqs.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(ReceiveMessageResponse.builder().build());

        ItemFila itemFila = sqsMessageConnect.adquirirMensagemFila();

        assertNull(itemFila);
    }

    @Test
    void testAdquirirMensagemFila_ErroDeSerializacao() {
        Message mockMessage = Message.builder()
                .body("{\"invalidJson\":")
                .build();

        when(sqs.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(ReceiveMessageResponse.builder().messages(mockMessage).build());

        ItemFila itemFila = sqsMessageConnect.adquirirMensagemFila();

        assertNull(itemFila);
    }

    @Test
    void testEnviarMensagemFila() {
        String queue = "https://sqs.mock.queue";
        String message = "Teste de mensagem";

        assertDoesNotThrow(() -> sqsMessageConnect.enviarMensagemFila(queue, message));
    }

    @Test
    void testDeletarMensagemFila() {
        Message mockMessage = Message.builder()
                .receiptHandle("mockHandle")
                .messageId("mockId")
                .build();

        assertDoesNotThrow(() -> sqsMessageConnect.deletarMensagemFila(mockMessage));

        verify(sqs, times(1)).deleteMessage(any(DeleteMessageRequest.class));
    }

    @Test
    void testDeletarMensagemFila_Erro() {
        Message mockMessage = Message.builder()
                .receiptHandle("mockHandle")
                .messageId("mockId")
                .build();

        doThrow(RuntimeException.class).when(sqs).deleteMessage(any(DeleteMessageRequest.class));

        sqsMessageConnect.deletarMensagemFila(mockMessage);
    }
}

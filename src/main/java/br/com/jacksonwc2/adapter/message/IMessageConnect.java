package br.com.jacksonwc2.adapter.message;

import br.com.jacksonwc2.core.domain.ItemFila;

/**
 * Responsável pela conexão e gerenciamento do pool de conexões com broker de mensagearia externo.
 */
public interface IMessageConnect {
   
    /**
     * Adquire uma mensagem do broker de mensagearia
     * @return ItemFila
     */
    public ItemFila adquirirMensagemFila();

    /**
     * Enviar uma mensagem para o broker de mensagearia
     * @param queue
     * @param message
     */
    public void enviarMensagemFila(String queue, String message);

    /**
     * Deleta uma mensagem do broker de mensagearia
     * @param originalMessage
     */
    public void deletarMensagemFila(Object originalMessage);

}

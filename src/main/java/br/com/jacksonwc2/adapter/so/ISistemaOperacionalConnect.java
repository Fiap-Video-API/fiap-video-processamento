package br.com.jacksonwc2.adapter.so;

/**
 * Responsável pela conexão com linha de comando do sistema operacional
 */
public interface ISistemaOperacionalConnect {
   
    /**
     * Executa o comando no prompt do Sistema Operacional
     * @param comando
     */
    public void executarComandoSO(String comando);

}

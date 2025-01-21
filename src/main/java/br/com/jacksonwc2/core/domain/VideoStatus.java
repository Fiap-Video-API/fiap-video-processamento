package br.com.jacksonwc2.core.domain;

import lombok.Getter;

@Getter
public enum VideoStatus {

    AGUARDANDO("AGUARDANDO"), 
    PROCESSANDO("PROCESSANDO"), 
    FALHA("FALHA"), 
    FINALIZADO("FINALIZADO");

    String descricao;

    VideoStatus(String descricao){
        this.descricao = descricao;
    }
}

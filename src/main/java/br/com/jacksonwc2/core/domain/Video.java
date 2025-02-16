package br.com.jacksonwc2.core.domain;

import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@RegisterForReflection
public class Video {
    
    String id;
    String idUsuario;
    String emailUsuario;
    String status;
    String pathVideo;
    String pathZip;    
    boolean dowload;

}

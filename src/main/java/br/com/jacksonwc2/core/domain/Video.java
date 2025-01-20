package br.com.jacksonwc2.core.domain;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Video {
    
    String id;
    String status;
    LocalDateTime inicio;
    LocalDateTime fim;
    String pathVideo;
    String pathZip;    

}

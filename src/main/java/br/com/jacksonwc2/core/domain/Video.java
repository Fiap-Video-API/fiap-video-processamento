package br.com.jacksonwc2.core.domain;

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
    String pathVideo;
    String pathZip;    

}

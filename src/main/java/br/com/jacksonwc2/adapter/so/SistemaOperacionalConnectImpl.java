package br.com.jacksonwc2.adapter.so;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import org.jboss.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class SistemaOperacionalConnectImpl implements ISistemaOperacionalConnect {

    private static final Logger LOGGER = Logger.getLogger(SistemaOperacionalConnectImpl.class);
    

	@Override
	public void executarComandoSO(String comando) {
        LOGGER.infof("SistemaOperacionalConnectImpl.executarComandoSO: Processando comando no S.O %s", comando);

        try {
            ProcessBuilder processBuilder = new ProcessBuilder();
            processBuilder.redirectErrorStream(true);

            processBuilder.command(comando);
            Process process = processBuilder.start();

            // Lê a saída do processo
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            } 

            int exitCode = process.waitFor();            
            LOGGER.infof("SistemaOperacionalConnectImpl.executarComandoSO: Processamento comando no S.O finalizado com sucesso. %s", exitCode);
        } catch (IOException | InterruptedException e) {
            LOGGER.error("SistemaOperacionalConnectImpl.executarComandoSO: Erro ao processar comando no S.O", e);
        }
	}
        
}

package br.com.jacksonwc2.core.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

@ApplicationScoped
public class ObjectMapperProducer {

    @Produces
    @ApplicationScoped
    public ObjectMapper createObjectMapper() {
        return new ObjectMapper();
    }
}

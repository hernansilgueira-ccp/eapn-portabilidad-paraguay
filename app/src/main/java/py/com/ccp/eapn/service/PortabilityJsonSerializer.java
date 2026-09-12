package py.com.ccp.eapn.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import py.com.ccp.eapn.model.PinConfirmation;
import py.com.ccp.eapn.model.PortabilityRequest;

public class PortabilityJsonSerializer {

    private final ObjectMapper objectMapper;

    public PortabilityJsonSerializer() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(
            new JavaTimeModule()
        );
        objectMapper.disable(
            SerializationFeature.WRITE_DATES_AS_TIMESTAMPS
        );
    }

    public String serialize(Object value)
        throws JsonProcessingException {

        return objectMapper.writeValueAsString(value);
    }

    public PortabilityRequest deserializeRequest(
        String json
    ) throws JsonProcessingException {

        return objectMapper.readValue(
            json,
            PortabilityRequest.class
        );
    }

    public PinConfirmation deserializePinConfirmation(
        String json
    ) throws JsonProcessingException {

        return objectMapper.readValue(
            json,
            PinConfirmation.class
        );
    }
}
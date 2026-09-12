package py.com.ccp.eapn.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import py.com.ccp.eapn.model.DonorApprovalRequest;
import py.com.ccp.eapn.model.PinConfirmation;
import py.com.ccp.eapn.model.PortabilityRequest;
import py.com.ccp.eapn.model.DonorApprovalResult;
import py.com.ccp.eapn.model.PortabilityAuditEvent;

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

    public DonorApprovalRequest deserializeDonorApprovalRequest(
        String json
    ) throws JsonProcessingException {

        return objectMapper.readValue(
            json,
            DonorApprovalRequest.class
        );
    }
    public DonorApprovalResult deserializeDonorApprovalResult(
        String json
    ) throws JsonProcessingException {

        return objectMapper.readValue(
            json,
            DonorApprovalResult.class
        );
    }
    public PortabilityAuditEvent deserializeAuditEvent(
    String json
) throws JsonProcessingException {

    return objectMapper.readValue(
        json,
        PortabilityAuditEvent.class
    );
}
}
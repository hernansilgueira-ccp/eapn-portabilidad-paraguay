package py.com.ccp.eapn.service;

import org.junit.jupiter.api.Test;
import py.com.ccp.eapn.model.DonorApprovalRequest;
import py.com.ccp.eapn.model.DonorApprovalResult;
import py.com.ccp.eapn.model.Operator;
import py.com.ccp.eapn.model.PortabilityStatus;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class DonorDecisionServiceTest {

    private final DonorDecisionService service =
        new DonorDecisionService();

    @Test
    void shouldApproveValidDocument() {
        DonorApprovalRequest request =
            createRequest("4567890");

        DonorApprovalResult result =
            service.decide(request);

        assertEquals(
            request.requestId(),
            result.requestId()
        );
        assertEquals(
            Operator.TIGO,
            result.donorOperator()
        );
        assertEquals(
            PortabilityStatus.APPROVED,
            result.status()
        );
        assertNull(
            result.rejectionReason()
        );
    }

    @Test
    void shouldRejectDocumentEndingInNine() {
        DonorApprovalRequest request =
            createRequest("4567899");

        DonorApprovalResult result =
            service.decide(request);

        assertEquals(
            request.requestId(),
            result.requestId()
        );
        assertEquals(
            Operator.TIGO,
            result.donorOperator()
        );
        assertEquals(
            PortabilityStatus.REJECTED,
            result.status()
        );
        assertEquals(
            "DOCUMENT_VALIDATION_FAILED",
            result.rejectionReason()
        );
    }

    private DonorApprovalRequest createRequest(
        String documentNumber
    ) {
        return new DonorApprovalRequest(
            UUID.randomUUID(),
            "595981123456",
            documentNumber,
            Operator.TIGO,
            Operator.PERSONAL,
            Instant.now()
        );
    }
}
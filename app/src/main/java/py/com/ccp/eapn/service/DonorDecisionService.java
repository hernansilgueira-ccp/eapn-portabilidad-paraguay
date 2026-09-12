package py.com.ccp.eapn.service;

import py.com.ccp.eapn.model.DonorApprovalRequest;
import py.com.ccp.eapn.model.DonorApprovalResult;

import java.util.Objects;

public class DonorDecisionService {

    private static final String REJECTION_DIGIT =
        "9";

    public DonorApprovalResult decide(
        DonorApprovalRequest request
    ) {
        Objects.requireNonNull(
            request,
            "La solicitud de aprobación es obligatoria"
        );

        if (
            request.documentNumber()
                .endsWith(REJECTION_DIGIT)
        ) {
            return DonorApprovalResult.rejected(
                request.requestId(),
                request.donorOperator(),
                "DOCUMENT_VALIDATION_FAILED"
            );
        }

        return DonorApprovalResult.approved(
            request.requestId(),
            request.donorOperator()
        );
    }
}
package py.com.ccp.eapn.route;

import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import py.com.ccp.eapn.model.DonorApprovalResult;
import py.com.ccp.eapn.service.DonorDecisionService;
import py.com.ccp.eapn.service.PortabilityJsonSerializer;

public class DonorApprovalConsumerRoute
    extends RouteBuilder {

    public static final String INPUT_ENDPOINT =
        "jms:queue:portability.approvals"
            + "?connectionFactory=#jmsConnectionFactory";

    public static final String APPROVED_ENDPOINT =
        "sql:UPDATE portability_requests "
            + "SET status = 'APPROVED', "
            + "rejection_reason = NULL, "
            + "updated_at = CURRENT_TIMESTAMP "
            + "WHERE request_id = CAST(:#requestId AS UUID)"
            + "?dataSource=#dataSource";

    public static final String REJECTED_ENDPOINT =
        "sql:UPDATE portability_requests "
            + "SET status = 'REJECTED', "
            + "rejection_reason = :#rejectionReason, "
            + "updated_at = CURRENT_TIMESTAMP "
            + "WHERE request_id = CAST(:#requestId AS UUID)"
            + "?dataSource=#dataSource";

    public static final String OUTPUT_ENDPOINT =
        "jms:queue:portability.results"
            + "?connectionFactory=#jmsConnectionFactory";

    public static final String DEAD_LETTER_ENDPOINT =
        "jms:queue:portability.errors.dlq"
            + "?connectionFactory=#jmsConnectionFactory";

    private final String inputEndpoint;
    private final String approvedEndpoint;
    private final String rejectedEndpoint;
    private final String outputEndpoint;
    private final String deadLetterEndpoint;
    private final DonorDecisionService decisionService;
    private final PortabilityJsonSerializer serializer;

    public DonorApprovalConsumerRoute() {
        this(
            INPUT_ENDPOINT,
            APPROVED_ENDPOINT,
            REJECTED_ENDPOINT,
            OUTPUT_ENDPOINT,
            DEAD_LETTER_ENDPOINT,
            new DonorDecisionService(),
            new PortabilityJsonSerializer()
        );
    }

    DonorApprovalConsumerRoute(
        String inputEndpoint,
        String approvedEndpoint,
        String rejectedEndpoint,
        String outputEndpoint,
        DonorDecisionService decisionService,
        PortabilityJsonSerializer serializer
    ) {
        this(
            inputEndpoint,
            approvedEndpoint,
            rejectedEndpoint,
            outputEndpoint,
            "mock:donor-errors-dlq",
            decisionService,
            serializer
        );
    }

    DonorApprovalConsumerRoute(
        String inputEndpoint,
        String approvedEndpoint,
        String rejectedEndpoint,
        String outputEndpoint,
        String deadLetterEndpoint,
        DonorDecisionService decisionService,
        PortabilityJsonSerializer serializer
    ) {
        this.inputEndpoint = inputEndpoint;
        this.approvedEndpoint = approvedEndpoint;
        this.rejectedEndpoint = rejectedEndpoint;
        this.outputEndpoint = outputEndpoint;
        this.deadLetterEndpoint = deadLetterEndpoint;
        this.decisionService = decisionService;
        this.serializer = serializer;
    }

    @Override
    public void configure() {
        errorHandler(
            deadLetterChannel(deadLetterEndpoint)
                .useOriginalMessage()
                .maximumRedeliveries(3)
                .redeliveryDelay(1000)
                .retryAttemptedLogLevel(
                    LoggingLevel.WARN
                )
        );

        from(inputEndpoint)
            .routeId("donor-approval-consumer")
            .bean(
                serializer,
                "deserializeDonorApprovalRequest"
            )
            .bean(
                decisionService,
                "decide"
            )
            .process(exchange -> {
                DonorApprovalResult result =
                    exchange.getMessage().getBody(
                        DonorApprovalResult.class
                    );

                exchange.setProperty(
                    "donorApprovalResult",
                    result
                );

                exchange.getMessage().setHeader(
                    "requestId",
                    result.requestId().toString()
                );
                exchange.getMessage().setHeader(
                    "decisionStatus",
                    result.status().name()
                );

                if (result.rejectionReason() != null) {
                    exchange.getMessage().setHeader(
                        "rejectionReason",
                        result.rejectionReason()
                    );
                }
            })
            .choice()
                .when(
                    header("decisionStatus")
                        .isEqualTo("APPROVED")
                )
                    .to(approvedEndpoint)
.setHeader(
    "auditStatus",
    constant("APPROVED")
)
.setHeader(
    "auditDetails",
    constant(
        "Solicitud aprobada por el operador donante"
    )
)
.to(
    PortabilityStatusPublisherRoute.INPUT_ENDPOINT
)
.log(
    "Operador donante aprobó la solicitud "
        + "${header.requestId}"
)
                .otherwise()
                    .to(rejectedEndpoint)
.setHeader(
    "auditStatus",
    constant("REJECTED")
)
.setHeader(
    "auditDetails",
    simple(
        "Solicitud rechazada por el operador "
            + "donante: ${header.rejectionReason}"
    )
)
.to(
    PortabilityStatusPublisherRoute.INPUT_ENDPOINT
)
.log(
    "Operador donante rechazó la solicitud "
        + "${header.requestId}: "
        + "${header.rejectionReason}"
)
            .end()
            .setBody(
                exchangeProperty("donorApprovalResult")
            )
            .bean(
                serializer,
                "serialize"
            )
            .to(outputEndpoint)
            .log(
                "Resultado del operador enviado para "
                    + "${header.requestId}"
            );
    }
}
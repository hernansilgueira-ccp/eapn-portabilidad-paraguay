package py.com.ccp.eapn.route;

import org.apache.camel.ExchangePattern;
import org.apache.camel.builder.RouteBuilder;
import py.com.ccp.eapn.model.DonorApprovalResult;
import py.com.ccp.eapn.model.Operator;
import py.com.ccp.eapn.model.PortabilityStatus;
import py.com.ccp.eapn.model.RecipientNotification;
import py.com.ccp.eapn.service.PortabilityJsonSerializer;

import java.util.Map;

public class PortabilityCompletionRoute
    extends RouteBuilder {

    public static final String INPUT_ENDPOINT =
        "jms:queue:portability.results"
            + "?connectionFactory=#jmsConnectionFactory";

    public static final String SELECT_ENDPOINT =
        "sql:SELECT msisdn, donor_operator, "
            + "recipient_operator "
            + "FROM portability_requests "
            + "WHERE request_id = CAST(:#requestId AS UUID)"
            + "?dataSource=#dataSource"
            + "&outputType=SelectOne";

    public static final String INSERT_PORTED_ENDPOINT =
        "sql:INSERT INTO ported_numbers ("
            + "request_id, msisdn, previous_operator, "
            + "current_operator, ported_at"
            + ") VALUES ("
            + "CAST(:#requestId AS UUID), "
            + ":#msisdn, :#previousOperator, "
            + ":#currentOperator, CURRENT_TIMESTAMP"
            + ") ON CONFLICT (request_id) DO NOTHING"
            + "?dataSource=#dataSource";

    public static final String COMPLETE_ENDPOINT =
        "sql:UPDATE portability_requests "
            + "SET status = 'COMPLETED', "
            + "updated_at = CURRENT_TIMESTAMP "
            + "WHERE request_id = CAST(:#requestId AS UUID) "
            + "AND status = 'APPROVED'"
            + "?dataSource=#dataSource";

    public static final String OUTPUT_ENDPOINT =
        "jms:queue:portability.recipient.notifications"
            + "?connectionFactory=#jmsConnectionFactory";

    private final String inputEndpoint;
    private final String selectEndpoint;
    private final String insertPortedEndpoint;
    private final String completeEndpoint;
    private final String outputEndpoint;
    private final PortabilityJsonSerializer serializer;

    public PortabilityCompletionRoute() {
        this(
            INPUT_ENDPOINT,
            SELECT_ENDPOINT,
            INSERT_PORTED_ENDPOINT,
            COMPLETE_ENDPOINT,
            OUTPUT_ENDPOINT,
            new PortabilityJsonSerializer()
        );
    }

    PortabilityCompletionRoute(
        String inputEndpoint,
        String selectEndpoint,
        String insertPortedEndpoint,
        String completeEndpoint,
        String outputEndpoint,
        PortabilityJsonSerializer serializer
    ) {
        this.inputEndpoint = inputEndpoint;
        this.selectEndpoint = selectEndpoint;
        this.insertPortedEndpoint = insertPortedEndpoint;
        this.completeEndpoint = completeEndpoint;
        this.outputEndpoint = outputEndpoint;
        this.serializer = serializer;
    }

    @Override
    public void configure() {
        from(inputEndpoint)
            .routeId("portability-completion")
            .bean(
                serializer,
                "deserializeDonorApprovalResult"
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
            })
            .to(selectEndpoint)
            .process(exchange -> {
                Map<?, ?> requestData =
                    exchange.getMessage().getBody(
                        Map.class
                    );

                if (requestData == null) {
                    throw new IllegalArgumentException(
                        "No existe la solicitud indicada"
                    );
                }

                DonorApprovalResult result =
                    exchange.getProperty(
                        "donorApprovalResult",
                        DonorApprovalResult.class
                    );

                String msisdn = String.valueOf(
                    value(requestData, "msisdn")
                );

                Operator donorOperator =
                    Operator.valueOf(
                        String.valueOf(
                            value(
                                requestData,
                                "donor_operator"
                            )
                        )
                    );

                Operator recipientOperator =
                    Operator.valueOf(
                        String.valueOf(
                            value(
                                requestData,
                                "recipient_operator"
                            )
                        )
                    );

                exchange.getMessage().setHeader(
                    "msisdn",
                    msisdn
                );
                exchange.getMessage().setHeader(
                    "previousOperator",
                    donorOperator.name()
                );
                exchange.getMessage().setHeader(
                    "currentOperator",
                    recipientOperator.name()
                );

                RecipientNotification notification;

                if (
                    result.status()
                        == PortabilityStatus.APPROVED
                ) {
                    notification =
                        RecipientNotification.completed(
                            result.requestId(),
                            msisdn,
                            recipientOperator
                        );
                } else {
                    notification =
                        RecipientNotification.rejected(
                            result.requestId(),
                            msisdn,
                            recipientOperator,
                            result.rejectionReason()
                        );
                }

                exchange.setProperty(
                    "recipientNotification",
                    notification
                );
            })
            .choice()
                .when(
                    header("decisionStatus")
                        .isEqualTo("APPROVED")
                )
                    .to(insertPortedEndpoint)
                    .to(completeEndpoint)
                    .log(
                        "Portabilidad completada para "
                            + "${header.msisdn}"
                    )
                .otherwise()
                    .log(
                        "Portabilidad rechazada para "
                            + "${header.msisdn}"
                    )
            .end()
            .setBody(
                exchangeProperty(
                    "recipientNotification"
                )
            )
            .bean(
                serializer,
                "serialize"
            )
            .setExchangePattern(
                ExchangePattern.InOnly
            )
            .to(outputEndpoint)
            .log(
                "Notificación final enviada al operador "
                    + "${header.currentOperator}"
            );
    }

    private static Object value(
        Map<?, ?> values,
        String expectedKey
    ) {
        return values.entrySet()
            .stream()
            .filter(entry ->
                expectedKey.equalsIgnoreCase(
                    String.valueOf(entry.getKey())
                )
            )
            .map(Map.Entry::getValue)
            .findFirst()
            .orElse(null);
    }
}
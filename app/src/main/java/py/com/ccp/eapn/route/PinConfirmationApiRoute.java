package py.com.ccp.eapn.route;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;

public class PinConfirmationApiRoute
    extends RouteBuilder {

    public static final String API_ENDPOINT =
        "netty-http:http://0.0.0.0:8082"
            + "/api/portability/pin-confirmations"
            + "?httpMethodRestrict=POST";

    @Override
    public void configure() {
        onException(Exception.class)
            .handled(true)
            .setHeader(
                Exchange.HTTP_RESPONSE_CODE,
                constant(400)
            )
            .setHeader(
                Exchange.CONTENT_TYPE,
                constant("application/json")
            )
            .setBody(
                constant(
                    """
                    {
                      "status": "ERROR",
                      "message": "No fue posible confirmar el PIN"
                    }
                    """
                )
            );

        from(API_ENDPOINT)
            .routeId("pin-confirmation-api")
            .convertBodyTo(String.class)
            .to(PinConfirmationRoute.INPUT_ENDPOINT)
            .setHeader(
                Exchange.CONTENT_TYPE,
                constant("application/json")
            )
            .choice()
                .when(
                    header("confirmationStatus")
                        .isEqualTo("CONFIRMED")
                )
                    .setHeader(
                        Exchange.HTTP_RESPONSE_CODE,
                        constant(200)
                    )
                    .setBody(
                        simple(
                            """
                            {
                              "requestId": "${header.requestId}",
                              "status": "CONFIRMED"
                            }
                            """
                        )
                    )
                .otherwise()
                    .setHeader(
                        Exchange.HTTP_RESPONSE_CODE,
                        constant(422)
                    )
                    .setBody(
                        simple(
                            """
                            {
                              "requestId": "${header.requestId}",
                              "status": "REJECTED",
                              "reason": "${header.rejectionReason}"
                            }
                            """
                        )
                    )
            .end();
    }
}
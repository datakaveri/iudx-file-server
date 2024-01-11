package iudx.file.server.apiserver.integrationtests;

import io.restassured.RestAssured;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import iudx.file.server.configuration.Configuration;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import static io.restassured.RestAssured.*;

/**
 * JUnit5 extension to allow {@link RestAssured} configuration to be injected into all integration
 * tests using {@link org.junit.jupiter.api.extension.ExtendWith}.
 */
public class RestAssuredConfiguration implements BeforeAllCallback {

    @Override
    public void beforeAll(ExtensionContext context) {
        Vertx vertx = Vertx.vertx();
        Configuration fileServerConfig = new Configuration();
        JsonObject config = fileServerConfig.configLoader(0, vertx);
        String testHost = config.getString("ip");
        //System.out.println(testHost);
        if (testHost != null) {
            baseURI = "http://" + testHost;
        } else {
            baseURI = "http://localhost";
        }

        String testPort = config.getString("httpPort");
        //System.out.println(testPort);

        if (testPort != null) {
            port = Integer.parseInt(testPort);
        } else {
            port = 8443;
        }
        basePath = "/iudx/v1";
        //System.out.println("This is the base path and port!"+ basePath + port);

        enableLoggingOfRequestAndResponseIfValidationFails();

    }
}

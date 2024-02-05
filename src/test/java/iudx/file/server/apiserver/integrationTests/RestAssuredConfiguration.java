package iudx.file.server.apiserver.integrationtests;

import io.restassured.RestAssured;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import iudx.file.server.authenticator.TokenSetup;
import iudx.file.server.configuration.Configuration;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.restassured.RestAssured.*;
import static iudx.file.server.authenticator.TokensForITs.*;

/**
 * JUnit5 extension to allow {@link RestAssured} configuration to be injected into all integration
 * tests using {@link org.junit.jupiter.api.extension.ExtendWith}.
 */
public class RestAssuredConfiguration implements BeforeAllCallback {

    private static final Logger logger = LoggerFactory.getLogger(RestAssuredConfiguration.class);

    @Override
    public void beforeAll(ExtensionContext context) {
        Vertx vertx = Vertx.vertx();
        Configuration fileServerConfig = new Configuration();
        JsonObject config = fileServerConfig.configLoader(0, vertx);
        String testHost = config.getString("ip");
        //  String testHost = System.getProperty("intTestHost");
        //System.out.println("testHost:"+testHost);
        JsonObject config2 = fileServerConfig.configLoader(1, vertx);
        String authServerHost = config2.getString("authHost");

        if (testHost != null) {
            baseURI = "http://" + testHost;
        } else {
            baseURI = "http://localhost";
        }

        String testPort = config.getString("httpPort");
        // String testPort = System.getProperty("intTestPort");
        if (testPort != null) {
            port = Integer.parseInt(testPort);
        } else {
            port = 8443;
        }
        // System.out.println(testPort+","+testHost);
        basePath = "/iudx/v1";
        String dxAuthBasePath = "auth/v1";
        String authEndpoint = "https://"+ authServerHost + "/" + dxAuthBasePath +  "/token";
        String proxyHost = System.getProperty("intTestProxyHost");
        String proxyPort = System.getProperty("intTestProxyPort");

        JsonObject clientCredentials = config2.getJsonObject("clientCredentials");
        String clientId = clientCredentials.getString("clientID");
        String clientSecret = clientCredentials.getString("clientSecret");
        String delegationId = clientCredentials.getString("delegationId");
        if (proxyHost != null && proxyPort != null) {
            proxy(proxyHost, Integer.parseInt(proxyPort));
        }
        logger.info("setting up the tokens");
        TokenSetup.setupTokens(authEndpoint, clientId, clientSecret, delegationId);

        // Wait for tokens to be available before proceeding
        waitForTokens();
        enableLoggingOfRequestAndResponseIfValidationFails();
    }
    private void waitForTokens() {
        int maxAttempts = 5;
        int attempt = 0;

        // Keep trying to get tokens until they are available or max attempts are reached
        while ((secureResourceToken == null || adminToken == null  || openResourceToken==null || delegateToken==null ) && attempt < maxAttempts) {
            logger.info("Waiting for tokens to be available. Attempt: " + (attempt + 1));
            // Introduce a delay between attempts
            try {
                Thread.sleep(3000); // Adjust the delay as we needed
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            attempt++;
        }

        if (secureResourceToken == null || adminToken == null ||  openResourceToken==null || delegateToken==null ) {
            // Log an error or throw an exception if tokens are still not available
            throw new RuntimeException("Failed to retrieve tokens after multiple attempts.");
        } else {
            logger.info("Tokens are now available. Proceeding with RestAssured configuration.");
        }
    }
}

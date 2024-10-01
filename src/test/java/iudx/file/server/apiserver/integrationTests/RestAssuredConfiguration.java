package iudx.file.server.apiserver.integrationTests;

import io.restassured.RestAssured;
import io.restassured.filter.log.LogDetail;
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
  private static String rsId;
  private static String openRsId;
  private static String openRsGroupId;
  private static String nonExistingArchiveId;
  private static String fileDownloadURL;

  // Getter methods for configuration values
  public static String getRsId() {
    return rsId;
  }

  public static String getOpenRsId() {
    return openRsId;
  }

  public static String getOpenRsGroupId() {
    return openRsGroupId;
  }

  public static String getNonExistingArchiveId() {
    return nonExistingArchiveId;
  }

  public static String getFileDownloadURL() {
    return fileDownloadURL;
  }

  @Override
  public void beforeAll(ExtensionContext context) {
    Vertx vertx = Vertx.vertx();
    Configuration fileServerConfig = new Configuration();
    JsonObject config = fileServerConfig.configLoader(0, vertx);
    JsonObject testValues = config.getJsonObject("testValues");
    Boolean isSsl = config.getBoolean("ssl");
    rsId = testValues.getString("rsId");
    openRsId = testValues.getString("openRsId");
    openRsGroupId = testValues.getString("openRsGroupId");
    nonExistingArchiveId = testValues.getString("nonExistingArchiveId");
    fileDownloadURL = testValues.getString("fileDownloadURL");

    JsonObject config2 = fileServerConfig.configLoader(1, vertx);
    String authServerHost = config2.getString("authHost");
    String  audience = config2.getString("audience");

    boolean testOnDepl = Boolean.parseBoolean(System.getProperty("intTestDepl"));
    if (testOnDepl) {
      String testHost = config.getString("host");
      baseURI = "https://" + testHost;
      port = 443;
    } else {
      String testHost = System.getProperty("intTestHost");
      //String testHost = config.getString("host");
      String httpProtocol = isSsl ? "https://" : "http://";
      if (testHost != null) {
        baseURI = httpProtocol + testHost;
      } else {
        baseURI = httpProtocol + "localhost";
      }

      String testPort = System.getProperty("intTestPort");

      if (testPort != null) {
        port = Integer.parseInt(testPort);
      } else {
        port = 8443;
      }
    }

    basePath = "/iudx/v1";
    String dxAuthBasePath = "auth/v1";
    String authEndpoint = "https://" + authServerHost + "/" + dxAuthBasePath + "/token";
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
    TokenSetup.setupTokens(authEndpoint, clientId, clientSecret, delegationId,audience);

    // Wait for tokens to be available before proceeding
    waitForTokens();
    enableLoggingOfRequestAndResponseIfValidationFails(LogDetail.BODY);
  }

  private void waitForTokens() {
    int maxAttempts = 5;
    int attempt = 0;

    // Keep trying to get tokens until they are available or max attempts are reached
    while ((secureResourceToken == null || adminToken == null || openResourceToken == null || delegateToken == null) && attempt < maxAttempts) {
      logger.info("Waiting for tokens to be available. Attempt: " + (attempt + 1));
      // Introduce a delay between attempts
      try {
        Thread.sleep(3000); // Adjust the delay as we aneeded
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      attempt++;
    }

    if (secureResourceToken == null || adminToken == null || openResourceToken == null || delegateToken == null) {
      // Log an error or throw an exception if tokens are still not available
      throw new RuntimeException("Failed to retrieve tokens after multiple attempts.");
    } else {
      logger.info("Tokens are now available. Proceeding with RestAssured configuration.");
    }
  }
}

/**
 * 
 */
package iudx.file.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.net.JksOptions;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;



/**
 * @author Ankita.Mishra
 *
 */
@ExtendWith(VertxExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class FileServerTest {


  static FileServerVerticle fileserver;
  private static final Logger logger = LoggerFactory.getLogger(FileServerTest.class);
  private static final int PORT = 8443;
  private static final String BASE_URL = "example.com";
  private static WebClient client;
  private static String keystore, keystorePassword, truststore, truststorePassword;
  private static InputStream inputstream;
  private static Properties properties;
  /* get the file server token */
  String fileServerToken = "88b281be-5a56-42b5-a09f-c45a202a8bfc";
  String invalidToken = "5b99518b-50a8-420b-8c5f-108ba8a";
  String userToken = "aabbcc";

  @BeforeAll
  public static void setup(Vertx vertx, VertxTestContext testContext) {



    properties = new Properties();
    inputstream = null;
    try {
      inputstream = new FileInputStream(Constants.CONFIG_FILE);
      properties.load(inputstream);



      keystore = properties.getProperty(Constants.KEYSTORE_FILE_NAME);
      keystorePassword = properties.getProperty(Constants.KEYSTORE_FILE_PASSWORD);
      truststore = properties.getProperty("truststore");
      truststorePassword = properties.getProperty("truststorePassword");

    } catch (Exception ex) {
      logger.info(ex.toString());
    }

    WebClientOptions clientOptions = new WebClientOptions().setSsl(true).setVerifyHost(false)
        .setKeyStoreOptions(new JksOptions().setPath(keystore).setPassword(keystorePassword))
        .setTrustStoreOptions(new JksOptions().setPath(truststore).setPassword(truststorePassword))
        .setDefaultHost(BASE_URL).setDefaultPort(PORT);
    client = WebClient.create(vertx, clientOptions);

    testContext.completeNow();
  }

  @Timeout(value = 2, timeUnit = TimeUnit.MINUTES)
  @Test
  @DisplayName("Testing successful (authorised) file download")
  @Order(1)
  void successDownloadFile(VertxTestContext testContext) {
    String apiURL = "/file/abc";

    client.get(PORT, BASE_URL, apiURL).putHeader("token", userToken)
        .putHeader("fileServerToken", fileServerToken).send(ar -> {
          if (ar.succeeded()) {
            System.out.println("status code 1: " + ar.result().statusCode());
            assertEquals(HttpStatus.SC_OK, ar.result().statusCode());
            testContext.completeNow();
          } else if (ar.failed()) {
            System.out.println(ar.cause());
            testContext.failed();
          }
        });
  }


  @Timeout(value = 2, timeUnit = TimeUnit.MINUTES)
  @Test
  @DisplayName("Testing unauthorised file download")
  @Order(2)
  void unauthorisedDownloadFile(VertxTestContext testContext) {
    String apiURL = "/file/abc";

    client.get(PORT, BASE_URL, apiURL).putHeader("token", userToken)
        .putHeader("fileServerToken", invalidToken).send(ar -> {
          if (ar.succeeded()) {
            System.out.println("status code 2: " + ar.result().statusCode());
            assertEquals(HttpStatus.SC_UNAUTHORIZED, ar.result().statusCode());
            testContext.completeNow();
          } else if (ar.failed()) {

            System.out.println(ar.cause());
            testContext.failed();
          }
        });
  }



  @Timeout(value = 2, timeUnit = TimeUnit.MINUTES)
  @Test
  @DisplayName("Testing successful(authorised) file delete")
  @Order(3)
  void successDeleteFile(VertxTestContext testContext) {
    String apiURL = "/file/abc";

    client.delete(PORT, BASE_URL, apiURL).putHeader("token", userToken)
        .putHeader("fileServerToken", fileServerToken).send(ar -> {
          if (ar.succeeded()) {
            System.out.println("status code 3: " + ar.result().statusCode());
            assertEquals(HttpStatus.SC_OK, ar.result().statusCode());
            testContext.completeNow();
          } else if (ar.failed()) {
            testContext.failed();
          }
        });
  }

  @Disabled
  @Timeout(value = 2, timeUnit = TimeUnit.MINUTES)
  @Test
  @DisplayName("Testing file doesnot exist failed file delete")
  @Order(4)
  void failedDeleteFile(VertxTestContext testContext) {
    String apiURL = "/file/abc";
    client.delete(PORT, BASE_URL, apiURL).putHeader("token", userToken)
        .putHeader("fileServerToken", fileServerToken).send(ar -> {
         
          if (ar.succeeded()) {
            System.out.println("status code 4: " + ar.result().statusCode());
            assertEquals(HttpStatus.SC_NOT_FOUND, ar.result().statusCode());
            testContext.completeNow();
          } else if (ar.failed()) {
            testContext.failed();
          }
        });
  }


  @Timeout(value = 2, timeUnit = TimeUnit.MINUTES)
  @Test
  @DisplayName("Testing unauthorised file delete")
  @Order(5)
  void unauthorisedDeleteFile(VertxTestContext testContext) {
    String apiURL = "/file/abc";

    client.delete(PORT, BASE_URL, apiURL).putHeader("token", userToken)
        .putHeader("fileServerToken", invalidToken).send(ar -> {
          if (ar.succeeded()) {
            System.out.println("status code 5: " + ar.result().statusCode());
            assertEquals(HttpStatus.SC_UNAUTHORIZED, ar.result().statusCode());
            testContext.completeNow();
          } else if (ar.failed()) {
            System.out.println(ar.cause());
            testContext.failed();
          }
        });
  }

  @Disabled 
  @Timeout(value = 2, timeUnit = TimeUnit.MINUTES)
  @Test
  @DisplayName("Testing authorised failed/file doesnot exist file download")
  @Order(6)
  void failedDownloadFile(VertxTestContext testContext) {
    String apiURL = "/file/abc";

    client.get(PORT, BASE_URL, apiURL).putHeader("token", userToken)
        .putHeader("fileServerToken", fileServerToken).send(ar -> {
          if (ar.succeeded()) {
            System.out.println("status code 6: " + ar.result().statusCode());
            assertEquals(HttpStatus.SC_NOT_FOUND, ar.result().statusCode());
            testContext.completeNow();
          } else if (ar.failed()) {
            System.out.println(ar.cause());
            testContext.failed();
          }
        });
  }
}

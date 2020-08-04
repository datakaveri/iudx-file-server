/**
 * 
 */
package iudx.file.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
  private static final Logger LOGGER = LoggerFactory.getLogger(FileServerTest.class);
  private static final int PORT = 8443;
  private static final String BASE_URL = "127.0.0.1";
  private static WebClient client;



  @BeforeAll
  public static void setup(Vertx vertx, VertxTestContext testContext) {
    WebClientOptions clientOptions = new WebClientOptions().setSsl(true).setVerifyHost(false)
        .setTrustAll(true).setDefaultHost(BASE_URL).setDefaultPort(PORT);
    client = WebClient.create(vertx, clientOptions);

    testContext.completeNow();
  }

  @Timeout(value = 2, timeUnit = TimeUnit.MINUTES)
  @Test
  @DisplayName("Testing successful file download")
  @Order(1)
  void successDownloadFile(VertxTestContext testContext) {
    String apiURL = "/file/abc.png";

    client.get(PORT, BASE_URL, apiURL).send(ar -> {
      if (ar.succeeded()) {
        System.out.println("success : " + ar.result().statusCode());
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
  @DisplayName("Testing failed/file doesnot exist file download")
  @Order(2)
  void failedDownloadFile(VertxTestContext testContext) {
    String apiURL = "/file/abc.png";

    client.get(PORT, BASE_URL, apiURL).send(ar -> {
      if (ar.succeeded()) {
        System.out.println("failure : " + ar.result().statusCode());
        assertEquals(HttpStatus.SC_NOT_FOUND, ar.result().statusCode());
        testContext.completeNow();
      } else if (ar.failed()) {
        System.out.println(ar.cause());
        testContext.failed();
      }
    });
  }

  @Test
  @DisplayName("Testing successful file delete")
  @Order(3)
  void successDeleteFile(VertxTestContext testContext) {
    String apiURL = "/file";

    client.delete(PORT, BASE_URL, apiURL).send(ar -> {

      if (ar.succeeded()) {
        assertEquals(HttpStatus.SC_OK, ar.result().statusCode());
        testContext.completeNow();
      } else if (ar.failed()) {
        testContext.failed();
      }
    });
  }


  @Test
  @DisplayName("Testing file doesnot exist failed file delete")
  @Order(4)
  void failedDeleteFile(VertxTestContext testContext) {
    String apiURL = "/file";

    client.delete(PORT, BASE_URL, apiURL).send(ar -> {
      System.out.println("ar.result() :" + ar.result().statusMessage());

      if (ar.succeeded()) {
        assertEquals(HttpStatus.SC_NOT_FOUND, ar.result().statusCode());
        testContext.completeNow();
      } else if (ar.failed()) {
        testContext.failed();
      }
    });
  }


}

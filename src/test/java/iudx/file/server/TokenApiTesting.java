package iudx.file.server;

import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.net.JksOptions;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.file.server.configuration.Configuration;

/**
 * @author Umesh.Pacholi
 *
 */
@ExtendWith(VertxExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TokenApiTesting {
  static FileServerVerticle fileserver;
  private static final Logger logger = LoggerFactory.getLogger(TokenApiTesting.class);
  private static final int PORT = 8443;
  private static final String BASE_URL = "localhost";
  private static WebClient client;
  private static Vertx vertx;
  private static Properties properties;
  private static InputStream inputstream;
  private static String keystore, keystorePassword, truststore, truststorePassword;

  private static Configuration config;

  @DisplayName("BeforeAll")
  @BeforeAll
  public static void startFileServerVerticle(VertxTestContext vertxTestContext,
      io.vertx.reactivex.core.Vertx vertx2) {
    System.out.println("BeforeAll called");
    vertx = Vertx.vertx();
    deployFileServerVerticle(vertx).onComplete(h -> {
      if (h.succeeded() && h.result().getBoolean("deployed")) {
        System.out.println("FileServerVerticle deployed successfully");
        vertxTestContext.completeNow();
      }
    });

    config = new Configuration();
    JsonObject apiConfig = config.configLoader(0, vertx2);

    keystore = apiConfig.getString(apiConfig.getString("keystore"));
    keystorePassword = apiConfig.getString(apiConfig.getString("keystorePassword"));
    truststore = apiConfig.getString("truststore");
    truststorePassword = apiConfig.getString("truststorePassword");

  }

  static Future<JsonObject> deployFileServerVerticle(Vertx vrtx) {
    Promise<JsonObject> promise = Promise.promise();
    JsonObject jsonObject = new JsonObject();
    DeploymentOptions options = new DeploymentOptions().setWorker(true).setWorkerPoolSize(10);
    vrtx.deployVerticle(new FileServerVerticle(), options, result -> {
      if (result.succeeded()) {
        jsonObject.put("deployed", true);
        promise.complete(jsonObject);
      } else {
        logger.info("The FileServerVerticle failed !");
        promise.fail("The FileServerVerticle failed !");
      }
    });

    return promise.future();
  }


  @Test
  @DisplayName("Success - validCertificate,password,userToken ")
  @Order(1)
  void successToken(VertxTestContext vertxTestContext) throws InterruptedException {
    Thread.sleep(8000);
    System.out.println("***** successToken called *****");
    String apiURL = "/token";
    WebClientOptions clientOptions = new WebClientOptions().setSsl(true).setVerifyHost(false)
        .setKeyStoreOptions(new JksOptions().setPath(keystore).setPassword(keystorePassword))
        .setTrustStoreOptions(new JksOptions().setPath(truststore).setPassword(truststorePassword));
    VertxOptions vertxOptions = new VertxOptions();
    vertxOptions.setMaxEventLoopExecuteTime(30);
    vertxOptions.setMaxEventLoopExecuteTimeUnit(TimeUnit.SECONDS);
    Vertx vrtx = Vertx.vertx(vertxOptions);
    client = WebClient.create(vrtx, clientOptions);
    HttpRequest<Buffer> req = client.get(PORT, BASE_URL, apiURL);
    req.putHeader("token", "userToken");
    req.send(ar -> {
      if (ar.succeeded()) {
        logger.info("ar.result() : " + ar.result().bodyAsString());
        vertxTestContext.completeNow();
      } else {
        logger.info("ar.cause() : " + ar.cause());
        vertxTestContext.failNow(ar.cause());
      }
    });
  }

  @Test
  @DisplayName("failure - userToken not provided")
  @Order(2)
  void failureToken_No_userToken(VertxTestContext vertxTestContext) throws InterruptedException {
    System.out.println("***** failureToken_No_userToken called *****");
    String apiURL = "/token";
    WebClientOptions clientOptions = new WebClientOptions().setSsl(true).setVerifyHost(false)
        .setKeyStoreOptions(new JksOptions().setPath(keystore).setPassword("password"))
        .setTrustStoreOptions(new JksOptions().setPath(truststore).setPassword("password"));
    Vertx vrtx = Vertx.vertx();
    client = WebClient.create(vrtx, clientOptions);
    HttpRequest<Buffer> req = client.get(PORT, BASE_URL, apiURL);
    req.send(ar -> {
      if (ar.succeeded()) {
        logger.info("ar.result() : " + ar.result().bodyAsString());
        vertxTestContext.completeNow();
      } else {
        logger.info("ar.cause() : " + ar.cause());
        vertxTestContext.failNow(ar.cause());
      }
    });
  }

  @Test
  @DisplayName("failure - ssl not enabled")
  @Order(3)
  void failureToken_SSL_Not_Enabled(VertxTestContext vertxTestContext) throws InterruptedException {
    System.out.println("***** failureToken_SSL_Not_Enabled called *****");
    String apiURL = "/token";
    WebClientOptions clientOptions = new WebClientOptions().setSsl(false).setVerifyHost(false)
        .setKeyStoreOptions(new JksOptions().setPath(keystore).setPassword("password"))
        .setTrustStoreOptions(new JksOptions().setPath(truststore).setPassword("password"));
    Vertx vrtx = Vertx.vertx();
    client = WebClient.create(vrtx, clientOptions);
    HttpRequest<Buffer> req = client.get(PORT, BASE_URL, apiURL);
    req.putHeader("token", "userToken");
    req.send(ar -> {
      if (ar.succeeded()) {
        logger.info("ar.result() : " + ar.result().bodyAsString());
        vertxTestContext.completeNow();
      } else {
        logger.info("reponse from server : " + ar.cause());
        vertxTestContext.failNow(ar.cause());
      }
    });
  }

  @Test
  @DisplayName("failure - Invalid Certificate")
  @Order(4)
  void failureTokenInvalidCertificate(VertxTestContext vertxTestContext)
      throws InterruptedException {
    System.out.println("***** failureTokenInvalidCertificate called *****");
    String apiURL = "/token";
    WebClientOptions clientOptions = new WebClientOptions().setSsl(true).setVerifyHost(false)
        .setKeyStoreOptions(new JksOptions().setPath(keystore).setPassword("wrong_password"))
        .setTrustStoreOptions(new JksOptions().setPath(truststore).setPassword("wrong_password"));
    Vertx vrtx = Vertx.vertx();
    client = WebClient.create(vrtx, clientOptions);
    HttpRequest<Buffer> req = client.get(PORT, BASE_URL, apiURL);
    req.putHeader("token", "userToken");
    req.send(ar -> {
      if (ar.succeeded()) {
        logger.info("ar.result() : " + ar.result().bodyAsString());
        vertxTestContext.completeNow();
      } else {
        logger.info("ar.cause() : " + ar.cause());
        vertxTestContext.failNow(ar.cause());
      }
    });
  }

}

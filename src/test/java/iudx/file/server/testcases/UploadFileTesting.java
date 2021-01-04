package iudx.file.server.testcases;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import org.apache.http.HttpStatus;
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
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.net.JksOptions;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.multipart.MultipartForm;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.file.server.FileServerVerticle;
import iudx.file.server.utilities.Constants;


/**
 * @author Umesh.Pacholi
 *
 */
@ExtendWith(VertxExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class UploadFileTesting {

  static FileServerVerticle fileserver;
  private static final Logger logger = LoggerFactory.getLogger(UploadFileTesting.class);
  private static final int PORT = 8443;
  private static final String BASE_URL = "localhost";
  private static WebClient client;
  private static Vertx vertx;
  private static Properties properties;
  private static InputStream inputstream;
  private static String keystore, keystorePassword, truststore, truststorePassword;

  @DisplayName("BeforeAll")
  @BeforeAll
  public static void startFileServerVerticle(VertxTestContext vertxTestContext) {
    System.out.println("BeforeAll called");
    vertx = Vertx.vertx();
    deployFileServerVerticle(vertx).onComplete(h -> {
      if (h.succeeded() && h.result().getBoolean("deployed")) {
        System.out.println("FileServerVerticle deployed successfully");
        vertxTestContext.completeNow();
      }
    });

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
  @Timeout(value = 30, timeUnit = TimeUnit.SECONDS)
  @DisplayName("Success - file upload")
  @Order(1)
  void successUploadFile(VertxTestContext vertxTestContext) throws InterruptedException {
    Thread.sleep(5000);
    System.out.println("successUploadFile called >>>>");
    String apiURL = "/file";
    WebClientOptions clientOptions = new WebClientOptions().setSsl(true).setVerifyHost(false)
        .setKeyStoreOptions(new JksOptions().setPath(keystore).setPassword(keystorePassword))
        .setTrustStoreOptions(new JksOptions().setPath(truststore).setPassword(truststorePassword));
    /*
     * VertxOptions vertxOptions = new VertxOptions(); vertxOptions.setMaxEventLoopExecuteTime(30);
     * vertxOptions.setMaxEventLoopExecuteTimeUnit(TimeUnit.SECONDS);
     * vertxOptions.setMaxWorkerExecuteTime(60);
     * vertxOptions.setMaxWorkerExecuteTimeUnit(TimeUnit.SECONDS); Vertx vrtx =
     * Vertx.vertx(vertxOptions); client = WebClient.create(vrtx, clientOptions);
     */

    client = WebClient.create(Vertx.vertx(), clientOptions);
    HttpRequest<Buffer> req = client.post(PORT, BASE_URL, apiURL).ssl(Boolean.TRUE);
    req.putHeader("token", "testing_key");
    req.putHeader("fileServerToken", "fileServerToken");
    MultipartForm form = MultipartForm.create().binaryFileUpload("text", "TestUploadFile.txt",
        "D:/IUDX_UploadDownload_Testing/TestUploadFile.txt", "text/plain");
    // MultipartForm form = MultipartForm.create().binaryFileUpload("mp4", "sample-mp4-file",
    // "D:/IUDX_UploadDownload_Testing/sample-mp4-file.mp4", "video/mp4");

    // below zip file is more than 1GB and this case displays warning as vertx event-loop-thread
    // blocked
    // MultipartForm form = MultipartForm.create().binaryFileUpload("fileParameter",
    // "elasticsearch.zip", "D:/IUDX_UploadDownload_Testing/elasticsearch.zip", "application/zip");

    req.sendMultipartForm(form, ar -> {
      logger
          .info("Inside successUploadFile testcase response - ar.succeeded() : " + ar.succeeded());
      if (ar.succeeded() && ar.result().statusCode() == HttpStatus.SC_OK) {
        vertxTestContext.completeNow();
      } else {
        logger.info("ar.cause() : " + ar.cause());
        vertxTestContext.failed();
      }

    });
  }

  @Test
  @Timeout(value = 2, timeUnit = TimeUnit.SECONDS)
  @DisplayName("failure - without client Certificate")
  @Order(2)
  void failureUploadFileWhenNoClientCertificate(VertxTestContext vertxTestContext)
      throws InterruptedException {
    System.out.println("failureUploadFileWhenNoClientCertificate called >>>>");
    String apiURL = "/file";
    WebClientOptions clientOptions = new WebClientOptions().setSsl(true).setVerifyHost(false);
    client = WebClient.create(Vertx.vertx(), clientOptions);
    HttpRequest<Buffer> req = client.post(PORT, BASE_URL, apiURL).ssl(Boolean.TRUE);
    req.putHeader("token", "testing_key");
    req.putHeader("fileServerToken", "fileServerToken");
    MultipartForm form = MultipartForm.create().binaryFileUpload("text", "TestUploadFile.txt",
        "D:/IUDX_UploadDownload_Testing/TestUploadFile.txt", "text/plain");
    req.sendMultipartForm(form, ar -> {
      logger.info(
          "Inside failureUploadFileWhenNoClientCertificate testcase response - ar.succeeded() : "
              + ar.succeeded());
      if (ar.succeeded()) {
        Integer statusCode = ar.result().bodyAsJsonObject().getInteger("statusCode");
        if (statusCode == HttpStatus.SC_OK)
          vertxTestContext.completeNow();
        else {
          logger.info(" Test being failed because of statusCode : " + statusCode);
          vertxTestContext.failed();
        }
      } else {
        logger.info("ar.cause() : " + ar.cause());
        vertxTestContext.failed();
      }
    });
  }

  @Test
  @DisplayName("failure - without token")
  @Order(3)
  void failureUploadFileWhenNoToken(VertxTestContext vertxTestContext) throws InterruptedException {
    System.out.println("failureUploadFileWhenNoToken called >>>>");
    String apiURL = "/file";
    WebClientOptions clientOptions = new WebClientOptions().setSsl(true).setVerifyHost(false)
        .setKeyStoreOptions(new JksOptions().setPath(keystore).setPassword(keystorePassword))
        .setTrustStoreOptions(new JksOptions().setPath(truststore).setPassword(truststorePassword));
    client = WebClient.create(Vertx.vertx(), clientOptions);
    HttpRequest<Buffer> req = client.post(PORT, BASE_URL, apiURL).ssl(Boolean.TRUE);
    // req.putHeader("token", "testing_key");
    // req.putHeader("fileServerToken", "fileServerToken");
    MultipartForm form = MultipartForm.create().binaryFileUpload("text", "TestUploadFile.txt",
        "D:/IUDX_UploadDownload_Testing/TestUploadFile.txt", "text/plain");
    req.sendMultipartForm(form, ar -> {
      if (ar.succeeded()) {
        Integer statusCode = ar.result().bodyAsJsonObject().getInteger("statusCode");
        logger.info(
            "Inside failureUploadFileWhenNoToken testcase response - statusCode : " + statusCode);
        if (statusCode == HttpStatus.SC_OK) {
          vertxTestContext.completeNow();
        } else {
          logger.info(" Test being failed because of statusCode : " + statusCode);
          vertxTestContext.failed();
        }
      } else {
        logger.info("ar.cause() : " + ar.cause());
        vertxTestContext.failed();
      }

    });
  }

}

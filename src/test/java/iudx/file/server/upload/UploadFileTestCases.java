package iudx.file.server.upload;

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
import io.vertx.core.VertxOptions;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.FileSystem;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.multipart.MultipartForm;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.file.server.FileServerVerticle;
 

/**
 * @author Umesh.Pacholi 
 *
 */
@ExtendWith(VertxExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class UploadFileTestCases {

  static FileServerVerticle fileserver;
  private static final Logger logger = LoggerFactory.getLogger(UploadFileTestCases.class);
  private static final int PORT = 8443;
  private static final String BASE_URL = "localhost";
  private static WebClient client;
  private static Vertx  vertx ;

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

  }

  static Future<JsonObject> deployFileServerVerticle(Vertx vrtx){
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
  @Timeout(value = 60, timeUnit = TimeUnit.SECONDS)
  @DisplayName("Testing file upload")
  @Order(1)
  void successUploadFile(VertxTestContext vertxTestContext) throws InterruptedException {
	Thread.sleep(5000);
	logger.info("successUploadFile called");
	String apiURL = "/file";
    WebClientOptions clientOptions = new WebClientOptions().setSsl(true).setVerifyHost(false).setTrustAll(true);
    VertxOptions vertxOptions = new VertxOptions();
	vertxOptions.setMaxEventLoopExecuteTime(30);
	vertxOptions.setMaxEventLoopExecuteTimeUnit(TimeUnit.SECONDS);
	vertxOptions.setMaxWorkerExecuteTime(60);
	vertxOptions.setMaxWorkerExecuteTimeUnit(TimeUnit.SECONDS);
	Vertx vrtx = Vertx.vertx(vertxOptions);
    client = WebClient.create(vrtx, clientOptions);
    HttpRequest<Buffer> req = client.post(PORT, BASE_URL,apiURL).ssl(Boolean.TRUE);

    MultipartForm form = MultipartForm.create().binaryFileUpload("text", "TestUploadFile.txt", "D:/IUDX_UploadDownload_Testing/TestUploadFile.txt", "text/plain");
    //MultipartForm form = MultipartForm.create().binaryFileUpload("mp4", "sample-mp4-file", "D:/IUDX_UploadDownload_Testing/sample-mp4-file.mp4", "video/mp4");
    
    // below zip file is more than 1GB and this case displays warning as vertx event-loop-thread blocked
    //MultipartForm form = MultipartForm.create().binaryFileUpload("fileParameter", "elasticsearch.zip", "D:/IUDX_UploadDownload_Testing/elasticsearch.zip", "application/zip");
    
    req.sendMultipartForm(form, ar->{
    	logger.info("ar.result().statusCode() : " + ar.result().statusCode());
    	if(ar.succeeded() && ar.result().statusCode() == HttpStatus.SC_OK) {
    		vertxTestContext.completeNow();
    	}
    	else {
    		logger.info("ar.cause() : " + ar.cause());
    		vertxTestContext.failed();
    	}

    });
  }
  
}

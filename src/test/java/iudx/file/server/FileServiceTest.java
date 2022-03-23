package iudx.file.server;

import static iudx.file.server.apiserver.utilities.Constants.API_FILE_DELETE;
import static iudx.file.server.apiserver.utilities.Constants.API_FILE_DOWNLOAD;
import static iudx.file.server.apiserver.utilities.Constants.API_FILE_UPLOAD;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.multipart.MultipartForm;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.file.server.configuration.Configuration;
import iudx.file.server.util.FileCheckUtil;

@ExtendWith(VertxExtension.class)
@Deprecated
@Disabled
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class FileServiceTest {

  private static Configuration configurations;
  private static WebClient client;
  private static JsonObject config;
  private static String resourceId =
      "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/file.iudx.io/surat-itms-realtime-information/surat-itms-live-eta";
  private static String groupId =
      "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/file.iudx.io/surat-itms-realtime-information";
  private static String token;
  private static String uploadedRLSampleFileId;
  private static String uploadedGLSampleFileId;
  private static String uploadedRLArchiveFileId;
  private static String uploadedGLArchiveFileId;
  private static FileCheckUtil fileUtil;
  private static String host;
  private static int port;

  @BeforeAll
  @DisplayName("Deploy a verticle")
  static void init(Vertx vertx,VertxTestContext testContext) {
    configurations = new Configuration();
    config = configurations.configLoader(0, vertx);

    DeploymentOptions options = new DeploymentOptions();
    options.setConfig(config);
    // vertx.deployVerticle(new FileServerVerticle(), options,testContext.succeeding());

    WebClientOptions clientOptions =
        new WebClientOptions().setSsl(true).setVerifyHost(false).setTrustAll(true);
    client = WebClient.create(vertx, clientOptions);
    token = config.getString("testToken");
    fileUtil = new FileCheckUtil(vertx, config.getString("upload_dir"));
    host=config.getString("host");
    port = config.getInteger("httpPort");
    testContext.completeNow();
  }


  @Test
  @DisplayName("400 - upload a file (invalid id format)")
  void testUploadFileInvalidId(Vertx vertx, VertxTestContext testContext) {
    MultipartForm form = MultipartForm.create()
        .binaryFileUpload("text", "uploadtest.txt", "src/test/resources/uploadtest.txt",
            "text/plain");
    form.attribute("isSample", "true");
    form.attribute("id", "invalid/id/format");

    HttpRequest<Buffer> req = client.post(port, host, API_FILE_UPLOAD);
    req.putHeader("token", token);
    req.sendMultipartForm(form, handler -> {
      if (handler.succeeded()) {
        assertEquals(HttpStatus.SC_BAD_REQUEST, handler.result().statusCode());
        JsonObject result = handler.result().bodyAsJsonObject();
        assertTrue(result.containsKey("type"));
        //assertEquals(result.getInteger("type"), HttpStatus.SC_BAD_REQUEST);
        testContext.completeNow();
      } else {
        testContext.failNow(handler.cause());
      }
    });

  }

  @Test
  @DisplayName("400 - upload a file (invalid isSample text)")
  void testUploadFileInvalidSampleValue(Vertx vertx, VertxTestContext testContext) {
    MultipartForm form = MultipartForm.create()
        .binaryFileUpload("text", "uploadtest.txt", "src/test/resources/uploadtest.txt",
            "text/plain");
    form.attribute("isSample", "abc");
    form.attribute("id", resourceId);

    HttpRequest<Buffer> req = client.post(port, host, API_FILE_UPLOAD);
    req.putHeader("token", token);
    req.sendMultipartForm(form, handler -> {
      if (handler.succeeded()) {
        assertEquals(HttpStatus.SC_BAD_REQUEST, handler.result().statusCode());
        JsonObject result = handler.result().bodyAsJsonObject();
        assertTrue(result.containsKey("type"));
       // assertEquals(result.getInteger("type"), HttpStatus.SC_BAD_REQUEST);
        testContext.completeNow();
      } else {
        testContext.failNow(handler.cause());
      }
    });

  }

  @Test
  @DisplayName("400 - upload a file (invalid file .exe)")
  void testUploadFileInvalidFileType(Vertx vertx, VertxTestContext testContext) {
    MultipartForm form = MultipartForm.create()
        .binaryFileUpload("file", "uploadtest.txt", "src/test/resources/uploadtest.exe",
            "application/vnd.microsoft.portable-executable");
    form.attribute("isSample", "true");
    form.attribute("id", resourceId);

    HttpRequest<Buffer> req = client.post(port, host, API_FILE_UPLOAD);
    req.putHeader("token", token);
    req.sendMultipartForm(form, handler -> {
      if (handler.succeeded()) {
        assertEquals(HttpStatus.SC_BAD_REQUEST, handler.result().statusCode());
        JsonObject result = handler.result().bodyAsJsonObject();
        assertTrue(result.containsKey("type"));
       // assertEquals(result.getInteger("type"), HttpStatus.SC_BAD_REQUEST);
        testContext.completeNow();
      } else {
        testContext.failNow(handler.cause());
      }
    });

  }



  @Test
  @Order(1)
  @DisplayName("upload a sampe file (Resource level)")
  void testUploadSampleRLFileSuccess(Vertx vertx, VertxTestContext testContext) {
    MultipartForm form = MultipartForm.create()
        .binaryFileUpload("text", "uploadtest.txt", "src/test/resources/uploadtest.txt",
            "text/plain");
    form.attribute("isSample", "true");
    form.attribute("id", resourceId);

    HttpRequest<Buffer> req = client.post(port, host, API_FILE_UPLOAD);
    req.putHeader("token", token);
    req.sendMultipartForm(form, handler -> {
      if (handler.succeeded()) {
        assertEquals(HttpStatus.SC_OK, handler.result().statusCode());
        JsonObject result = handler.result().bodyAsJsonObject();
        assertTrue(result.containsKey("fileId"));
        uploadedRLSampleFileId = result.getString("fileId");
        assertTrue(fileUtil.isFileExist(uploadedRLSampleFileId));
        testContext.completeNow();
      } else {
        testContext.failNow(handler.cause());
      }
    });
  }

  @Test
  @Order(1)
  @DisplayName("upload a sample file (Group level)")
  void testUploadSampleGLFileSuccess(Vertx vertx, VertxTestContext testContext) {
    MultipartForm form = MultipartForm.create()
        .binaryFileUpload("text", "uploadtest.txt", "src/test/resources/uploadtest.txt",
            "text/plain");
    form.attribute("isSample", "true");
    form.attribute("id", groupId);

    HttpRequest<Buffer> req = client.post(port, host, API_FILE_UPLOAD);
    req.putHeader("token", token);
    req.sendMultipartForm(form, handler -> {
      if (handler.succeeded()) {
        assertEquals(HttpStatus.SC_OK, handler.result().statusCode());
        JsonObject result = handler.result().bodyAsJsonObject();
        assertTrue(result.containsKey("fileId"));
        uploadedGLSampleFileId = result.getString("fileId");
        assertTrue(fileUtil.isFileExist(uploadedGLSampleFileId));
        testContext.completeNow();
      } else {
        testContext.failNow(handler.cause());
      }
    });
  }
  
  
  @Test
  @Order(1)
  @DisplayName("upload a Archive file (Resource level)")
  void testUploadArchiveRLFileSuccess(Vertx vertx, VertxTestContext testContext) {
    MultipartForm form = MultipartForm.create()
        .binaryFileUpload("text", "uploadtest.txt", "src/test/resources/uploadtest.txt",
            "text/plain");
    form.attribute("id", resourceId);
    form.attribute("startTime", "2020-09-05T00:00:00Z");
    form.attribute("endTime","2020-09-15T00:00:00Z");

    HttpRequest<Buffer> req = client.post(port, host, API_FILE_UPLOAD);
    req.putHeader("token", token);
    req.sendMultipartForm(form, handler -> {
      if (handler.succeeded()) {
        assertEquals(HttpStatus.SC_OK, handler.result().statusCode());
        JsonObject result = handler.result().bodyAsJsonObject();
        assertTrue(result.containsKey("fileId"));
        uploadedRLArchiveFileId = result.getString("fileId");
        assertTrue(fileUtil.isFileExist(uploadedRLArchiveFileId));
        testContext.completeNow();
      } else {
        testContext.failNow(handler.cause());
      }
    });
  }
  
  @Test
  @Order(1)
  @DisplayName("upload a Archive file (Group level)")
  void testUploadArchiveGLFileSuccess(Vertx vertx, VertxTestContext testContext) {
    MultipartForm form = MultipartForm.create()
        .binaryFileUpload("text", "uploadtest.txt", "src/test/resources/uploadtest.txt",
            "text/plain");
    form.attribute("id", groupId);
    form.attribute("startTime", "2020-09-05T00:00:00Z");
    form.attribute("endTime","2020-09-15T00:00:00Z");

    HttpRequest<Buffer> req = client.post(port, host, API_FILE_UPLOAD);
    req.putHeader("token", token);
    req.sendMultipartForm(form, handler -> {
      if (handler.succeeded()) {
        assertEquals(HttpStatus.SC_OK, handler.result().statusCode());
        JsonObject result = handler.result().bodyAsJsonObject();
        assertTrue(result.containsKey("fileId"));
        uploadedGLArchiveFileId = result.getString("fileId");
        assertTrue(fileUtil.isFileExist(uploadedGLArchiveFileId));
        testContext.completeNow();
      } else {
        testContext.failNow(handler.cause());
      }
    });
  }
  
  @Test
  @Order(1)
  @DisplayName("upload a Archive file (Group level) without start date")
  void testUploadArchiveGLFileNoEndDate(Vertx vertx, VertxTestContext testContext) {
    MultipartForm form = MultipartForm.create()
        .binaryFileUpload("text", "uploadtest.txt", "src/test/resources/uploadtest.txt",
            "text/plain");
    form.attribute("id", groupId);
    form.attribute("startTime", "2020-09-05T00:00:00Z");

    HttpRequest<Buffer> req = client.post(port, host, API_FILE_UPLOAD);
    req.putHeader("token", token);
    req.sendMultipartForm(form, handler -> {
      if (handler.succeeded()) {
        assertEquals(HttpStatus.SC_BAD_REQUEST, handler.result().statusCode());
        testContext.completeNow();
      } else {
        testContext.failNow(handler.cause());
      }
    });
  }
  
  @Test
  @Order(1)
  @DisplayName("upload a Archive file (Group level) without end date")
  void testUploadArchiveGLFile400NoStartDate(Vertx vertx, VertxTestContext testContext) {
    MultipartForm form = MultipartForm.create()
        .binaryFileUpload("text", "uploadtest.txt", "src/test/resources/uploadtest.txt",
            "text/plain");
    form.attribute("id", groupId);
    form.attribute("startTime", "2020-09-05T00:00:00Z");

    HttpRequest<Buffer> req = client.post(port, host, API_FILE_UPLOAD);
    req.putHeader("token", token);
    req.sendMultipartForm(form, handler -> {
      if (handler.succeeded()) {
        assertEquals(HttpStatus.SC_BAD_REQUEST, handler.result().statusCode());
        testContext.completeNow();
      } else {
        testContext.failNow(handler.cause());
      }
    });
  }



  @Test
  @DisplayName("404 - download a file (Invalid file)")
  void testDownloadFileInvalidFileId(Vertx vertx, VertxTestContext testContext) {
    client.get(port, host, API_FILE_DOWNLOAD)
        .putHeader("token", token)
        .addQueryParam("file-id", uploadedRLSampleFileId + "invalidfile.txt")
        .send(handler -> {
          if (handler.succeeded()) {
            assertEquals(HttpStatus.SC_NOT_FOUND, handler.result().statusCode());
            testContext.completeNow();
          } else {
            testContext.failNow(handler.cause());
          }
        });
  }

  @Test
  @DisplayName("401 - download a file (Invalid id)")
  void testDownloadFileInvalidWithoutToken(Vertx vertx, VertxTestContext testContext) {
    client.get(port, host, API_FILE_DOWNLOAD)
        .addQueryParam("file-id", uploadedGLArchiveFileId)
        .send(handler -> {
          if (handler.succeeded()) {
            assertEquals(HttpStatus.SC_UNAUTHORIZED, handler.result().statusCode());
            testContext.completeNow();
          } else {
            testContext.failNow(handler.cause());
          }
        });
  }

  @Test
  @Order(2)
  @DisplayName("download a sample file (resource level)")
  void testDownloadSampleRLFileSuccess(Vertx vertx, VertxTestContext testContext) {
    client.get(port, host, API_FILE_DOWNLOAD)
        .putHeader("token", token)
        .addQueryParam("file-id", uploadedRLSampleFileId)
        .send(handler -> {
          if (handler.succeeded()) {
            assertEquals(HttpStatus.SC_OK, handler.result().statusCode());
            assertTrue(handler.result().headers().contains("Content-Disposition"));
            assertEquals("application/octet-stream",
                handler.result().getHeader(HttpHeaders.CONTENT_TYPE.toString()));
            testContext.completeNow();
          } else {
            testContext.failNow(handler.cause());
          }
        });
  }
  
  @Test
  @Order(2)
  @DisplayName("download a sample file (group level)")
  void testDownloadSampleGLFileSuccess(Vertx vertx, VertxTestContext testContext) {
    client.get(port, host, API_FILE_DOWNLOAD)
        .putHeader("token", token)
        .addQueryParam("file-id", uploadedGLSampleFileId)
        .send(handler -> {
          if (handler.succeeded()) {
            assertEquals(HttpStatus.SC_OK, handler.result().statusCode());
            assertTrue(handler.result().headers().contains("Content-Disposition"));
            assertEquals("application/octet-stream",
                handler.result().getHeader(HttpHeaders.CONTENT_TYPE.toString()));
            testContext.completeNow();
          } else {
            testContext.failNow(handler.cause());
          }
        });
  }
  
  @Test
  @Order(2)
  @DisplayName("download a archive file (resource level)")
  void testDownloadArchiveRLFileSuccess(Vertx vertx, VertxTestContext testContext) {
    client.get(port, host, API_FILE_DOWNLOAD)
        .putHeader("token", token)
        .addQueryParam("file-id", uploadedRLArchiveFileId)
        .send(handler -> {
          if (handler.succeeded()) {
            assertEquals(HttpStatus.SC_OK, handler.result().statusCode());
            assertTrue(handler.result().headers().contains("Content-Disposition"));
            assertEquals("application/octet-stream",
                handler.result().getHeader(HttpHeaders.CONTENT_TYPE.toString()));
            testContext.completeNow();
          } else {
            testContext.failNow(handler.cause());
          }
        });
  }
  
  @Test
  @Order(2)
  @DisplayName("download a archive file (group level)")
  void testDownloadArchiveGLFileSuccess(Vertx vertx, VertxTestContext testContext) {
    client.get(port, host, API_FILE_DOWNLOAD)
        .putHeader("token", token)
        .addQueryParam("file-id", uploadedGLArchiveFileId)
        .send(handler -> {
          if (handler.succeeded()) {
            assertEquals(HttpStatus.SC_OK, handler.result().statusCode());
            assertTrue(handler.result().headers().contains("Content-Disposition"));
            assertEquals("application/octet-stream",
                handler.result().getHeader(HttpHeaders.CONTENT_TYPE.toString()));
            testContext.completeNow();
          } else {
            testContext.failNow(handler.cause());
          }
        });
  }


  @Test
  @DisplayName("404 - delete a file (Invalid file)")
  void testDeleteFileInvalidFileId(Vertx vertx, VertxTestContext testContext) {
    client.delete(port, host, API_FILE_DELETE)
        .putHeader("token", token)
        .addQueryParam("file-id", uploadedRLSampleFileId + "invalidfile.txt")
        .send(handler -> {
          if (handler.succeeded()) {
            assertEquals(HttpStatus.SC_NOT_FOUND, handler.result().statusCode());
            testContext.completeNow();
          } else {
            testContext.failNow(handler.cause());
          }
        });
  }

  @Test
  @DisplayName("401 - delete a file (Invalid id)")
  void testDeleteFileWithoutToken(Vertx vertx, VertxTestContext testContext) {
    client.delete(port, host, API_FILE_DELETE)
        .addQueryParam("file-id", uploadedRLSampleFileId)
        .send(handler -> {
          if (handler.succeeded()) {
            assertEquals(HttpStatus.SC_BAD_REQUEST, handler.result().statusCode());
            testContext.completeNow();
          } else {
            testContext.failNow(handler.cause());
          }
        });
  }

  @Test
  @Order(3)
  @DisplayName("delete a sample file (resource level)")
  void testDeleteRLSampleFileSuccess(Vertx vertx, VertxTestContext testContext) {
    client.delete(port, host, API_FILE_DELETE)
        .putHeader("token", token)
        .addQueryParam("file-id", uploadedRLSampleFileId)
        .send(handler -> {
          if (handler.succeeded()) {
            assertEquals(HttpStatus.SC_OK, handler.result().statusCode());
            assertFalse(fileUtil.isFileExist(uploadedRLSampleFileId));
            testContext.completeNow();
          } else {
            testContext.failNow(handler.cause());
          }
        });
  }
  
  @Test
  @Order(3)
  @DisplayName("delete a sample file(group level)")
  void testDeleteGLSampleFileSuccess(Vertx vertx, VertxTestContext testContext) {
    client.delete(port, host, API_FILE_DELETE)
        .putHeader("token", token)
        .addQueryParam("file-id", uploadedGLSampleFileId)
        .send(handler -> {
          if (handler.succeeded()) {
            assertEquals(HttpStatus.SC_OK, handler.result().statusCode());
            assertFalse(fileUtil.isFileExist(uploadedGLSampleFileId));
            testContext.completeNow();
          } else {
            testContext.failNow(handler.cause());
          }
        });
  }
  
  @Test
  @Order(3)
  @DisplayName("delete a archive file(resource level)")
  void testDeleteRLArchiveFileSuccess(Vertx vertx, VertxTestContext testContext) {
    client.delete(port, host, API_FILE_DELETE)
        .putHeader("token", token)
        .addQueryParam("file-id", uploadedRLArchiveFileId)
        .send(handler -> {
          if (handler.succeeded()) {
            assertEquals(HttpStatus.SC_OK, handler.result().statusCode());
            assertFalse(fileUtil.isFileExist(uploadedRLArchiveFileId));
            testContext.completeNow();
          } else {
            testContext.failNow(handler.cause());
          }
        });
  }
  
  @Test
  @Order(3)
  @DisplayName("delete a archive file(group level)")
  void testDeleteGLArchiveFileSuccess(Vertx vertx, VertxTestContext testContext) {
    client.delete(port, host, API_FILE_DELETE)
        .putHeader("token", token)
        .addQueryParam("file-id", uploadedGLArchiveFileId)
        .send(handler -> {
          if (handler.succeeded()) {
            assertEquals(HttpStatus.SC_OK, handler.result().statusCode());
            assertFalse(fileUtil.isFileExist(uploadedGLArchiveFileId));
            testContext.completeNow();
          } else {
            testContext.failNow(handler.cause());
          }
        });
  }


}

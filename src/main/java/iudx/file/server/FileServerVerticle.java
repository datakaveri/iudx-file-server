package iudx.file.server;

import static iudx.file.server.utilities.Constants.API_FILE_DELETE;
import static iudx.file.server.utilities.Constants.API_FILE_DOWNLOAD;
import static iudx.file.server.utilities.Constants.API_FILE_UPLOAD;
import static iudx.file.server.utilities.Constants.API_TEMPORAL;
import static iudx.file.server.utilities.Constants.APPLICATION_JSON;
import static iudx.file.server.utilities.Constants.CONTENT_TYPE;
import static iudx.file.server.utilities.Constants.JSON_TYPE;
import static iudx.file.server.utilities.Constants.MAX_SIZE;
import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.file.FileSystem;
import io.vertx.core.http.ClientAuth;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.JksOptions;
import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.api.validation.ValidationException;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;
import iudx.file.server.handlers.AuthHandler;
import iudx.file.server.service.AuthService;
import iudx.file.server.service.FileService;
import iudx.file.server.service.TokenStore;
import iudx.file.server.service.impl.AuthServiceImpl;
import iudx.file.server.service.impl.LocalStorageFileServiceImpl;
import iudx.file.server.service.impl.PGTokenStoreImpl;
import iudx.file.server.utilities.DefaultAsyncResult;
import iudx.file.server.utilities.RestResponse;
import iudx.file.server.validations.ContentTypeValidator;
import iudx.file.server.validations.RequestType;
import iudx.file.server.validations.ValidationFailureHandler;
import iudx.file.server.validations.ValidationHandlerFactory;

/**
 * The File Server API Verticle.
 *
 * <h1>File Server API Verticle</h1>
 *
 * <p>
 * The API Server verticle implements the IUDX File Server APIs. It handles the API requests from
 * the clients and interacts with the associated Service to respond.
 * </p>
 *
 * @see io.vertx.core.Vertx
 * @see io.vertx.core.AbstractVerticle
 * @see io.vertx.core.http.HttpServer
 * @see io.vertx.ext.web.Router
 * @version 1.0
 * @since 2020-07-15
 */
public class FileServerVerticle extends AbstractVerticle {

  private static final Logger LOGGER = LogManager.getLogger(FileServerVerticle.class);

  private Vertx vertx;
  private ClusterManager mgr;
  private VertxOptions options;
  private HttpServer server;
  private Properties properties;
  private InputStream inputstream;
  private String keystore;
  private String keystorePassword;
  private Router router;
  // private FileServer fileServer;
  private FileService fileService;
  private Map<String, String> tokens = new HashMap<String, String>();
  private Map<String, Date> validity = new HashMap<String, Date>();
  private String allowedDomain, truststore, truststorePassword;
  private String[] allowedDomains;
  private HashSet<String> instanceIDs = new HashSet<String>();

  private String directory;
  private String temp_directory;

  private AuthService authService;
  private WebClient webClient;


  @Override
  public void start() throws Exception {

    /* Create a reference to HazelcastClusterManager. */

    mgr = new HazelcastClusterManager();
    options = new VertxOptions().setClusterManager(mgr);

    /* Create or Join a Vert.x Cluster. */

    Vertx.clusteredVertx(options, res -> {
      if (res.succeeded()) {

        vertx = res.result();
        router = Router.router(vertx);
        properties = new Properties();
        inputstream = null;
        ValidationHandlerFactory validations = new ValidationHandlerFactory();
        ValidationFailureHandler validationsFailureHandler = new ValidationFailureHandler();
        authService = new AuthServiceImpl(vertx, getWebClient(vertx, config()), config());

        // router.route().handler(BodyHandler.create());
        // router.route().handler(AuthHandler.create(authService));


        directory = config().getString("upload_dir");
        temp_directory = config().getString("tmp_dir");

        router.post(API_TEMPORAL).handler(this::query);

        router.post(API_FILE_UPLOAD)
            .handler(BodyHandler.create()
                .setUploadsDirectory(temp_directory)
                .setBodyLimit(MAX_SIZE)
                .setDeleteUploadedFilesOnEnd(true))
            .handler(validations.create(RequestType.UPLOAD))
            .handler(AuthHandler.create(authService))
            .handler(this::upload)
            .failureHandler(validationsFailureHandler);

        router.get(API_FILE_DOWNLOAD)
            .handler(BodyHandler.create())
            .handler(validations.create(RequestType.DOWNLOAD))
            .handler(AuthHandler.create(authService))
            .handler(this::download)
            .failureHandler(validationsFailureHandler);

        router.delete(API_FILE_DELETE)
            .handler(BodyHandler.create())
            .handler(validations.create(RequestType.DELETE))
            .handler(AuthHandler.create(authService))
            .handler(this::delete)
            .failureHandler(validationsFailureHandler);

        /* Read the configuration and set the HTTPs server properties. */
        ClientAuth clientAuth = ClientAuth.REQUEST;


        keystore = config().getString("keystore");
        keystorePassword = config().getString("keystorePassword");
        truststore = config().getString("truststore");
        truststorePassword = config().getString("truststorePassword");
        allowedDomain = config().getString("domains");


        allowedDomains = allowedDomain.split(",");

        for (int i = 0; i < allowedDomains.length; i++) {
          LOGGER.info(allowedDomains[i].toLowerCase());
          instanceIDs.add(allowedDomains[i].toLowerCase());
        }

        LOGGER.info("Updated domain list. Serving " + instanceIDs.size() + " tenants");

        LOGGER.info("starting server");
        server =
            vertx.createHttpServer(new HttpServerOptions().setSsl(true).setClientAuth(clientAuth)
                .setKeyStoreOptions(
                    new JksOptions().setPath(keystore).setPassword(keystorePassword))
                .setTrustStoreOptions(
                    new JksOptions().setPath(truststore).setPassword(truststorePassword)));

        server.requestHandler(router).listen(config().getInteger("port"));

      }
      fileService = new LocalStorageFileServiceImpl(vertx.fileSystem(), directory);
      // check upload dir exist or not.
      mkdirsIfNotExists(vertx.fileSystem(), directory, new Handler<AsyncResult<Void>>() {
        @Override
        public void handle(AsyncResult<Void> event) {
          if (event.succeeded()) {
            LOGGER.info("directory exist/created successfully.");
          } else {
            LOGGER.error(event.cause().getMessage(), event.cause());
          }
        }
      });
    });

    LOGGER.info("FileServerVerticle started successfully");
    // for unit testing - later it must be removed
  }

  /**
   * 
   * @param routingContext
   */
  public void upload(RoutingContext routingContext) {
    LOGGER.debug("upload() started.");
    HttpServerResponse response = routingContext.response();
    MultiMap formParam = routingContext.request().formAttributes();
    String id = formParam.get("id");
    Boolean isSample = Boolean.valueOf(formParam.get("isSample"));
    response.putHeader("content-type", "application/json");
    LOGGER.debug("id :" + id);
    String fileIdComponent[] = getFileIdComponents(id);
    StringBuilder uploadPath = new StringBuilder();
    uploadPath.append(fileIdComponent[1] + "/" + fileIdComponent[3]);
    Set<FileUpload> files = routingContext.fileUploads();
    if (!isValidFileContentType(files)) {
      ValidationException ex = new ValidationException("Invalid file type");
      ex.setParameterName("content-type");
      routingContext.fail(ex);
      return;
    }
    if (isSample) {
      if (fileIdComponent.length >= 5)
        uploadPath.append("/" + fileIdComponent[4]);
      sampleFileUpload(response, files, "sample", uploadPath.toString(), id);
    } else {
      archiveFileUpload(response, files, uploadPath.toString(), id);
    }
  }

  /**
   * Helper method to upload a sample file.
   * 
   * @param response
   * @param files
   * @param fileName
   * @param filePath
   * @param id
   */
  private void sampleFileUpload(HttpServerResponse response, Set<FileUpload> files, String fileName,
      String filePath, String id) {
    fileService.upload(files, fileName, filePath, handler -> {
      if (handler.succeeded()) {
        JsonObject uploadResult = handler.result();
        JsonObject responseJson = new JsonObject();
        responseJson.put("file-id", id + "/" + uploadResult.getString("file-id"));
        response.putHeader(CONTENT_TYPE, APPLICATION_JSON)
            .setStatusCode(HttpStatus.SC_OK)
            .end(responseJson.toString());
      } else {
        processResponse(response, handler.cause().getMessage());
      }
    });
  }

  /**
   * Helper method to upload a archieve file.
   * 
   * @param response
   * @param files
   * @param filePath
   * @param id
   */
  private void archiveFileUpload(HttpServerResponse response, Set<FileUpload> files,
      String filePath, String id) {
    fileService.upload(files, filePath, handler -> {
      if (handler.succeeded()) {
        JsonObject uploadResult = handler.result();
        JsonObject responseJson = new JsonObject();
        responseJson.put("file-id", id + "/" + uploadResult.getString("file-id"));
        response.putHeader(CONTENT_TYPE, APPLICATION_JSON)
            .setStatusCode(HttpStatus.SC_OK)
            .end(responseJson.toString());
      } else {
        processResponse(response, handler.cause().getMessage());
      }
    });
  }

  /**
   * Download File service allows to download a file from the server after authenticating the user.
   *
   * @param routingContext Handles web request in Vert.x web
   */

  public void download(RoutingContext routingContext) {
    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();
    response.setChunked(true);
    String id = request.getParam("file-id");
    String fileIdComponent[] = getFileIdComponents(id);
    StringBuilder uploadDir = new StringBuilder();
    uploadDir.append(fileIdComponent[1] + "/" + fileIdComponent[3]);
    // extract the file-uuid from supplied id, since position of file-id in id will be different for
    // group level(pos:5) and resource level(pos:4)
    String fileUUID = fileIdComponent.length >= 6 ? fileIdComponent[5] : fileIdComponent[4];
    System.out.println(fileUUID);
    if (fileUUID.contains("sample") && fileIdComponent.length >= 6) {
      uploadDir.append("/" + fileIdComponent[4]);
    }
    System.out.println(uploadDir);
    fileService.download(fileUUID, uploadDir.toString(), response, handler -> {
      if (handler.succeeded()) {
        // do nothing response is already written and file is served using content-disposition.
      } else {
        processResponse(response, handler.cause().getMessage());
      }
    });
  }


  public void query(RoutingContext context) {
    HttpServerRequest request = context.request();
    HttpServerResponse response = context.response();
    MultiMap queryParams = MultiMap.caseInsensitiveMultiMap();
  }


  /**
   * Download File service allows to download a file from the server after authenticating the user.
   */

  public void delete(RoutingContext routingContext) {
    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();
    response.putHeader("content-type", "application/json");
    String id = request.getParam("file-id");
    String fileIdComponent[] = getFileIdComponents(id);
    StringBuilder uploadDir = new StringBuilder();
    uploadDir.append(fileIdComponent[1] + "/" + fileIdComponent[3]);
    // extract the file-uuid from supplied id, since position of file-id in id will be different for
    // group level(pos:5) and resource level(pos:4)
    String fileUUID = fileIdComponent.length >= 6 ? fileIdComponent[5] : fileIdComponent[4];
    System.out.println(fileUUID);
    if (fileUUID.contains("sample") && fileIdComponent.length >= 6) {
      uploadDir.append("/" + fileIdComponent[4]);
    }
    System.out.println(uploadDir);
    fileService.delete(fileUUID, uploadDir.toString(), handler -> {
      if (handler.succeeded()) {
        JsonObject deleteResult = handler.result();
        LOGGER.info(deleteResult);
        response.putHeader(CONTENT_TYPE, APPLICATION_JSON)
            .setStatusCode(HttpStatus.SC_OK)
            .end(new RestResponse.Builder()
                .type(200)
                .title(deleteResult.getString("title"))
                .details("File with id : " + id + " deleted successfully").build().toJson()
                .toString());
      } else {
        processResponse(response, handler.cause().getMessage());
      }
    });
  }

  /**
   * Helper method to check/create initial directory structure
   * 
   * @param fileSystem vert.x FileSystem
   * @param basePath base derfault directory structure
   * @param handler Async handler
   */
  private void mkdirsIfNotExists(FileSystem fileSystem, String basePath,
      final Handler<AsyncResult<Void>> handler) {
    LOGGER.info("mkidr check started");
    fileSystem.exists(basePath, new Handler<AsyncResult<Boolean>>() {
      @Override
      public void handle(AsyncResult<Boolean> event) {
        if (event.succeeded()) {
          if (Boolean.FALSE.equals(event.result())) {
            LOGGER.info("Creating parent directory structure ...  " + basePath);
            fileSystem.mkdirs(basePath, new Handler<AsyncResult<Void>>() {
              @Override
              public void handle(AsyncResult<Void> event) {
                handler.handle(event);
              }
            });
          } else {
            handler.handle(new DefaultAsyncResult<>((Void) null));
          }
        } else {
          handler.handle(new DefaultAsyncResult<Void>(event.cause()));
        }
      }
    });
  }

  /**
   * retreicve components from id index : 0 - Domain 1 - user SHA 2 - File Server 3 - File group 4 -
   * Resource/ File id(for Group level file, index 4 represent file Id(optional)) 5 - File id
   * (optional)
   * 
   * @param fileId
   * @return
   */
  private String[] getFileIdComponents(String fileId) {
    return fileId.split("/");
  }

  private void processResponse(HttpServerResponse response, String message) {
    LOGGER.debug("Info : " + message);
    try {
      JsonObject json = new JsonObject(message);
      int type = json.getInteger(JSON_TYPE);
      response.putHeader(CONTENT_TYPE, APPLICATION_JSON)
          .setStatusCode(type)
          .end(new RestResponse.Builder()
              .type(type)
              .title(json.getString("title"))
              .details(json.getString("detail"))
              .build().toJson().toString());
    } catch (DecodeException ex) {
      LOGGER.error("ERROR : Expecting Json received else from backend service");
      response.putHeader(CONTENT_TYPE, APPLICATION_JSON)
          .setStatusCode(500)
          .end(new RestResponse.Builder()
              .type(500)
              .title("Internal server error")
              .details("ERROR : Expecting Json received else from backend service")
              .build().toJson().toString());
    }

  }


  private WebClient getWebClient(Vertx vertx, JsonObject config) {
    WebClientOptions options =
        new WebClientOptions().setTrustAll(true).setVerifyHost(false).setSsl(true)
            .setKeyStoreOptions(
                new JksOptions()
                    .setPath(config.getString("keystore"))
                    .setPassword(config.getString("keystorePassword")));
    return WebClient.create(vertx, options);
  }

  private boolean isValidFileContentType(Set<FileUpload> files) {
    for (FileUpload file : files) {
      if (!ContentTypeValidator.isValid(file.contentType())) {
        return false;
      }
    }
    return true;
  }

}

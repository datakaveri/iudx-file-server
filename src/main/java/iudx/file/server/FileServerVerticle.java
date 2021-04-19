package iudx.file.server;

import static iudx.file.server.utilities.Constants.*;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.netty.handler.codec.http.HttpConstants;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.file.FileSystem;
import io.vertx.core.http.ClientAuth;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.JksOptions;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.api.validation.ValidationException;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.handler.BodyHandler;
import iudx.file.server.handlers.AuthHandler;
import iudx.file.server.service.AuthService;
import iudx.file.server.service.DBService;
import iudx.file.server.service.FileService;
import iudx.file.server.service.impl.AuthServiceImpl;
import iudx.file.server.service.impl.DBServiceImpl;
import iudx.file.server.service.impl.LocalStorageFileServiceImpl;
import iudx.file.server.utilities.RestResponse;
import iudx.file.server.validations.ContentTypeValidator;
import iudx.file.server.validations.QueryParamsValidator;
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

  private HttpServer server;
  private String keystore;
  private String keystorePassword;
  private Router router;
  // private FileServer fileServer;
  private FileService fileService;
  private String allowedDomain, truststore, truststorePassword;
  private String[] allowedDomains;
  private HashSet<String> instanceIDs = new HashSet<String>();

  private String directory;
  private String temp_directory;

  private AuthService authService;
  private DBService dbService;
  private QueryParamsValidator queryParamValidator;
  private ContentTypeValidator contentTypeValidator;


  @Override
  public void start() throws Exception {
    int port;
    boolean isSSL;
    router = Router.router(vertx);
    ValidationHandlerFactory validations = new ValidationHandlerFactory();
    ValidationFailureHandler validationsFailureHandler = new ValidationFailureHandler();
    queryParamValidator = new QueryParamsValidator();
    contentTypeValidator=new ContentTypeValidator(config().getJsonObject("allowedContentType"));

    authService = new AuthServiceImpl(vertx, getWebClient(vertx, config()), config());
    dbService = new DBServiceImpl(config());

    directory = config().getString("upload_dir");
    temp_directory = config().getString("tmp_dir");

    router.get(API_TEMPORAL).handler(BodyHandler.create())
        .handler(validations.create(RequestType.QUERY)).handler(this::query)
        .failureHandler(validationsFailureHandler);

    router.post(API_FILE_UPLOAD)
        .handler(BodyHandler.create().setUploadsDirectory(temp_directory).setBodyLimit(MAX_SIZE)
            .setDeleteUploadedFilesOnEnd(true))
        .handler(validations.create(RequestType.UPLOAD)).handler(AuthHandler.create(authService))
        .handler(this::upload).failureHandler(validationsFailureHandler);

    router.get(API_FILE_DOWNLOAD).handler(BodyHandler.create())
        .handler(validations.create(RequestType.DOWNLOAD)).handler(AuthHandler.create(authService))
        .handler(this::download).failureHandler(validationsFailureHandler);

    router.delete(API_FILE_DELETE).handler(BodyHandler.create())
        .handler(validations.create(RequestType.DELETE)).handler(AuthHandler.create(authService))
        .handler(this::delete).failureHandler(validationsFailureHandler);



    router.get("/apis/spec").produces("application/json").handler(routingContext -> {
      HttpServerResponse response = routingContext.response();
      response.sendFile("docs/openapi.yaml");
    });
    /* Get redoc */
    router.get("/apis").produces("text/html").handler(routingContext -> {
      HttpServerResponse response = routingContext.response();
      response.sendFile("docs/apidoc.html");
    });



    /* Read the configuration and set the HTTPs server properties. */
    ClientAuth clientAuth = ClientAuth.REQUEST;
    truststore = config().getString("truststore");
    truststorePassword = config().getString("truststorePassword");

    LOGGER.info("starting server");
    /* Read ssl configuration. */
    isSSL = config().getBoolean("ssl");
    port = config().getInteger("port");
    HttpServerOptions serverOptions = new HttpServerOptions();
    if (isSSL) {
      LOGGER.debug("Info: Starting HTTPs server");

      /* Read the configuration and set the HTTPs server properties. */

      keystore = config().getString("keystore");
      keystorePassword = config().getString("keystorePassword");

      /* Setup the HTTPs server properties, APIs and port. */

      serverOptions.setSsl(true)
          .setKeyStoreOptions(new JksOptions().setPath(keystore).setPassword(keystorePassword));

    } else {
      LOGGER.debug("Info: Starting HTTP server");

      /* Setup the HTTP server properties, APIs and port. */

      serverOptions.setSsl(false);
    }
    server = vertx.createHttpServer(serverOptions.setTrustStoreOptions(
        new JksOptions().setPath(truststore).setPassword(truststorePassword)));

    server.requestHandler(router).listen(port);

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
    LOGGER.info("FileServerVerticle started successfully");
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
      sampleFileUpload(response, formParam, files, "sample", uploadPath.toString(), id);
    } else {
      if (!formParam.contains("startTime") || !formParam.contains("endTime")) {
        ValidationException ex = new ValidationException("Mandatory fields required");
        ex.setParameterName("startTime/endTime");
        routingContext.fail(ex);
        return;
      }
      archiveFileUpload(response, formParam, files, uploadPath.toString(), id);
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
  private void sampleFileUpload(HttpServerResponse response, MultiMap params, Set<FileUpload> files,
      String fileName,
      String filePath, String id) {
    fileService.upload(files, fileName, filePath, handler -> {
      if (handler.succeeded()) {
        JsonObject uploadResult = handler.result();
        JsonObject responseJson = new JsonObject();
        String fileId = id + "/" + uploadResult.getString("file-id");
        responseJson.put("fileId", fileId);
        // insertFileRecord(params, fileId); no need to insert in DB
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
  private void archiveFileUpload(HttpServerResponse response, MultiMap params,
      Set<FileUpload> files,
      String filePath, String id) {

    fileService.upload(files, filePath, handler -> {
      if (handler.succeeded()) {
        JsonObject uploadResult = handler.result();
        JsonObject responseJson = new JsonObject();
        String fileId = id + "/" + uploadResult.getString("file-id");
        responseJson.put("fileId", fileId);
        saveFileRecord(params, fileId).onComplete(dbInsertHandler -> {
          if (dbInsertHandler.succeeded()) {
            response.putHeader(CONTENT_TYPE, APPLICATION_JSON)
                .setStatusCode(HttpStatus.SC_OK)
                .end(responseJson.toString());
          } else {
            LOGGER.debug(dbInsertHandler.cause());
            // DB insert fail, run Compensating service to clean/undo upload.
            String fileIdComponent[] = getFileIdComponents(responseJson.getString("fileId"));
            StringBuilder uploadDir = new StringBuilder();
            uploadDir.append(fileIdComponent[1] + "/" + fileIdComponent[3]);
            String fileUUID = fileIdComponent.length >= 6 ? fileIdComponent[5] : fileIdComponent[4];
            
            LOGGER.debug("deleting file :"+fileUUID);
            vertx.fileSystem().deleteBlocking(directory+"/"+uploadDir+"/"+fileUUID);
            processResponse(response, dbInsertHandler.cause().getMessage());
          }
        });
      } else {
        processResponse(response, handler.cause().getMessage());
      }
    });
  }


  private Future<Boolean> saveFileRecord(MultiMap formParams, String fileId) {
    Promise<Boolean> promise = Promise.promise();
    JsonObject json = new JsonObject();
    json.put("id", formParams.get("id"));
    json.put("timeRange", new JsonObject()
        .put("startTime", formParams.get("startTime"))
        .put("endTime", formParams.get("endTime")));
    json.put("fileId", fileId);
    // insert record in elastic index.
    dbService.save(json, handler -> {
      if (handler.succeeded()) {
        LOGGER.info("Record inserted in DB");
        promise.complete();
      } else {
        LOGGER.error("failed to PUT record");
        promise.fail(handler.cause().getMessage());
      }
    });
    return promise.future();
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
    HttpServerResponse response = context.response();
    MultiMap queryParams = getQueryParams(context, response).get();
    System.out.println(queryParams);
    Future<Boolean> queryParamsValidator = queryParamValidator.isValid(queryParams);
    JsonObject query = new JsonObject();
    for (Map.Entry<String, String> entry : queryParams.entries()) {
      query.put(entry.getKey(), entry.getValue());
    }
    queryParamsValidator.onComplete(validationHandler -> {
      if (validationHandler.succeeded()) {
        dbService.query(query, queryHandler -> {
          if (queryHandler.succeeded()) {
            response.putHeader(CONTENT_TYPE, APPLICATION_JSON)
                .setStatusCode(HttpStatus.SC_OK)
                .end(queryHandler.result().toString());

          } else {
            processResponse(response, queryHandler.cause().getMessage());
          }
        });
      } else {
        LOGGER.error(validationHandler.cause().getMessage());
      }
    });
  }


  private void deleteFileRecord(String id) {
    dbService.delete(id, handler -> {
      if (handler.succeeded()) {
        LOGGER.info("Record deleted in DB");
      } else {
        LOGGER.error("failed to DELETE record");
        LOGGER.error(id);
      }
    });
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
    boolean isArchiveFile = true;
    if (fileUUID.contains("sample")) {
      isArchiveFile = false;
      if (fileIdComponent.length >= 6) {
        uploadDir.append("/" + fileIdComponent[4]);
      }
    }
    System.out.println(uploadDir);
    System.out.println("is archieve : " + isArchiveFile);
    if (isArchiveFile) {
      deleteArchiveFile(response, id, fileUUID, uploadDir.toString());
    } else {
      deleteSampleFile(response, id, fileUUID, uploadDir.toString());
    }



  }

  private void deleteSampleFile(HttpServerResponse response, String id, String fileUUID,
      String uploadDir) {
    System.out.println("delete sample file");
    fileService.delete(fileUUID, uploadDir.toString(), handler -> {
      if (handler.succeeded()) {
        JsonObject deleteResult = handler.result();
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

  private void deleteArchiveFile(HttpServerResponse response, String id, String fileUUID,
      String uploadDir) {
    dbService.delete(id, dbDeleteHandler -> {
      if (dbDeleteHandler.succeeded()) {
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
      } else {
        processResponse(response, dbDeleteHandler.cause().getMessage());
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
            handler.handle(Future.succeededFuture());
          }
        } else {
          handler.handle(Future.failedFuture("Failed to create directory."));
        }
      }
    });
  }

  /**
   * retreive components from id index : 0 - Domain 1 - user SHA 2 - File Server 3 - File group 4 -
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
      System.out.println(file.contentType());
      if (!contentTypeValidator.isValid(file.contentType())) {
        return false;
      }
    }
    return true;
  }

  private Optional<MultiMap> getQueryParams(RoutingContext routingContext,
      HttpServerResponse response) {
    MultiMap queryParams = null;
    try {
      queryParams = MultiMap.caseInsensitiveMultiMap();
      // Internally + sign is dropped and treated as space, replacing + with %2B do the trick
      String uri = routingContext.request().uri().toString().replaceAll("\\+", "%2B");
      Map<String, List<String>> decodedParams =
          new QueryStringDecoder(uri, HttpConstants.DEFAULT_CHARSET, true, 1024, true).parameters();
      for (Map.Entry<String, List<String>> entry : decodedParams.entrySet()) {
        queryParams.add(entry.getKey(), entry.getValue());
      }
      LOGGER.debug("Info: Decoded multimap");
    } catch (IllegalArgumentException ex) {
      response.putHeader(CONTENT_TYPE, APPLICATION_JSON)
          .setStatusCode(HttpStatus.SC_BAD_REQUEST)
          .end(new RestResponse.Builder()
              .type(HttpStatus.SC_BAD_REQUEST)
              .title("Bad request")
              .details("Error while decoding params.")
              .build().toJson().toString());


    }
    return Optional.of(queryParams);
  }

  @Override
  public void stop() {
    LOGGER.info("Stopping the File server");
  }
}

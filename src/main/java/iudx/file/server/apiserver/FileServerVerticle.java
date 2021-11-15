package iudx.file.server.apiserver;

import static iudx.file.server.apiserver.utilities.Constants.API_APIS;
import static iudx.file.server.apiserver.utilities.Constants.API_API_SPECS;
import static iudx.file.server.apiserver.utilities.Constants.API_FILE_DELETE;
import static iudx.file.server.apiserver.utilities.Constants.API_FILE_DOWNLOAD;
import static iudx.file.server.apiserver.utilities.Constants.API_FILE_UPLOAD;
import static iudx.file.server.apiserver.utilities.Constants.API_LIST_METADATA;
import static iudx.file.server.apiserver.utilities.Constants.API_SPATIAL;
import static iudx.file.server.apiserver.utilities.Constants.API_TEMPORAL;
import static iudx.file.server.apiserver.utilities.Constants.APPLICATION_JSON;
import static iudx.file.server.apiserver.utilities.Constants.CONTENT_TYPE;
import static iudx.file.server.apiserver.utilities.Constants.HEADER_ACCEPT;
import static iudx.file.server.apiserver.utilities.Constants.HEADER_ALLOW_ORIGIN;
import static iudx.file.server.apiserver.utilities.Constants.HEADER_CONTENT_LENGTH;
import static iudx.file.server.apiserver.utilities.Constants.HEADER_CONTENT_TYPE;
import static iudx.file.server.apiserver.utilities.Constants.HEADER_HOST;
import static iudx.file.server.apiserver.utilities.Constants.HEADER_ORIGIN;
import static iudx.file.server.apiserver.utilities.Constants.HEADER_REFERER;
import static iudx.file.server.apiserver.utilities.Constants.HEADER_TOKEN;
import static iudx.file.server.apiserver.utilities.Constants.JSON_TYPE;
import static iudx.file.server.apiserver.utilities.Constants.MAX_SIZE;
import static iudx.file.server.apiserver.utilities.Constants.PARAM_ID;
import static iudx.file.server.apiserver.utilities.Utilities.getFileIdComponents;
import static iudx.file.server.apiserver.utilities.Utilities.getQueryType;
import static iudx.file.server.common.Constants.DB_SERVICE_ADDRESS;

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
import io.vertx.core.file.FileSystem;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.JksOptions;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import iudx.file.server.apiserver.handlers.AuthHandler;
import iudx.file.server.apiserver.handlers.ValidationsHandler;
import iudx.file.server.apiserver.query.QueryParams;
import iudx.file.server.apiserver.service.FileService;
import iudx.file.server.apiserver.service.impl.LocalStorageFileServiceImpl;
import iudx.file.server.apiserver.utilities.RestResponse;
import iudx.file.server.apiserver.validations.ContentTypeValidator;
import iudx.file.server.apiserver.validations.RequestType;
import iudx.file.server.apiserver.validations.RequestValidator;
import iudx.file.server.apiserver.validations.ValidationFailureHandler;
import iudx.file.server.common.QueryType;
import iudx.file.server.common.WebClientFactory;
import iudx.file.server.common.service.CatalogueService;
import iudx.file.server.common.service.impl.CatalogueServiceImpl;
import iudx.file.server.database.DatabaseService;

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

  /** Service addresses */
  private static final String DATABASE_SERVICE_ADDRESS = DB_SERVICE_ADDRESS;

  private HttpServer server;
  private String keystore;
  private String keystorePassword;
  private Router router;
  // private FileServer fileServer;
  private FileService fileService;
  private DatabaseService database;
  private String truststore, truststorePassword;

  private String directory;
  private String temp_directory;

  private RequestValidator requestValidator;
  private ContentTypeValidator contentTypeValidator;
  private WebClientFactory webClientFactory;
  private CatalogueService catalogueService;

  private final ValidationFailureHandler validationsFailureHandler = new ValidationFailureHandler();

  @Override
  public void start() throws Exception {
    int port;
    boolean isSSL;
    
    Set<String> allowedHeaders = new HashSet<>();
    allowedHeaders.add(HEADER_ACCEPT);
    allowedHeaders.add(HEADER_TOKEN);
    allowedHeaders.add(HEADER_CONTENT_LENGTH);
    allowedHeaders.add(HEADER_CONTENT_TYPE);
    allowedHeaders.add(HEADER_HOST);
    allowedHeaders.add(HEADER_ORIGIN);
    allowedHeaders.add(HEADER_REFERER);
    allowedHeaders.add(HEADER_ALLOW_ORIGIN);

    Set<HttpMethod> allowedMethods = new HashSet<>();
    allowedMethods.add(HttpMethod.GET);
    allowedMethods.add(HttpMethod.POST);
    allowedMethods.add(HttpMethod.OPTIONS);
    allowedMethods.add(HttpMethod.DELETE);
    allowedMethods.add(HttpMethod.PATCH);
    allowedMethods.add(HttpMethod.PUT);
    
    router = Router.router(vertx);
    router.route().handler(
            CorsHandler.create("*").allowedHeaders(allowedHeaders).allowedMethods(allowedMethods));
    
    requestValidator = new RequestValidator();
    contentTypeValidator = new ContentTypeValidator(config().getJsonObject("allowedContentType"));

    webClientFactory = new WebClientFactory(vertx, config());

    // authService = new AuthServiceImpl(vertx, webClientFactory, config());
    catalogueService = new CatalogueServiceImpl(vertx, webClientFactory, config());

    directory = config().getString("upload_dir");
    temp_directory = config().getString("tmp_dir");

    ValidationsHandler temporalQueryVaidationHandler =
        new ValidationsHandler(RequestType.TEMPORAL_QUERY);
    router.get(API_TEMPORAL).handler(BodyHandler.create())
        .handler(temporalQueryVaidationHandler)
        .handler(AuthHandler.create(vertx))
        .handler(this::query).failureHandler(validationsFailureHandler);

    ValidationsHandler uploadValidationHandler = new ValidationsHandler(RequestType.UPLOAD);
    router.post(API_FILE_UPLOAD)
        .handler(BodyHandler.create().setUploadsDirectory(temp_directory).setBodyLimit(MAX_SIZE)
            .setDeleteUploadedFilesOnEnd(false))
        .handler(uploadValidationHandler)
        .handler(AuthHandler.create(vertx))
        .handler(this::upload).failureHandler(validationsFailureHandler);

    ValidationsHandler downloadValidationHandler = new ValidationsHandler(RequestType.DOWNLOAD);
    router.get(API_FILE_DOWNLOAD).handler(BodyHandler.create())
        .handler(downloadValidationHandler)
        .handler(AuthHandler.create(vertx))
        .handler(this::download).failureHandler(validationsFailureHandler);

    ValidationsHandler deleteValidationHandler = new ValidationsHandler(RequestType.DELETE);
    router.delete(API_FILE_DELETE).handler(BodyHandler.create())
        .handler(deleteValidationHandler)
        .handler(AuthHandler.create(vertx))
        .handler(this::delete).failureHandler(validationsFailureHandler);

    ValidationsHandler listQueryValidationHandler = new ValidationsHandler(RequestType.LIST_QUERY);
    router.get(API_LIST_METADATA).handler(BodyHandler.create())
        .handler(listQueryValidationHandler)
        .handler(AuthHandler.create(vertx))
        .handler(this::listMetadata).failureHandler(validationsFailureHandler);

    ValidationsHandler geoQueryValidationHandler = new ValidationsHandler(RequestType.GEO_QUERY);
    router.get(API_SPATIAL).handler(BodyHandler.create())
        .handler(geoQueryValidationHandler)
        .handler(AuthHandler.create(vertx))
        .handler(this::query).failureHandler(validationsFailureHandler);

    router.get(API_API_SPECS).produces("application/json")
        .handler(AuthHandler.create(vertx))
        .handler(routingContext -> {
          HttpServerResponse response = routingContext.response();
          response.sendFile("docs/openapi.yaml");
        });

    /* Get redoc */
    router.get(API_APIS).produces("text/html")
        .handler(AuthHandler.create(vertx))
        .handler(routingContext -> {
          HttpServerResponse response = routingContext.response();
          response.sendFile("docs/apidoc.html");
        });



    /* Read the configuration and set the HTTPs server properties. */
    truststore = config().getString("file-keystore");
    truststorePassword = config().getString("file-keystorePassword");

    LOGGER.info("starting server");
    /* Read ssl configuration. */
    isSSL = config().getBoolean("ssl");
    port = config().getInteger("port");
    HttpServerOptions serverOptions = new HttpServerOptions();
    if (isSSL) {
      LOGGER.debug("Info: Starting HTTPs server");

      /* Read the configuration and set the HTTPs server properties. */

      keystore = config().getString("file-keystore");
      keystorePassword = config().getString("file-keystorePassword");

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

    database = DatabaseService.createProxy(vertx, DATABASE_SERVICE_ADDRESS);

    fileService = new LocalStorageFileServiceImpl(vertx.fileSystem(), directory);
    // check upload dir exist or not.
    mkdirsIfNotExists(vertx.fileSystem(), directory, event -> {
      if (event.succeeded()) {
        LOGGER.info("directory exist/created successfully.");
      } else {
        LOGGER.error(event.cause().getMessage(), event.cause());
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
    if (files.size() == 0 || !isValidFileContentType(files)) {
      String message = new RestResponse.Builder()
          .type(400)
          .title("Bad Request")
          .details("bad request").build().toJsonString();
      LOGGER.error("Invalid File type or no file attached");
      processResponse(response, message);
      return;
    }
    if (isSample) {
      if (fileIdComponent.length >= 5)
        uploadPath.append("/" + fileIdComponent[4]);
      sampleFileUpload(response, files, "sample", uploadPath.toString(), id);
    } else {
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
  private void sampleFileUpload(HttpServerResponse response, Set<FileUpload> files,
      String fileName,
      String filePath, String id) {

    Future<JsonObject> uploadFuture = fileService.upload(files, fileName, filePath);

    uploadFuture.onComplete(uploadHandler -> {
      if (uploadHandler.succeeded()) {
        JsonObject uploadResult = uploadHandler.result();
        JsonObject responseJson = new JsonObject();
        String fileId = id + "/" + uploadResult.getString("file-id");
        responseJson.put("fileId", fileId);
        // insertFileRecord(params, fileId); no need to insert in DB
        response.putHeader(CONTENT_TYPE, APPLICATION_JSON)
            .setStatusCode(HttpStatus.SC_OK)
            .end(responseJson.toString());
      } else {
        processResponse(response, uploadHandler.cause().getMessage());
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
    JsonObject uploadJson = new JsonObject();
    JsonObject responseJson = new JsonObject();

    Future<Boolean> requestValidatorFuture = requestValidator.isValidArchiveRequest(params);

    requestValidatorFuture.compose(requestValidatorhandler -> {
      return fileService.upload(files, filePath);
    }).compose(uploadHandler -> {
      LOGGER.debug("upload json :" + uploadHandler);
      if (uploadHandler.containsKey("detail")) {
        return Future.failedFuture(uploadHandler.toString());
      }
      String fileId = id + "/" + uploadHandler.getString("file-id");
      uploadJson.put("upload", true);
      uploadJson.put("fileId", fileId);
      return catalogueService.isAllowedMetaDataField(params);
    }).compose(metaDataValidatorHandler -> {
      LOGGER.debug("file-id : " + uploadJson.getString("fileId"));
      return saveFileRecord(params, uploadJson.getString("fileId"));
    }).onComplete(handler -> {
      if (handler.succeeded()) {
        responseJson.put("fileId", uploadJson.getString("fileId"));
        response.putHeader(CONTENT_TYPE, APPLICATION_JSON)
            .setStatusCode(HttpStatus.SC_OK)
            .end(responseJson.toString());
      } else {
        LOGGER.debug(handler.cause());
        if (uploadJson.containsKey("upload") && uploadJson.getBoolean("upload")) {
          // fail, run Compensating service to clean/undo upload.
          String fileIdComponent[] = getFileIdComponents(uploadJson.getString("fileId"));
          StringBuilder uploadDir = new StringBuilder();
          uploadDir.append(fileIdComponent[1] + "/" + fileIdComponent[3]);
          String fileUUID = fileIdComponent.length >= 6 ? fileIdComponent[5] : fileIdComponent[4];

          LOGGER.debug("deleting file :" + fileUUID);
          vertx.fileSystem().deleteBlocking(directory + "/" + uploadDir + "/" + fileUUID);

        }
        processResponse(response, handler.cause().getMessage());

      }
    });

  }

  private Future<Boolean> saveFileRecord(MultiMap formParams, String fileId) {
    Promise<Boolean> promise = Promise.promise();
    JsonObject json = new JsonObject();
    json.put("id", formParams.get("id"));
    json.put("timeRange", new JsonObject()
        .put("gte", formParams.get("startTime"))
        .put("lte", formParams.get("endTime")));
    json.put("fileId", fileId);

    json.put("location", new JsonObject()
        .put("type", formParams.get("geometry"))
        .put("coordinates", new JsonArray(formParams.get("coordinates"))));

    // remove already added default metadata fields from formParams.
    formParams.remove("id");
    formParams.remove("startTime");
    formParams.remove("endTime");
    formParams.remove("geometry");
    formParams.remove("coordinates");

    formParams.entries().stream().forEach(e -> json.put(e.getKey(), e.getValue()));

    // insert record in elastic index.
    database.save(json, handler -> {
      if (handler.succeeded()) {
        LOGGER.info("Record inserted in DB");
        promise.complete(true);
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
    LOGGER.info(fileUUID);
    if (fileUUID.contains("sample") && fileIdComponent.length >= 6) {
      uploadDir.append("/" + fileIdComponent[4]);
    }
    LOGGER.info(uploadDir);
    fileService.download(fileUUID, uploadDir.toString(), response)
        .onComplete(handler -> {
          if (handler.failed()) {
            processResponse(response, handler.cause().getMessage());
          }
          // do nothing response is already written and file is served using content-disposition.
        });
  }


  public void query(RoutingContext context) {
    HttpServerResponse response = context.response();
    MultiMap queryParams = getQueryParams(context, response).get();
    Future<Boolean> queryParamsValidator = requestValidator.isValid(queryParams);
    Future<List<String>> allowedFilters =
        catalogueService.getAllowedFilters4Queries(queryParams.get(PARAM_ID));

    JsonObject query = new JsonObject();
    for (Map.Entry<String, String> entry : queryParams.entries()) {
      query.put(entry.getKey(), entry.getValue());
    }
    QueryParams params = query.mapTo(QueryParams.class).build();
    QueryType type = getQueryType(params);

    queryParamsValidator.compose(paramsValidator -> {
      return allowedFilters;
    }).onComplete(handler -> {
      if (handler.succeeded()) {
        boolean isValidFilters = true; // TODO :change to false once filters available in cat for
                                       // file
        List<String> applicableFilters = handler.result();
        if (QueryType.TEMPORAL_GEO.equals(type)
            && (applicableFilters.contains("SPATIAL") && applicableFilters.contains("TEMPORAL"))) {
          isValidFilters = true;
        } else if (QueryType.GEO.equals(type) && applicableFilters.contains("SPATIAL")) {
          isValidFilters = true;
        } else if (QueryType.TEMPORAL.equals(type) && applicableFilters.contains("TEMPORAL")) {
          isValidFilters = true;
        }
        if (isValidFilters) {
          executeSearch(JsonObject.mapFrom(params), type, response);
        } else {
          response.putHeader(CONTENT_TYPE, APPLICATION_JSON)
              .setStatusCode(HttpStatus.SC_OK)
              .end(new RestResponse.Builder()
                  .type(400)
                  .title("Bad query")
                  .details("Either geo or temporal parameter is not allowed for resource").build()
                  .toJson()
                  .toString());
        }
      } else {
        LOGGER.error(handler.cause().getMessage());
        response.putHeader(CONTENT_TYPE, APPLICATION_JSON)
            .setStatusCode(HttpStatus.SC_OK)
            .end(new RestResponse.Builder()
                .type(400)
                .title("Bad query")
                .details("Bad query").build()
                .toJson()
                .toString());
      }
    });
  }

  private void executeSearch(JsonObject json, QueryType type, HttpServerResponse response) {
    database.search(json, type, queryHandler -> {
      if (queryHandler.succeeded()) {
        response.putHeader(CONTENT_TYPE, APPLICATION_JSON)
            .setStatusCode(HttpStatus.SC_OK)
            .end(queryHandler.result().toString());

      } else {
        processResponse(response, queryHandler.cause().getMessage());
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
    LOGGER.info(fileUUID);
    boolean isArchiveFile = true;
    if (fileUUID.contains("sample")) {
      isArchiveFile = false;
      if (fileIdComponent.length >= 6) {
        uploadDir.append("/" + fileIdComponent[4]);
      }
    }
    LOGGER.info(uploadDir);
    LOGGER.info("is archieve : " + isArchiveFile);
    if (isArchiveFile) {
      deleteArchiveFile(response, id, fileUUID, uploadDir.toString());
    } else {
      deleteSampleFile(response, id, fileUUID, uploadDir.toString());
    }
  }

  public void listMetadata(RoutingContext context) {
    HttpServerRequest request = context.request();
    HttpServerResponse response = context.response();
    String id = request.getParam("id");
    JsonObject query = new JsonObject().put("id", id);
    database.search(query, QueryType.LIST, queryHandler -> {
      if (queryHandler.succeeded()) {
        response.putHeader(CONTENT_TYPE, APPLICATION_JSON)
            .setStatusCode(HttpStatus.SC_OK)
            .end(queryHandler.result().toString());

      } else {
        processResponse(response, queryHandler.cause().getMessage());
      }
    });

  }

  private void deleteSampleFile(HttpServerResponse response, String id, String fileUUID,
      String uploadDir) {
    LOGGER.info("delete sample file");
    fileService.delete(fileUUID, uploadDir.toString())
        .onComplete(handler -> {
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
    database.delete(id, dbDeleteHandler -> {
      if (dbDeleteHandler.succeeded()) {
        fileService.delete(fileUUID, uploadDir.toString()).onComplete(handler -> {
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
              .details(json.getString("details"))
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


  private boolean isValidFileContentType(Set<FileUpload> files) {
    for (FileUpload file : files) {
      LOGGER.info(file.contentType());
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

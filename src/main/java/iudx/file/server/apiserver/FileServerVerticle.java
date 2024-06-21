package iudx.file.server.apiserver;

import static iudx.file.server.apiserver.response.ResponseUrn.*;
import static iudx.file.server.apiserver.response.ResponseUrn.SUCCESS;
import static iudx.file.server.apiserver.utilities.Constants.*;
import static iudx.file.server.apiserver.utilities.Constants.FORWARD_SLASH;
import static iudx.file.server.apiserver.utilities.Utilities.getQueryType;
import static iudx.file.server.auditing.util.Constants.*;
import static iudx.file.server.auditing.util.Constants.RESPONSE_SIZE;
import static iudx.file.server.authenticator.utilities.Constants.DID;
import static iudx.file.server.authenticator.utilities.Constants.DRL;
import static iudx.file.server.authenticator.utilities.Constants.ROLE;
import static iudx.file.server.common.Constants.AUDIT_SERVICE_ADDRESS;
import static iudx.file.server.common.Constants.DB_SERVICE_ADDRESS;
import static iudx.file.server.common.QueryType.TEMPORAL_GEO;
import static iudx.file.server.database.elasticdb.utilities.Constants.TYPE_KEY;

import io.netty.handler.codec.http.HttpConstants;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.vertx.core.*;
import io.vertx.core.file.FileSystem;
import io.vertx.core.http.*;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.JksOptions;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import iudx.file.server.apiserver.handlers.AuthHandler;
import iudx.file.server.apiserver.handlers.ValidationFailureHandler;
import iudx.file.server.apiserver.handlers.ValidationsHandler;
import iudx.file.server.apiserver.query.QueryParams;
import iudx.file.server.apiserver.response.ResponseType;
import iudx.file.server.apiserver.response.ResponseUrn;
import iudx.file.server.apiserver.response.RestResponse;
import iudx.file.server.apiserver.service.FileService;
import iudx.file.server.apiserver.service.impl.LocalStorageFileServiceImpl;
import iudx.file.server.apiserver.utilities.HttpStatusCode;
import iudx.file.server.apiserver.validations.ContentTypeValidator;
import iudx.file.server.apiserver.validations.RequestType;
import iudx.file.server.apiserver.validations.RequestValidator;
import iudx.file.server.auditing.AuditingService;
import iudx.file.server.common.Api;
import iudx.file.server.common.QueryType;
import iudx.file.server.common.WebClientFactory;
import iudx.file.server.common.service.CatalogueService;
import iudx.file.server.common.service.impl.CatalogueServiceImpl;
import iudx.file.server.database.elasticdb.DatabaseService;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The File Server API Verticle.
 *
 * <h1>File Server API Verticle</h1>
 *
 * <p>The API Server verticle implements the IUDX File Server APIs. It handles the API requests from
 * the clients and interacts with the associated Service to respond.
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

  /** Service addresses. */
  private static final String DATABASE_SERVICE_ADDRESS = DB_SERVICE_ADDRESS;

  private final ValidationFailureHandler validationsFailureHandler = new ValidationFailureHandler();
  private HttpServer server;
  private String keystore;
  private String keystorePassword;
  private Router router;
  // private FileServer fileServer;
  private FileService fileService;
  private DatabaseService database;
  private AuditingService auditingService;
  private String directory;
  private String tempDirectory;
  private RequestValidator requestValidator;
  private ContentTypeValidator contentTypeValidator;
  private WebClientFactory webClientFactory;
  private CatalogueService catalogueService;
  private String dxApiBasePath;
  private String dxV1BasePath;

  public JsonArray datalimit;

  @Override
  public void start() throws Exception {
    int port;

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

    dxApiBasePath = config().getString("dxApiBasePath");
    dxV1BasePath = config().getString("iudxApiBasePath");

    router = Router.router(vertx);
    router
        .route()
        .handler(
            CorsHandler.create("*").allowedHeaders(allowedHeaders).allowedMethods(allowedMethods));

    requestValidator = new RequestValidator();
    contentTypeValidator = new ContentTypeValidator(config().getJsonObject("allowedContentType"));
    router
        .route()
        .handler(
            requestHandler -> {
              requestHandler
                  .response()
                  .putHeader("Cache-Control", "no-cache, no-store,  must-revalidate,max-age=0")
                  .putHeader("Pragma", "no-cache")
                  .putHeader("Expires", "0")
                  .putHeader("X-Content-Type-Options", "nosniff");
              requestHandler.next();
            });

    webClientFactory = new WebClientFactory(vertx);

    // authService = new AuthServiceImpl(vertx, webClientFactory, config());
    catalogueService = new CatalogueServiceImpl(webClientFactory, config());

    directory = config().getString("upload_dir");
    tempDirectory = config().getString("tmp_dir");
    Api api = Api.getInstance(dxApiBasePath, dxV1BasePath);

    ValidationsHandler temporalQueryVaidationHandler =
        new ValidationsHandler(RequestType.TEMPORAL_QUERY);
    router
        .get(api.getApiTemporal())
        .handler(BodyHandler.create())
        .handler(temporalQueryVaidationHandler)
        .handler(AuthHandler.create(vertx))
        .handler(this::query)
        .failureHandler(validationsFailureHandler);

    ValidationsHandler uploadValidationHandler = new ValidationsHandler(RequestType.UPLOAD);
    router
        .post(api.getApiFileUpload())
        .handler(
            BodyHandler.create()
                .setUploadsDirectory(tempDirectory)
                .setBodyLimit(MAX_SIZE)
                .setDeleteUploadedFilesOnEnd(false))
        .handler(uploadValidationHandler)
        .handler(AuthHandler.create(vertx))
        .handler(this::upload)
        .failureHandler(validationsFailureHandler);

    ValidationsHandler downloadValidationHandler = new ValidationsHandler(RequestType.DOWNLOAD);
    router
        .get(api.getApiFileDownload())
        .handler(BodyHandler.create())
        .handler(downloadValidationHandler)
        .handler(AuthHandler.create(vertx))
        .handler(this::download)
        .failureHandler(validationsFailureHandler);

    ValidationsHandler deleteValidationHandler = new ValidationsHandler(RequestType.DELETE);
    router
        .delete(api.getApiFileDelete())
        .handler(BodyHandler.create())
        .handler(deleteValidationHandler)
        .handler(AuthHandler.create(vertx))
        .handler(this::delete)
        .failureHandler(validationsFailureHandler);

    ValidationsHandler listQueryValidationHandler = new ValidationsHandler(RequestType.LIST_QUERY);
    router
        .get(api.getListMetaData())
        .handler(BodyHandler.create())
        .handler(listQueryValidationHandler)
        .handler(AuthHandler.create(vertx))
        .handler(this::listMetadata)
        .failureHandler(validationsFailureHandler);

    ValidationsHandler geoQueryValidationHandler = new ValidationsHandler(RequestType.GEO_QUERY);
    router
        .get(api.getApiSpatial())
        .handler(BodyHandler.create())
        .handler(geoQueryValidationHandler)
        .handler(AuthHandler.create(vertx))
        .handler(this::query)
        .failureHandler(validationsFailureHandler);

    router
        .get(API_API_SPECS)
        .produces("application/json")
        .handler(
            routingContext -> {
              HttpServerResponse response = routingContext.response();
              response.sendFile("docs/openapi.yaml");
            });

    /* Get redoc */
    router
        .get(API_APIS)
        .produces("text/html")
        .handler(
            routingContext -> {
              HttpServerResponse response = routingContext.response();
              response.sendFile("docs/apidoc.html");
            });
    boolean isssl;
    LOGGER.info("starting server");
    /* Read ssl configuration. */
    isssl = config().getBoolean("ssl");
    HttpServerOptions serverOptions = new HttpServerOptions();

    if (isssl) {
      /* Read the configuration and set the HTTPs server properties. */

      keystore = config().getString("keystore");
      keystorePassword = config().getString("keystorePassword");

      /*
       * Default port when ssl is enabled is 8443. If set through config, then that value is taken
       */
      port = config().getInteger("httpPort") == null ? 8443 : config().getInteger("httpPort");

      /* Setup the HTTPs server properties, APIs and port. */

      serverOptions
          .setSsl(true)
          .setKeyStoreOptions(new JksOptions().setPath(keystore).setPassword(keystorePassword));
      LOGGER.info("Info: Starting HTTPs server at port " + port);

    } else {
      LOGGER.debug("Info: Starting HTTP server");

      /* Setup the HTTP server properties, APIs and port. */

      serverOptions.setSsl(false);
      /*
       * Default port when ssl is disabled is 8080. If set through config, then that value is taken
       */
      port = config().getInteger("httpPort") == null ? 8080 : config().getInteger("httpPort");
      LOGGER.info("Info: Starting HTTP server at port " + port);
    }

    server = vertx.createHttpServer(serverOptions);

    server.requestHandler(router).listen(port);

    database = DatabaseService.createProxy(vertx, DATABASE_SERVICE_ADDRESS);

    fileService = new LocalStorageFileServiceImpl(vertx.fileSystem(), directory);

    auditingService = AuditingService.createProxy(vertx, AUDIT_SERVICE_ADDRESS);

    // check upload dir exist or not.
    mkdirsIfNotExists(
        vertx.fileSystem(),
        directory,
        new Handler<AsyncResult<Void>>() {
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

    /* Print the deployed endpoints */
    printDeployedEndpoints(router);
  }

  private void printDeployedEndpoints(Router router) {
    for (Route route : router.getRoutes()) {
      if (route.getPath() != null) {
        LOGGER.info("API Endpoints deployed : " + route.methods() + " : " + route.getPath());
      }
    }
  }

  /**
   * Upload File service allows to upload a file into the server after authenticating the user.
   *
   * @param routingContext Handles web request in Vert.x web
   */
  public void upload(RoutingContext routingContext) {
    LOGGER.debug("upload() started.");
    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();
    MultiMap formParam = request.formAttributes();
    JsonObject authInfo = (JsonObject) routingContext.data().get("authInfo");
    String id = formParam.get("id");
    response.putHeader("content-type", "application/json");
    JsonObject auditParams =
        new JsonObject()
            .put("api", request.path())
            .put(USER_ID, authInfo.getString(USER_ID))
            .put(ROLE, authInfo.getString(ROLE))
            .put(DRL, authInfo.getString(DRL))
            .put(DID, authInfo.getString(DID))
            .put(RESOURCE_ID, id);

    Boolean isExternalStorage = Boolean.parseBoolean(request.getHeader("externalStorage"));
    Boolean isSample = Boolean.valueOf(formParam.get("isSample"));
    Future<String> uploadPathFuture = getPath(id);

    List<FileUpload> files = routingContext.fileUploads();
    if (!isExternalStorage && (files.size() == 0 || !isValidFileContentType(files))) {
      handleResponse(response, HttpStatusCode.BAD_REQUEST);
      String message =
          new RestResponse.Builder()
              .type(String.valueOf(400))
              .title(HttpStatusCode.BAD_REQUEST.getUrn())
              .details("bad request")
              .build()
              .toJsonString();
      LOGGER.error("Invalid File type or no file attached");
      processResponse(response, message);
      return;
    }
    uploadPathFuture.onComplete(
        itemHadler -> {
          if (itemHadler.succeeded()) {
            String uploadPath = itemHadler.result();
            if (isExternalStorage) {
              JsonObject responseJson = new JsonObject();
              String fileId = id + "/" + UUID.randomUUID();
              Future<Boolean> saveRecordFuture = saveFileRecord(formParam, fileId);

              saveRecordFuture.onComplete(
                  saveRecordHandler -> {
                    if (saveRecordHandler.succeeded()) {
                      responseJson
                          .put(JSON_TYPE, SUCCESS.getUrn())
                          .put(JSON_TITLE, "Success")
                          .put(
                              RESULTS, new JsonArray().add(new JsonObject().put("fileId", fileId)));

                      handleResponse(response, HttpStatusCode.SUCCESS, responseJson);
                      auditParams.put(RESPONSE_SIZE, 0);
                      updateAuditTable(auditParams);
                    } else {
                      processResponse(response, saveRecordHandler.cause().getMessage());
                    }
                  });
            } else if (isSample) {
              sampleFileUpload(response, files, "sample", uploadPath, id);
            } else {
              archiveFileUpload(response, formParam, files, uploadPath, id, auditParams);
            }
          } else {
            LOGGER.debug("unable to construct folder structure");
            processResponse(response, itemHadler.cause().getMessage());
          }
        });
  }

  /**
   * Helper method to upload a sample file.
   *
   * @param response HttpServerResponse
   * @param files set of file (although there will always be a single file to upload) * @param
   * @param filePath path for the upload file * @param id resource id
   */
  private void sampleFileUpload(
      HttpServerResponse response,
      List<FileUpload> files,
      String fileName,
      String filePath,
      String id) {

    Future<JsonObject> uploadFuture = fileService.upload(files, fileName, filePath);

    uploadFuture.onComplete(
        uploadHandler -> {
          if (uploadHandler.succeeded()) {
            JsonObject uploadResult = uploadHandler.result();
            JsonObject responseJson = new JsonObject();
            String fileId = id + "/" + uploadResult.getString("file-id");

            responseJson
                .put(JSON_TYPE, SUCCESS.getUrn())
                .put(JSON_TITLE, "Success")
                .put("results", new JsonArray().add(new JsonObject().put("fileId", fileId)));
            // insertFileRecord(params, fileId); no need to insert in DB
            handleResponse(response, HttpStatusCode.SUCCESS, responseJson);
          } else {
            processResponse(response, uploadHandler.cause().getMessage());
          }
        });
  }

  /**
   * Helper method to upload a archieve file.
   *
   * @param response HttpServerResponse
   * @param files set of file (although there will always be a single file to upload)
   * @param filePath path for the upload file
   * @param id resource id
   * @param auditParams for auditing
   */
  private void archiveFileUpload(
      HttpServerResponse response,
      MultiMap params,
      List<FileUpload> files,
      String filePath,
      String id,
      JsonObject auditParams) {
    JsonObject uploadJson = new JsonObject();
    JsonObject responseJson = new JsonObject();
    Future<Boolean> requestValidatorFuture = requestValidator.isValidArchiveRequest(params);
    requestValidatorFuture
        .compose(
            requestValidatorhandler -> {
              return fileService.upload(files, filePath);
            })
        .compose(
            uploadHandler -> {
              LOGGER.debug("upload json :" + uploadHandler);
              if (uploadHandler.containsKey("detail")) {
                return Future.failedFuture(uploadHandler.toString());
              }
              String fileId = id + "/" + uploadHandler.getString("file-id");
              uploadJson.put("upload", true);
              uploadJson.put("fileId", fileId);
              return catalogueService.isAllowedMetaDataField(params);
            })
        .compose(
            metaDataValidatorHandler -> {
              LOGGER.debug("file-id : " + uploadJson.getString("fileId"));
              return saveFileRecord(params, uploadJson.getString("fileId"));
            })
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                responseJson
                    .put(JSON_TYPE, SUCCESS.getUrn())
                    .put(JSON_TITLE, "Success")
                    .put(
                        RESULTS,
                        new JsonArray()
                            .add(new JsonObject().put("fileId", uploadJson.getString("fileId"))));
                handleResponse(response, HttpStatusCode.SUCCESS, responseJson);
                auditParams.put(RESPONSE_SIZE, 0);
                updateAuditTable(auditParams);
              } else {
                LOGGER.debug(handler.cause());
                if (uploadJson.containsKey("upload") && uploadJson.getBoolean("upload")) {
                  // fail, run Compensating service to clean/undo upload.
                  String fileId = uploadJson.getString("fileId");
                  String fileuuId = StringUtils.substringAfterLast(fileId, FORWARD_SLASH);
                  LOGGER.debug("deleting file :" + fileuuId + " uploadDir: " + filePath);
                  vertx.fileSystem().deleteBlocking(directory + "/" + filePath + "/" + fileuuId);
                }
                processResponse(response, handler.cause().getMessage());
              }
            });
  }

  private Future<Boolean> saveFileRecord(MultiMap formParams, String fileId) {
    JsonObject json = new JsonObject();
    json.put("id", formParams.get("id"));
    json.put(
        "timeRange",
        new JsonObject()
            .put("gte", formParams.get("startTime"))
            .put("lte", formParams.get("endTime")));
    json.put("fileId", fileId);

    json.put(
        "location",
        new JsonObject()
            .put("type", formParams.get("geometry"))
            .put("coordinates", new JsonArray(formParams.get("coordinates"))));

    // remove already added default metadata fields from formParams.
    formParams.remove("id");
    formParams.remove("startTime");
    formParams.remove("endTime");
    formParams.remove("geometry");
    formParams.remove("coordinates");

    formParams.entries().stream().forEach(e -> json.put(e.getKey(), e.getValue()));
    Promise<Boolean> promise = Promise.promise();

    Future<JsonObject> saveDb = database.save(json);
    saveDb.onComplete(
        handler -> {
          if (handler.succeeded()) {
            LOGGER.info("Record inserted in DB");
            promise.complete(true);
          } else if (handler.failed()) {
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
    JsonObject authInfo = (JsonObject) routingContext.data().get("authInfo");
    LOGGER.info("authinfo inside download{}", authInfo);
    response.setChunked(true);
    String id = request.getParam("file-id");
    LOGGER.debug("id: " + id);
    String resource = StringUtils.substringBeforeLast(id, FORWARD_SLASH);
    String fileuuId = StringUtils.substringAfterLast(id, FORWARD_SLASH);
    String fileName = id.substring(id.lastIndexOf(FORWARD_SLASH));
    Future<String> uploadDirFuture = getPath(resource);
    JsonObject auditParams =
        new JsonObject()
            .put("api", request.path())
            .put(USER_ID, authInfo.getString(USER_ID))
            .put(ROLE, authInfo.getString(ROLE))
            .put(DRL, authInfo.getString(DRL))
            .put(DID, authInfo.getString(DID))
            .put(RESOURCE_ID, resource);

    uploadDirFuture.onComplete(
        dirHanler -> {
          if (dirHanler.succeeded()) {
            String uploadDir = dirHanler.result();
            LOGGER.debug(
                "uploadDir: " + uploadDir + "; fileuuId: " + fileuuId + "; fileName: " + fileName);
            LOGGER.info(" response size is {}",response.bytesWritten());

            fileService
                .download(fileuuId, uploadDir, response)

                .onComplete(
                    handler -> {
                      if (handler.failed()) {
                        processResponse(response, handler.cause().getMessage());
                      } else {
                        if (!fileName.toLowerCase().contains("sample")) {
                          auditParams.put(RESPONSE_SIZE, response.bytesWritten());
                          updateAuditTable(auditParams);
                        }
                      }
                      // do nothing response is already written and file is served using
                      // content-disposition.
                    });
          } else {
            LOGGER.debug("unable to construct folder structure");
            processResponse(response, dirHanler.cause().getMessage());
          }
        });
  }

  public void query(RoutingContext context) {
    HttpServerResponse response = context.response();
    JsonObject authInfo = (JsonObject) context.data().get("authInfo");




    MultiMap queryParams = getQueryParams(context, response).get();
//    LOGGER.info(authInfo.getJsonArray("access").getValue(0).);
    // Log queryParams details with DEBUG level
//    LOGGER.debug("Extracted queryParams:");
//    for (String key : queryParams.names()) {
//      LOGGER.trace("  - {}: {}", key, queryParams);
//    }

    /*JsonArray accessArray = authInfo.getJsonArray("access");
    if (accessArray != null && accessArray.size() > 0) {
      JsonObject firstAccessObject = accessArray.getJsonObject(0);
      if (firstAccessObject != null) {
        String fileValue;
          fileValue = firstAccessObject.getString("file", null);
          // fileValue now contains the value of the "file" key from the first "access" array element
        LOGGER.info("the file value is{}",   fileValue);
      }
    }*/

//    LOGGER.debug("Extracted queryParams:");

    Future<Boolean> queryParamsValidator = requestValidator.isValid(queryParams);

    Future<List<String>> allowedFilters =
        catalogueService.getAllowedFilters4Queries(queryParams.get(PARAM_ID));

    JsonObject query = new JsonObject();
    for (Map.Entry<String, String> entry : queryParams.entries()) {
      query.put(entry.getKey(), entry.getValue());
    }

    if (authInfo.containsKey("attrs")) {
      List<String> maskedAttributesList = authInfo.getJsonArray("attrs").getList();

      query.put("attrs", maskedAttributesList);
    }

    QueryParams params = query.mapTo(QueryParams.class).build();


    QueryType type = getQueryType(params);
    JsonObject auditParams =
        new JsonObject()
            .put(RESOURCE_ID, query.getString("id"))
            .put("api", context.request().path())
            .put(USER_ID, authInfo.getString(USER_ID))
            .put(ROLE, authInfo.getString(ROLE))
            .put(DRL, authInfo.getString(DRL))
            .put(DID, authInfo.getString(DID));




    queryParamsValidator
        .compose(
            paramsValidator -> {
              return allowedFilters;
            })
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                boolean isValidFilters =
                    true; // TODO :change to false once filters available in cat for
                // file
                List<String> applicableFilters = handler.result();

                if (TEMPORAL_GEO.equals(type)
                    && applicableFilters.contains("SPATIAL")
                    && applicableFilters.contains("TEMPORAL")) {
                  isValidFilters = true;
                } else if (QueryType.GEO.equals(type) && applicableFilters.contains("SPATIAL")) {
                  isValidFilters = true;
                } else if (QueryType.TEMPORAL.equals(type)
                    && applicableFilters.contains("TEMPORAL")) {
                  isValidFilters = true;
                }
                if (isValidFilters) {
                  executeSearch(JsonObject.mapFrom(params), type, response, auditParams);
                } else {
                  handleResponse(
                      response,
                      HttpStatusCode.BAD_REQUEST,
                      "Either geo or temporal parameter is not allowed for resource");
                }
              } else {
                LOGGER.error(handler.cause().getMessage());
                handleResponse(response, HttpStatusCode.BAD_REQUEST, (ResponseUrn) null);
              }
              //LOGGER.trace(JsonObject.mapFrom(params)); //{"id":"b58da193-23d9-43eb-b98a-a103d4b6103c","timerel":"between","time":"202
             // 0-09-10T00:00:00Z","endTime":"2020-09-15T00:00:00Z"}
            });
  }

  private void executeSearch(
      JsonObject json, QueryType type, HttpServerResponse response, JsonObject auditParams) {
    Future<JsonObject> searchDbFuture = database.search(json, type);

    searchDbFuture.onComplete(    /// before searchDbfuturecomplete we need to check , whether he is exceeding the file restriction
        handler -> {
          if (handler.succeeded()) {

//            if (response.bytesWritten() > datalimit)




            LOGGER.info("Success: Search Success");

            handleSuccessResponse(response, ResponseType.Ok.getCode(), handler.result().toString());
            auditParams.put(RESPONSE_SIZE, response.bytesWritten());

            Future.future(fu -> updateAuditTable(auditParams));
          } else if (handler.failed()) {
            LOGGER.error("Fail: Search Fail");
            processBackendResponse(response, handler.cause().getMessage());
          }
        });
  }

  /**
   * Download File service allows to download a file from the server after authenticating the user.
   */
  public void delete(RoutingContext routingContext) {
    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();
  JsonObject authInfo = (JsonObject) routingContext.data().get("authInfo");
//  JsonArray datalimit = authInfo.getJsonArray("access").getValue(2);
    response.putHeader("content-type", "application/json");
    String id = request.getParam("file-id");
    String resource = StringUtils.substringBefore(id, FORWARD_SLASH);
      JsonObject auditParams =
        new JsonObject()
            .put("api", request.path())
            .put(USER_ID, authInfo.getString(USER_ID))
            .put(ROLE, authInfo.getString(ROLE))
            .put(DRL, authInfo.getString(DRL))
            .put(DID, authInfo.getString(DID))
            .put(RESOURCE_ID, resource);

    String fileuuId = StringUtils.substringAfterLast(id, FORWARD_SLASH);
    boolean isArchiveFile;
    if (fileuuId.contains("sample")) {
      isArchiveFile = false;
    } else {
      isArchiveFile = true;
    }

    Boolean isExternalStorage = Boolean.parseBoolean(request.getHeader("externalStorage"));
    Future<String> uploadDirFuture = getPath(resource);
    if (isExternalStorage) {
      Future<JsonObject> deleteDbFuture = database.delete(id);
      deleteDbFuture.onComplete(
          handlers -> {
            if (handlers.succeeded()) {
              JsonObject dbHandlerResult = handlers.result();
              String resultTitle = dbHandlerResult.getJsonObject("result").getString(TITLE);
              int resultType = dbHandlerResult.getJsonObject("result").getInteger(TYPE_KEY);
              HttpStatusCode code = HttpStatusCode.getByValue(resultType);
              ResponseUrn urn = urnFromCode(resultTitle);
              if (urn.equals(SUCCESS)) {
                JsonObject responseJson =
                    new JsonObject()
                        .put(TYPE_KEY, SUCCESS.getUrn())
                        .put(TITLE, "Successful Operation")
                        .put(
                            "results",
                            new JsonArray()
                                .add(
                                    new JsonObject()
                                        .put(
                                            "detail",
                                            "File with id : " + id + " deleted successfully")));
                handleResponse(response, HttpStatusCode.SUCCESS, responseJson);
                auditParams.put(RESPONSE_SIZE, 0);
                updateAuditTable(auditParams);
              } else if (urn.equals(RESOURCE_NOT_FOUND)) {
                String resultDetails = dbHandlerResult.getJsonObject("result").getString("details");
                handleResponse(response, code, urn, resultDetails);
              }
            } else {
              processResponse(response, handlers.cause().getMessage());
            }
          });
    } else {
      uploadDirFuture.onComplete(
          dirHanler -> {
            if (dirHanler.succeeded()) {
              String uploadDir = dirHanler.result();
              LOGGER.debug(
                  "uploadDir: "
                      + uploadDir
                      + "; fileuuId: "
                      + fileuuId
                      + "; auditParams: "
                      + auditParams);
              if (isArchiveFile) {
                deleteArchiveFile(response, id, fileuuId, uploadDir, auditParams);
              } else {
                deleteSampleFile(response, id, fileuuId, uploadDir);
              }
            } else {
              LOGGER.debug("unable to construct folder structure");
              processResponse(response, dirHanler.cause().getMessage());
            }
          });
    }
  }

  /** get list of metatdata form server. */
  public void listMetadata(RoutingContext context) {
    HttpServerRequest request = context.request();
    HttpServerResponse response = context.response();
    JsonObject authInfo = (JsonObject) context.data().get("authInfo");
    String id = request.getParam("id");
    JsonObject query = new JsonObject().put("id", id);


    List<String> maskedAttributesList = authInfo.getJsonArray("attrs").getList();

    query.put("attrs", maskedAttributesList);

    JsonObject auditParams =
        new JsonObject()
            .put("api", request.path())
            .put(USER_ID, authInfo.getString(USER_ID))
            .put(ROLE, authInfo.getString(ROLE))
            .put(DRL, authInfo.getString(DRL))
            .put(DID, authInfo.getString(DID))
            .put(RESOURCE_ID, id);

    Future<JsonObject> searchDbFuture = database.search(query, QueryType.LIST);
    searchDbFuture.onComplete(
        handler -> {
          if (handler.succeeded()) {
            handleSuccessResponse(response, ResponseType.Ok.getCode(), handler.result().toString());
            auditParams.put(RESPONSE_SIZE, response.bytesWritten());
            Future.future(fu -> updateAuditTable(auditParams));
          } else if (handler.failed()) {
            LOGGER.error("Fail: Search Fail");
            processBackendResponse(response, handler.cause().getMessage());
          }
        });
  }

  private void deleteSampleFile(
      HttpServerResponse response, String id, String fileuuId, String uploadDir) {
    LOGGER.info("delete sample file");
    fileService
        .delete(fileuuId, uploadDir.toString())
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                JsonObject responseJson =
                    new JsonObject()
                        .put(JSON_TYPE, SUCCESS.getUrn())
                        .put(JSON_TITLE, "Successful Operation")
                        .put(
                            RESULTS,
                            new JsonArray()
                                .add(
                                    new JsonObject()
                                        .put(
                                            "detail",
                                            "File with id : " + id + " deleted successfully")));
                handleResponse(response, HttpStatusCode.SUCCESS, responseJson);

              } else {
                processResponse(response, handler.cause().getMessage());
              }
            });
  }

  private void deleteArchiveFile(
      HttpServerResponse response,
      String id,
      String fileuuId,
      String uploadDir,
      JsonObject auditParams) {
    Future<JsonObject> deleteDbFuture = database.delete(id);
    deleteDbFuture.onComplete(
        handlers -> {
          if (handlers.succeeded()) {
            fileService
                .delete(fileuuId, uploadDir)
                .onComplete(
                    handler -> {
                      if (handler.succeeded()) {
                        JsonObject responseJson =
                            new JsonObject()
                                .put(TYPE_KEY, SUCCESS.getUrn())
                                .put(TITLE, "Successful Operation")
                                .put(
                                    RESULTS,
                                    new JsonArray()
                                        .add(
                                            new JsonObject()
                                                .put(
                                                    "detail",
                                                    "File with id : "
                                                        + id
                                                        + " deleted successfully")));
                        handleResponse(response, HttpStatusCode.SUCCESS, responseJson);
                        auditParams.put(RESPONSE_SIZE, 0);
                        updateAuditTable(auditParams);
                      } else {
                        processResponse(response, handler.cause().getMessage());
                      }
                    });
          } else {
            processResponse(response, handlers.cause().getMessage());
          }
        });
  }

  /**
   * Helper method to check/create initial directory structure.
   *
   * @param fileSystem vert.x FileSystem
   * @param basePath base derfault directory structure
   * @param handler Async handler
   */
  private void mkdirsIfNotExists(
      FileSystem fileSystem, String basePath, final Handler<AsyncResult<Void>> handler) {
    LOGGER.info("mkidr check started");
    fileSystem.exists(
        basePath,
        new Handler<AsyncResult<Boolean>>() {
          @Override
          public void handle(AsyncResult<Boolean> event) {
            if (event.succeeded()) {
              if (Boolean.FALSE.equals(event.result())) {
                LOGGER.info("Creating parent directory structure ...  " + basePath);
                fileSystem.mkdirs(
                    basePath,
                    new Handler<AsyncResult<Void>>() {
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

  private void processResponse(HttpServerResponse response, String failureMessage) {
    try {
      JsonObject json = new JsonObject(failureMessage);
      int type = json.getInteger(JSON_TYPE);
      HttpStatusCode status = HttpStatusCode.getByValue(type);
      String urnTitle = json.getString(JSON_TITLE);
      ResponseUrn urn;
      if (urnTitle != null) {
        urn = urnFromCode(urnTitle);
      } else {
        urn = urnFromCode(type + "");
      }

      String message = json.getString(ERROR_MESSAGE);
      response.putHeader(CONTENT_TYPE, APPLICATION_JSON).setStatusCode(type);

      if (message == null || message.isEmpty()) {
        response.end(generateResponse(status, urn).toString());
      } else {
        response.end(generateResponse(status, urn, message).toString());
      }
    } catch (DecodeException ex) {
      LOGGER.error("ERROR : Expecting Json received else from backend service");
      handleResponse(response, HttpStatusCode.BAD_REQUEST, BACKING_SERVICE_FORMAT);
    }
  }

  private void handleResponse(HttpServerResponse response, HttpStatusCode code, JsonObject result) {
    response
        .putHeader(CONTENT_TYPE, APPLICATION_JSON)
        .setStatusCode(code.getValue())
        .end(result.toString());
  }

  private void handleResponse(HttpServerResponse response, HttpStatusCode code, ResponseUrn urn) {
    handleResponse(response, code, urn, code.getDescription());
  }

  private void handleResponse(HttpServerResponse response, HttpStatusCode code) {
    ResponseUrn urn = urnFromCode(code.getUrn());
    handleResponse(response, code, urn, code.getDescription());
  }

  private void handleResponse(HttpServerResponse response, HttpStatusCode code, String message) {
    ResponseUrn urn = urnFromCode(code.getUrn());
    handleResponse(response, code, urn, message);
  }

  private void handleResponse(
      HttpServerResponse response, HttpStatusCode code, ResponseUrn urn, String message) {
    response
        .putHeader(CONTENT_TYPE, APPLICATION_JSON)
        .setStatusCode(code.getValue())
        .end(generateResponse(code, urn, message).toString());
  }

  private JsonObject generateResponse(HttpStatusCode code, ResponseUrn urn) {
    return generateResponse(code, urn, urn.getMessage());
  }

  private JsonObject generateResponse(HttpStatusCode code, ResponseUrn urn, String message) {
    String type = urn != null ? urn.getUrn() : code.getUrn();
    return new RestResponse.Builder()
        .type(type)
        .title(code.getDescription())
        .details(message)
        .build()
        .toJson();
  }

  private boolean isValidFileContentType(List<FileUpload> files) {
    for (FileUpload file : files) {
      LOGGER.debug(file.contentType());
      if (!contentTypeValidator.isValid(file.contentType())) {
        return false;
      }
    }
    return true;
  }

  private Optional<MultiMap> getQueryParams(
      RoutingContext routingContext, HttpServerResponse response) {
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
    } catch (IllegalArgumentException ex) {
      response
          .putHeader(CONTENT_TYPE, APPLICATION_JSON)
          .setStatusCode(HttpStatus.SC_BAD_REQUEST)
          .end(generateResponse(HttpStatusCode.BAD_REQUEST, INVALID_ATTR_PARAM).toString());
    }
    return Optional.of(queryParams);
  }

  /**
   * function to handle call to audit service.
   *
   * @param auditInfo contains userid, api-endpoint and the resourceid
   */
  private void updateAuditTable(JsonObject auditInfo) {
    catalogueService
        .getRelItem(auditInfo.getString(ID))
        .onComplete(
            catHandler -> {
              if (catHandler.succeeded()) {
                JsonObject catResult = catHandler.result();
                String providerId = catResult.getString("provider");
                String type = catResult.containsKey(RESOURCE_GROUP) ? "RESOURCE" : "RESOURCE_GROUP";
                String resourceGroup =
                    catResult.containsKey(RESOURCE_GROUP)
                        ? catResult.getString(RESOURCE_GROUP)
                        : catResult.getString(ID);
                ZonedDateTime zst = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));
                long epochTime = zst.toInstant().toEpochMilli();
                String isoTime = zst.truncatedTo(ChronoUnit.SECONDS).toString();
                auditInfo.put(RESOURCE_GROUP, resourceGroup);
                auditInfo.put(TYPE_KEY, type);
                auditInfo.put(PROVIDER_ID, providerId);
                auditInfo.put(EPOCH_TIME, epochTime);
                auditInfo.put(ISO_TIME, isoTime);
                String role = auditInfo.getString(ROLE);
                String drl = auditInfo.getString(DRL);
                if (role.equalsIgnoreCase("delegate") && drl != null) {
                  auditInfo.put(DELEGATOR_ID, auditInfo.getString(DID));
                } else {
                  auditInfo.put(DELEGATOR_ID, auditInfo.getString(USER_ID));
                }
                auditingService.executeWriteQuery(
                    auditInfo,
                    auditHandler -> {
                      if (auditHandler.succeeded()) {
                        LOGGER.info("audit table updated");
                      } else {
                        LOGGER.error("failed to update audit table");
                      }
                    });
              } else {
                LOGGER.error("failed to update audit table");
              }
            });
  }

  @Override
  public void stop() {
    LOGGER.info("Stopping the File server");
  }

  private void handleSuccessResponse(HttpServerResponse response, int statusCode, String result) {
    response.putHeader(CONTENT_TYPE, APPLICATION_JSON).setStatusCode(statusCode).end(result);
  }

  private void processBackendResponse(HttpServerResponse response, String failureMessage) {
    try {
      JsonObject json = new JsonObject(failureMessage);
      int type = json.getInteger(TYPE_KEY);
      HttpStatusCode httpStatusCode = HttpStatusCode.getByValue(type);
      String urnTitle = json.getString(JSON_TITLE);
      ResponseUrn urn;
      if (urnTitle != null) {
        urn = urnFromCode(urnTitle);
      } else {
        urn = urnFromCode(type + "");
      }
      // return urn in body
      response
          .putHeader(CONTENT_TYPE, APPLICATION_JSON)
          .setStatusCode(type)
          .end(generateResponse(httpStatusCode, urn).toString());
    } catch (DecodeException ex) {
      LOGGER.error("ERROR : Expecting Json from backend service [ jsonFormattingException ]");
      handleResponse(response, HttpStatusCode.BAD_REQUEST, BACKING_SERVICE_FORMAT_URN);
    }
  }

  private Future<String> getPath(String id) {
    Promise promise = Promise.promise();
    catalogueService
        .getRelItem(id)
        .onComplete(
            itemHadler -> {
              if (itemHadler.succeeded()) {
                JsonObject catItemJson = itemHadler.result();
                StringBuilder uploadPath;
                if (catItemJson.containsKey("resourceGroup")) {
                  uploadPath =
                      new StringBuilder()
                          .append(catItemJson.getString("resourceGroup"))
                          .append("/")
                          .append(id);
                } else {
                  uploadPath = new StringBuilder(id);
                }
                promise.complete(uploadPath.toString());
              } else {
                promise.fail("fail");
              }
            });
    return promise.future();
  }
}

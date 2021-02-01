package iudx.file.server;

import static iudx.file.server.utilities.Constants.*;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.Principal;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import javax.net.ssl.SSLPeerUnverifiedException;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
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
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.vertx.core.net.JksOptions;
import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;
import iudx.file.server.handlers.UserAuthorizationHandler;
import iudx.file.server.service.FileService;
import iudx.file.server.service.TokenStore;
import iudx.file.server.service.impl.LocalStorageFileServiceImpl;
import iudx.file.server.service.impl.PGTokenStoreImpl;
import iudx.file.server.utilities.Constants;
import iudx.file.server.utilities.CustomResponse;
import iudx.file.server.utilities.DefaultAsyncResult;
// import iudx.file.server.utilities.FileServer;
import iudx.file.server.utilities.FileServer;

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
  private TokenStore tokenStoreClient;


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
        tokenStoreClient = new PGTokenStoreImpl(vertx, config());

        router.route().handler(BodyHandler.create());

        // router.route().handler(UserAuthorizationHandler.create(tokenStoreClient));

        router.route().failureHandler(failureHandler -> {
          failureHandler.failure().printStackTrace();
          LOGGER.error(failureHandler.failure());
        });

        router.post(API_TEMPORAL).handler(this::query);

        router.post(API_FILE_UPLOAD).handler(BodyHandler.create().setUploadsDirectory(TMP_DIR)
            .setBodyLimit(MAX_SIZE).setDeleteUploadedFilesOnEnd(true)).handler(this::upload);

        router.get(API_FILE_DOWNLOAD).handler(this::download);
        router.delete(API_FILE_DELETE).handler(this::delete);
        router.get(API_TOKEN).handler(this::getFileServerToken);

        /* Read the configuration and set the HTTPs server properties. */
        ClientAuth clientAuth = ClientAuth.REQUEST;

        try {

          inputstream = new FileInputStream(Constants.CONFIG_FILE);
          properties.load(inputstream);

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
        } catch (Exception ex) {
          LOGGER.info(ex.toString());
        }
        LOGGER.info("starting server");
        server =
            vertx.createHttpServer(new HttpServerOptions().setSsl(true).setClientAuth(clientAuth)
                .setKeyStoreOptions(
                    new JksOptions().setPath(keystore).setPassword(keystorePassword))
                .setTrustStoreOptions(
                    new JksOptions().setPath(truststore).setPassword(truststorePassword)));

        server.requestHandler(router).listen(Constants.PORT);

      }
      fileService = new LocalStorageFileServiceImpl(vertx.fileSystem());
      // check upload dir exist or not.
      mkdirsIfNotExists(vertx.fileSystem(), DIR, new Handler<AsyncResult<Void>>() {
        @Override
        public void handle(AsyncResult<Void> event) {
          if (event.succeeded()) {
            LOGGER.info("mkdirsIfNotExists's handler");
          } else {
            LOGGER.error(event.cause().getMessage(), event.cause());
          }
        }
      });
    });

    LOGGER.info("FileServerVerticle started successfully");
    // for unit testing - later it must be removed
    tokens.put("testing_key", "fileServerToken");
    // set validity of token for 10 hours
    Date currDate = new Date();
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(currDate);
    calendar.add(Calendar.HOUR_OF_DAY, 10);
    validity.put("testing_key", calendar.getTime());
  }

  /*
   * token service get file-server-token with its validity for given userToken
   */
  @Deprecated // use getFileServerToken()
  public void fileServerToken(RoutingContext routingContext) {
    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();
    response.setChunked(true);
    if (!isValidCertificate(routingContext)) {
      response.write("Invalid client certificate error").end();
    } else {
      /* create a temporary token, save in a hashmap */
      String userToken = request.getHeader("token");
      if (null == userToken || userToken.isEmpty()) {
        response.write("user token not provided").end();
      } else {
        // generate file server temporary token
        String fileServerToken = UUID.randomUUID().toString();
        // save user token and file server token in hashmap for later use
        tokens.put(userToken, fileServerToken);
        LOGGER.info("value stored inside tokens hashmap [ key : " + userToken + ", value : "
            + fileServerToken + " ]");
        // set validity of token for 10 hours
        Date currDate = new Date();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(currDate);
        calendar.add(Calendar.HOUR_OF_DAY, 10);
        // save validity in hashmap for later use
        validity.put(userToken, calendar.getTime());
        LOGGER.info("value stored inside validity hashmap [ key : " + userToken + ", value : "
            + calendar.getTime() + " ]");
        // TODO: need to save above values in database too (userToken,file-server-token,validity)
        JsonObject json = new JsonObject();
        json.put("fileServerToken", fileServerToken);
        json.put("validity", calendar.getTime().toString());
        LOGGER.info("json.toString()" + json.toString());
        /* send the token as response */
        response.write(json.toString()).end();
      }
    }
  }

  private void getFileServerToken(RoutingContext routingContext) {
    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();
    response.setChunked(true);
    if (!isValidCertificate(routingContext)) {
      response.putHeader(CONTENT_TYPE, APPLICATION_JSON).setStatusCode(400)
          .end(new CustomResponse.ResponseBuilder().withStatusCode(400)
              .withMessage("Invalid Client Certificate").build().toJsonString());
    } else {
      String authToken = request.getHeader("token");
      if (null == authToken || authToken.isEmpty()) {
        response.putHeader(CONTENT_TYPE, APPLICATION_JSON).setStatusCode(400)
            .end(new CustomResponse.ResponseBuilder().withStatusCode(400)
                .withMessage("User Token Not Provided").build().toJsonString());
      } else {
        String fileServerToken = UUID.randomUUID().toString();
        String serverId = routingContext.request().host();
        Future<JsonObject> issueToken = tokenStoreClient.put(authToken, fileServerToken, serverId);
        issueToken.onComplete(handler -> {
          if (handler.succeeded()) {
            LOGGER.info("token succeeded : " + handler.result());
            response.putHeader(CONTENT_TYPE, APPLICATION_JSON).setStatusCode(200)
                .end(new CustomResponse.ResponseBuilder().withStatusCode(200)
                    .withMessage(handler.result().toString()).build().toJsonString());
          } else {
            response.end(new CustomResponse.ResponseBuilder().withStatusCode(500)
                .withMessage(handler.cause().toString()).build().toJsonString());
          }
        });
      }
    }
  }

  /*
   * helper method : Check token validity
   */
  private boolean isTokenValid(HttpServerRequest request) {
    boolean isTokenValid = false;
    String token = request.getHeader("token");
    String fsToken = request.getHeader("fileServerToken");

    if (null != token && !token.isEmpty() && !token.isBlank() && null != fsToken
        && !fsToken.isEmpty() && !fsToken.isBlank()) {
      String fileServerToken = tokens.get(token);
      Date allowedTimeLimit = validity.get(token);
      if (null != fileServerToken && null != allowedTimeLimit) {
        // token is valid only when current date is before allowedTimeLimit and given fsToken equals
        // to fileServerToken
        if (fsToken.equals(fileServerToken) && new Date().before(allowedTimeLimit))
          isTokenValid = true;
      } else {
        // TODO: check with database and set token validity
      }
    }
    return isTokenValid;
  }

  /**
   * Upload File service allows to upload a file to the server after authenticating the user. Once
   * the file is uploaded, the metadata of it is stored in the resource server using the file
   * metadata API.
   *
   * @param routingContext Handles web request in Vert.x web
   */

  @Deprecated // use upload()
  public void uploadFile(RoutingContext routingContext) {
    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();
    response.putHeader("content-type", "application/json");

    /* Allow or deny upload of the file. If allow, Update the metadata to Resource Server */
    if (isTokenValid(request)) {
      LOGGER.info("Token is valid. file upload starts");
      FileServer fileServer = new FileServer(vertx, Constants.DIR);
      fileServer.writeUploadFile(request, Constants.MAX_SIZE, handler -> {
        String status = handler.getString("status");
        if (null != status && !status.isEmpty() && !status.isBlank()) {
          if ("ok".equals(status)) {
            response.end(new CustomResponse.ResponseBuilder().withStatusCode(200)
                .withMessage(handler.getString("message"))
                .withCustomMessage("" + handler.getJsonObject("metadata")).build().toJson()
                .toString());
            // now remove token from both hashMap and database
            tokens.remove(request.getHeader("token"));
            validity.remove(request.getHeader("token"));
          } else if ("error".equals(status) || "exception".equals(status)) {
            response.end(new CustomResponse.ResponseBuilder().withStatusCode(400)
                .withMessage(handler.getString("message")).build().toJson().toString());
          }
        } else {
          response.end(new CustomResponse.ResponseBuilder().withStatusCode(400).withMessage(status)
              .build().toJson().toString());
        }
      });
    } else {
      response.end(new CustomResponse.ResponseBuilder().withStatusCode(400)
          .withMessage("invalid token provided").build().toJson().toString());
    }

  }

  public void upload(RoutingContext routingContext) {
    LOGGER.debug("upload() started.");
    HttpServerResponse response = routingContext.response();
    String id = routingContext.request().getFormAttribute("id");
    response.putHeader("content-type", "application/json");
    LOGGER.debug("id :" + id);
    String fileIdComponent[] = getFileIdComponents(id);
    String uploadPath = fileIdComponent[1] + "/" + fileIdComponent[3];
    Set<FileUpload> files = routingContext.fileUploads();
    fileService.upload(files, uploadPath, handler -> {
      if (handler.succeeded()) {
        JsonObject uploadResult = handler.result();
        JsonObject responseJson = new JsonObject();
        responseJson.put("file-id", id + "/" + uploadResult.getString("file-id"));
        response.end(responseJson.toString());
      } else {
        response.end(new CustomResponse.ResponseBuilder().withStatusCode(400)
            .withMessage(handler.cause().getMessage()).build().toJson().toString());
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
    response.putHeader("content-type", "application/octet-stream");
    response.setChunked(true);
    String id = request.getParam("file-id");
    String fileIdComponent[] = getFileIdComponents(id);
    String uploadDir = fileIdComponent[1] + "/" + fileIdComponent[3];
    // extract the file-uuid from supplied id. since position of file in id will be different for
    // group level(pos:5) and resource level(pos:4)
    String fileUUID = fileIdComponent.length >= 6 ? fileIdComponent[5] : fileIdComponent[4];
    fileService.download(fileUUID, uploadDir, response, handler -> {
      if (handler.succeeded()) {
        // do nothing response is already written and file is served using content-disposition.
      } else {
        response.end(new CustomResponse.ResponseBuilder().withStatusCode(400)
            .withMessage(handler.cause().getMessage()).build().toString());
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
    /* TODO : Connect to RabbitMQ */
    /* TODO : Get the file id object. Delete the file from the server. */
    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();
    response.putHeader("content-type", "application/json");
    String fileName = request.getParam("fileId");
    fileService.delete(fileName, "", handler -> {
      if (handler.succeeded()) {
        JsonObject deleteResult = handler.result();
        response.end(new CustomResponse.ResponseBuilder().withStatusCode(200)
            .withMessage(deleteResult.getString("statusMessage"))
            .withCustomMessage("" + deleteResult.getJsonObject("metadata")).build().toJsonString());
      } else {
        response.end(new CustomResponse.ResponseBuilder().withStatusCode(400)
            .withMessage(handler.cause().getMessage()).build().toString());
      }
    });
  }

  private boolean isValidCertificate(RoutingContext routingContext) {
    System.out.println("Inside decodeCertificate");
    boolean status = false;
    try {
      Principal peerPrincipal =
          routingContext.request().connection().sslSession().getPeerPrincipal();
      String certificate_class[] = peerPrincipal.toString().split(",");
      String class_level = certificate_class[0];
      String emailInfo = certificate_class[1];
      String[] email = emailInfo.split("=")[1].split("@");
      String emailID = email[0];
      String domain = email[1];
      String cn = certificate_class[2];
      String cnValue = cn.split("=")[1];
      // Now check existence of cnValue inside instanceIDs set (allowedDomains)
      if (instanceIDs.contains(cnValue.toLowerCase())) {
        status = true;
        LOGGER.info("Valid Certificate");
      } else {
        status = false;
        LOGGER.info("Invalid Certificate");
      }
    } catch (SSLPeerUnverifiedException e) {
      status = false;
      LOGGER.info("decodeCertificate - Certificate exception message : " + e.getMessage());
    }
    return status;
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


  private String[] getFileIdComponents(String fileId) {
    return fileId.split("/");
  }

}

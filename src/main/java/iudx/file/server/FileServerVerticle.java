package iudx.file.server;

import java.io.FileInputStream;
import java.io.InputStream;
import java.security.Principal;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import javax.net.ssl.SSLPeerUnverifiedException;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.ClientAuth;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.net.JksOptions;
import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;
import iudx.file.server.utilities.CustomResponse;
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

  private static final Logger logger = LoggerFactory.getLogger(FileServerVerticle.class);
  private Vertx vertx;
  private ClusterManager mgr;
  private VertxOptions options;
  private HttpServer server;
  private Properties properties;
  private InputStream inputstream;
  private String keystore;
  private String keystorePassword;
  private Router router;
  private FileServer fileServer;
  private Map<String, String> tokens = new HashMap<String, String>();
  private Map<String, Date> validity = new HashMap<String, Date>();
  private String allowedDomain, truststore, truststorePassword;
  private String[] allowedDomains;
  private HashSet<String> instanceIDs = new HashSet<String>();

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

        router.post("/file").handler(this::uploadFile);
        router.get("/file/:fileId").handler(this::downloadFile);
        router.get("/token").handler(this::fileServerToken);

        /* Read the configuration and set the HTTPs server properties. */
        ClientAuth clientAuth = ClientAuth.REQUEST;

        try {

          inputstream = new FileInputStream(Constants.CONFIG_FILE);
          properties.load(inputstream);

          keystore = properties.getProperty(Constants.KEYSTORE_FILE_NAME);
          keystorePassword = properties.getProperty(Constants.KEYSTORE_FILE_PASSWORD);
          truststore = properties.getProperty("truststore");
          truststorePassword = properties.getProperty("truststorePassword");
          allowedDomain = properties.getProperty("domains");
          allowedDomains = allowedDomain.split(",");

          for (int i = 0; i < allowedDomains.length; i++) {
            instanceIDs.add(allowedDomains[i].toLowerCase());
          }

          logger.info("Updated domain list. Serving " + instanceIDs.size() + " tenants");
        } catch (Exception ex) {
          logger.info(ex.toString());
        }

        server =
            vertx.createHttpServer(new HttpServerOptions().setSsl(true).setClientAuth(clientAuth)
                .setKeyStoreOptions(
                    new JksOptions().setPath(keystore).setPassword(keystorePassword))
                .setTrustStoreOptions(
                    new JksOptions().setPath(truststore).setPassword(truststorePassword)));

        server.requestHandler(router).listen(Constants.PORT);

      }
    });

    logger.info("FileServerVerticle started successfully");
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
  public void fileServerToken(RoutingContext routingContext) {
    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();
    response.setChunked(true);
    if (!decodeCertificate(routingContext)) {
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
        logger.info("value stored inside tokens hashmap [ key : " + userToken + ", value : "
            + fileServerToken + " ]");
        // set validity of token for 10 hours
        Date currDate = new Date();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(currDate);
        calendar.add(Calendar.HOUR_OF_DAY, 10);
        // save validity in hashmap for later use
        validity.put(userToken, calendar.getTime());
        logger.info("value stored inside validity hashmap [ key : " + userToken + ", value : "
            + calendar.getTime() + " ]");
        // TODO: need to save above values in database too (userToken,file-server-token,validity)
        JsonObject json = new JsonObject();
        json.put("fileServerToken", fileServerToken);
        json.put("validity", calendar.getTime().toString());
        logger.info("json.toString()" + json.toString());
        /* send the token as response */
        response.write(json.toString()).end();
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

  public void uploadFile(RoutingContext routingContext) {
    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();
    response.putHeader("content-type", "application/json");

    /* Allow or deny upload of the file. If allow, Update the metadata to Resource Server */
    if (isTokenValid(request)) {
      logger.info("Token is valid. file upload starts");
      fileServer = new FileServer(vertx, Constants.DIR);
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

  /**
   * Download File service allows to download a file from the server after authenticating the user.
   *
   * @param routingContext Handles web request in Vert.x web
   */

  public void downloadFile(RoutingContext routingContext) {

    /* TODO : Initialize web client */
    /* TODO : Get user token from header. Perform TIP for Resource Server */
    /* TODO : Allow or deny download to the file */
  }


  /**
   * Download File service allows to download a file from the server after authenticating the user.
   */

  public void deleteFile() {

    /* TODO : Connect to RabbitMQ */
    /* TODO : Get the file id object. Delete the file from the server. */

  }

  private boolean decodeCertificate(RoutingContext routingContext) {
    System.out.println("Inside decodeCertificate");
    boolean status = false;
    try {
      Principal peerPrincipal =
          routingContext.request().connection().sslSession().getPeerPrincipal();
      logger.info("peerPrincipal.toString() : " + peerPrincipal.toString());
      String certificate_class[] = peerPrincipal.toString().split(",");
      String class_level = certificate_class[0];
      logger.info("class_level :" + class_level);
      String emailInfo = certificate_class[1];
      logger.info("EMAILADDRESS :" + emailInfo);
      String[] email = emailInfo.split("=")[1].split("@");
      String emailID = email[0];
      logger.info("email : " + emailID);
      String domain = email[1];
      // String emailID_SHA_1 = DigestUtils.md5Hex(email[1]);
      logger.info("domain : " + domain);
      String cn = certificate_class[2];
      String cnValue = cn.split("=")[1];
      logger.info("cnValue :" + cnValue);
      // Now check existence of cnValue inside instanceIDs set (allowedDomains)
      if (instanceIDs.contains(cnValue.toLowerCase())) {
        status = true;
        logger.info("Valid Certificate");
      } else {
        status = false;
        logger.info("Invalid Certificate");
      }

    } catch (SSLPeerUnverifiedException e) {
      status = false;
      logger.info("decodeCertificate - Certificate exception message : " + e.getMessage());
    }

    return status;
  }

}

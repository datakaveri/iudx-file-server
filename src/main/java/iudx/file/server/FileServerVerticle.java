package iudx.file.server;


import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.OpenOptions;
import io.vertx.core.http.ClientAuth;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.net.JksOptions;
import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.core.streams.ReadStream;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.Principal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashSet;
import java.util.Properties;
import java.util.UUID;
import javax.net.ssl.SSLPeerUnverifiedException;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.HttpStatus;

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
  private InputStream sqlstream;
  private Properties sqlproperties;
  private String keystore;
  private String keystorePassword;
  private Router router;
  private PgPool client;


  private String databaseIP;
  private int databasePort;
  private String databaseUser;
  private String databaseUsed;
  private String databasePassword;
  private String allowedDomain;
  private String truststore;
  private String truststorePassword;
  private String[] allowedDomains;
  private HashSet<String> instanceIDs = new HashSet<String>();
  String hashmapfileServerToken = "";

  Date validityTime1 = null;
  ClientAuth clientAuth = ClientAuth.REQUEST;

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
        sqlstream = null;
        sqlproperties = new Properties();

        router.post("/file").handler(this::isUserAuthorised);
        router.get("/file/:fileId").handler(this::isUserAuthorised);
        router.delete("/file/:fileId").handler(this::isUserAuthorised);
        router.get("/token").handler(this::fileServerToken);

        // timer fixed at a regular interval to delete expired tokens
        vertx.setPeriodic(Constants.TOKEN_DELETE_TIMER, id -> {
          deleteFileToken();
        });

        /* Read the configuration and set the HTTPs server properties. */
        ClientAuth clientAuth = ClientAuth.REQUEST;

        /* Read the configuration and set the HTTPs server properties. */

        try {

          keystore = properties.getProperty(Constants.KEYSTORE_FILE_NAME);
          keystorePassword = properties.getProperty(Constants.KEYSTORE_FILE_PASSWORD);

          inputstream = new FileInputStream("config.properties");
          properties.load(inputstream);

          /* Read the configuration and set the postgres database. */

          databaseIP = properties.getProperty("databaseIP");
          databasePort = Integer.parseInt(properties.getProperty("databasePort"));
          databaseUser = properties.getProperty("databaseUser");
          databaseUsed = properties.getProperty("databaseUsed");
          databasePassword = properties.getProperty("databasePassword");

          // load the sql prperties file
          sqlstream = new FileInputStream(Constants.SQL_FILE);
          sqlproperties.load(sqlstream);

          PgConnectOptions connectOptions = new PgConnectOptions().setPort(databasePort)
              .setHost(databaseIP).setDatabase(databaseUsed).setUser(databaseUser)
              .setPassword(databasePassword).addProperty("search_path", "public");


          // Pool options
          PoolOptions poolOptions = new PoolOptions().setMaxSize(5);

          // Create the pooled client
          client = PgPool.pool(vertx, connectOptions, poolOptions);

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
  }

  /**
   * create a random file server token and save it into the database.
   * 
   * @param routingContext Handles web request in Vert.x web
   */
  public void fileServerToken(RoutingContext routingContext) {

    /* TODO : create a temporary token, save in a hashmap */
    /* TODO : send the token as response */

    logger.info("Inside file server token");
    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();

    if (!decodeCertificate(routingContext)) {
      response.write("Invalid certificate error").end();
    } else {
      String userToken = request.getHeader("token");

      // generate file server temporary token
      String fileServerToken = UUID.randomUUID().toString();
      logger.info("fileServerToken : " + fileServerToken);

      String serverId = routingContext.request().host();
      System.out.println("serverId " + serverId);

      // set validity of token for 10 hours
      LocalDateTime currDate = LocalDateTime.now();
      LocalDateTime datetime2 = currDate.plusHours(10);

      client.preparedQuery(sqlproperties.getProperty("insertFileToken"))
          .execute(Tuple.of(userToken, fileServerToken, datetime2, serverId), ar -> {
            if (ar.succeeded()) {

              // build a json object to send in response
              JsonObject json = new JsonObject();
              json.put("fileServerToken", fileServerToken);
              json.put("validity", datetime2.toString());

              response.setChunked(true);
              response.write(json.toString()).end();

            } else {
              logger.error("Failure: " + ar.cause());
              response.setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR).end();
            }
          });
    }
  }


  /**
   * checks if the user token and file token sent by user is valid or not. If it is valid it calls
   * the respective methods.
   * 
   * @author Ankita.Mishra
   * @param routingContext Handles web request in Vert.x web
   */

  public void isUserAuthorised(RoutingContext routingContext) {

    /* TODO : check if the user is authorised */
    /* TODO : if authorised, then call the respective method */

    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();
    Date currDate = new Date();

    String userToken = request.getHeader("token");
    logger.info("userToken :" + userToken);
    String fileServerToken = request.getHeader("fileServerToken");
    logger.info("fileServerToken :" + fileServerToken);
    HttpMethod method = request.method();

    try {
      if (fileServerToken != null && userToken != null) {

        client.preparedQuery(sqlproperties.getProperty("selectfileToken"))
            .execute(Tuple.of(userToken), ar -> {
              if (ar.succeeded()) {
                RowSet<Row> rows = ar.result();

                if (rows.size() > 0) {
                  for (Row row : rows) {

                    Boolean tokenMatched = false;
                    Boolean timeValidity = false;

                    String fileServerToken1 = row.getValue("file_token").toString();

                    Date validityTime1 = Timestamp.valueOf(row.getLocalDateTime("validity_date"));

                    // check if header file token is same as database file token for the user
                    if (fileServerToken1 != null
                        && fileServerToken1.equalsIgnoreCase(fileServerToken)) {
                      tokenMatched = true;

                      // check if validity time is within 10 hours of the created time of token
                      if (validityTime1 != null && currDate.compareTo(validityTime1) <= 0) {
                        timeValidity = true;
                      }
                    }



                    if (tokenMatched && timeValidity) {

                      Future<JsonObject> result = null;
                      if (method != null && method.toString().equalsIgnoreCase("GET")) {
                        result = downloadIfValid(routingContext);
                      }
                      if (method != null && method.toString().equalsIgnoreCase("DELETE")) {
                        result = deleteIfValid(routingContext);
                      }
                      if (method != null && method.toString().equalsIgnoreCase("POST")) {
                        // result = uploadFile(routingContext);
                      }
                      result.onComplete(resultHandler -> {
                        int statusCode = resultHandler.result().getInteger("statusCode");
                        if (resultHandler.succeeded()) {
                          response.setStatusCode(statusCode).end();
                        }
                        if (resultHandler.failed()) {
                          logger.error(resultHandler.cause());
                          response.setStatusCode(statusCode).end();
                        }
                      });

                    } else {
                      response.setStatusCode(HttpStatus.SC_UNAUTHORIZED).end();
                    }

                  }
                } else {
                  logger.info("No token found for the user");
                  response.setStatusCode(HttpStatus.SC_NOT_FOUND).end();
                }
              } else {
                logger.error("Failure: " + ar.cause().getMessage());
              }
            });

      } else {
        logger.info("No token found for the user");
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

  }

  /**
   * Upload File service allows to upload a file to the server after authenticating the user. Once
   * the file is uploaded, the metadata of it is stored in the resource server using the file
   * metadata API.
   *
   * @param routingContext Handles web request in Vert.x web
   */

  public void uploadFile(RoutingContext routingContext) {

    /* TODO : Initialize web client */
    /* TODO : Get user token from header. Perform TIP for Resource Server */
    /* TODO : Allow or deny upload of the file */
    /* TODO : If allow, Update the metadata to Resource Server */

  }

  /**
   * it downloads the file if the user is authorized.
   * 
   * @author Ankita.Mishra
   * @param routingContext request
   * @return Future object containing the status code and message
   */

  private Future<JsonObject> downloadIfValid(RoutingContext routingContext) {

    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();
    JsonObject finalResponse = new JsonObject();
    response.setChunked(true);

    Promise<JsonObject> promise = Promise.promise();
    // String filePath =request.absoluteURI();

    String filePath = "D:/AnkitaMishra/IUDX/IUDXPractice/VertxExamples-master/"
        + "VertxExamples-master/UploadedFiles/USR948/1.pdf.png";

    System.out.println("filepath :" + filePath);
    vertx.fileSystem().exists(filePath, handler -> {
      if (handler.succeeded()) {
        if (handler.result()) {
          vertx.fileSystem().open(filePath, new OpenOptions().setCreate(true), readEvent -> {

            if (readEvent.failed()) {
              finalResponse.put("statusCode", HttpStatus.SC_BAD_REQUEST);
              promise.complete(finalResponse);

            }
            ReadStream<Buffer> asyncFile = readEvent.result();

            String fileName = filePath.substring(filePath.lastIndexOf("/") + 1);

            asyncFile.pipeTo(response, handler1 -> {
              asyncFile.endHandler(avoid -> {
                response.putHeader("Content-Disposition", "attachment; filename=" + fileName);
                finalResponse.put("statusCode", HttpStatus.SC_OK);
                finalResponse.put("statusMessage", "File downloaded");
                logger.error("File downloaded");
                promise.complete(finalResponse);

              });
            });


          });
        } else {
          finalResponse.put("statusCode", HttpStatus.SC_NOT_FOUND);
          finalResponse.put("statusMessage", "File does not exist");
          logger.error("File does not exist");
          promise.complete(finalResponse);

        }
      } else {
        logger.error(handler.cause());
        finalResponse.put("statusCode", HttpStatus.SC_INTERNAL_SERVER_ERROR);
        finalResponse.put("statusMessage", "");
        promise.complete(finalResponse);
      }
    });
    return promise.future();

  }


  /**
   * it deletes the file if the user is authorized.
   * 
   * @author Ankita.Mishra
   * @param routingContext Handles web request in Vert.x web
   * @return Future object containing the status code and message
   */
  private Future<JsonObject> deleteIfValid(RoutingContext routingContext) {

    JsonObject finalResponse = new JsonObject();
    HttpServerRequest request = routingContext.request();


    Promise<JsonObject> promise = Promise.promise();
    // String filePath =request.absoluteURI(); // uncomment in final code

    String filePath = "D:/AnkitaMishra/IUDX/IUDXPractice/VertxExamples-master/"
        + "VertxExamples-master/UploadedFiles/USR948/1.pdf.png"; //delete in final code
    logger.info("filePath : " + filePath);

    vertx.fileSystem().exists(filePath, handler -> {
      if (handler.succeeded()) {

        if (handler.result()) {
          vertx.fileSystem().delete(filePath, readEvent -> {
            if (readEvent.failed()) {
              finalResponse.put("statusCode", HttpStatus.SC_INTERNAL_SERVER_ERROR);
              promise.complete(finalResponse);
            } else {
              logger.info("File deleted");
              finalResponse.put("statusCode", HttpStatus.SC_OK);
              finalResponse.put("statusMessage", "File Deleted");
              promise.complete(finalResponse);
            }
          });
        } else {
          logger.error("File does not exist");
          finalResponse.put("statusCode", HttpStatus.SC_NOT_FOUND);
          finalResponse.put("statusMessage", "File does not exist");
          promise.complete(finalResponse);
        }
      } else {
        logger.error(handler.cause());
        promise.complete(finalResponse);
      }
    });
    return promise.future();
  }



  /**
   * It deletes the expired file tokens on a fixed time basis.
   * 
   * @author Ankita.Mishra
   */
  public void deleteFileToken() {

    /* TODO : delete all the expired tokens from the database */

    logger.info("Inside delete file server token");

    client.query(sqlproperties.getProperty("deleteFileToken")).execute(ar -> {
      if (ar.succeeded()) {

        logger.info("Deleted " + ar.result().rowCount() + " row(s) ");


      } else {
        logger.error("Failure : " + ar.cause());
      }
    });
  }

  private boolean decodeCertificate(RoutingContext routingContext) {
    System.out.println("Inside decodeCertificate");
    boolean status = false;
    try {
      Principal peerPrincipal =
          routingContext.request().connection().sslSession().getPeerPrincipal();
      logger.info("peerPrincipal.toString() : " + peerPrincipal.toString());
      String[] certificateClass = peerPrincipal.toString().split(",");
      String classLevel = certificateClass[0];
      logger.info("class_level :" + classLevel);
      String emailInfo = certificateClass[1];
      logger.info("EMAILADDRESS :" + emailInfo);
      String[] email = emailInfo.split("=")[1].split("@");
      String emailID = email[0];
      logger.info("email : " + emailID);
      String domain = email[1];
      DigestUtils.md5Hex(email[1]);
      logger.info("domain : " + domain);
      String cn = certificateClass[2];
      String cnValue = cn.split("=")[1];
      logger.info("cnValue :" + cnValue);
      // Now check existence of CN in instanceIDs set (allowedDomains)
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

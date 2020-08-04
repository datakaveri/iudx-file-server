package iudx.file.server;


import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.OpenOptions;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.net.JksOptions;
import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.core.streams.ReadStream;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.rabbitmq.RabbitMQClient;
import io.vertx.rabbitmq.RabbitMQOptions;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;
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
  private String keystore;
  private String keystorePassword;
  private Router router;
  private RabbitMQOptions config;
  private String dataBrokerIP;
  private int dataBrokerPort;
  private int dataBrokerManagementPort;
  private String dataBrokerVhost;
  private String dataBrokerUserName;
  private String dataBrokerPassword;
  private int connectionTimeout;
  private int requestedHeartbeat;
  private int handshakeTimeout;
  private int requestedChannelMax;
  private int networkRecoveryInterval;
  private WebClientOptions webConfig;

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
        router.get("/file").handler(this::downloadFile);
        router.delete("/file").handler(this::deleteFile);

        /* Read the configuration and set the HTTPs server properties. */
        /* TODO : Get RabbitMQ configurations */
        try {

          inputstream = new FileInputStream(Constants.CONFIG_FILE);
          properties.load(inputstream);

          keystore = properties.getProperty(Constants.KEYSTORE_FILE_NAME);
          keystorePassword = properties.getProperty(Constants.KEYSTORE_FILE_PASSWORD);

          /* Read the configuration and set the rabbitMQ server properties. */
          properties = new Properties();
          inputstream = null;

          try {

            inputstream = new FileInputStream("config.properties");
            properties.load(inputstream);

            dataBrokerIP = properties.getProperty("dataBrokerIP");
            dataBrokerPort = Integer.parseInt(properties.getProperty("dataBrokerPort"));
            dataBrokerManagementPort =
                Integer.parseInt(properties.getProperty("dataBrokerManagementPort"));
            dataBrokerVhost = properties.getProperty("dataBrokerVhost");
            dataBrokerUserName = properties.getProperty("dataBrokerUserName");
            dataBrokerPassword = properties.getProperty("dataBrokerPassword");
            connectionTimeout = Integer.parseInt(properties.getProperty("connectionTimeout"));
            requestedHeartbeat = Integer.parseInt(properties.getProperty("requestedHeartbeat"));
            handshakeTimeout = Integer.parseInt(properties.getProperty("handshakeTimeout"));
            requestedChannelMax = Integer.parseInt(properties.getProperty("requestedChannelMax"));
            networkRecoveryInterval =
                Integer.parseInt(properties.getProperty("networkRecoveryInterval"));

          } catch (Exception ex) {

            logger.info(ex.toString());

          }

          /* Configure the RabbitMQ Data Broker client with input from config files. */

          config = new RabbitMQOptions();
          config.setUser(dataBrokerUserName);
          config.setPassword(dataBrokerPassword);
          config.setHost(dataBrokerIP);
          config.setPort(dataBrokerPort);
          config.setVirtualHost(dataBrokerVhost);
          config.setConnectionTimeout(connectionTimeout);
          config.setRequestedHeartbeat(requestedHeartbeat);
          config.setHandshakeTimeout(handshakeTimeout);
          config.setRequestedChannelMax(requestedChannelMax);
          config.setNetworkRecoveryInterval(networkRecoveryInterval);
          config.setAutomaticRecoveryEnabled(true);

          /* Configure the WebClient with input from config files. */
          webConfig = new WebClientOptions();
          webConfig.setKeepAlive(true);
          webConfig.setConnectTimeout(86400000);
          webConfig.setDefaultHost(dataBrokerIP);
          webConfig.setDefaultPort(dataBrokerManagementPort);
          webConfig.setKeepAliveTimeout(86400000);

          RabbitMQClient.create(vertx, config);

          WebClient.create(vertx, webConfig);

        } catch (Exception ex) {
          logger.info(ex.toString());
        }

        server = vertx.createHttpServer(new HttpServerOptions().setSsl(true)
            .setKeyStoreOptions(new JksOptions().setPath(keystore).setPassword(keystorePassword)));

        server.requestHandler(router).listen(8443);



      }
    });
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
   * Download File service allows to download a file from the server after authenticating the user.
   *
   * @param routingContext Handles web request in Vert.x web
   */

  public void downloadFile(RoutingContext routingContext) {
    System.out.println("inside download");
    /* TODO : Initialize web client */
    /* TODO : Get user token from header. Perform TIP for Resource Server */
    /* TODO : Allow or deny download to the file */

    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();

    request.getHeader("token");
    response.setChunked(true);
    String filePath = routingContext.request().absoluteURI();
    // String filePath = request.getParam("fileId");
    /*
     * String filePath = "D:/AnkitaMishra/IUDX/IUDXPractice/VertxExamples-master/" +
     * "VertxExamples-master/UploadedFiles/USR377/1GB2.zip";
     */
    System.out.println("filepath :" + filePath);
    vertx.fileSystem().exists(filePath, handler -> {
      if (handler.succeeded()) {
        if (handler.result()) {
          vertx.fileSystem().open(filePath, new OpenOptions().setCreate(true), readEvent -> {

            if (readEvent.failed()) {

              response.setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR).end();
              return;
            }
            ReadStream<Buffer> asyncFile = readEvent.result();

            String fileName = filePath.substring(filePath.lastIndexOf("\\") + 1);
            System.out.println(fileName);
            response.putHeader("Content-Disposition", "attachment; filename=" + fileName);

            asyncFile.pipeTo(response, handler1 -> {
              asyncFile.endHandler(avoid -> {
                response.setStatusCode(HttpStatus.SC_OK).end();
              });
            });

          });
        } else {
          logger.error("File does not exist");
          response.setStatusCode(HttpStatus.SC_NOT_FOUND).write("File does not exist").end();
        }
      } else {
        logger.error(handler.cause());
        response.setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR)
            .write(handler.cause().toString()).end();
      }
    });

  }

  /**
   * Delete File service allows to delete a file from the RabbitMq queue to which the file server
   * subscribes. Resource Server publishes the file Id to the queue after authenticating the user.
   */

  public void deleteFile(RoutingContext routingContext) {

    /* TODO : Connect to RabbitMQ */
    /* TODO : Get the file id object. Delete the file from the server. */

    logger.info("Inside delete");

    /*
     * String filePath = routingContext.request().getParam("link"); System.out.println("filepath :"
     * + filePath);
     */

    String fileId = "D:/AnkitaMishra/IUDX/IUDXPractice/VertxExamples-master/VertxExamples-master/"
        + "UploadedFiles/23b801de-43e7-4cb7-a3d6-bf13e4c27879.png";

    System.out.println("fileId : " + fileId);
    vertx.fileSystem().exists(fileId, handler -> {
      if (handler.succeeded()) {
        System.out.println("handler.result()" + handler.result());
        if (handler.result()) {
          vertx.fileSystem().delete(fileId, readEvent -> {
            if (readEvent.failed()) {
              routingContext.response().setStatusCode(500).end();
              return;
            } else {
              logger.info("File deleted");
              routingContext.response().setStatusCode(HttpStatus.SC_OK)
                  .setStatusMessage("File deleted").end();

            }
          });
        } else {
          logger.error("File does not exist");
          routingContext.response().setStatusCode(HttpStatus.SC_NOT_FOUND)
              .setStatusMessage("File does not exist").end();
        }
      } else {
        logger.error(handler.cause());
      }
    });


  }


}

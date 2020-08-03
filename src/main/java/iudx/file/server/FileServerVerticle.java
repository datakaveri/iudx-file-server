package iudx.file.server;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.net.JksOptions;
import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;
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

        /* Read the configuration and set the HTTPs server properties. */
        /* TODO : Get RabbitMQ configurations */
        try {

          inputstream = new FileInputStream(Constants.CONFIG_FILE);
          properties.load(inputstream);

          keystore = properties.getProperty(Constants.KEYSTORE_FILE_NAME);
          keystorePassword = properties.getProperty(Constants.KEYSTORE_FILE_PASSWORD);

        } catch (Exception ex) {
          logger.info(ex.toString());
        }

        server = vertx.createHttpServer(new HttpServerOptions().setSsl(true)
            .setKeyStoreOptions(new JksOptions().setPath(keystore).setPassword(keystorePassword)));

        server.requestHandler(router).listen(Constants.PORT);

      }
    });
    logger.info("FileServerVerticle started successfully");
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

		HttpServerRequest request = routingContext.request();
		HttpServerResponse response = routingContext.response();
		response.putHeader("content-type", "application/json");
		fileServer = new FileServer(vertx, Constants.DIR);

		//upload file logic
		fileServer.writeUploadFile(request, Constants.MAX_SIZE, handler->{
			String status = handler.getString("status");
			logger.info("FileServerVerticle received uploadFile status : " + status );
			if( null != status && !status.isEmpty() && !status.isBlank() ) {
				if( "ok".equals(status) ) {
					response.end(new CustomResponse.ResponseBuilder().withStatusCode(200).withMessage(handler.getString("message"))
							.withCustomMessage(""+handler.getJsonObject("metadata")).build().toJson().toString());
				} else if("error".equals(status) || "exception".equals(status)){
					response.end(new CustomResponse.ResponseBuilder().withStatusCode(400).withMessage(handler.getString("message")).build().toJson().toString()); 
				  }
			}
			else {
				response.end(new CustomResponse.ResponseBuilder().withStatusCode(400).withMessage(status).build().toJson().toString()); 
			}
		});

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

}

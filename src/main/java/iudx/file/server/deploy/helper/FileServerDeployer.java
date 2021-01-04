package iudx.file.server.deploy.helper;

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;
import iudx.file.server.FileServerVerticle;


public class FileServerDeployer {

  private static final Logger logger = LogManager.getLogger(FileServerDeployer.class);
  private static Vertx vertx;
  private static ClusterManager mgr;
  private static VertxOptions options;

  /**
   * The main method implements the deploy helper script for deploying the File Server verticle
   * in the file server.
   * 
   * @param args which is a String array
   */

  public static void main(String[] args) {

    /* Create a reference to HazelcastClusterManager. */

    mgr = new HazelcastClusterManager();
    options = new VertxOptions().setClusterManager(mgr);

    /* Create or Join a Vert.x Cluster. */

    Vertx.clusteredVertx(options, res -> {
      if (res.succeeded()) {
        vertx = res.result();

        /* Deploy the File Server Verticle. */

        vertx.deployVerticle(new FileServerVerticle(), fileVerticle -> {
          if (fileVerticle.succeeded()) {
            logger.info("The FileServerVerticle is ready");
          } else {
            logger.info("The FileServerVerticle failed !");
            logger.info(fileVerticle.cause());
            fileVerticle.cause().printStackTrace();
          }
        });
      } else {
        logger.info("The Vertx Cluster failed !");
      }
    });

  }

}

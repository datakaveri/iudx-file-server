package iudx.file.server.authenticator.utilities;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.JksOptions;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import iudx.file.server.apiserver.service.ServerType;

public class WebClientFactory {

  private static final Logger LOGGER = LogManager.getLogger(WebClientFactory.class);
  
  private final Vertx vertx;
  private final JsonObject config;
  
  
  public WebClientFactory(final Vertx vertx,final JsonObject config) {
    this.vertx=vertx;
    this.config=config;
  }

  public WebClient getWebClientFor(final ServerType serverType) {
    if (serverType.equals(ServerType.FILE_SERVER)) {
      return getFileServerWebClient(vertx, config);
    } else if (serverType.equals(ServerType.RESOURCE_SERVER)) {
      return getRSServerWebClient(vertx, config);
    } else {
      LOGGER.error("Unknown type passed." + serverType);
      return null;
    }
  }


  private WebClient getFileServerWebClient(final Vertx vertx,final JsonObject config) {
    WebClientOptions options =
        new WebClientOptions().setTrustAll(true).setVerifyHost(false).setSsl(true)
            .setKeyStoreOptions(
                new JksOptions()
                    .setPath(config.getString("file-keystore"))
                    .setPassword(config.getString("file-keystorePassword")));
    return WebClient.create(vertx, options);
  }


  private WebClient getRSServerWebClient(final Vertx vertx,final JsonObject config) {
    WebClientOptions options =
        new WebClientOptions().setTrustAll(true).setVerifyHost(false).setSsl(true)
            .setKeyStoreOptions(
                new JksOptions()
                    .setPath(config.getString("rs-keystore"))
                    .setPassword(config.getString("rs-keystorePassword")));
    return WebClient.create(vertx, options);
  }

}

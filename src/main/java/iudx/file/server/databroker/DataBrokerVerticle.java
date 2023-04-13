package iudx.file.server.databroker;

import static iudx.file.server.common.Constants.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.rabbitmq.QueueOptions;
import io.vertx.rabbitmq.RabbitMQClient;
import io.vertx.rabbitmq.RabbitMQConsumer;
import io.vertx.rabbitmq.RabbitMQOptions;
import io.vertx.serviceproxy.ServiceBinder;
import iudx.file.server.cache.CacheService;
import iudx.file.server.cache.cacheImpl.CacheType;
import iudx.file.server.common.VHosts;

public class DataBrokerVerticle extends AbstractVerticle {

  private static final Logger LOGGER = LogManager.getLogger(DataBrokerVerticle.class);

  private RabbitMQOptions config;
  private RabbitMQClient client;
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

  private ServiceBinder binder;
  private MessageConsumer<JsonObject> consumer;
  private CacheService cacheService;

  @Override
  public void start() throws Exception {

    dataBrokerIP = config().getString("dataBrokerIP");
    dataBrokerPort = config().getInteger("dataBrokerPort");
    dataBrokerManagementPort = config().getInteger("dataBrokerManagementPort");
    /*
     * since file server only use internal vhost currently, so using internal vhost directly for
     * client
     */
    dataBrokerVhost = config().getString(VHosts.IUDX_INTERNAL.value);
    dataBrokerUserName = config().getString("dataBrokerUserName");
    dataBrokerPassword = config().getString("dataBrokerPassword");
    connectionTimeout = config().getInteger("connectionTimeout");
    requestedHeartbeat = config().getInteger("requestedHeartbeat");
    handshakeTimeout = config().getInteger("handshakeTimeout");
    requestedChannelMax = config().getInteger("requestedChannelMax");
    networkRecoveryInterval = config().getInteger("networkRecoveryInterval");

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

    webConfig = new WebClientOptions();
    webConfig.setKeepAlive(true);
    webConfig.setConnectTimeout(86400000);
    webConfig.setDefaultHost(dataBrokerIP);
    webConfig.setDefaultPort(dataBrokerManagementPort);
    webConfig.setKeepAliveTimeout(86400000);

    client = RabbitMQClient.create(vertx, config);

    client.start(resultHandler -> {
      if (resultHandler.succeeded()) {
        LOGGER.info("Rabbit mq client started successfully.");

        binder = new ServiceBinder(vertx);
        cacheService = CacheService.createProxy(vertx, CACHE_SERVICE_ADDRESS);

        consumer = binder
            .setAddress(DATABROKER_SERVICE_ADDRESS)
            .register(DataBrokerService.class, new DataBrokerServiceImpl(client));

        startRevokedClientListener(cacheService);
      } else {
        LOGGER.info("Rabbit mq client startup failed");
        LOGGER.error(resultHandler.cause());
      }
    });
    LOGGER.info("Databroker verticle started.");
  }

  private final QueueOptions options =
      new QueueOptions()
          .setMaxInternalQueueSize(1000)
          .setKeepMostRecent(true);

  private void startRevokedClientListener(CacheService cacheService) {
    client.basicConsumer(INVALID_SUB_Q, options, revokedTokenReceivedHandler -> {
      if (revokedTokenReceivedHandler.succeeded()) {
        RabbitMQConsumer mqConsumer = revokedTokenReceivedHandler.result();
        mqConsumer.handler(message -> {
          Buffer body = message.body();
          if (body != null) {
            JsonObject invalidClientJson = new JsonObject(body);
            String key = invalidClientJson.getString("sub");
            String value = invalidClientJson.getString("expiry");
            LOGGER.debug("message received from RMQ : " + invalidClientJson);
            JsonObject cacheJson = new JsonObject();
            cacheJson.put("type", CacheType.REVOKED_CLIENT);
            cacheJson.put("key", key);
            cacheJson.put("value", value);

            cacheService.refresh(cacheJson, cacheHandler -> {
              if (cacheHandler.succeeded()) {
                LOGGER.info("revoked ['client : " + key + "'] value published to Cache Verticle");
              } else {
                LOGGER.info("revoked client : " + key + " published to Cache Verticle fail");
              }
            });
          } else {
            LOGGER.error("Empty json received from revoke_token queue");
          }
        });
      }
    });
  }
  @Override
  public void stop() {
    binder.unregister(consumer);
  }

}

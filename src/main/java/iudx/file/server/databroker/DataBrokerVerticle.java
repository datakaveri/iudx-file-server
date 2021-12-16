package iudx.file.server.databroker;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.rabbitmq.RabbitMQClient;
import io.vertx.rabbitmq.RabbitMQOptions;
import io.vertx.serviceproxy.ServiceBinder;
import io.vertx.sqlclient.PoolOptions;

/**
 * The Data Broker Verticle.
 * <h1>Data Broker Verticle</h1>
 * <p>
 *   The Data Broker Verticle implementation in the IUDX File Server exposes the
 *   {@link iudx.file.server.databroker.DataBrokerService} over the Vert.x Event Bus.
 * </p>
 *
 * @version 1.0
 * @since 2021-12-14
 */

public class DataBrokerVerticle extends AbstractVerticle {

  private static final String BROKER_SERVICE_ADDRESS = "iudx.file.broker.service";
  private static final Logger LOGGER = LogManager.getLogger(DataBrokerVerticle.class);
  private DataBrokerService dataBroker;
  private PostgresClient pgClient;
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

  /* Database Properties */
  private String databaseIP;
  private int databasePort;
  private String databaseName;
  private String databaseUserName;
  private String databasePassword;
  private int poolSize;
  private PgPool pgPool;
  private PoolOptions poolOptions;
  private PgConnectOptions pgConnectOptions;

  @Override
  public void start() throws Exception {

    /* Read the configuration and set the rabbitMQ server properties. */
    dataBrokerIP = config().getString("dataBrokerIP");
    dataBrokerPort = config().getInteger("dataBrokerPort");
    dataBrokerManagementPort =
            config().getInteger("dataBrokerManagementPort");
    dataBrokerVhost = config().getString("dataBrokerVhost");
    dataBrokerUserName = config().getString("dataBrokerUserName");
    dataBrokerPassword = config().getString("dataBrokerPassword");
    connectionTimeout = config().getInteger("connectionTimeout");
    requestedHeartbeat = config().getInteger("requestedHeartbeat");
    handshakeTimeout = config().getInteger("handshakeTimeout");
    requestedChannelMax = config().getInteger("requestedChannelMax");
    networkRecoveryInterval =
            config().getInteger("networkRecoveryInterval");

    databaseIP = config().getString("callbackDatabaseIP");
    databasePort = config().getInteger("callbackDatabasePort");
    databaseName = config().getString("callbackDatabaseName");
    databaseUserName = config().getString("callbackDatabaseUserName");
    databasePassword = config().getString("callbackDatabasePassword");
    poolSize = config().getInteger("callbackpoolSize");

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

    if (pgConnectOptions == null) {
      pgConnectOptions = new PgConnectOptions().setPort(databasePort).setHost(databaseIP)
              .setDatabase(databaseName).setUser(databaseUserName).setPassword(databasePassword);
    }

    /* Pool options */
    if (poolOptions == null) {
      poolOptions = new PoolOptions().setMaxSize(poolSize);
    }

    /* Create the client pool */
    pgPool = PgPool.pool(vertx, pgConnectOptions, poolOptions);

    /* Create a RabbitMQ Client with the configuration and vertx cluster instance. */
    client = RabbitMQClient.create(vertx, config);

    /* Call the databroker constructor with the RabbitMQ client. */
    client.start(resultHandler -> {
      if(resultHandler.succeeded()) {
        LOGGER.info("RMQ client started successfully");

        pgClient = new PostgresClient(pgPool);
        dataBroker = new DataBrokerServiceImpl(client, pgClient);

        binder = new ServiceBinder(vertx);

        /* Publish the Data Broker service with the Event Bus against an address. */
        consumer = binder
                .setAddress(BROKER_SERVICE_ADDRESS)
                .register(DataBrokerService.class, dataBroker);
      } else {
        LOGGER.info("RMQ client startup failed");
        LOGGER.error(resultHandler.cause());
      }
    });
  }


  @Override
  public void stop() {
    binder.unregister(consumer);
  }
}

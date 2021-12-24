package iudx.file.server.databroker;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.vertx.pgclient.PgConnectOptions;
import io.vertx.sqlclient.PoolOptions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.rabbitmq.RabbitMQClient;
import io.vertx.rabbitmq.RabbitMQOptions;
import iudx.file.server.configuration.Configuration;
import io.vertx.pgclient.PgPool;

import java.util.HashMap;

@ExtendWith(VertxExtension.class)
public class DataBrokerServiceTest {

  static DataBrokerServiceImpl databroker;
  static PostgresClient pgClient;
  static private String dataBrokerIP;
  static private int dataBrokerPort;
  static private int dataBrokerManagementPort;
  static private String dataBrokerVhost;
  static private String dataBrokerUserName;
  static private String dataBrokerPassword;
  static private int connectionTimeout;
  static private int requestedHeartbeat;
  static private int handshakeTimeout;
  static private int requestedChannelMax;
  static private int networkRecoveryInterval;
  private static RabbitMQOptions config;
  private static RabbitMQClient client;
  private static Configuration appConfig;
  private static WebClientOptions webConfig;
  private static String databaseIP;
  private static Integer databasePort;
  private static String databaseName;
  private static String databaseUserName;
  private static String databasePassword;
  private static Integer databasePoolSize;
  private static PgPool pgPool;
  private static PgConnectOptions pgConnectOptions;
  private static PoolOptions poolOptions;

  private static final Logger logger = LogManager.getLogger(DataBrokerServiceTest.class);

  @BeforeAll
  @DisplayName("Deploy verticle")
  static void startVertx(Vertx vertx, VertxTestContext testContext) {
    appConfig = new Configuration();
    JsonObject brokerConfig =  appConfig.configLoader(1, vertx);
    dataBrokerIP = brokerConfig.getString("dataBrokerIP");
    dataBrokerPort = brokerConfig.getInteger("dataBrokerPort");
    dataBrokerManagementPort =
            brokerConfig.getInteger("dataBrokerManagementPort");
    dataBrokerVhost = brokerConfig.getString("dataBrokerVhost");
    dataBrokerUserName = brokerConfig.getString("dataBrokerUserName");
    dataBrokerPassword = brokerConfig.getString("dataBrokerPassword");
    connectionTimeout = brokerConfig.getInteger("connectionTimeout");
    requestedHeartbeat = brokerConfig.getInteger("requestedHeartbeat");
    handshakeTimeout = brokerConfig.getInteger("handshakeTimeout");
    requestedChannelMax = brokerConfig.getInteger("requestedChannelMax");
    networkRecoveryInterval = brokerConfig.getInteger("networkRecoveryInterval");

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

    databaseIP = brokerConfig.getString("callbackDatabaseIP");
    databasePort = brokerConfig.getInteger("callbackDatabasePort");
    databaseName = brokerConfig.getString("callbackDatabaseName");
    databaseUserName = brokerConfig.getString("callbackDatabaseUserName");
    databasePassword = brokerConfig.getString("callbackDatabasePassword");
    databasePoolSize = brokerConfig.getInteger("callbackpoolSize");

    pgConnectOptions = new PgConnectOptions().setPort(databasePort).setHost(databaseIP)
            .setDatabase(databaseName).setUser(databaseUserName).setPassword(databasePassword);
    poolOptions = new PoolOptions().setMaxSize(databasePoolSize);

    client = RabbitMQClient.create(vertx, config);
    pgPool = PgPool.pool(vertx, pgConnectOptions, poolOptions);
    pgClient = new PostgresClient(pgPool);

    client.start(startHandler -> {
      if(startHandler.succeeded()) {
        databroker = new DataBrokerServiceImpl(client, pgClient, vertx);
        testContext.completeNow();
      } else {
        logger.error("startup failed");
        testContext.failNow("fail");
      }
    });
  }

  @Test
  @DisplayName("Get Message from Queue")
  void successfulGetMessage(VertxTestContext testContext) {

    databroker.consumeMessageFromQueue(handler -> {
      if(handler.succeeded()) {
        testContext.completeNow();
      } else {
        testContext.failNow("fail");
      }
    });
  }

  @Test
  @DisplayName("Get invalidation value from database")
  void successfulGetFromDB(VertxTestContext testContext) {
    String expected = "2021-12-22T09:18";
    String _id = "844e251b-574b-46e6-9247-f76f1f70a637";

    databroker.getInvalidationDataFromDB(resultHandler -> {
      if(resultHandler.succeeded()) {
        String actual = resultHandler.result().getString(_id);
        assertEquals(expected,actual);
        testContext.completeNow();
      } else {
        testContext.failNow("fail");
      }
    });


  }
}

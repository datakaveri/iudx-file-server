package iudx.file.server.databroker;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.rabbitmq.RabbitMQClient;
import io.vertx.rabbitmq.RabbitMQOptions;
import iudx.file.server.configuration.Configuration;

@ExtendWith(VertxExtension.class)
public class DataBrokerServiceTest {

  static DataBrokerService databroker;
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


  private static final Logger logger = LogManager.getLogger(DataBrokerServiceTest.class);

  @BeforeAll
  @DisplayName("Deploy verticle")
  static void startVertx(Vertx vertx, VertxTestContext testContext) {
    appConfig = new Configuration();
    JsonObject brokerConfig =  appConfig.configLoader(4, vertx);
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

    client = RabbitMQClient.create(vertx, config);

    client.start(startHandler -> {
      if(startHandler.succeeded()) {
        databroker = new DataBrokerServiceImpl(client);
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
    JsonObject expected = new JsonObject()
            .put("sub","123e4567-e89b-12d3-a456-426614174000")
                    .put("time","timestamp");
    databroker.getMessage("file-server-token-invalidation", handler -> {
      if(handler.succeeded()) {
        JsonObject response = handler.result();
        logger.debug("Message from queue is: " + response);
        assertEquals(expected, response);
        testContext.completeNow();
      } else {
        testContext.failNow("fail");
      }
    });
  }
}

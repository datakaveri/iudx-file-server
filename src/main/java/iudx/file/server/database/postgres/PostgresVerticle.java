package iudx.file.server.database.postgres;

import static iudx.file.server.common.Constants.PG_SERVICE_ADDRESS;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.serviceproxy.ServiceBinder;
import io.vertx.sqlclient.PoolOptions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PostgresVerticle extends AbstractVerticle {

  private static final Logger LOGGER = LogManager.getLogger(PostgresVerticle.class);

  private ServiceBinder binder;

  private PgConnectOptions connectOptions;
  private PoolOptions poolOptions;
  private PgPool pool;

  private String databaseIp;
  private int databasePort;
  private String databaseName;
  private String databaseUserName;
  private String databasePassword;
  private int poolSize;
  private MessageConsumer<JsonObject> consumer;

  private PostgresService pgService;

  @Override
  public void start() throws Exception {

    databaseIp = config().getString("databaseIp");
    databasePort = config().getInteger("databasePort");
    databaseName = config().getString("databaseName");
    databaseUserName = config().getString("databaseUserName");
    databasePassword = config().getString("databasePassword");
    poolSize = config().getInteger("poolSize");

    this.connectOptions =
        new PgConnectOptions()
            .setPort(databasePort)
            .setHost(databaseIp)
            .setDatabase(databaseName)
            .setUser(databaseUserName)
            .setPassword(databasePassword)
            .setReconnectAttempts(2)
            .setReconnectInterval(1000);

    this.poolOptions = new PoolOptions().setMaxSize(poolSize);
    this.pool = PgPool.pool(vertx, connectOptions, poolOptions);

    pgService = new PostgresServiceImpl(this.pool);

    binder = new ServiceBinder(vertx);
    consumer = binder.setAddress(PG_SERVICE_ADDRESS).register(PostgresService.class, pgService);

    LOGGER.info("Postgres Verticle deployed.");
  }

  @Override
  public void stop() {
    binder.unregister(consumer);
  }
}

package iudx.file.server.database.elasticdb;

import static iudx.file.server.database.elasticdb.utilities.Constants.*;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.serviceproxy.ServiceBinder;

public class DatabaseVerticle extends AbstractVerticle {

  private DatabaseService database;
  private static final String dbAddress = DB_SERVICE_ADDRESS;
  private ServiceBinder binder;
  private MessageConsumer<JsonObject> consumer;

  @Override
  public void start() {

    database = new DatabaseServiceImpl(config());

    binder = new ServiceBinder(vertx);

    consumer =
        binder.setAddress(dbAddress)
            .register(DatabaseService.class, database);

  }

  @Override
  public void stop() {
    binder.unregister(consumer);
  }
}

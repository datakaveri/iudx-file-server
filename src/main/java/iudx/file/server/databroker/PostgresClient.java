package iudx.file.server.databroker;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.SqlConnection;

public class PostgresClient {
  private static final Logger LOGGER = LogManager.getLogger(PostgresClient.class);

  private PgPool pgPool;

  public PostgresClient(PgPool pgPool) {
    this.pgPool = pgPool;
  }

  public Future<RowSet<Row>> getAsync(String preparedQuery) {

    Promise<RowSet<Row>> promise = Promise.promise();

    pgPool.getConnection(connectionHandler -> {
      if(connectionHandler.succeeded()) {
        SqlConnection pgConnection = connectionHandler.result();
        pgConnection.query(preparedQuery).execute(handler -> {
          if(handler.succeeded()) {
            pgConnection.close();
            promise.complete(handler.result());
          } else {
            pgConnection.close();
            LOGGER.fatal("Fail: "+ handler.cause());
            promise.fail(handler.cause());
          }
        });
      }
    });

    return promise.future();
  }
}

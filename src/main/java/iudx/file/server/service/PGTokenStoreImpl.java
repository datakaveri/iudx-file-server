package iudx.file.server.service;

import static iudx.file.server.utilities.Constants.*;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import iudx.file.server.utilities.Constants;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PGTokenStoreImpl implements TokenStore {

  private static final Logger LOGGER = LogManager.getLogger(PGTokenStoreImpl.class);

  private PgPool client;

  public PGTokenStoreImpl(Vertx vertx, JsonObject configs) {

    String databaseIP = configs.getString("databaseIP");
    Integer databasePort = configs.getInteger("databasePort");
    String databaseUser = configs.getString("databaseUser");
    String databaseUsed = configs.getString("databaseUsed");
    String databasePassword = configs.getString("databasePassword");

    PgConnectOptions connectOptions = new PgConnectOptions()
        .setPort(databasePort)
        .setHost(databaseIP)
        .setDatabase(databaseUsed)
        .setUser(databaseUser)
        .setPassword(databasePassword)
        .addProperty("search_path", "public");
    // Pool options
    PoolOptions poolOptions = new PoolOptions().setMaxSize(5);
    // Create the pooled client
    this.client = PgPool.pool(vertx, connectOptions, poolOptions);

  }

  @Override
  public Future<Map<String, String>> getTokenDetails(String token) {
    Promise<Map<String, String>> promise = Promise.promise();
    Map<String, String> result = new HashMap<>();
    client.preparedQuery(SQL_SELECT).execute(Tuple.of(token), ar -> {
      if (ar.succeeded()) {
        RowSet<Row> rows = ar.result();
        LOGGER.info("size :" + rows.size());
        if (rows.size() > 0) {
          for (Row row : rows) {
            result.put("file_token", row.getValue("file_token").toString());
            result.put("validity_date", row.getValue("validity_date").toString());
          }
          promise.complete(result);
        } else {
          LOGGER.info("no token in DB.");
          promise.fail("no token exist.");
        }
      } else {
        promise.fail("no token exist.");
      }
    });
    return promise.future();
  }

  @Override
  public Future<JsonObject> put(String authToken, String fileToken, String serverId) {
    Promise<JsonObject> promise = Promise.promise();
    LocalDateTime now = LocalDateTime.now();
    JsonObject response = new JsonObject();
    LocalDateTime tokenValidity = now.plusHours(10);
    client.preparedQuery(SQL_INSERT)
        .execute(Tuple.of(authToken, fileToken, tokenValidity, serverId), ar -> {
          LOGGER.info("ar.result" + ar.result());
          LOGGER.info("ar.cause" + ar.cause());
          if (ar.succeeded()) {
            LOGGER.info("inside success");
            response.put("fileServerToken", fileToken).put("validity", tokenValidity.toString());
            LOGGER.info("token info : " + response);
            promise.complete(response);
          } else {
            LOGGER.error(ar.cause());
            promise.fail("DB Error : saving token failed.");
          }
        });
    return promise.future();
  }

  @Override
  public Future<Boolean> delete(String token) {
    Promise<Boolean> promise = Promise.promise();
    client.preparedQuery(SQL_DELETE).execute(Tuple.of(token), ar -> {
      if (ar.succeeded()) {
        LOGGER.info("file server token deleted.");
        promise.complete(true);
      } else {
        LOGGER.info("file server token deletion failed.");
        promise.fail(ar.cause());
      }
    });
    return promise.future();
  }

}

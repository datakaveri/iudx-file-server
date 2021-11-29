package iudx.file.server.auditing;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import iudx.file.server.auditing.util.QueryBuilder;
import iudx.file.server.auditing.util.ResponseBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static iudx.file.server.auditing.util.Constants.*;


public class AuditingServiceImpl implements AuditingService{
 
  private static final Logger LOGGER = LogManager.getLogger(AuditingServiceImpl.class);
  PgConnectOptions connectOptions;
  PoolOptions poolOptions;
  PgPool pool;
  private QueryBuilder queryBuilder =  new QueryBuilder();
  private JsonObject query = new JsonObject();
  private String databaseIP;
  private int databasePort;
  private String databaseName;
  private String databaseUserName;
  private String databasePassword;
  private int databasePoolSize;
  private ResponseBuilder responseBuilder;

  public AuditingServiceImpl(JsonObject propObj, Vertx vertxInstance) {
    if(propObj != null && !propObj.isEmpty()) {
      databaseIP = propObj.getString("auditingDatabaseIP");
      databasePort = propObj.getInteger("auditingDatabasePort");
      databaseName = propObj.getString("auditingDatabaseName");
      databaseUserName = propObj.getString("auditingDatabaseUserName");
      databasePassword = propObj.getString("auditingDatabasePassword");
      databasePoolSize = propObj.getInteger("auditingPoolSize");
    }

    this.connectOptions =
            new PgConnectOptions()
                    .setPort(databasePort)
                    .setHost(databaseIP)
                    .setDatabase(databaseName)
                    .setUser(databaseUserName)
                    .setPassword(databasePassword);

    this.poolOptions = new PoolOptions().setMaxSize(databasePoolSize);
    this.pool = PgPool.pool(vertxInstance, connectOptions, poolOptions);

    LOGGER.info("IP: " + databaseIP);
    LOGGER.info("Port: " + databasePort);
    LOGGER.info("database: " + databaseName);
    LOGGER.info("user: " + databaseUserName);
    LOGGER.info("pass: " + databasePassword);
  }

  @Override
  public AuditingService executeWriteQuery(
          JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    query = queryBuilder.buildWriteQuery(request);

    if (query.containsKey(ERROR)) {
      LOGGER.error("Fail: Query returned with an error: " + query.getString(ERROR));
      responseBuilder =
              new ResponseBuilder(FAILED).setTypeAndTitle(400).setMessage(query.getString(ERROR));
      handler.handle(Future.failedFuture(responseBuilder.getResponse().toString()));
      return null;
    }

    Future<JsonObject> result = writeInDatabase(query);
    result.onComplete(
            resultHandler -> {
              if(resultHandler.succeeded()) {
                handler.handle(Future.succeededFuture(resultHandler.result()));
              } else if (resultHandler.failed()) {
                LOGGER.error("failed ::" + resultHandler.cause());
                handler.handle(Future.failedFuture((resultHandler.cause().getMessage())));
              }
            });
    return this;
  }

  @Override
  public AuditingService executeReadQuery(
          JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    LOGGER.debug("Info: Read Query" + request.toString());

    if (!request.containsKey(USER_ID)) {
      LOGGER.error("Fail: " + USERID_NOT_FOUND);
      responseBuilder =
              new ResponseBuilder(FAILED).setTypeAndTitle(400).setMessage(USERID_NOT_FOUND);
      handler.handle(Future.failedFuture(responseBuilder.getResponse().toString()));
      return null;
    }

    query = queryBuilder.buildReadQuery(request);
    if (query.containsKey(ERROR)) {
      LOGGER.error("Fail: Query returned with an error: " + query.getString(ERROR));
      responseBuilder =
              new ResponseBuilder(FAILED).setTypeAndTitle(400).setMessage(query.getString(ERROR));
      handler.handle(Future.failedFuture(responseBuilder.getResponse().toString()));
      return null;
    }
    LOGGER.debug("Info: Query constructed: " + query.getString(QUERY_KEY));

    Future<JsonObject> result = executeReadQuery(query);
    result.onComplete(
            resultHandler -> {
              if (resultHandler.succeeded()) {
                if (resultHandler.result().getString(TITLE).equals(FAILED)) {
                  LOGGER.error("Read from DB failed:" + resultHandler.result());
                  handler.handle(Future.failedFuture(resultHandler.result().toString()));
                } else {
                  LOGGER.info("Read from DB succeeded.");
                  handler.handle(Future.succeededFuture(resultHandler.result()));
                }
              } else if (resultHandler.failed()) {
                LOGGER.error("Read from DB failed:" + resultHandler.cause());
                handler.handle(Future.failedFuture(resultHandler.cause().getMessage()));
              }
            });
    return this;
  }

  private Future<JsonObject> executeReadQuery(JsonObject query) {
    Promise<JsonObject> promise = Promise.promise();
    JsonArray jsonArray = new JsonArray();
    pool.getConnection()
            .compose(connection -> connection.query(query.getString(QUERY_KEY)).execute())
            .onComplete(
                    rows -> {
                      RowSet<Row> result = rows.result();
                      if (result == null) {
                        responseBuilder =
                                new ResponseBuilder(FAILED).setTypeAndTitle(204).setMessage(EMPTY_RESPONSE);
                      } else {
                        for (Row rs : result) {
                          jsonArray.add(getJsonObject(rs));
                        }
                        if (jsonArray.isEmpty()) {
                          responseBuilder =
                                  new ResponseBuilder(FAILED).setTypeAndTitle(204).setMessage(EMPTY_RESPONSE);
                        } else {
                          responseBuilder =
                                  new ResponseBuilder(SUCCESS).setTypeAndTitle(200).setJsonArray(jsonArray);
                          LOGGER.info("Info: RESPONSE" + responseBuilder.getResponse().getString(RESULTS));
                        }
                      }
                      promise.complete(responseBuilder.getResponse());
                    });
    return promise.future();
  }

  private Object getJsonObject(Row rs) {
    JsonObject entries = new JsonObject();
    entries
            .put(API,rs.getString(API_COLUMN_NAME))
            .put(USER_ID, rs.getString(USERID_COLUMN_NAME))
            .put(RESOURCE_ID,rs.getString(RESOURCE_COLUMN_NAME))
            .put(PROVIDER_ID,rs.getString(PROVIDER_COLUMN_NAME))
            .put(TIME,rs.getString(TIME_COLUMN_NAME));

    return entries;
  }

  private Future<JsonObject> writeInDatabase(JsonObject query) {
    Promise<JsonObject> promise = Promise.promise();
    JsonObject response = new JsonObject();
    pool.getConnection()
            .compose(connection -> connection.query(query.getString(QUERY_KEY)).execute())
            .onComplete(
                    rows -> {
                      if (rows.succeeded()) {
                        response.put(MESSAGE, "Table Updated Successfully");
                        responseBuilder =
                                new ResponseBuilder(SUCCESS)
                                        .setTypeAndTitle(200)
                                        .setMessage(response.getString(MESSAGE));
                        LOGGER.info("Info: " + responseBuilder.getResponse().toString());
                        promise.complete(responseBuilder.getResponse());
                      }
                      if (rows.failed()) {
                        LOGGER.error("Info: failed :" + rows.cause());
                        response.put(MESSAGE, rows.cause().getMessage());
                        responseBuilder =
                                new ResponseBuilder(FAILED)
                                        .setTypeAndTitle(400)
                                        .setMessage(response.getString(MESSAGE));
                        LOGGER.info("Info: " + responseBuilder.getResponse().toString());
                        promise.fail(responseBuilder.getResponse().toString());
                      }
                    });
    return promise.future();
  }
}

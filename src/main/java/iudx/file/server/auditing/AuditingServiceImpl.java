package iudx.file.server.auditing;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.PoolOptions;
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
                LOGGER.debug("Info: " + responseBuilder.getResponse().toString());
                        promise.complete(responseBuilder.getResponse());
                      }
                      if (rows.failed()) {
                        LOGGER.error("Info: failed :" + rows.cause());
                        response.put(MESSAGE, rows.cause().getMessage());
                        responseBuilder =
                                new ResponseBuilder(FAILED)
                                        .setTypeAndTitle(400)
                                        .setMessage(response.getString(MESSAGE));
                        LOGGER.debug("Info: " + responseBuilder.getResponse().toString());
                        promise.fail(responseBuilder.getResponse().toString());
                      }
                    });
    return promise.future();
  }
}

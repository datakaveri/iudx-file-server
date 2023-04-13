package iudx.file.server.database.elasticdb;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import iudx.file.server.apiserver.response.ResponseUrn;
import iudx.file.server.common.QueryType;
import iudx.file.server.database.elasticdb.elastic.ElasticClient;
import iudx.file.server.database.elasticdb.elastic.ElasticQueryGenerator;
import iudx.file.server.database.elasticdb.elastic.exception.ESQueryException;
import iudx.file.server.database.elasticdb.utilities.ResponseBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static iudx.file.server.database.elasticdb.utilities.Constants.*;

public class DatabaseServiceImpl implements DatabaseService {

  private static final Logger LOGGER = LogManager.getLogger(DatabaseServiceImpl.class);
  private final ElasticClient client;
  private final String fileMetadataIndex;
  public DatabaseServiceImpl(JsonObject config) {
    this.fileMetadataIndex = config.getString("file-metadata-index");
    if (fileMetadataIndex == null) {
      LOGGER.fatal("file metadata index is not passed in configs");
      throw new RuntimeException("requires file-metadata index");
    }
    String ip = config.getString("databaseIP");
    int port = config.getInteger("databasePort");
    String username = config.getString("databaseUser");
    String password = config.getString("databasePassword");
    client = new ElasticClient(ip, port, username, password);
  }

  @Override
  public Future<JsonObject> search(JsonObject apiQuery, QueryType type) {
    Promise<JsonObject> promise = Promise.promise();
    if ((apiQuery == null || apiQuery.isEmpty()) || type == null) {
      ResponseBuilder responseBuilder =
          new ResponseBuilder()
              .setTypeAndTitle(400)
              .setMessage("invalid parameters passed to search.");
      promise.fail(responseBuilder.getResponse().toString());
    }

    LOGGER.debug("Api Query : " + apiQuery);
    LOGGER.debug("Query type= " + type);

    ElasticQueryGenerator queryGenerator = new ElasticQueryGenerator();
    Query query = queryGenerator.getQuery(apiQuery, type);

    LOGGER.debug("fileMetadataIndex=" + fileMetadataIndex);

    try {
      final String searchIndexUrl = fileMetadataIndex;
      final String countIndexUrl = fileMetadataIndex;

      final int sizeKeyValue = getOrDefault(apiQuery, PARAM_LIMIT, DEFAULT_SIZE_VALUE);
      final int fromKeyValue = getOrDefault(apiQuery, PARAM_OFFSET, DEFAULT_FROM_VALUE);

      CountResultPlaceholder countPlaceHolder = new CountResultPlaceholder();

      Future<JsonObject> countFuture = client.asyncCount(countIndexUrl, query);

      countFuture
          .compose(
              countQueryHandler -> {
                long count =
                    countQueryHandler.getJsonArray("results").getJsonObject(0).getInteger("count");

                if (count > 50000) {
                  JsonObject json = new JsonObject();
                  json.put("type", 413);
                  json.put("title", ResponseUrn.PAYLOAD_TOO_LARGE_URN.getUrn());
                  json.put("details", ResponseUrn.PAYLOAD_TOO_LARGE_URN.getMessage());
                  return Future.failedFuture("Result Limit exceeds");
                }
                countPlaceHolder.setCount(count);
                return client.asyncSearch(searchIndexUrl, query, sizeKeyValue, fromKeyValue);
              })
          .onSuccess(
              successHandler -> {
                LOGGER.debug("Success: Successful DB request");
                JsonObject responseJson = successHandler;
                responseJson
                    .put(PARAM_LIMIT, sizeKeyValue)
                    .put(PARAM_OFFSET, fromKeyValue)
                    .put(TOTAL_HITS_KEY, countPlaceHolder.getCount());
                promise.complete(responseJson);
              })
          .onFailure(
              failureHandler -> {
                LOGGER.info("failed to query : " + failureHandler);
                promise.fail(failureHandler.getMessage());
              });
    } catch (ESQueryException ex) {
      ResponseUrn exception_urn = ResponseUrn.BAD_REQUEST_URN;
      promise.fail(new ESQueryException(exception_urn, ex.getMessage()).toString());
    } catch (Exception ex) {
      promise.fail(new ESQueryException("Exception occured executing query").toString());
    }

    return promise.future();
  }

  @Override
  public Future<JsonObject> save(JsonObject document) {

    LOGGER.debug("Documents = " + document);
    Promise<JsonObject> promise = Promise.promise();
    if (document == null || document.isEmpty()) {
      ResponseBuilder responseBuilder =
          new ResponseBuilder()
              .setTypeAndTitle(400)
              .setMessage("empty document passed to save.");
      promise.fail(responseBuilder.getResponse().toString());
    }
    LOGGER.debug(fileMetadataIndex);
    client
        .insertAsync(fileMetadataIndex, document)
        .onComplete(
            insertHandler -> {
              if (insertHandler.succeeded()) {
                LOGGER.debug("Insert handler = " + insertHandler.result().toString());
                JsonObject result = new JsonObject();
                result.put("result", insertHandler.result());
                promise.complete(result);
              } else {
                LOGGER.info(insertHandler.cause().getMessage());
                promise.fail(insertHandler.cause().getMessage());
              }
            });
    return promise.future();
  }

  @Override
  public Future<JsonObject> delete(String id) {

    Promise<JsonObject> promise = Promise.promise();
    if (id == null || id.isBlank()) {
      ResponseBuilder responseBuilder =
          new ResponseBuilder().setTypeAndTitle(400).setMessage("empty id passed to delete.");
      promise.fail(responseBuilder.getResponse().toString());
    }
    ElasticQueryGenerator queryGenerator = new ElasticQueryGenerator();

    Query deleteQuery = queryGenerator.deleteQuery(id);

    LOGGER.debug("index : " + fileMetadataIndex);
    client
        .deleteAsync(fileMetadataIndex, id, deleteQuery)
        .onComplete(
            deleteHandler -> {
              if (deleteHandler.succeeded()) {
                JsonObject result = new JsonObject();
                result.put("result", deleteHandler.result());
                promise.complete(result);
              } else {
                promise.fail(deleteHandler.cause().getMessage());
              }
            });
    return promise.future();
  }

  public int getOrDefault(JsonObject json, String key, int def) {
    if (json.containsKey(key)) {
      int value = Integer.parseInt(json.getString(key));
      return value;
    }
    return def;
  }

  private class CountResultPlaceholder {
    private long count;

    public CountResultPlaceholder() {
      this.count = 0L;
    }

    public long getCount() {
      return this.count;
    }

    public void setCount(long count) {
      this.count = count;
    }
  }
}

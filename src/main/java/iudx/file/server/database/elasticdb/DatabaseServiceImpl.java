package iudx.file.server.database.elasticdb;

import static iudx.file.server.database.elasticdb.utilities.Constants.*;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import iudx.file.server.common.QueryType;
import iudx.file.server.database.elasticdb.elastic.ElasticClient;
import iudx.file.server.database.elasticdb.elastic.ElasticQueryGenerator;
import iudx.file.server.database.elasticdb.utilities.ResponseBuilder;

public class DatabaseServiceImpl implements DatabaseService {

  private static final Logger LOGGER = LogManager.getLogger(DatabaseServiceImpl.class);
  private final ElasticClient client;
  private final String fileMetadataIndex;

  public DatabaseServiceImpl(JsonObject config) {
    this.fileMetadataIndex=config.getString("file-metadata-index");
    if(fileMetadataIndex==null) {
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
  public DatabaseService search(JsonObject apiQuery, QueryType type,
      Handler<AsyncResult<JsonObject>> handler) {
    if ((apiQuery == null || apiQuery.isEmpty()) || type == null) {
      ResponseBuilder responseBuilder = new ResponseBuilder(FAILED).setTypeAndTitle(400)
          .setMessage("invalid parameters passed to search.");
      handler.handle(Future.failedFuture(responseBuilder.getResponse().toString()));
      return this;
    }

    LOGGER.debug("query : " + apiQuery);

    ElasticQueryGenerator queryGenerator = new ElasticQueryGenerator();
    JsonObject boolQuery = new JsonObject(queryGenerator.getQuery(apiQuery, type));

    JsonObject elasticQuery = new JsonObject();

    elasticQuery.put("query", boolQuery);

    LOGGER.debug(boolQuery);
    LOGGER.debug(fileMetadataIndex);
    final String searchIndexUrl = fileMetadataIndex.concat(SEARCH_REQ_PARAM);
    final String countIndexUrl = fileMetadataIndex.concat(COUNT_REQ_PARAM);

    client.countAsync(countIndexUrl, elasticQuery.toString(), countHandler -> {
      if (countHandler.succeeded()) {
        elasticQuery.put(SIZE_KEY, getOrDefault(apiQuery, PARAM_LIMIT, DEFAULT_SIZE_VALUE));
        elasticQuery.put(FROM_KEY, getOrDefault(apiQuery, PARAM_OFFSET, DEFAULT_FROM_VALUE));
        JsonObject countJson = countHandler.result();
        LOGGER.debug("count json : " + countJson);
        int count = countJson.getJsonArray("results").getJsonObject(0).getInteger("count");
        client.searchAsync(searchIndexUrl, FILTER_PATH_VAL, elasticQuery.toString(),
            searchHandler -> {
              if (searchHandler.succeeded()) {
                handler.handle(Future.succeededFuture(
                    searchHandler.result()
                        .put(PARAM_LIMIT, elasticQuery.getInteger(SIZE_KEY))
                        .put(PARAM_OFFSET, elasticQuery.getInteger(FROM_KEY))
                        .put(TOTAL_HITS_KEY, count)));
              } else {
                handler.handle(Future.failedFuture(searchHandler.cause().getMessage()));
              }
            });
      } else {
        handler.handle(Future.failedFuture(countHandler.cause().getMessage()));
      }
    });
    return this;
  }

  @Override
  public DatabaseService save(JsonObject document, Handler<AsyncResult<JsonObject>> handler) {
    if (document == null || document.isEmpty()) {
      ResponseBuilder responseBuilder = new ResponseBuilder(FAILED).setTypeAndTitle(400)
          .setMessage("empty document passed to save.");
      handler.handle(Future.failedFuture(responseBuilder.getResponse().toString()));
      return this;
    }
    LOGGER.debug(fileMetadataIndex);
    client.insertAsync(fileMetadataIndex, document, insertHandler -> {
      if (insertHandler.succeeded()) {
        handler.handle(Future.succeededFuture(insertHandler.result()));
      } else {
        LOGGER.info(insertHandler.cause().getMessage());
        handler.handle(Future.failedFuture(insertHandler.cause().getMessage()));
      }
    });
    return this;
  }

  @Override
  public DatabaseService delete(String id, Handler<AsyncResult<JsonObject>> handler) {
    if (id == null || id.isBlank()) {
      ResponseBuilder responseBuilder = new ResponseBuilder(FAILED).setTypeAndTitle(400)
          .setMessage("empty id passed to delete.");
      handler.handle(Future.failedFuture(responseBuilder.getResponse().toString()));
      return this;
    }
    ElasticQueryGenerator queryGenerator = new ElasticQueryGenerator();
    JsonObject deleteQuery = new JsonObject(queryGenerator.deleteQuery(id));

    JsonObject elasticQuery = new JsonObject();
    elasticQuery.put("query", deleteQuery);
    LOGGER.debug("delete query :: " + elasticQuery);


    LOGGER.debug("index : " + fileMetadataIndex);
    client.deleteAsync(fileMetadataIndex, id, elasticQuery.toString(), deleteHandler -> {
      if (deleteHandler.succeeded()) {
        handler.handle(Future.succeededFuture(deleteHandler.result()));
      } else {
        handler.handle(Future.failedFuture(deleteHandler.cause().getMessage()));
      }
    });
    return this;
  }

  public int getOrDefault(JsonObject json, String key, int def) {
    if (json.containsKey(key)) {
      int value = Integer.parseInt(json.getString(key));
      return value;
    }
    return def;
  }

}

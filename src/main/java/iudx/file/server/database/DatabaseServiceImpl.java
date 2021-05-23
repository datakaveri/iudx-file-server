package iudx.file.server.database;

import static iudx.file.server.database.utilities.Constants.*;
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
import iudx.file.server.database.elastic.ElasticClient;
import iudx.file.server.database.elastic.ElasticQueryGenerator;

public class DatabaseServiceImpl implements DatabaseService {

  private static final Logger LOGGER = LogManager.getLogger(DatabaseServiceImpl.class);
  private final ElasticClient client;

  public DatabaseServiceImpl(JsonObject config) {
    String ip = config.getString("databaseIP");
    int port = config.getInteger("databasePort");
    String username = config.getString("databaseUser");
    String password = config.getString("databasePassword");
    client = new ElasticClient(ip, port, username, password);
  }

  @Override
  public DatabaseService search(JsonObject apiQuery, QueryType type,
      Handler<AsyncResult<JsonObject>> handler) {
    if((apiQuery==null || apiQuery.isEmpty()) || type==null) {
      handler.handle(Future.failedFuture("invalid parameters passed to search."));
    }
    
    ElasticQueryGenerator queryGenerator = new ElasticQueryGenerator();
    JsonObject elasticQuery = new JsonObject(queryGenerator.getQuery(apiQuery, type));

    JsonObject elasticSearchQuery = new JsonObject();
    elasticSearchQuery.put("size", 1000);
    elasticSearchQuery.put("query", elasticQuery);

    LOGGER.info(elasticSearchQuery);
    String index = getIndex(apiQuery);
    LOGGER.info(index);
    index = index.concat(SEARCH_REQ_PARAM);
    client.searchAsync(index, FILTER_PATH_VAL, elasticSearchQuery.toString(), searchHandler -> {
      if (searchHandler.succeeded()) {
        handler.handle(Future.succeededFuture(searchHandler.result()));
      } else {
        handler.handle(Future.failedFuture(searchHandler.cause().getMessage()));
      }
    });
    return this;
  }

  @Override
  public DatabaseService save(JsonObject document, Handler<AsyncResult<JsonObject>> handler) {
    String index = getIndex(document);
    LOGGER.info(index);
    client.insertAsync(index, document, insertHandler -> {
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
    ElasticQueryGenerator queryGenerator = new ElasticQueryGenerator();
    JsonObject deleteQuery = new JsonObject(queryGenerator.deleteQuery(id));

    JsonObject elasticQuery = new JsonObject();
    elasticQuery.put("query", deleteQuery);
    LOGGER.debug("delete query :: " + elasticQuery);

    String index = getIndexFromFileId(id);

    LOGGER.debug("index : " + index);
    client.deleteAsync(index, id, elasticQuery.toString(), deleteHandler -> {
      if (deleteHandler.succeeded()) {
        handler.handle(Future.succeededFuture(deleteHandler.result()));
      } else {
        handler.handle(Future.failedFuture(deleteHandler.cause().getMessage()));
      }
    });
    return this;
  }

  private String getIndex(JsonObject requestJson) {
    String id = requestJson.getString(ID);
    return getIndexFromId(id);
  }


  private String getIndexFromId(String id) {
    List<String> splitId = new LinkedList<>(Arrays.asList(id.split("/")));
    if (isResourceLevelId(splitId)) { // only Resource level id needs to remove last element, since
                                      // all items will be stored at Group level
      splitId.remove(splitId.size() - 1);
    }
    String index = String.join("__", splitId);
    return index;
  }

  private String getIndexFromFileId(String id) {
    List<String> splitId = new LinkedList<>(Arrays.asList(id.split("/")));
    if (isResourceLevelFileId(splitId)) { // only Resource level id needs to remove last element,
                                          // since
      // all items will be stored at Group level
      splitId.remove(splitId.size() - 1);
      splitId.remove(splitId.size() - 1);
    } else {
      splitId.remove(splitId.size() - 1);
    }
    String index = String.join("__", splitId);
    return index;
  }


  public boolean isResourceLevelId(List<String> id) {
    return id.size() >= 5;
  }

  public boolean isResourceLevelFileId(List<String> id) {
    return id.size() >= 6;
  }

}

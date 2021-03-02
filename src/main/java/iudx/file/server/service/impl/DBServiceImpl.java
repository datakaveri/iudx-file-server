package iudx.file.server.service.impl;

import static iudx.file.server.utilities.Constants.*;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import iudx.file.server.query.ElasticClient;
import iudx.file.server.query.ElasticQueryGenerator;
import iudx.file.server.query.QueryType;
import iudx.file.server.service.DBService;

public class DBServiceImpl implements DBService {

  private final ElasticClient client;

  public DBServiceImpl(JsonObject config) {
    String ip = config.getString("databaseIP");
    int port = config.getInteger("databasePort");
    String username = config.getString("databaseUser");
    String password = config.getString("databasePassword");
    client = new ElasticClient(ip, port, username, password);
  }

  /*
   * query json will look like
   * 
   * { "id":"<iudx-file-id>", "startTime":"ISO 8601", "endTime":"ISO 8601", "timerel":"<during>" }
   * 
   */

  @Override
  public void query(JsonObject query, Handler<AsyncResult<JsonObject>> handler) {
    ElasticQueryGenerator queryGenerator = new ElasticQueryGenerator();
    JsonObject boolQuery = new JsonObject(queryGenerator.getQuery(query, QueryType.TEMPORAL));

    JsonObject elasticQuery = new JsonObject();
    elasticQuery.put("size", 1000);
    elasticQuery.put("query", boolQuery);

    System.out.println(elasticQuery);
    String index = getIndex(query);
    System.out.println(index);
    index = index.concat(SEARCH_REQ_PARAM);
    client.searchAsync(index, FILTER_PATH_VAL, elasticQuery.toString(), searchHandler -> {
      if (searchHandler.succeeded()) {
        handler.handle(Future.succeededFuture(searchHandler.result()));
      } else {
        handler.handle(Future.failedFuture(searchHandler.cause().getMessage()));
      }
    });
  }

  @Override
  public void insert(JsonObject document, Handler<AsyncResult<JsonObject>> handler) {
    String index = getIndex(document);
    System.out.println(index);
    client.insertAsync(index, document, insertHandler -> {
      if (insertHandler.succeeded()) {
        handler.handle(Future.succeededFuture(insertHandler.result()));
      } else {
        handler.handle(Future.failedFuture(insertHandler.cause().getMessage()));
      }
    });
  }


  private String getIndex(JsonObject requestJson) {
    String id = requestJson.getString(ID);
    List<String> splitId = new LinkedList<>(Arrays.asList(id.split("/")));
    if (isResourceLevelId(splitId)) { // only Resource level id needs to remove last element, since
                                      // all items will be stored at Group level
      splitId.remove(splitId.size() - 1);
    }
    String index = String.join("__", splitId);
    return index;
  }


  public boolean isResourceLevelId(List<String> id) {
    return id.size() >= 5;
  }

}

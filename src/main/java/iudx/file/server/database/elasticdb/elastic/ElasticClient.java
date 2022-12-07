package iudx.file.server.database.elasticdb.elastic;

import static iudx.file.server.database.elasticdb.utilities.Constants.BAD_PARAMETERS;
import static iudx.file.server.database.elasticdb.utilities.Constants.EMPTY_RESPONSE;
import static iudx.file.server.database.elasticdb.utilities.Constants.FAILED;
import static iudx.file.server.database.elasticdb.utilities.Constants.SUCCESS;
import java.io.IOException;
import java.util.UUID;

import co.elastic.clients.elasticsearch.ElasticsearchAsyncClient;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.CountRequest;
import co.elastic.clients.elasticsearch.core.DeleteByQueryRequest;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.search.SourceConfig;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.vertx.core.Promise;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.client.*;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import iudx.file.server.database.elasticdb.utilities.ResponseBuilder;

public class ElasticClient {

  private RestClient client;
  private ResponseBuilder responseBuilder;
  ElasticsearchClient esClient;
  ElasticsearchAsyncClient asyncClient;
  private static final Logger LOGGER = LogManager.getLogger(ElasticClient.class);

  /**
   * ElasticClient - Elastic Low level wrapper.
   * 
   * @param databaseIP IP of the ElasticDB
   * @param databasePort Port of the ElasticDB
   */

  private String ip;
  private int port;
  private String user;
  private String password;


  public ElasticClient(String databaseIP, int databasePort, String user, String password) {
    this.ip = databaseIP;
    this.port = databasePort;
    this.user = user;
    this.password = password;
    CredentialsProvider credentials = new BasicCredentialsProvider();
    credentials.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(user, password));
    RestClientBuilder restClientBuilder = RestClient
            .builder(new HttpHost(databaseIP, databasePort))
            .setHttpClientConfigCallback(
                    httpClientBuilder -> httpClientBuilder.setDefaultCredentialsProvider(credentials));
    client = restClientBuilder.build();

    ElasticsearchTransport transport = new RestClientTransport(client, new JacksonJsonpMapper());
    // And create the API client
    esClient = new ElasticsearchClient(transport);
    asyncClient = new ElasticsearchAsyncClient(transport);
  }

  /**
   * searchAsync - Wrapper around elasticsearch async search requests.
   * 
   * @param index Index to search on
   * @param query Query
   * @param searchHandler JsonObject result {@link AsyncResult}
   */
  public Future<JsonObject> asyncSearch(String index, Query query, int size, int from){
    Promise<JsonObject> promise = Promise.promise();
    SearchRequest searchRequest = SearchRequest
            .of(e -> e.index(index).query(query).size(size).from(from));
//LOGGER.debug("search 101 = "+ searchRequest);
    asyncClient.search(searchRequest, ObjectNode.class).whenCompleteAsync((response, exception) -> {
      if (exception != null) {
        LOGGER.error("async search query failed : {}", exception);
        promise.fail(exception);
        return;
      }
      try {
        JsonArray dbResponse = new JsonArray();
        if (response.hits().total().value() == 0) {
          responseBuilder = new ResponseBuilder(FAILED).setTypeAndTitle(204);
          responseBuilder.setMessage(EMPTY_RESPONSE);
          promise.fail(responseBuilder.getResponse().toString());
          return;
        }

        // TODO : explore client API docs to directly get response, avoid loop over response to
        // create a separate Json
        response
                .hits()
                .hits()
                .stream()
                .forEach(hit -> dbResponse.add(new JsonObject(hit.source().toString())));

        responseBuilder = new ResponseBuilder(SUCCESS).setTypeAndTitle(200);
        responseBuilder.setMessage(dbResponse);
        promise.complete(responseBuilder.getResponse());
      } catch (Exception ex) {
        LOGGER.error("Exception occurred while executing query: {}", ex);
        JsonObject dbException = new JsonObject(ex.getMessage());
        responseBuilder = new ResponseBuilder(FAILED).setTypeAndTitle(400).setMessage(dbException);
        promise.fail(responseBuilder.getResponse().toString());
      }
    });
    return promise.future();
  }


  public ElasticClient insertAsync(String index, JsonObject document,
      Handler<AsyncResult<JsonObject>> insertHandler) {
    LOGGER.debug("insertAsyn = "+ index + "  " + document.toString() + " ");
    StringBuilder putRequestIndex = new StringBuilder(index);
    putRequestIndex.append("/_doc/");
    putRequestIndex.append(UUID.randomUUID().toString());
    Request queryRequest = new Request("PUT", putRequestIndex.toString());
    queryRequest.setJsonEntity(document.toString());

    client.performRequestAsync(queryRequest, new ResponseListener() {

      @Override
      public void onSuccess(Response response) {
        try {
          JsonObject responseJson = new JsonObject(EntityUtils.toString(response.getEntity()));
          LOGGER.debug("response :" + responseJson);
          if (!responseJson.containsKey("result")) {
            responseBuilder = new ResponseBuilder(FAILED).setTypeAndTitle(400)
                .setMessage("Error while inserting.");
            insertHandler.handle(Future.failedFuture(responseBuilder.getResponse().toString()));
          }
          if (responseJson.containsKey("result")
              && responseJson.getString("result").equals("created")) {
            responseBuilder = new ResponseBuilder(SUCCESS).setTypeAndTitle(200);
            insertHandler.handle(Future.succeededFuture(responseBuilder.getResponse()));
          }
        } catch (IOException e) {
          LOGGER.error("IO Execption from Database: " + e.getMessage());
          JsonObject ioError = new JsonObject(e.getMessage());
          responseBuilder = new ResponseBuilder(FAILED).setTypeAndTitle(400).setMessage(ioError);
          insertHandler.handle(Future.failedFuture(responseBuilder.getResponse().toString()));
        }

      }

      @Override
      public void onFailure(Exception ex) {
        try {
          LOGGER.info("error :" + ex.getMessage());
          String error = ex.getMessage().substring(ex.getMessage().indexOf("{"),
              ex.getMessage().lastIndexOf("}") + 1);
          JsonObject dbError = new JsonObject(error);
          responseBuilder = new ResponseBuilder(FAILED).setTypeAndTitle(400).setMessage(dbError);
          insertHandler.handle(Future.failedFuture(responseBuilder.getResponse().toString()));
        } catch (DecodeException jsonError) {
          LOGGER.error("Json parsing exception: " + jsonError);
          responseBuilder = new ResponseBuilder(FAILED).setTypeAndTitle(400)
              .setMessage(BAD_PARAMETERS);
          insertHandler.handle(Future.failedFuture(responseBuilder.getResponse().toString()));
        } catch (Exception e) {
          responseBuilder = new ResponseBuilder(FAILED).setTypeAndTitle(400)
              .setMessage("DB error");
          insertHandler.handle(Future.failedFuture(responseBuilder.getResponse().toString()));
        }
      }
    });

    return this;
  }


  public ElasticClient deleteAsync(String index, String id, String query,
      Handler<AsyncResult<JsonObject>> deletetHandler)
  /*public ElasticClient asyncDelete(String index, String id, Query query)*/{

    StringBuilder deleteURI = new StringBuilder(index);
    deleteURI.append("/_delete_by_query");

    Request deleteRequest = new Request("POST", deleteURI.toString());
    deleteRequest.setJsonEntity(query);


    /*DeleteByQueryRequest request = new DeleteByQueryRequest("source1","source 2");*/
    client.performRequestAsync(deleteRequest, new ResponseListener() {

      @Override
      public void onSuccess(Response response) {
        try {
          JsonObject responseJson = new JsonObject(EntityUtils.toString(response.getEntity()));
          int deletedDocs = responseJson.getInteger("deleted");
          if(deletedDocs == 0) {
            responseBuilder = new ResponseBuilder(FAILED).setTypeAndTitle(404)
                    .setMessage("File does not exist");
            deletetHandler.handle(Future.succeededFuture(responseBuilder.getResponse()));
            return;
          }
          JsonArray failures = responseJson.getJsonArray("failures");
          if (failures != null && failures.size() > 0) {
            responseBuilder = new ResponseBuilder(FAILED).setTypeAndTitle(400)
                .setMessage("Error while deleting.");
            deletetHandler.handle(Future.failedFuture(responseBuilder.getResponse().toString()));
          }
          responseBuilder = new ResponseBuilder(SUCCESS).setTypeAndTitle(200);
          deletetHandler.handle(Future.succeededFuture(responseBuilder.getResponse()));
        } catch (IOException e) {
          LOGGER.error("IO Execption from Database: " + e.getMessage());
          JsonObject ioError = new JsonObject(e.getMessage());
          responseBuilder = new ResponseBuilder(FAILED).setTypeAndTitle(400).setMessage(ioError);
          deletetHandler.handle(Future.failedFuture(responseBuilder.getResponse().toString()));
        }
      }

      @Override
      public void onFailure(Exception ex) {
        LOGGER.error("error : " + ex.getMessage());
        try {
          String error = ex.getMessage().substring(ex.getMessage().indexOf("{"),
              ex.getMessage().lastIndexOf("}") + 1);
          JsonObject dbError = new JsonObject(error);
          responseBuilder = new ResponseBuilder(FAILED).setTypeAndTitle(400).setMessage(dbError);
          deletetHandler.handle(Future.failedFuture(responseBuilder.getResponse().toString()));
        } catch (DecodeException jsonError) {
          LOGGER.error("Json parsing exception: " + jsonError);
          responseBuilder = new ResponseBuilder(FAILED).setTypeAndTitle(400)
              .setMessage(BAD_PARAMETERS);
          deletetHandler.handle(Future.failedFuture(responseBuilder.getResponse().toString()));
        }
      }
    });
    return this;
  }

  public Future<JsonObject> asyncCount(String index, Query query) {

    Promise<JsonObject> promise = Promise.promise();

    LOGGER.debug("In asyncount =" + index + "  " + query);

    CountRequest countRequest = CountRequest.of(e -> e.index(index).query(query));
    LOGGER.debug("In countrequest =" + countRequest);

    asyncClient.count(e -> e.index(index).query(query)).whenCompleteAsync((response, exception) -> {
      if (exception != null) {
        LOGGER.error("async count query failed : {}", exception);
        promise.fail(exception);
        return;
      }

      try {
        LOGGER.debug("Response in es ="+ response);
        long count = response.count();
        LOGGER.debug("count  in es==" + count);
        if (count == 0) {
          responseBuilder = new ResponseBuilder(FAILED).setTypeAndTitle(204);
          responseBuilder.setMessage(EMPTY_RESPONSE);
          promise.fail(responseBuilder.getResponse().toString());
          return;
        }
        responseBuilder = new ResponseBuilder(SUCCESS).setTypeAndTitle(200);
        responseBuilder.setCount(count);
        promise.complete(responseBuilder.getResponse());
        LOGGER.debug("ends==" + count);
      } catch (Exception ex) {
        LOGGER.error("Exception occurred while executing query: {}", ex);
        JsonObject dbException = new JsonObject(ex.getMessage());
        responseBuilder = new ResponseBuilder(FAILED).setTypeAndTitle(400).setMessage(dbException);
        promise.fail(responseBuilder.getResponse().toString());
      }

    });
    return promise.future();

   /* Request queryRequest = new Request("GET", index);
    queryRequest.setJsonEntity(query);*/



   /* client.performRequestAsync(queryRequest, new ResponseListener() {
      @Override
      public void onSuccess(Response response) {

        try {
          int statusCode = response.getStatusLine().getStatusCode();
          if (statusCode != 200 && statusCode != 204) {
            countHandler.handle(Future.failedFuture(DB_ERROR_2XX));
            responseBuilder =
                new ResponseBuilder(FAILED).setTypeAndTitle(400).setMessage(DB_ERROR_2XX);
            countHandler.handle(Future.failedFuture(responseBuilder.getResponse().toString()));
            return;
          }

          JsonObject responseJson = new JsonObject(EntityUtils.toString(response.getEntity()));
          if (responseJson.getInteger(COUNT) == 0) {
            responseBuilder =
                new ResponseBuilder(FAILED).setTypeAndTitle(204).setMessage(EMPTY_RESPONSE);
            countHandler.handle(Future.failedFuture(responseBuilder.getResponse().toString()));
            return;
          }
          responseBuilder =
              new ResponseBuilder(SUCCESS).setTypeAndTitle(200)
                  .setCount(responseJson.getInteger(COUNT));
          countHandler.handle(Future.succeededFuture(responseBuilder.getResponse()));
        } catch (IOException e) {
          LOGGER.error("IO Execption from Database: " + e.getMessage());
          JsonObject ioError = new JsonObject(e.getMessage());
          responseBuilder = new ResponseBuilder(FAILED).setTypeAndTitle(400).setMessage(ioError);
          countHandler.handle(Future.failedFuture(responseBuilder.getResponse().toString()));
        }
      }

      @Override
      public void onFailure(Exception e) {
        try {
          JsonObject dbError = new JsonObject().put("error", e.getMessage()).put("status", 400);;
          responseBuilder = new ResponseBuilder(FAILED).setTypeAndTitle(400).setMessage(e.getMessage());
          countHandler.handle(Future.failedFuture(responseBuilder.getResponse().toString()));
        } catch (DecodeException jsonError) {
          LOGGER.error("Json parsing exception: " + jsonError);
          responseBuilder = new ResponseBuilder(FAILED).setTypeAndTitle(400)
              .setMessage(BAD_PARAMETERS);
          countHandler.handle(Future.failedFuture(responseBuilder.getResponse().toString()));
        }
      }
    });
    return this;*/
  }
}

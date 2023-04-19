package iudx.file.server.database.elasticdb.elastic;

import static iudx.file.server.database.elasticdb.utilities.Constants.EMPTY_RESPONSE;

import co.elastic.clients.elasticsearch.ElasticsearchAsyncClient;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.DeleteByQueryRequest;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import iudx.file.server.database.elasticdb.utilities.ResponseBuilder;
import java.io.Reader;
import java.io.StringReader;
import java.util.UUID;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;

/**
 * ElasticClient.
 *
 * <h1>ElasticClient</h1>
 *
 * <p>ElasticClient to communicate with elastic (to insert,search and delete tha data)
 */
public class ElasticClient {

  private static final Logger LOGGER = LogManager.getLogger(ElasticClient.class);
  ElasticsearchClient esClient;
  ElasticsearchAsyncClient asyncClient;
  private RestClient client;
  private ResponseBuilder responseBuilder;

  /**
   * ElasticClient - Elastic Low level wrapper.
   *
   * @param databaseIp IP of the ElasticDB
   * @param databasePort Port of the ElasticDB
   */
  public ElasticClient(String databaseIp, int databasePort, String user, String password) {
    CredentialsProvider credentials = new BasicCredentialsProvider();
    credentials.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(user, password));
    RestClientBuilder restClientBuilder =
        RestClient.builder(new HttpHost(databaseIp, databasePort))
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
   * @param size int type
   * @param from from int type
   */
  public Future<JsonObject> asyncSearch(String index, Query query, int size, int from) {
    Promise<JsonObject> promise = Promise.promise();
    SearchRequest searchRequest =
        SearchRequest.of(e -> e.index(index).query(query).size(size).from(from));
    asyncClient
        .search(searchRequest, ObjectNode.class)
        .whenCompleteAsync(
            (response, exception) -> {
              if (exception != null) {
                LOGGER.error("async search query failed : {}", exception);
                promise.fail(exception);
                return;
              }
              try {
                JsonArray dbResponse = new JsonArray();
                if (response.hits().total().value() == 0) {
                  responseBuilder = new ResponseBuilder().setTypeAndTitle(204);
                  responseBuilder.setMessage(EMPTY_RESPONSE);
                  promise.fail(responseBuilder.getResponse().toString());
                  return;
                }

                // TODO : explore client API docs to directly get response, avoid loop over response
                // to
                // create a separate Json
                response.hits().hits().stream()
                    .forEach(hit -> dbResponse.add(new JsonObject(hit.source().toString())));

                responseBuilder = new ResponseBuilder().setTypeAndTitle(200);
                responseBuilder.setMessage(dbResponse);
                promise.complete(responseBuilder.getResponse());
              } catch (Exception ex) {
                LOGGER.error("Exception occurred while executing query: {}", ex);
                JsonObject dbException = new JsonObject(ex.getMessage());
                responseBuilder =
                    new ResponseBuilder().setTypeAndTitle(400).setMessage(dbException);
                promise.fail(responseBuilder.getResponse().toString());
              }
            });
    return promise.future();
  }

  /**
   * insertAsync - insert data into the respective index of elastic.
   *
   * @param index Index to insert
   * @param document json type
   */
  public Future<JsonObject> insertAsync(String index, JsonObject document) {
    StringBuilder putRequestIndex = new StringBuilder(index);
    putRequestIndex.append("/_doc/");
    putRequestIndex.append(UUID.randomUUID().toString());

    Request queryRequest = new Request("PUT", putRequestIndex.toString());
    queryRequest.setJsonEntity(document.toString());

    Reader docs = new StringReader(document.toString());
    Promise<JsonObject> promise = Promise.promise();
    asyncClient
        .index(i -> i.index(index).withJson(docs))
        .whenCompleteAsync(
            (response, exception) -> {
              if (exception != null) {
                LOGGER.error("saved query failed : {}", exception);
                promise.fail(exception);
                return;
              }

              try {
                String result = response.result().toString();

                if (result.equalsIgnoreCase("CREATED")) {
                  responseBuilder = new ResponseBuilder().setTypeAndTitle(200);
                  promise.complete(responseBuilder.getResponse());
                } else {
                  responseBuilder =
                      new ResponseBuilder()
                          .setTypeAndTitle(400)
                          .setMessage("Error while inserting.");
                  promise.fail(responseBuilder.getResponse().toString());
                }
              } catch (Exception ex) {
                responseBuilder =
                    new ResponseBuilder().setTypeAndTitle(400).setMessage(ex.toString());
                promise.fail(responseBuilder.getResponse().toString());
              }
            });
    return promise.future();
  }

  /**
   * deleteAsync - delete tha data from respective index.
   *
   * @param index Index to insert
   * @param query elastic query
   */
  public Future<JsonObject> deleteAsync(String index, Query query) {

    Promise<JsonObject> promise = Promise.promise();
    DeleteByQueryRequest deleteByQueryRequest =
        DeleteByQueryRequest.of(d -> d.index(index).query(query));

    asyncClient
        .deleteByQuery(deleteByQueryRequest)
        .whenCompleteAsync(
            (response, exception) -> {
              if (exception != null) {
                LOGGER.error("delete query failed : {}", exception);
                promise.fail(exception);
                return;
              }
              try {
                long deleteDocs = response.deleted();
                if (deleteDocs == 0) {
                  responseBuilder =
                      new ResponseBuilder().setTypeAndTitle(404).setMessage("File does not exist");
                  promise.complete(responseBuilder.getResponse());
                  return;
                }
                if (response.failures() != null && response.failures().size() > 0) {
                  responseBuilder =
                      new ResponseBuilder()
                          .setTypeAndTitle(400)
                          .setMessage("Error while deleting.");
                  promise.fail(responseBuilder.getResponse().toString());
                }
                responseBuilder = new ResponseBuilder().setTypeAndTitle(200);
                promise.complete(responseBuilder.getResponse());
              } catch (Exception ex) {
                responseBuilder =
                    new ResponseBuilder().setTypeAndTitle(400).setMessage(ex.getMessage());
                promise.fail(responseBuilder.getResponse().toString());
              }
            });
    return promise.future();
  }

  /**
   * asyncCount - count data from respective index.
   *
   * @param index Index to insert
   * @param query elastic query
   */
  public Future<JsonObject> asyncCount(String index, Query query) {

    Promise<JsonObject> promise = Promise.promise();

    asyncClient
        .count(e -> e.index(index).query(query))
        .whenCompleteAsync(
            (response, exception) -> {
              if (exception != null) {
                LOGGER.error("async count query failed : {}", exception);
                promise.fail(exception);
                return;
              }

              try {

                long count = response.count();
                if (count == 0) {
                  responseBuilder = new ResponseBuilder().setTypeAndTitle(204);
                  responseBuilder.setMessage(EMPTY_RESPONSE);
                  promise.fail(responseBuilder.getResponse().toString());
                  return;
                }
                responseBuilder = new ResponseBuilder().setTypeAndTitle(200);
                responseBuilder.setCount(count);
                promise.complete(responseBuilder.getResponse());

              } catch (Exception ex) {
                LOGGER.error("Exception occurred while executing query: {}", ex);
                JsonObject dbException = new JsonObject(ex.getMessage());
                responseBuilder =
                    new ResponseBuilder().setTypeAndTitle(400).setMessage(dbException);
                promise.fail(responseBuilder.getResponse().toString());
              }
            });
    return promise.future();
  }
}

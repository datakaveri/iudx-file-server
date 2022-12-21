package iudx.file.server.database;


import static iudx.file.server.database.elasticdb.utilities.Constants.COORDINATES;
import static org.junit.Assert.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

import co.elastic.clients.util.MissingRequiredPropertyException;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.junit.jupiter.api.*;

import org.elasticsearch.client.RestClientBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import io.vertx.core.Vertx;
import io.vertx.core.cli.annotations.Description;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.file.server.common.QueryType;
import iudx.file.server.configuration.Configuration;
import iudx.file.server.database.elasticdb.DatabaseService;
import iudx.file.server.database.elasticdb.DatabaseServiceImpl;
import org.apache.http.HttpResponseInterceptor;

@Disabled
@Testcontainers
@ExtendWith({VertxExtension.class})
@TestMethodOrder(OrderAnnotation.class)
public class DatabaseServiceTest {

  private static final Logger LOGGER = LogManager.getLogger(DatabaseServiceTest.class);
  private static RestClient client;
  private static ElasticsearchContainer elasticContainer;
  public static String CONTAINER = "docker.elastic.co/elasticsearch/elasticsearch:7.16.1";
  //public static String CONTAINER = "docker.elastic.co/elasticsearch/elasticsearch:8.3.3";
  public static String index = "file-metadata";

  private static DatabaseService dbService;

  private static Configuration config;
  private static JsonObject dbConfig;

  public static JsonObject mapping;
  public static JsonArray data;
  static RestClientBuilder.HttpClientConfigCallback httpClientConfigCallback;

  @BeforeAll
  public static void setup(Vertx vertx, VertxTestContext testContext)
      throws TimeoutException, IOException {

    config = new Configuration();
    dbConfig = config.configLoader(2, vertx);
    mapping = vertx.fileSystem().readFileBlocking("src/test/resources/mapping.json").toJsonObject();
    data = vertx.fileSystem().readFileBlocking("src/test/resources/data.json").toJsonArray();
    // LOGGER.debug("mapping : " + mapping);
    // LOGGER.debug("data :" + data);

    dbConfig.put("databaseIP", "localhost");
    dbConfig.put("databasePort", 9200);
    dbConfig.put("databaseUser", "elastic");
    dbConfig.put("databasePassword", "elk@elastic.in");
    dbConfig.put("file-metadata-index", index);

    // dbService = new DatabaseServiceImpl(dbConfig);

    elasticContainer = new ElasticsearchContainer(CONTAINER);
    elasticContainer.withPassword(dbConfig.getString("databasePassword"));
    elasticContainer.withExposedPorts(dbConfig.getInteger("databasePort"));
    elasticContainer.withEnv("discovery.type", "single-node");

    elasticContainer.start();
    if (elasticContainer.isRunning()) {
      // create a client
      final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
      credentialsProvider.setCredentials(AuthScope.ANY,
          new UsernamePasswordCredentials("elastic", dbConfig.getString("databasePassword")));


      httpClientConfigCallback = httpClientBuilder ->
              httpClientBuilder
                      .setDefaultCredentialsProvider(credentialsProvider)
                      // this request & response header manipulation helps get around newer (>=7.16) versions
                      // of elasticsearch-java client not working with older (<7.14) versions of Elasticsearch
                      // server
                      .setDefaultHeaders(
                              List.of(
                                      new BasicHeader(
                                              HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString())))
                      .addInterceptorLast(
                              (HttpResponseInterceptor)
                                      (response, context) ->
                                              response.addHeader("X-Elastic-Product", "Elasticsearch"));
     /* client = RestClient.builder(HttpHost.create(elasticContainer.getHttpHostAddress()))
          .setHttpClientConfigCallback(httpClientBuilder -> httpClientBuilder
              .setDefaultCredentialsProvider(credentialsProvider))
          .build();*/
       client =
              RestClient.builder(HttpHost.create(elasticContainer.getHttpHostAddress()))
                      .setHttpClientConfigCallback(httpClientConfigCallback)
                      .build();

      LOGGER.info("client running :" + client.isRunning());

      // create index in container
      Request indexCreationRequest = new Request("PUT", index);
      indexCreationRequest.setJsonEntity(mapping.toString());
      client.performRequest(indexCreationRequest);

      dbConfig.put("databasePort", elasticContainer.getFirstMappedPort());
      dbService = new DatabaseServiceImpl(dbConfig);

      testContext.completeNow();
    }
  }
  
  @Test
  @Description("exception for incomplete config")
  public void testObjectCreation4IncompleteConfi(Vertx vertx,VertxTestContext testContext) {
    JsonObject config=dbConfig.copy();
    config.remove("file-metadata-index");
    assertThrows(RuntimeException.class,()-> new DatabaseServiceImpl(config));
    testContext.completeNow();
  }
  

  @Test
  @Order(1)
  @Description("Ensure elastic container is created and runnning")
  public void testSetup(Vertx vertx, VertxTestContext testContext) {
    assertTrue(elasticContainer.isCreated());
    assertTrue(elasticContainer.isRunning());
    testContext.completeNow();
  }

  static Stream<Arguments> docsList() {
    return Stream.of(
        Arguments.of(data.getJsonObject(0)),
        Arguments.of(data.getJsonObject(1)),
        Arguments.of(data.getJsonObject(2)),
        Arguments.of(data.getJsonObject(3))

    );
  }

  @ParameterizedTest
  @Order(3)
  @MethodSource("docsList")
  @Description("test documents upload in elastic instance")
  public void testUploadData(JsonObject document, Vertx vertx, VertxTestContext testContext) {
    assertTrue(elasticContainer.isRunning());
    dbService.save(document).onComplete(handler -> {
      if (handler.succeeded()) {
        testContext.completeNow();
      } else {
        testContext.failNow(handler.cause().getMessage());
      }
    });
  }

  @Test
  @Order(3)
  public void testUploadNullDocuments(Vertx vertx, VertxTestContext testContext) {
        assertThrows(NullPointerException.class , ()-> dbService.save(null));
        testContext.completeNow();
  }
//TODO: NO USE TEST CASE
  @Test
  @Order(4)
  public void getAllDocs(Vertx vertx, VertxTestContext testContext)
      throws IOException, InterruptedException {
    Thread.sleep(2000);// to let elastic index docs
    assertTrue(elasticContainer.isRunning());
    JsonObject query = new JsonObject("{\n" +
        "    \"query\": {\n" +
        "        \"match_all\": {}\n" +
        "    }\n" +
        "}");
    Request getAllDocsRequest =
        new Request("GET", index + "/_search?filter_path=took,hits.hits._source");
    getAllDocsRequest.setJsonEntity(query.toString());
    Response response = client.performRequest(getAllDocsRequest);
    String responseString = EntityUtils.toString(response.getEntity(), "UTF-8");
    JsonObject responseJson = new JsonObject(responseString);

    JsonArray records = responseJson.getJsonObject("hits").getJsonArray("hits");
    System.out.println(records);
    assertEquals(4, records.size());
    assertTrue(records.getJsonObject(0).containsKey("_source"));
    testContext.completeNow();
  }

  @Test
  @Order(4)
  public void testQueryFailure(Vertx vertx, VertxTestContext testContext) {
    assertTrue(elasticContainer.isRunning());
    JsonObject temporalQuery = new JsonObject("{\n" +
        "    \"id\": \"iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/file.iudx.io/surat-itms-realtime-information/surat-itms-live-eta/123\",\n"
        +
        "    \"timerel\": \"during\",\n" +
        "    \"time\": \"2020-09-10T00:00:00Z\",\n" +
        "    \"endTime\": \"2020-09-15T00:00:00Z\"\n" +
        "}");

    dbService.search(temporalQuery, QueryType.TEMPORAL).onComplete(handler-> {
      if (handler.succeeded()) {
        testContext.failNow("failed");
      } else {
        testContext.completeNow();
      }
    });
  }
//TODO: "Future{cause=[es/count] Missing [X-Elastic-Product] header. Please check that you are connecting to an Elasticsearch instance, and that any networking filters are preserving that header.}"
 /* @Test
  @Order(5)
  public void testTemporalQuery(Vertx vertx, VertxTestContext testContext) {
    assertTrue(elasticContainer.isRunning());
    JsonObject temporalQuery = new JsonObject("{\n" +
        "    \"id\": \"iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/file.iudx.io/surat-itms-realtime-information/surat-itms-live-eta\",\n"
        +
        "    \"timerel\": \"during\",\n" +
        "    \"time\": \"2020-09-10T00:00:00Z\",\n" +
        "    \"endTime\": \"2020-09-15T00:00:00Z\"\n" +
        "}");
JsonObject jsonObject =new JsonObject().put("id","iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs.iudx.io/pune-env-flood/FWR055")
                .put("timerel","between").put("time","2020-09-10T00:00:00Z")
                .put("endTime","2020-09-15T00:00:00Z");
    dbService
        .search(jsonObject, QueryType.TEMPORAL)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                LOGGER.debug("234");
                //  assertEquals(4, handler.result().getJsonArray("results").size());
                testContext.completeNow();
              } else {
                LOGGER.debug(handler.toString());
               testContext.completeNow();
                LOGGER.debug("239");
                 //testContext.failNow("handler.cause().getMessage()");
              }

            });
  }*/

  @Test
  @Order(5)
  public void testTemporalQueryFail(Vertx vertx, VertxTestContext testContext) {
    assertTrue(elasticContainer.isRunning());
    JsonObject temporalQuery = new JsonObject("{\n" +
            "    \"id\": \"iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/file.iudx.io/surat-itms-realtime-information/surat-itms-live-eta\",\n"
            +
            "    \"timerel\": \"during\",\n" +
            "    \"time\": \"2020-09-10T00:00:00Z\",\n" +
            "    \"endTime\": \"2020-09-15T00:00:00Z\"\n" +
            "}");

    dbService.search(temporalQuery, null).onSuccess(handler -> {
      testContext.failNow("Failed");
    }).onFailure( handler->{
      JsonObject jsonObject = new JsonObject(handler.getMessage());
      assertEquals(400,jsonObject.getInteger("type"));
      assertEquals("urn:dx:rs:badRequest",jsonObject.getString("title"));
      assertEquals("invalid parameters passed to search.",jsonObject.getString("details"));
      testContext.completeNow();
    });
  }

  @Test
  @Order(5)
  public void testTemporalQuery2(Vertx vertx, VertxTestContext testContext) {
    assertTrue(elasticContainer.isRunning());
    JsonObject temporalQuery = new JsonObject("{\n" +
        "    \"id\": \"iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/file.iudx.io/surat-itms-realtime-information/surat-itms-live-eta\",\n"
        +
        "    \"timerel\": \"during\",\n" +
        "    \"time\": \"2020-09-26T00:00:00Z\",\n" +
        "    \"endTime\": \"2020-09-28T00:00:00Z\"\n" +
        "}");

    dbService.search(temporalQuery, QueryType.TEMPORAL).onComplete(handler -> {
      if (handler.succeeded()) {
        testContext.failNow(handler.cause().getMessage());
      } else {
        testContext.completeNow();
      }
    });
  }

//TODO: "Future{cause=[es/count] Missing [X-Elastic-Product] header. Please check that you are connecting to an Elasticsearch instance, and that any networking filters are preserving that header.}"
  @Test
  @Order(5)
  public void testTemporalQuery3(Vertx vertx, VertxTestContext testContext) {
    assertTrue(elasticContainer.isRunning());
    JsonObject temporalQuery = new JsonObject("{\n" +
        "    \"id\": \"iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/file.iudx.io/surat-itms-realtime-information/surat-itms-live-eta\",\n"
        +
        "    \"timerel\": \"during\",\n" +
        "    \"time\": \"2020-09-01T00:00:00Z\",\n" +
        "    \"endTime\": \"2020-09-04T00:00:00Z\"\n" +
        "}");

    dbService
        .search(temporalQuery, QueryType.TEMPORAL)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                assertEquals(1, handler.result().getJsonArray("results").size());
                testContext.completeNow();
              } else {
                testContext.failNow(handler.cause().getMessage());
              }
            });
  }
//TODO: "Future{cause=[es/count] Missing [X-Elastic-Product] header. Please check that you are connecting to an Elasticsearch instance, and that any networking filters are preserving that header.}"
  @Test
  @Order(6)
  public void testGeoQuery(Vertx vertx, VertxTestContext testContext) {
    assertTrue(elasticContainer.isRunning());
    JsonObject temporalQuery = new JsonObject("{\n" +
        "    \"id\": \"iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/file.iudx.io/surat-itms-realtime-information/surat-itms-live-eta\",\n"
        +
        "    \"georel\": \"near\",\n" +
        "    \"geometry\":\"point\",\n" +
        "    \"coordinates\":\"[72.8058,21.1835]\",\n" +
        "    \"radius\":\"200\"\n" +
        "}");
    dbService.search(temporalQuery, QueryType.GEO).onComplete(handler -> {
      if (handler.succeeded()) {
        LOGGER.info("result : " + handler.result());
        assertTrue(handler.result().containsKey("type"));
        assertTrue(handler.result().containsKey("title"));
        testContext.completeNow();
      } else {
        LOGGER.info("failure : " + handler.cause().getMessage());
        testContext.failNow(handler.cause().getMessage());
      }
    });
  }
//TODO: "Future{cause=[es/count] Missing [X-Elastic-Product] header. Please check that you are connecting to an Elasticsearch instance, and that any networking filters are preserving that header.}"
  @Test
  @Order(6)
  public void testGeoQuery2(Vertx vertx, VertxTestContext testContext) {
    assertTrue(elasticContainer.isRunning());
    JsonObject temporalQuery = new JsonObject("{\n" +
        "    \"id\": \"iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/file.iudx.io/surat-itms-realtime-information/surat-itms-live-eta\",\n"
        +
        "    \"georel\": \"near\",\n" +
        "    \"geometry\":\"point\",\n" +
        "    \"coordinates\":\"[72.8058,21.1833]\",\n" +
        "    \"radius\":\"100\"\n" +
        "}");
    dbService.search(temporalQuery, QueryType.GEO).onComplete(handler -> {
      if (handler.succeeded()) {
        assertEquals(3, handler.result().getJsonArray("results").size());
        testContext.completeNow();
      } else {
        testContext.failNow(handler.cause().getMessage());
      }
    });
  }
//TODO: "Future{cause=[es/count] Missing [X-Elastic-Product] header. Please check that you are connecting to an Elasticsearch instance, and that any networking filters are preserving that header.}"
  @Test
  @Order(8)
  public void testDefaultPaginationParams(Vertx vertx, VertxTestContext testContext) {
    assertTrue(elasticContainer.isRunning());
    JsonObject temporalQuery = new JsonObject("{\n" +
        "    \"id\": \"iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/file.iudx.io/surat-itms-realtime-information/surat-itms-live-eta\",\n"
        +
        "    \"timerel\": \"during\",\n" +
        "    \"time\": \"2020-09-10T00:00:00Z\",\n" +
        "    \"endTime\": \"2020-09-15T00:00:00Z\"\n" +
        "}");

    dbService.search(temporalQuery, QueryType.TEMPORAL).onComplete( handler -> {
      if (handler.succeeded()) {
        JsonObject result = handler.result();
        assertTrue(result.containsKey("totalHits"));
        assertTrue(result.containsKey("offset"));
        assertTrue(result.containsKey("limit"));
        assertEquals(4, result.getJsonArray("results").size());
        assertEquals(4, result.getInteger("totalHits"));
        assertEquals(0, result.getInteger("offset"));
        assertEquals(5000, result.getInteger("limit"));
        testContext.completeNow();
      } else {
        testContext.failNow(handler.cause().getMessage());
      }
    });
  }

//TODO: "Future{cause=[es/count] Missing [X-Elastic-Product] header. Please check that you are connecting to an Elasticsearch instance, and that any networking filters are preserving that header.}"
  @Test
  @Order(8)
  public void testPaginationParams(Vertx vertx, VertxTestContext testContext) {
    assertTrue(elasticContainer.isRunning());
    JsonObject temporalQuery = new JsonObject("{\n" +
        "    \"id\": \"iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/file.iudx.io/surat-itms-realtime-information/surat-itms-live-eta\",\n"
        +
        "    \"timerel\": \"during\",\n" +
        "    \"time\": \"2020-09-10T00:00:00Z\",\n" +
        "    \"endTime\": \"2020-09-15T00:00:00Z\",\n" +
        "    \"limit\":2,\n" +
        "    \"offset\":0\n" +
        "}");

    dbService.search(temporalQuery, QueryType.TEMPORAL).onComplete( handler -> {
      if (handler.succeeded()) {
        JsonObject result = handler.result();
        assertTrue(result.containsKey("totalHits"));
        assertTrue(result.containsKey("offset"));
        assertTrue(result.containsKey("limit"));
        assertEquals(2, result.getJsonArray("results").size());
        assertEquals(4, result.getInteger("totalHits"));
        assertEquals(0, result.getInteger("offset"));
        assertEquals(2, result.getInteger("limit"));
        testContext.completeNow();
      } else {
        testContext.failNow(handler.cause().getMessage());
      }
    });
  }

//TODO: "Future{cause=[es/delete_by_query] Missing [X-Elastic-Product] header. Please check that you are connecting to an Elasticsearch instance, and that any networking filters are preserving that header.}"
  @Test
  @Order(9)
  public void testDeleteDocument(Vertx vertx, VertxTestContext testContext) {
    String id =
        "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/file.iudx.io/surat-itms-realtime-information/surat-itms-live-eta/2a553c97-e873-4983-86b6-070774e4e671.txt";
    dbService.delete(id).onComplete(handler -> {
      if (handler.succeeded()) {
        testContext.completeNow();
      } else {
        testContext.failNow(handler.cause().getMessage());
      }
    });
  }



  @Test
  @Order(10)
  public void testSearchWithInvalidParam(Vertx vertx, VertxTestContext testContext) {
    assertThrows(NullPointerException.class, ()-> dbService.search(null,QueryType.GEO));
    testContext.completeNow();
  }

  @Test
  @Order(10)
  public void testSaveWithInvalidDocument(Vertx vertx, VertxTestContext testContext) {
   assertThrows(NullPointerException.class,()->dbService.save(null));
   testContext.completeNow();
  }

  @Test
  @Order(10)
  public void testDeleteWithNullId(Vertx vertx, VertxTestContext testContext) {
    assertThrows(MissingRequiredPropertyException.class,()->dbService.delete(null));
    testContext.completeNow();
  }

  @Test
  @Order(11)
  public void closeContainer(Vertx vertx, VertxTestContext testContext) {
    elasticContainer.close();
    testContext.completeNow();
  }


  @Test
  @Order(12)
  public void test4NetworkIssues(Vertx vertx, VertxTestContext testContext) {
    JsonObject temporalQuery = new JsonObject("{\n" +
        "    \"id\": \"iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/file.iudx.io/surat-itms-realtime-information/surat-itms-live-eta\",\n"
        +
        "    \"timerel\": \"during\",\n" +
        "    \"time\": \"2020-09-10T00:00:00Z\",\n" +
        "    \"endTime\": \"2020-09-15T00:00:00Z\"\n" +
        "}");

    dbService.search(temporalQuery, QueryType.TEMPORAL).onComplete( handler -> {
      if (handler.succeeded()) {
        testContext.failNow(handler.cause().getMessage());
      } else {
        assertNull(handler.result());
        testContext.completeNow();
      }
    });
  }



  // @Test
  // @Order(12)
  // public void testPaginationParamsFail(Vertx vertx, VertxTestContext testContext) {
  // assertFalse(elasticContainer.isRunning());
  // JsonObject temporalQuery = new JsonObject("{\n" +
  // " \"id\":
  // \"iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/file.iudx.io/surat-itms-realtime-information/surat-itms-live-eta\",\n"
  // +
  // " \"timerel\": \"during\",\n" +
  // " \"time\": \"2020-09-10T00:00:00Z\",\n" +
  // " \"endTime\": \"2020-09-15T00:00:00Z\",\n" +
  // " \"limit\":2,\n" +
  // " \"offset\":2\n" +
  // "}");
  //
  // dbService.search(temporalQuery, QueryType.TEMPORAL, handler -> {
  // if (handler.succeeded()) {
  // testContext.failNow(handler.cause().getMessage());
  // } else {
  // testContext.completeNow();
  // }
  // });
  // }
  //
  // @Test
  // //@Order(12)
  // public void testDeleteDocumentFail(Vertx vertx, VertxTestContext testContext) {
  // String id =
  // "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/file.iudx.io/surat-itms-realtime-information/surat-itms-live-eta/2a553c97-e873-4983-86b6-070774e4e671.txt";
  // dbService.delete(id, handler -> {
  // if (handler.succeeded()) {
  // testContext.failNow(handler.cause().getMessage());
  // } else {
  // testContext.completeNow();
  // }
  // });
  // }



  @AfterAll
  public static void destroy(Vertx vertx, VertxTestContext testContext) {
    elasticContainer.close();
    testContext.completeNow();
  }

}

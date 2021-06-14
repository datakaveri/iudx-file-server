package iudx.file.server.database;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import java.io.IOException;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
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

@Testcontainers
@ExtendWith({VertxExtension.class})
@TestMethodOrder(OrderAnnotation.class)
public class DatabaseServiceTest {

  private static final Logger LOGGER = LogManager.getLogger(DatabaseServiceTest.class);
  private static RestClient client;
  private static ElasticsearchContainer elasticContainer;
  public static String CONTAINER = "docker.elastic.co/elasticsearch/elasticsearch:7.12.1";
  public static String index =
      "iisc.ac.in__89a36273d77dac4cf38114fca1bbe64392547f86__file.iudx.io__surat-itms-realtime-information";

  private static DatabaseService dbService;

  private static Configuration config;
  private static JsonObject dbConfig;

  public static JsonObject mapping;
  public static JsonArray data;


  @BeforeAll
  public static void setup(Vertx vertx, VertxTestContext testContext)
      throws TimeoutException, IOException {

    config = new Configuration();
    dbConfig = config.configLoader(2, vertx);
    mapping = vertx.fileSystem().readFileBlocking("src/test/resources/mapping.json").toJsonObject();
    data = vertx.fileSystem().readFileBlocking("src/test/resources/data.json").toJsonArray();
    // LOGGER.debug("mapping : " + mapping);
    // LOGGER.debug("data :" + data);
    dbService = new DatabaseServiceImpl(dbConfig);

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



      client = RestClient.builder(HttpHost.create(elasticContainer.getHttpHostAddress()))
          .setHttpClientConfigCallback(httpClientBuilder -> httpClientBuilder
              .setDefaultCredentialsProvider(credentialsProvider))
          .build();

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
    dbService.save(document, handler -> {
      if (handler.succeeded()) {
        testContext.completeNow();
      } else {
        testContext.failNow(handler.cause().getMessage());
      }
    });
  }

  @Test
  @Order(3)
  public void testUploadForIncorrectIndex(Vertx vertx, VertxTestContext testContext) {
    JsonObject document = new JsonObject("{\n" +
        "        \"id\": \"iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/ABC.XYZ.io/surat-itms-realtime-information/surat-itms-live-eta\",\n"
        +
        "        \"timeRange\": {\n" +
        "            \"gte\": \"2020-09-08T00:00:00Z\",\n" +
        "            \"lte\": \"2020-09-25T00:00:00Z\"\n" +
        "        },\n" +
        "        \"fileId\": \"iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/ABC.XYZ.io/surat-itms-realtime-information/surat-itms-live-eta/7b39bec2-365a-4efe-b543-7a8c9b25be14.txt\",\n"
        +
        "        \"location\": {\n" +
        "            \"type\": \"point\",\n" +
        "            \"coordinates\": [\n" +
        "                72.8055,\n" +
        "                21.1833\n" +
        "            ]\n" +
        "        },\n" +
        "        \"area\": \"surat\"\n" +
        "    }");

    dbService.save(document, handler -> {
      if (handler.succeeded()) {
        testContext.failNow(handler.cause().getMessage());
      } else {
        LOGGER.error(handler.cause().getMessage());
        testContext.completeNow();
      }
    });
  }

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
    assertEquals(4, records.size());
    assertTrue(records.getJsonObject(0).getJsonObject("_source").containsKey("fileId"));

    testContext.completeNow();
  }

  @Test
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

    dbService.search(temporalQuery, QueryType.TEMPORAL, handler -> {
      if (handler.succeeded()) {
        assertEquals(4, handler.result().getJsonArray("results").size());
        testContext.completeNow();
      } else {
        testContext.failNow(handler.cause().getMessage());
      }
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
        "    \"time\": \"2020-09-01T00:00:00Z\",\n" +
        "    \"endTime\": \"2020-09-04T00:00:00Z\"\n" +
        "}");

    dbService.search(temporalQuery, QueryType.TEMPORAL, handler -> {
      if (handler.succeeded()) {
        assertEquals(1, handler.result().getJsonArray("results").size());
        testContext.completeNow();
      } else {
        testContext.failNow(handler.cause().getMessage());
      }
    });
  }

  @Test
  @Order(6)
  public void testGeoQuery(Vertx vertx, VertxTestContext testContext) {
    assertTrue(elasticContainer.isRunning());
    JsonObject temporalQuery = new JsonObject("{\n" +
        "    \"id\": \"iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/file.iudx.io/surat-itms-realtime-information/surat-itms-live-eta\",\n"
        +
        "    \"georel\": \"near\",\n" +
        "    \"geometry\":\"point\",\n" +
        "    \"coordinates\":\"[72.8058,21.1833]\",\n" +
        "    \"radius\":\"20\"\n" +
        "}");
    dbService.search(temporalQuery, QueryType.GEO, handler -> {
      if (handler.succeeded()) {
        assertEquals(2, handler.result().getJsonArray("results").size());
        testContext.completeNow();
      } else {
        testContext.failNow(handler.cause().getMessage());
      }
    });
  }

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
    dbService.search(temporalQuery, QueryType.GEO, handler -> {
      if (handler.succeeded()) {
        assertEquals(3, handler.result().getJsonArray("results").size());
        testContext.completeNow();
      } else {
        testContext.failNow(handler.cause().getMessage());
      }
    });
  }

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

    dbService.search(temporalQuery, QueryType.TEMPORAL, handler -> {
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

    dbService.search(temporalQuery, QueryType.TEMPORAL, handler -> {
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


  @Test
  @Order(8)
  public void testPaginationParams2(Vertx vertx, VertxTestContext testContext) {
    assertTrue(elasticContainer.isRunning());
    JsonObject temporalQuery = new JsonObject("{\n" +
        "    \"id\": \"iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/file.iudx.io/surat-itms-realtime-information/surat-itms-live-eta\",\n"
        +
        "    \"timerel\": \"during\",\n" +
        "    \"time\": \"2020-09-10T00:00:00Z\",\n" +
        "    \"endTime\": \"2020-09-15T00:00:00Z\",\n" +
        "    \"limit\":2,\n" +
        "    \"offset\":2\n" +
        "}");

    dbService.search(temporalQuery, QueryType.TEMPORAL, handler -> {
      if (handler.succeeded()) {
        JsonObject result = handler.result();
        assertTrue(result.containsKey("totalHits"));
        assertTrue(result.containsKey("offset"));
        assertTrue(result.containsKey("limit"));
        assertEquals(2, result.getJsonArray("results").size());
        assertEquals(4, result.getInteger("totalHits"));
        assertEquals(2, result.getInteger("offset"));
        assertEquals(2, result.getInteger("limit"));
        testContext.completeNow();
      } else {
        testContext.failNow(handler.cause().getMessage());
      }
    });
  }

  @Test
  @Order(9)
  public void testDeleteDocument(Vertx vertx, VertxTestContext testContext) {
    String id =
        "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/file.iudx.io/surat-itms-realtime-information/surat-itms-live-eta/2a553c97-e873-4983-86b6-070774e4e671.txt";
    dbService.delete(id, handler -> {
      if (handler.succeeded()) {
        testContext.completeNow();
      } else {
        testContext.failNow(handler.cause().getMessage());
      }
    });
  }

  @Test
  public void testSearchWithInvalidParam(Vertx vertx, VertxTestContext testContext) {
    dbService.search(null, QueryType.GEO, handler -> {
      if (handler.failed()) {
        JsonObject response = new JsonObject(handler.cause().getMessage());
        assertTrue(response.containsKey("type"));
        testContext.completeNow();
      } else {
        testContext.failNow("failed");;
      }
    });
  }

  @Test
  public void testSaveWithInvalidDocument(Vertx vertx, VertxTestContext testContext) {
    dbService.save(null, handler -> {
      if (handler.failed()) {
        JsonObject response = new JsonObject(handler.cause().getMessage());
        assertTrue(response.containsKey("type"));
        testContext.completeNow();
      } else {
        testContext.failNow("failed");;
      }
    });
  }

  @Test
  public void testDeleteWithInvalidId(Vertx vertx, VertxTestContext testContext) {
    dbService.delete(null, handler -> {
      if (handler.failed()) {
        JsonObject response = new JsonObject(handler.cause().getMessage());
        assertTrue(response.containsKey("type"));
        testContext.completeNow();
      } else {
        testContext.failNow("failed");;
      }
    });
  }


  @AfterAll
  public static void destroy(Vertx vertx, VertxTestContext testContext) {
    elasticContainer.close();
    testContext.completeNow();
  }

}

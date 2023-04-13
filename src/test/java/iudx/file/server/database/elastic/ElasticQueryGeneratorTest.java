package iudx.file.server.database.elastic;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.file.server.common.QueryType;
import iudx.file.server.database.elasticdb.elastic.ElasticQueryGenerator;
import iudx.file.server.database.elasticdb.utilities.ResponseBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(VertxExtension.class)
public class ElasticQueryGeneratorTest {

  private ElasticQueryGenerator elasticQueryGenerator;

  @BeforeEach
  public void setup(Vertx vertx, VertxTestContext testContext) {
    elasticQueryGenerator = new ElasticQueryGenerator();
    testContext.completeNow();
  }

  @Test
  public void testGenerateTemporalQuery(Vertx vertx, VertxTestContext testContext) {

    // query
    JsonObject temporalQuery =
        new JsonObject(
            "{\n"
                + "    \"id\": \"iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/file.iudx.io/surat-itms-realtime-information/surat-itms-live-eta\",\n"
                + "    \"timerel\": \"during\",\n"
                + "    \"time\": \"2020-09-10T00:00:00Z\",\n"
                + "    \"endTime\": \"2020-09-15T00:00:00Z\"\n"
                + "}");

    // expected elastic query
    String elasticQuery =
        "Query: {\"bool\":{\"filter\":[{\"terms\":{\"id\":[\"iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/file.iudx.io/surat-itms-realtime-information/surat-itms-live-eta\"]}},{\"range\":{\"timeRange\":{\"from\":\"2020-09-10T00:00:00Z\",\"to\":\"2020-09-15T00:00:00Z\"}}}]}}";

    // call test method
    String generatedQuery =
        String.valueOf(elasticQueryGenerator.getQuery(temporalQuery, QueryType.TEMPORAL));
    // assertions
    assertEquals(elasticQuery, generatedQuery);
    assertTrue(generatedQuery.contains("bool"));
    assertTrue(generatedQuery.contains("timeRange"));
    testContext.completeNow();
  }

  @Test
  public void testGenerateGeoQuery(Vertx vertx, VertxTestContext testContext) {

    // query
    JsonObject geoQuery =
        new JsonObject(
            "{\n"
                + "    \"id\": \"iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/file.iudx.io/surat-itms-realtime-information/surat-itms-live-eta\",\n"
                + "    \"timerel\": \"during\",\n"
                + "    \"time\": \"2020-09-10T00:00:00Z\",\n"
                + "    \"endTime\": \"2020-09-15T00:00:00Z\",\n"
                + "    \"georel\": \"within\",\n"
                + "    \"geometry\": \"polygon\",\n"
                + "    \"coordinates\": \"[[[72.7815,21.1726],[72.7856,21.1519],[72.807,21.1527],[72.8170,21.1680],[72.800,21.1808],[72.7815,21.1726]]]\"\n"
                + "}");

    // expected elastic query
    String elasticQuery =
        "Query: {\"bool\":{\"filter\":[{\"terms\":{\"id\":[\"iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/file.iudx.io/surat-itms-realtime-information/surat-itms-live-eta\"]}},{\"wrapper\":{\"query\":\"eyAiZ2VvX3NoYXBlIjogeyAibG9jYXRpb24iOiB7ICJzaGFwZSI6IHsidHlwZSI6InBvbHlnb24iLCJjb29yZGluYXRlcyI6W1tbNzIuNzgxNSwyMS4xNzI2XSxbNzIuNzg1NiwyMS4xNTE5XSxbNzIuODA3LDIxLjE1MjddLFs3Mi44MTcsMjEuMTY4XSxbNzIuOCwyMS4xODA4XSxbNzIuNzgxNSwyMS4xNzI2XV1dfSwgInJlbGF0aW9uIjogIndpdGhpbiIgfSB9IH0=\"}}]}}";

    // call test method
    String generatedQuery = String.valueOf(elasticQueryGenerator.getQuery(geoQuery, QueryType.GEO));
    // assertions
    assertEquals(elasticQuery, generatedQuery);
    assertTrue(generatedQuery.contains("bool"));
    assertTrue(generatedQuery.contains("wrapper"));
    testContext.completeNow();
  }

  @Test
  public void testGenerateTemporalGeoQuery(Vertx vertx, VertxTestContext testContext) {

    // query
    JsonObject temporalGeoQuery =
        new JsonObject(
            "{\n"
                + "    \"id\": \"iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/file.iudx.io/surat-itms-realtime-information/surat-itms-live-eta\",\n"
                + "    \"timerel\": \"during\",\n"
                + "    \"time\": \"2020-09-10T00:00:00Z\",\n"
                + "    \"endTime\": \"2020-09-15T00:00:00Z\",\n"
                + "    \"georel\": \"within\",\n"
                + "    \"geometry\": \"polygon\",\n"
                + "    \"coordinates\": \"[[[72.7815,21.1726],[72.7856,21.1519],[72.807,21.1527],[72.8170,21.1680],[72.800,21.1808],[72.7815,21.1726]]]\"\n"
                + "}");

    // expected elastic query
    String elasticQuery =
        "Query: {\"bool\":{\"filter\":[{\"terms\":{\"id\":[\"iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/file.iudx.io/surat-itms-realtime-information/surat-itms-live-eta\"]}},{\"range\":{\"timeRange\":{\"from\":\"2020-09-10T00:00:00Z\",\"to\":\"2020-09-15T00:00:00Z\"}}},{\"wrapper\":{\"query\":\"eyAiZ2VvX3NoYXBlIjogeyAibG9jYXRpb24iOiB7ICJzaGFwZSI6IHsidHlwZSI6InBvbHlnb24iLCJjb29yZGluYXRlcyI6W1tbNzIuNzgxNSwyMS4xNzI2XSxbNzIuNzg1NiwyMS4xNTE5XSxbNzIuODA3LDIxLjE1MjddLFs3Mi44MTcsMjEuMTY4XSxbNzIuOCwyMS4xODA4XSxbNzIuNzgxNSwyMS4xNzI2XV1dfSwgInJlbGF0aW9uIjogIndpdGhpbiIgfSB9IH0=\"}}]}}";

    // call test method
    String generatedQuery =
        String.valueOf(elasticQueryGenerator.getQuery(temporalGeoQuery, QueryType.TEMPORAL_GEO));
    // assertions
    assertEquals(elasticQuery, generatedQuery);
    assertTrue(generatedQuery.contains("bool"));
    assertTrue(generatedQuery.contains("timeRange"));
    testContext.completeNow();
  }

  @Test
  public void testResponseBuilder(VertxTestContext testContext) {
    JsonObject error = new JsonObject();
    error.put("status", 404);
    error.put("error", new JsonObject().put("type", "index_not_found_exception"));

    ResponseBuilder responseBuilder = new ResponseBuilder();
    responseBuilder.setMessage(error);

    assertEquals("Invalid resource id", responseBuilder.getResponse().getString("errorMessage"));
    testContext.completeNow();
  }

  @Test
  public void testResponseBuilderAlt(VertxTestContext testContext) {
    JsonObject error = new JsonObject();
    error.put("status", 404);
    error.put(
        "error",
        new JsonObject()
            .put(
                "root_cause",
                new JsonArray().add(new JsonObject().put("reason", "backend connect failure"))));

    ResponseBuilder responseBuilder = new ResponseBuilder();
    responseBuilder.setMessage(error);

    assertEquals(
        "backend connect failure", responseBuilder.getResponse().getString("errorMessage"));
    testContext.completeNow();
  }
}

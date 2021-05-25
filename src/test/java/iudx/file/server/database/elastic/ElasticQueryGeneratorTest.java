package iudx.file.server.database.elastic;

import static iudx.file.server.database.utilities.Constants.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.file.server.common.QueryType;

@ExtendWith(VertxExtension.class)
public class ElasticQueryGeneratorTest {

  private ElasticQueryGenerator elasticQueryGenerator;

  @BeforeEach
  public void setup(Vertx vertx, VertxTestContext testContext) {
    elasticQueryGenerator = new ElasticQueryGenerator();
    testContext.completeNow();
  }

  @Test
  public void testGenerateListQuery() {
    // query
    JsonObject query = new JsonObject();
    query.put(ID, "IUDX_ID");

    // expected elastic query;
    String elasticQuery = "{\n" +
        "  \"bool\" : {\n" +
        "    \"filter\" : [\n" +
        "      {\n" +
        "        \"term\" : {\n" +
        "          \"id\" : {\n" +
        "            \"value\" : \"IUDX_ID\",\n" +
        "            \"boost\" : 1.0\n" +
        "          }\n" +
        "        }\n" +
        "      }\n" +
        "    ],\n" +
        "    \"adjust_pure_negative\" : true,\n" +
        "    \"boost\" : 1.0\n" +
        "  }\n" +
        "}";

    String generatedQuery = elasticQueryGenerator.getQuery(query, QueryType.LIST);
    assertEquals(elasticQuery, generatedQuery);
    assertTrue(generatedQuery.contains("bool"));
    assertTrue(generatedQuery.contains("IUDX_ID"));
  }
}

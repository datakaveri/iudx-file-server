package iudx.file.server.apiserver.utilities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.file.server.apiserver.query.QueryParams;
import iudx.file.server.apiserver.validations.QueryParamsValidator;
import iudx.file.server.common.QueryType;

@ExtendWith(VertxExtension.class)
public class UtilitiesTest {

  private Utilities utilities;

  @BeforeEach
  public void setup(Vertx vertx, VertxTestContext testContext) {
    utilities = new Utilities();
    testContext.completeNow();
  }

  @Test
  public void testTemporalQuery() {
    JsonObject query = new JsonObject();
    query.put("time", "2020-09-15T00:00:00Z").put("endTime", "2020-09-15T00:00:00Z");
    QueryParams queryParam = query.mapTo(QueryParams.class).build();
    assertEquals(QueryType.TEMPORAL, utilities.getQueryType(queryParam));
  }

  @Test
  public void testGeoTemporalQuery() {
    JsonObject query = new JsonObject();
    query.put("time", "2020-09-15T00:00:00Z").put("endTime", "2020-09-15T00:00:00Z").put("georel",
        "within");
    QueryParams queryParam = query.mapTo(QueryParams.class).build();
    assertEquals(QueryType.TEMPORAL_GEO, utilities.getQueryType(queryParam));
  }

  @Test
  public void testGeoQuery() {
    JsonObject query = new JsonObject();
    query.put("georel", "within").put("lat", "72").put("lon", "23");
    QueryParams queryParam = query.mapTo(QueryParams.class).build();
    assertEquals(QueryType.GEO, utilities.getQueryType(queryParam));
  }

  @Test
  public void testListQuery() {
    JsonObject query = new JsonObject();
    query.put("id", "IUDX_ID");
    QueryParams queryParam = query.mapTo(QueryParams.class).build();
    assertEquals(QueryType.LIST, utilities.getQueryType(queryParam));
  }


  @Test
  public void testEmptyQuery() {
    JsonObject query = new JsonObject();
    QueryParams queryParam = query.mapTo(QueryParams.class).build();
    assertEquals(QueryType.LIST, utilities.getQueryType(queryParam));
  }
}

package iudx.file.server.apiserver.utilities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
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
    assertEquals(QueryType.TEMPORAL, utilities.getQueryType(query));
  }
  
  @Test
  public void testGeoTemporalQuery() {
    JsonObject query = new JsonObject();
    query.put("time", "2020-09-15T00:00:00Z").put("endTime", "2020-09-15T00:00:00Z").put("georel", "within");
    assertEquals(QueryType.TEMPORAL_GEO, utilities.getQueryType(query));
  }
  
  @Test
  public void testGeoQuery() {
    JsonObject query = new JsonObject();
    query.put("georel", "within").put("lat", "72").put("lon", "23");
    assertEquals(QueryType.GEO, utilities.getQueryType(query));
  }
  
  @Test
  public void testListQuery() {
    JsonObject query = new JsonObject();
    query.put("id", "IUDX_ID");
    assertEquals(QueryType.LIST, utilities.getQueryType(query));
  }
  
  
  @Test
  public void testEmptyQuery() {
    JsonObject query = new JsonObject();
    assertEquals(QueryType.LIST, utilities.getQueryType(query));
  }
}

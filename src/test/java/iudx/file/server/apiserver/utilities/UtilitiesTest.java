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
import iudx.file.server.apiserver.validations.RequestValidator;
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
    query.put("timerel", "during")
        .put("time", "2020-09-15T00:00:00Z")
        .put("endTime", "2020-09-15T00:00:00Z");
    QueryParams queryParam = query.mapTo(QueryParams.class).build();
    assertEquals(QueryType.TEMPORAL, utilities.getQueryType(queryParam));
  }

  @Test
  public void testGeoTemporalQuery() {
    JsonObject query = new JsonObject();
    query.put("timerel", "during")
        .put("time", "2020-09-15T00:00:00Z")
        .put("endTime", "2020-09-15T00:00:00Z")
        .put("georel", "near;maxDistance=10")
        .put("coordinates", "[72.23,23.14]")
        .put("geometry", "point")
        .put("offset", 0)
        .put("limit", 10)
        .put("attrs","attr1,attr2")
        .put("q", "abc>123");
    QueryParams queryParam = query.mapTo(QueryParams.class).build();
    assertEquals(QueryType.TEMPORAL_GEO, utilities.getQueryType(queryParam));
  }

  @Test
  public void testGeoQuery() {
    JsonObject query = new JsonObject();
    query.put("georel", "near;maxDistance=10").put("coordinates", "[72.23,23.14]").put("geometry",
        "point");
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
  
  @Test
  public void testFileIdComponentsRL() {
    String id="datakaveri.org/04a15c9960ffda227e9546f3f46e629e1fe4132b/rs.iudx.org.in/pune-env-flood/FWR065/sample.txt";
    assertEquals(6, utilities.getFileIdComponents(id).length);
  }
  
  @Test
  public void testFileIdComponentsGL() {
    String id="suratmunicipal.org/6db486cb4f720e8585ba1f45a931c63c25dbbbda/rs.iudx.org.in/surat-itms-realtime-info/sample.txt";
    assertEquals(5, utilities.getFileIdComponents(id).length);
  }
  
}

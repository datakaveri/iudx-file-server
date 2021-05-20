package iudx.file.server.apiserver.utilities;

import io.vertx.core.json.JsonObject;
import iudx.file.server.common.QueryType;

public class Utilities {

  
  public static QueryType getQueryType(JsonObject query) {
    QueryType queryType = null;
    if ((query.containsKey("time") || query.containsKey("temporalrel")) &&
        (query.containsKey("georel") || query.containsKey("lat") || query.containsKey("lon"))) {
      queryType = QueryType.TEMPORAL_GEO;
    } else if (query.containsKey("time") || query.containsKey("temporalrel")) {
      queryType = QueryType.TEMPORAL;
    } else if (query.containsKey("georel") || query.containsKey("lat")
        || query.containsKey("lon") || query.containsKey("coordinates")) {
      queryType = QueryType.GEO;
    } else {
      queryType = QueryType.LIST;
    }
    return queryType;
  }
}

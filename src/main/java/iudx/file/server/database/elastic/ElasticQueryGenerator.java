package iudx.file.server.database.elastic;


import static iudx.file.server.database.utilities.Constants.*;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import io.vertx.core.json.JsonObject;
import iudx.file.server.common.QueryType;

public class ElasticQueryGenerator {

  private static QueryDecoder temporalQueryDecoder = new TemporalQueryDecoder();
  private static QueryDecoder geoQueryDecoder = new GeoQueryDecoder();
  private static QueryDecoder listQueryDecoder = new ListQueryDecoder();


  public String getQuery(JsonObject json, QueryType type) {
    BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
    if (QueryType.TEMPORAL_GEO.equals(type)) {
      boolQuery = temporalQueryDecoder.decode(boolQuery, json);
      boolQuery = geoQueryDecoder.decode(boolQuery, json);
    } else if (QueryType.TEMPORAL.equals(type)) {
      boolQuery = temporalQueryDecoder.decode(boolQuery, json);
    } else if (QueryType.GEO.equals(type)) {
      boolQuery = geoQueryDecoder.decode(boolQuery, json);
    } else if (QueryType.LIST.equals(type)) {
      boolQuery = listQueryDecoder.decode(boolQuery, json);
    }

    return boolQuery.toString();
  }

  // TODO : discuss if it will be better to include other filters.
  public String deleteQuery(String id) {

    JsonObject json = new JsonObject();
    JsonObject matchJson = new JsonObject();
    matchJson.put(FILE_ID, id);
    json.put("match", matchJson);

    return json.toString();
  }
}

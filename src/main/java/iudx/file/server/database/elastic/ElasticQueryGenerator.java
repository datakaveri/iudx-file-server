package iudx.file.server.database.elastic;


import static iudx.file.server.database.utilities.Constants.*;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import io.vertx.core.json.JsonObject;
import iudx.file.server.common.QueryType;

public class ElasticQueryGenerator {

  private static QueryParser temporalQueryParser = new TemporalQueryParser();
  private static QueryParser geoQueryParser = new GeoQueryParser();
  private static QueryParser listQueryParser = new ListQueryParser();


  public String getQuery(JsonObject json, QueryType type) {
    BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
    boolQuery.filter(QueryBuilders.termsQuery(ID, json.getString(ID)));
    if (QueryType.TEMPORAL_GEO.equals(type)) {
      boolQuery = temporalQueryParser.parse(boolQuery, json);
      boolQuery = geoQueryParser.parse(boolQuery, json);
    } else if (QueryType.TEMPORAL.equals(type)) {
      boolQuery = temporalQueryParser.parse(boolQuery, json);
    } else if (QueryType.GEO.equals(type)) {
      boolQuery = geoQueryParser.parse(boolQuery, json);
    } else if (QueryType.LIST.equals(type)) {
      // boolQuery = listQueryParser.parse(boolQuery, json);
      // since id is already added so don't add iof based filter again
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

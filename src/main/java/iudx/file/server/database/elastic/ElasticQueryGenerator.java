package iudx.file.server.database.elastic;


import static iudx.file.server.database.utilities.Constants.*;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import io.vertx.core.json.JsonObject;
import iudx.file.server.common.QueryType;

public class ElasticQueryGenerator {


  public String getQuery(JsonObject json, QueryType type) {
    String elasticQuery = null;
    if (QueryType.TEMPORAL.equals(type)) {
      elasticQuery = getTemporalQuery(json);
    } else if (QueryType.LIST.equals(type)) {
      elasticQuery = getListQuery(json);
    } else {
      System.out.println("unknown query type");
    }

    return elasticQuery;
  }

  // TODO : discuss if it will be better to include other filters.
  public String deleteQuery(String id) {

    JsonObject json = new JsonObject();
    JsonObject matchJson = new JsonObject();
    matchJson.put(FILE_ID, id);
    json.put("match", matchJson);

    return json.toString();
  }


  private String getTemporalQuery(JsonObject json) {
    BoolQueryBuilder boolQuery = QueryBuilders.boolQuery()
        .filter(QueryBuilders.termsQuery(ID, json.getString(ID)))
        .filter(QueryBuilders.rangeQuery(TIMERANGE_START_TIME).lte(json.getString(TIME)))
        .filter(QueryBuilders.rangeQuery(TIMERANGE_END_TIME).gte(json.getString(END_TIME)));

    return boolQuery.toString();
  }

  private String getListQuery(JsonObject json) {
    BoolQueryBuilder boolQuery = QueryBuilders.boolQuery()
        .filter(QueryBuilders.termQuery(ID , json.getString(ID)));

    return boolQuery.toString();
  }
}

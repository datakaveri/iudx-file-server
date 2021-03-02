package iudx.file.server.query;


import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import io.vertx.core.json.JsonObject;

public class ElasticQueryGenerator {

  
  public String getQuery(JsonObject json,QueryType type) {
    
    BoolQueryBuilder boolQuery=QueryBuilders.boolQuery()
        .filter(QueryBuilders.termsQuery("id", json.getString("id")))
        .filter(QueryBuilders.rangeQuery("timeRange.startTime").lte(json.getString("time")))
        .filter(QueryBuilders.rangeQuery("timeRange.endTime").gte(json.getString("endTime")));
    
    return boolQuery.toString();
  }
}

package iudx.file.server.database.elasticdb.elastic;

import static iudx.file.server.database.elasticdb.utilities.Constants.ID;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import io.vertx.core.json.JsonObject;

public class ListQueryParser implements QueryParser{

  @Override
  public BoolQueryBuilder parse(BoolQueryBuilder builder, JsonObject json) {
    builder
        .filter(QueryBuilders.termQuery(ID, json.getString(ID)));
    return builder;
  }

  
}

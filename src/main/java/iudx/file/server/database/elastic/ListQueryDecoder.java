package iudx.file.server.database.elastic;

import static iudx.file.server.database.utilities.Constants.ID;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import io.vertx.core.json.JsonObject;

public class ListQueryDecoder implements QueryDecoder{

  @Override
  public BoolQueryBuilder decode(BoolQueryBuilder builder, JsonObject json) {
    builder
        .filter(QueryBuilders.termQuery(ID, json.getString(ID)));
    return builder;
  }

  
}

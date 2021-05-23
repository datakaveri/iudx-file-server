package iudx.file.server.database.elastic;

import static iudx.file.server.database.utilities.Constants.*;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import io.vertx.core.json.JsonObject;

public class TemporalQueryDecoder implements QueryDecoder {


  @Override
  public BoolQueryBuilder decode(BoolQueryBuilder builder, JsonObject json) {
    builder
        .filter(QueryBuilders.termsQuery(ID, json.getString(ID)))
        .filter(QueryBuilders.rangeQuery(TIME_RANGE)
            .lte(json.getString(END_TIME))
            .gte(json.getString(TIME)));

    return builder;
  }

}

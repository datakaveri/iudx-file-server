package iudx.file.server.database.elasticdb.elastic;

import static iudx.file.server.database.elasticdb.utilities.Constants.*;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import io.vertx.core.json.JsonObject;

public class TemporalQueryParser implements QueryParser {


  @Override
  public BoolQueryBuilder parse(BoolQueryBuilder builder, JsonObject json) {
    builder
        .filter(QueryBuilders.rangeQuery(TIME_RANGE)
            .lte(json.getString(END_TIME))
            .gte(json.getString(TIME)));

    return builder;
  }

}

package iudx.file.server.database.elasticdb.elastic;

import org.elasticsearch.index.query.BoolQueryBuilder;
import io.vertx.core.json.JsonObject;

public interface QueryParser {

  BoolQueryBuilder parse(BoolQueryBuilder builder, JsonObject json);
}

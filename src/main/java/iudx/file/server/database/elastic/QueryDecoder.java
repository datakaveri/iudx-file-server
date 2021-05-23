package iudx.file.server.database.elastic;

import org.elasticsearch.index.query.BoolQueryBuilder;
import io.vertx.core.json.JsonObject;

public interface QueryDecoder {

  public BoolQueryBuilder decode(BoolQueryBuilder  builder,JsonObject json);
}

package iudx.file.server.database.elasticdb.elastic;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;

import java.util.List;
import java.util.Map;

public interface ElasticsearchQueryDecorator {
  Map<FilterType, List<Query>> add();
}

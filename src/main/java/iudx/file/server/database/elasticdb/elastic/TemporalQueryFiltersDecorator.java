package iudx.file.server.database.elasticdb.elastic;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.RangeQuery;
import io.vertx.core.json.JsonObject;

import java.util.List;
import java.util.Map;

import static iudx.file.server.database.elasticdb.utilities.Constants.*;

public class TemporalQueryFiltersDecorator implements ElasticsearchQueryDecorator {
  private Map<FilterType, List<Query>> queryFilters;
  private JsonObject requestQuery;

  public TemporalQueryFiltersDecorator(
      Map<FilterType, List<Query>> queryFilters, JsonObject requestQuery) {
    this.queryFilters = queryFilters;
    this.requestQuery = requestQuery;
  }

  @Override
  public Map<FilterType, List<Query>> add() {
    final String time = requestQuery.getString(TIME);
    final String endTime = requestQuery.getString(END_TIME);
    Query temporalQuery = RangeQuery.of(r -> r.field(TIME_RANGE).from(time).to(endTime))._toQuery();

    List<Query> queryList = queryFilters.get(FilterType.FILTER);
    queryList.add(temporalQuery);
    return queryFilters;
  }
}

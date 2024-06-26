package iudx.file.server.database.elasticdb.elastic;

import static iudx.file.server.database.elasticdb.utilities.Constants.*;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.RangeQuery;
import io.vertx.core.json.JsonObject;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TemporalQueryFiltersDecorator implements ElasticsearchQueryDecorator {
  private Map<FilterType, List<Query>> queryFilters;
  private JsonObject requestQuery;
  private static final Logger LOGGER = LogManager.getLogger(TemporalQueryFiltersDecorator.class);


  public TemporalQueryFiltersDecorator(
      Map<FilterType, List<Query>> queryFilters, JsonObject requestQuery) {
    this.queryFilters = queryFilters;
    this.requestQuery = requestQuery;
  }

  @Override
  public Map<FilterType, List<Query>> add() {
    final String time = requestQuery.getString(TIME);
    final String endTime = requestQuery.getString(END_TIME);
    LOGGER.trace(time);
    LOGGER.trace(endTime);
    Query temporalQuery = RangeQuery.of(r -> r.field(TIME_RANGE).from(time).to(endTime))._toQuery();
    LOGGER.trace(temporalQuery);
    List<Query> queryList = queryFilters.get(FilterType.FILTER);
    queryList.add(temporalQuery);
    LOGGER.trace(queryList);
    LOGGER.trace(queryFilters);
    return queryFilters;
  }
}

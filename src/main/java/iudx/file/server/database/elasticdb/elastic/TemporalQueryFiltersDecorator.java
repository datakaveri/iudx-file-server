package iudx.file.server.database.elasticdb.elastic;

import static com.hazelcast.internal.diagnostics.DiagnosticsOutputType.LOGGER;
import static iudx.file.server.database.elasticdb.utilities.Constants.*;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.RangeQuery;

import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonObject;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;


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
    LOGGER.trace(time); //2020-09-10T00:00:00Z
    LOGGER.trace(endTime); //2020-09-15T00:00:00Z
    Query temporalQuery = RangeQuery.of(r -> r.field(TIME_RANGE).from(time).to(endTime))._toQuery();
    LOGGER.trace(temporalQuery); //Query: {"range":{"timeRange":{"from":"2020-09-10T00:00:00Z","to":"2020-09-15T00:00:00Z"}}}
    List<Query> queryList = queryFilters.get(FilterType.FILTER);
    queryList.add(temporalQuery);
    LOGGER.trace(queryList); //[Query: {"terms":{"id":["b58da193-23d9-43eb-b98a-a103d4b6103c"]}}, Query: {"range":{"timeRange":{"from":"2020-09-10T00:00:00Z","to":"2020-09-15T00:00:00Z"}}}]
    LOGGER.trace(queryFilters); //{MUST=[], MUST_NOT=[], FILTER=[Query: {"terms":{"id":"b58da193-23d9-43eb-b98a-a103d4b6103c"]}}, Query: {"range":{"timeRange":{"from":"2020-09-10T00:00:00Z","to":"2020-09-15T00:00:00Z"}}}], SHOULD=[]}
    return queryFilters;
  }
}

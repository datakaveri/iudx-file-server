package iudx.file.server.database.elasticdb.elastic;

import static iudx.file.server.database.elasticdb.utilities.Constants.*;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.search.SourceConfig;
import co.elastic.clients.elasticsearch.core.search.SourceFilter;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import iudx.file.server.common.QueryType;

import java.util.*;

import iudx.file.server.database.elasticdb.elastic.exception.EsqueryException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ElasticQueryGenerator {
  private static final Logger LOGGER = LogManager.getLogger(ElasticQueryGenerator.class);
  Map<FilterType, List<Query>> queryLists = new HashMap<>();
  ElasticsearchQueryDecorator queryDecorator = null;

  public Query getQuery(JsonObject json, QueryType type) {

    for (FilterType filterType : FilterType.values()) {
      queryLists.put(filterType, new ArrayList<Query>());
    }
    FieldValue field = FieldValue.of(json.getString(ID));
    TermsQueryField termQueryField = TermsQueryField.of(e -> e.value(List.of(field)));
    Query idTermsQuery = TermsQuery.of(query -> query.field("id").terms(termQueryField))._toQuery();



    queryLists.get(FilterType.FILTER).add(idTermsQuery);


    if (QueryType.TEMPORAL_GEO.equals(type)) {
      queryDecorator = new TemporalQueryFiltersDecorator(queryLists, json);
      queryDecorator.add();
      queryDecorator = new GeoQueryFiltersDecorator(queryLists, json);
      queryDecorator.add();
    } else if (QueryType.TEMPORAL.equals(type)) {
      queryDecorator = new TemporalQueryFiltersDecorator(queryLists, json);
      queryDecorator.add();
    } else if (QueryType.GEO.equals(type)) {
      queryDecorator = new GeoQueryFiltersDecorator(queryLists, json);
      queryDecorator.add();
    }
    Query q = getBoolQuery(queryLists);

    return q;
  }

  public Query deleteQuery(String id) {
    Query deleteQuery = MatchQuery.of(e -> e.field(FILE_ID).query(id))._toQuery();
    return deleteQuery;
  }


  private static SourceConfig getSourceFilter(List<String> sourceFilterList) {
    SourceFilter sourceFilter = SourceFilter.of(f -> f.includes(sourceFilterList));
    SourceConfig sourceFilteringFields = SourceConfig.of(c -> c.filter(sourceFilter));
    return sourceFilteringFields;
  }

  private Query getBoolQuery(Map<FilterType, List<Query>> filterQueries) {
    BoolQuery.Builder boolQuery = new BoolQuery.Builder();

    for (Map.Entry<FilterType, List<Query>> entry : filterQueries.entrySet()) {
      if (FilterType.FILTER.equals(entry.getKey())
          && filterQueries.get(FilterType.FILTER).size() > 0) {
        boolQuery.filter(filterQueries.get(FilterType.FILTER));
      }

      if (FilterType.MUST_NOT.equals(entry.getKey())
          && filterQueries.get(FilterType.MUST_NOT).size() > 0) {
        boolQuery.mustNot(filterQueries.get(FilterType.MUST_NOT));
      }

      if (FilterType.MUST.equals(entry.getKey()) && filterQueries.get(FilterType.MUST).size() > 0) {
        boolQuery.must(filterQueries.get(FilterType.MUST));
      }

      if (FilterType.SHOULD.equals(entry.getKey())
          && filterQueries.get(FilterType.SHOULD).size() > 0) {
        boolQuery.should(filterQueries.get(FilterType.SHOULD));
      }
    }

//
    return boolQuery.build()._toQuery();

  }


}

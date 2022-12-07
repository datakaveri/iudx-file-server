package iudx.file.server.database.elasticdb.elastic;


import static iudx.file.server.database.elasticdb.utilities.Constants.*;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.search.SourceConfig;
import co.elastic.clients.elasticsearch.core.search.SourceFilter;
import co.elastic.clients.json.JsonData;
import io.vertx.core.json.JsonArray;
import iudx.file.server.database.elasticdb.elastic.exception.ESQueryException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


import io.vertx.core.json.JsonObject;
import iudx.file.server.common.QueryType;

import java.util.*;

public class ElasticQueryGenerator {

  //private static QueryParser temporalQueryParser = new TemporalQueryParser();
 // private static QueryParser geoQueryParser = new GeoQueryParser();
  private static final Logger LOGGER = LogManager.getLogger(ElasticQueryGenerator.class);
  Map<FilterType, List<Query>> queryLists = new HashMap<>();



    ElasticsearchQueryDecorator queryDecorator/*,queryDecorator1 */= null;

  public Query getQuery(JsonObject json, QueryType type) {

      for (FilterType filterType : FilterType.values()) {
          queryLists.put(filterType, new ArrayList<Query>());
      }

    LOGGER.debug("Json in EQG == " + json);

      FieldValue field = FieldValue.of(json.getString(ID));

      TermsQueryField termQueryField = TermsQueryField.of(e -> e.value(List.of(field)));
      Query idTermsQuery = TermsQuery.of(query -> query.field("id").terms(termQueryField))._toQuery();
      //LOGGER.debug("idTerm in EQG == " + idTermsQuery);
      queryLists.get(FilterType.FILTER).add(idTermsQuery);

      //LOGGER.info("query  from elastic line at 39" + json + " " +type);
    if (QueryType.TEMPORAL_GEO.equals(type)) {
      //boolQuery = temporalQueryParser.parse(boolQuery, json);
      queryDecorator = new TemporalQueryFiltersDecorator(queryLists, json);

      queryDecorator.add();

      //boolQuery = geoQueryParser.parse(boolQuery, json);
      queryDecorator = new GeoQueryFiltersDecorator(queryLists, json);

      queryDecorator.add();
    } else if (QueryType.TEMPORAL.equals(type)) {
      //boolQuery = temporalQueryParser.parse(boolQuery, json);
        queryDecorator = new TemporalQueryFiltersDecorator(queryLists, json);
        queryDecorator.add();
    } else if (QueryType.GEO.equals(type)) {
      //boolQuery = geoQueryParser.parse(boolQuery, json);
      queryDecorator = new GeoQueryFiltersDecorator(queryLists, json);

      queryDecorator.add();
    }
      Query q = getBoolQuery(queryLists);
      LOGGER.info("query from elastic: {}", q.toString());

    return q;
  }

  // TODO : discuss if it will be better to include other filters.
  public String deleteQuery(String id) {

    JsonObject json = new JsonObject();
    JsonObject matchJson = new JsonObject();
    matchJson.put(FILE_ID, id);
    json.put("match", matchJson);

    return json.toString();
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

        return boolQuery.build()._toQuery();

    }


   /* public SourceConfig getSourceConfigFilters(JsonObject queryJson) {
        String searchType = queryJson.getString(SEARCH_TYPE);

        if (!searchType.matches(RESPONSE_FILTER_REGEX)) {
            return getSourceFilter(Collections.emptyList());
        }

        JsonArray responseFilteringFileds = queryJson.getJsonArray(RESPONSE_ATTRS);
        if (responseFilteringFileds == null) {
            LOGGER.error("response filtering fields are not passed in attrs parameter");
            throw new ESQueryException(
                    "response filtering fields are not passed in attrs parameter");
        }

        return getSourceFilter(responseFilteringFileds.getList());

    }*/

   /* private SourceConfig getSourceFilter(List<String> sourceFilterList) {
        SourceFilter sourceFilter = SourceFilter.of(f -> f.includes(sourceFilterList));
        SourceConfig sourceFilteringFields = SourceConfig.of(c -> c.filter(sourceFilter));
        return sourceFilteringFields;
    }*/
}

package iudx.file.server.database.elasticdb.elastic;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.WrapperQuery;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Base64;
import java.util.List;
import java.util.Map;

import static iudx.file.server.common.Constants.GEOM_POINT;
import static iudx.file.server.database.elasticdb.utilities.Constants.GEOMETRY;
import static iudx.file.server.database.elasticdb.utilities.Constants.CIRCLE;
import static iudx.file.server.database.elasticdb.utilities.Constants.NEAR;
import static iudx.file.server.database.elasticdb.utilities.Constants.WITHIN;
import static iudx.file.server.database.elasticdb.utilities.Constants.GEO_REL;
import static iudx.file.server.database.elasticdb.utilities.Constants.LOCATION;
import static iudx.file.server.database.elasticdb.utilities.Constants.RADIUS;
import static iudx.file.server.database.elasticdb.utilities.Constants.UNIT_METERS;
import static iudx.file.server.database.elasticdb.utilities.Constants.COORDINATES;
import static iudx.file.server.database.elasticdb.utilities.Constants.TYPE_KEY;

public class GeoQueryFiltersDecorator implements ElasticsearchQueryDecorator {
  private static final Logger LOGGER = LogManager.getLogger(GeoQueryFiltersDecorator.class);
  private Map<FilterType, List<Query>> queryFilters;
  private JsonObject requestQuery;
  private String geoQuery =
      "{ \"geo_shape\": { \"%s\": { \"shape\": %s, \"relation\": \"%s\" } } }";

  public GeoQueryFiltersDecorator(
      Map<FilterType, List<Query>> queryFilters, JsonObject requestQuery) {
    this.queryFilters = queryFilters;
    this.requestQuery = requestQuery;
  }

  @Override
  public Map<FilterType, List<Query>> add() {
    Query geoWrapperQuery;
    LOGGER.debug("parsing geo query  paramaters");
    String shapeRelation =
        NEAR.equalsIgnoreCase(requestQuery.getString(GEO_REL))
            ? WITHIN
            : requestQuery.getString(GEO_REL);
    String query = String.format(geoQuery, LOCATION, getGeoJson(requestQuery), shapeRelation);
    String encodedString = Base64.getEncoder().encodeToString(query.getBytes());
    geoWrapperQuery = WrapperQuery.of(w -> w.query(encodedString))._toQuery();
    List<Query> queryList = queryFilters.get(FilterType.FILTER);

    queryList.add(geoWrapperQuery);
    return queryFilters;
  }

  private JsonObject getGeoJson(JsonObject json) {
    JsonObject geoJson = new JsonObject();
    String geom;
    if (GEOM_POINT.equalsIgnoreCase(json.getString(GEOMETRY))) {
      geom = CIRCLE;
      geoJson.put(RADIUS, json.getString(RADIUS) + UNIT_METERS);
    } else {
      geom = json.getString(GEOMETRY);
    }
    geoJson.put(TYPE_KEY, geom).put(COORDINATES, new JsonArray(json.getString(COORDINATES)));
    LOGGER.debug(geoJson);
    return geoJson;
  }
}

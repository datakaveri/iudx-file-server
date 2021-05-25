package iudx.file.server.database.elastic;

import static iudx.file.server.database.utilities.Constants.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.common.geo.ShapeRelation;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class GeoQueryParser implements QueryParser {

  private static final Logger LOGGER = LogManager.getLogger(GeoQueryParser.class);

  @Override
  public BoolQueryBuilder parse(BoolQueryBuilder builder, JsonObject json) {
    LOGGER.debug("parsing geo query  paramaters");
    String shapeRelation =
        NEAR.equalsIgnoreCase(json.getString(GEO_REL)) ? WITHIN : json.getString(GEO_REL);
    builder.filter(QueryBuilders.wrapperQuery(String
        .format("{ \"geo_shape\": { \"%s\": { \"shape\": %s, \"relation\": \"%s\" } } }",
            LOCATION,
            getGeoJson(json),
            ShapeRelation.getRelationByName(shapeRelation).getRelationName())));
    return builder;
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
    geoJson.put(TYPE_KEY, geom)
        .put(COORDINATES, new JsonArray(json.getString(COORDINATES)));
    return geoJson;
  }

}


package iudx.file.server.database.elastic;

import static iudx.file.server.database.utilities.Constants.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.common.geo.ShapeRelation;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class GeoQueryDecoder implements QueryDecoder {

  private static final Logger LOGGER = LogManager.getLogger(GeoQueryDecoder.class);

  @Override
  public BoolQueryBuilder decode(BoolQueryBuilder builder, JsonObject json) {
    LOGGER.debug("builder : " + builder + " json : " + json);
    String shapeRelation =
        "near".equalsIgnoreCase(json.getString("georel")) ? "within" : json.getString("georel");
    builder.filter(QueryBuilders.wrapperQuery(String
        .format("{ \"geo_shape\": { \"%s\": { \"shape\": %s, \"relation\": \"%s\" } } }",
            "location",
            getGeoJson(json),
            ShapeRelation.getRelationByName(shapeRelation).getRelationName())));
    LOGGER.debug("geo shape builder : "+builder);
    return builder;
  }

  private JsonObject getGeoJson(JsonObject json) {
    JsonObject geoJson = new JsonObject();
    String geom = GEOM_POINT.equalsIgnoreCase(json.getString("geometry")) ? "Circle"
        : json.getString("geometry");
    if (GEOM_POINT.equalsIgnoreCase(json.getString("geometry"))) {
      geoJson.put("radius", json.getString("radius") + "m");
    }
    geoJson.put("type", geom)
        .put("coordinates", new JsonArray(json.getString("coordinates")));
    LOGGER.debug("geo-json  :"+geoJson);
    return geoJson;
  }

}


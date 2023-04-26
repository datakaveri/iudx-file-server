package iudx.file.server.apiserver.query;

import static iudx.file.server.common.Constants.GEOM_POINT;
import static iudx.file.server.common.Constants.JSON_NEAR;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * The QueryParams.
 *
 * <h1>QueryParams</h1>
 *
 * <p>The QueryParams class hepls to build query
 */
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class QueryParams {

  @JsonProperty("id")
  private String id;

  @JsonProperty("q")
  private String textQuery;

  @JsonProperty("georel")
  private String geoRel;

  @JsonProperty("maxdistance")
  private String maxDistance;

  @JsonProperty("geometry")
  private String geometry;

  @JsonProperty("coordinates")
  private String coordinates;

  @JsonProperty("geoproperty")
  private String geoProperty;

  @JsonProperty("timerel")
  private String temporalRelation;

  @JsonProperty("time")
  private String startTime;

  @JsonProperty("endTime")
  private String endTime;

  @JsonProperty("lat")
  private Double lat;

  @JsonProperty("lon")
  private Double lon;

  @JsonProperty("radius")
  private Double radius;

  @JsonProperty("offset")
  private Integer size;

  @JsonProperty("limit")
  private Integer from;

  private QueryParams() {
    super();
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getTextQuery() {
    return textQuery;
  }

  public void setTextQuery(String textQuery) {
    this.textQuery = textQuery;
  }

  public String getMaxDistance() {
    return maxDistance;
  }

  public void setMaxDistance(String maxDistance) {
    this.maxDistance = maxDistance;
  }

  public String getGeometry() {
    return geometry;
  }

  public void setGeometry(String geometry) {
    this.geometry = geometry;
  }

  public String getCoordinates() {
    return coordinates;
  }

  public void setCoordinates(String coordinates) {
    this.coordinates = coordinates;
  }

  public String getGeoProperty() {
    return geoProperty;
  }

  public void setGeoProperty(String geoProperty) {
    this.geoProperty = geoProperty;
  }

  public String getTemporalRelation() {
    return temporalRelation;
  }

  public void setTemporalRelation(String temporalRelation) {
    this.temporalRelation = temporalRelation;
  }

  public String getStartTime() {
    return startTime;
  }

  public void setStartTime(String startTime) {
    this.startTime = startTime;
  }

  public String getEndTime() {
    return endTime;
  }

  public void setEndTime(String endTime) {
    this.endTime = endTime;
  }

  public Double getLat() {
    return lat;
  }

  public void setLat(Double lat) {
    this.lat = lat;
  }

  public Double getLon() {
    return lon;
  }

  public void setLon(Double lon) {
    this.lon = lon;
  }

  public Double getRadius() {
    return radius;
  }

  public void setRadius(Double radius) {
    this.radius = radius;
  }

  public Integer getSize() {
    return size;
  }

  public void setSize(Integer size) {
    this.size = size;
  }

  public Integer getFrom() {
    return from;
  }

  public void setFrom(Integer from) {
    this.from = from;
  }

  /**
   * set georel.
   *
   * @param geoRel a geo rel data
   */
  public void setGeoRel(String geoRel) {
    String[] relation = geoRel.split(";");
    if (relation.length == 2) {
      String[] distance = relation[1].split("=");
      this.setMaxDistance(distance[1]);
    }
    this.geoRel = relation[0];
  }

  @JsonIgnore
  public boolean isGeoParamsPresent() {
    return this.geoRel != null && this.coordinates != null && this.geometry != null;
  }

  @JsonIgnore
  public boolean isTemporalParamsPresent() {
    return this.temporalRelation != null && this.startTime != null;
  }

  /** build QueryParams. */
  public QueryParams build() {
    if (isGeoParamsPresent()) {
      if (this.geoRel != null && this.coordinates != null && this.geometry != null) {
        if (this.geometry.equalsIgnoreCase(GEOM_POINT) && this.geoRel.equalsIgnoreCase(JSON_NEAR)) {
          String[] coords = this.coordinates.replaceAll("\\[|\\]", "").split(",");

          this.setLat(Double.parseDouble(coords[0]));
          this.setLon(Double.parseDouble(coords[1]));
          this.setRadius(Double.parseDouble(this.maxDistance));
        }
      }
    }
    return this;
  }
}

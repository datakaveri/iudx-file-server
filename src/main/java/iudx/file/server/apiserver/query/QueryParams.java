package iudx.file.server.apiserver.query;

import static iudx.file.server.common.Constants.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

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



  public void setSize(Integer size) {
    this.size = size;
  }

  public void setFrom(Integer from) {
    this.from = from;
  }

  private QueryParams() {
    super();
  }

  public void setId(String id) {
    this.id = id;
  }

  public void setTextQuery(String textQuery) {
    this.textQuery = textQuery;
  }

  public void setGeoRel(String geoRel) {
    String[] relation = geoRel.split(";");
    if (relation.length == 2) {
      String[] distance = relation[1].split("=");
      this.setMaxDistance(distance[1]);
    }
    this.geoRel = relation[0];
  }

  public void setMaxDistance(String maxDistance) {
    this.maxDistance = maxDistance;
  }

  public void setStartTime(String startTime) {
    this.startTime = startTime;
  }

  public void setEndTime(String endTime) {
    this.endTime = endTime;
  }

  public void setLat(Double lat) {
    this.lat = lat;
  }

  public void setLon(Double lon) {
    this.lon = lon;
  }

  public void setRadius(Double radius) {
    this.radius = radius;
  }

  public void setGeometry(String geometry) {
    this.geometry = geometry.toLowerCase();
  }

  public void setCoordinates(String coordinates) {
    this.coordinates = coordinates;
  }

  public void setGeoProperty(String geoProperty) {
    this.geoProperty = geoProperty;
  }

  public void setTemporalRelation(String temporalRelation) {
    this.temporalRelation = temporalRelation;
  }

  @JsonIgnore
  public boolean isGeoParamsPresent() {
    return this.geoRel != null
        && this.coordinates != null
        && this.geometry != null;
  }


  @JsonIgnore
  public boolean isTemporalParamsPresent() {
    return this.temporalRelation != null
        && this.startTime != null;
  }

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

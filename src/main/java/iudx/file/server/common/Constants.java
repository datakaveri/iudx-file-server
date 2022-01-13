package iudx.file.server.common;

public class Constants {
  public static final String DB_SERVICE_ADDRESS = "iudx.file.database.service";
  public static final String AUTH_SERVICE_ADDRESS = "iudx.file.auth.service";
  public static final String AUDIT_SERVICE_ADDRESS = "iudx.file.auditing.service";
  public static final String PG_SERVICE_ADDRESS = "iudx.file.pgsql.service";
  public static final String CACHE_SERVICE_ADDRESS = "iudx.file.cache.service";
  public static final String DATABROKER_SERVICE_ADDRESS = "iudx.file.databroker.address";

  public static final String GEOM_POINT = "point";
  public static final String GEOM_POLYGON = "polygon";
  public static final String GEOM_LINE = "linestring";
  public static final String GEOM_BBOX = "bbox";

  public static final String JSON_NEAR = "near";
  public static final String JSON_DURING = "during";
  public static final String JSON_TIME = "time";
  public static final String JSON_ENDTIME = "endtime";
  public static final String JSON_TIMEREL = "timerel";

  public static final long CACHE_TIMEOUT_AMOUNT = 30;
  public static final String CAT_RSG_PATH = "/iudx/cat/v1/search";
  public static final String CAT_ITEM_PATH = "/iudx/cat/v1/item";


  // pagination parameters
  public static final String PARAM_OFFSET = "offset";
  public static final String PARAM_LIMIT = "limit";
}

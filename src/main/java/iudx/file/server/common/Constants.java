package iudx.file.server.common;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Constants.
 *
 * <h1>Constants</h1>
 *
 */

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
  //  public static final String CAT_RSG_PATH = "/iudx/cat/v1/search";
  public static final String CAT_SEARCH_PATH = "/search";

  public static final String CAT_ITEM_PATH = "/item";
  public static final String AUTH_CERTIFICATE_PATH = "/cert";

  // pagination parameters
  public static final String PARAM_OFFSET = "offset";
  public static final String PARAM_LIMIT = "limit";

  // RMQ queues and exchanges.
  public static String INVALID_SUB_Q = "fs-invalid-sub";
  public static final String ITEM_TYPE_RESOURCE = "Resource";
  public static final String ITEM_TYPE_RESOURCE_GROUP = "ResourceGroup";
  public static final String ITEM_TYPE_RESOURCE_SERVER = "ResourceServer";
  public static final String ITEM_TYPE_PROVIDER = "Provider";
  public static final ArrayList<String> ITEM_TYPES =
      new ArrayList<String>(
          Arrays.asList(
              ITEM_TYPE_RESOURCE,
              ITEM_TYPE_RESOURCE_GROUP,
              ITEM_TYPE_RESOURCE_SERVER,
              ITEM_TYPE_PROVIDER));
  public static final int JWT_LEEWAY_TIME = 30;

}

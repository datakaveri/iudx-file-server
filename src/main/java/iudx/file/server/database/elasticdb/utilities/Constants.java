package iudx.file.server.database.elasticdb.utilities;

public class Constants extends iudx.file.server.common.Constants {

  public static final String ERROR = "Error";
  public static final String COUNT = "count";
  public static final String DETAIL = "details";
  public static final String ERROR_TYPE = "type";
  public static final String FAILED = "Failed";
  public static final String INDEX_NOT_FOUND = "index_not_found_exception";
  public static final String INVALID_RESOURCE_ID = "Invalid resource id";
  public static final String RESULTS = "results";
  public static final String ROOT_CAUSE = "root_cause";
  public static final String REASON = "reason";
  public static final String SUCCESS = "Success";
  public static final String TITLE = "title";
  public static final String TYPE_KEY = "type";
  public static final String STATUS = "status";
  public static final String HITS = "hits";
  public static final String DOCS_KEY = "docs";
  public static final String EMPTY_RESPONSE = "Empty response";
  public static final String BAD_PARAMETERS = "Bad parameters";
  public static final String SOURCE_FILTER_KEY = "_source";
  public static final String FILTER_PATH = "filter_path";
  public static final String FILTER_PATH_VAL = "took,hits.hits._source,hits.total.value";
  public static final String SEARCH_REQ_PARAM = "/_search";
  public static final String COUNT_REQ_PARAM = "/_count";
  public static final String ID = "id";
  public static final String FILE_ID="fileId";
  public static final String TIME_RANGE="timeRange";
  public static final String TIME_RANGE_STARTTIME="startTime";
  public static final String TIME_RANGE_ENDTIME="endTime";
  public static final String TIME="time";
  public static final String END_TIME="endTime";
  public static final String LOCATION="location";
  public static final String RADIUS="radius";
  public static final String COORDINATES="coordinates";
  public static final String GEO_REL="georel";
  public static final String NEAR="near";
  public static final String WITHIN="within";
  public static final String CIRCLE="Circle";
  public static final String GEOMETRY="geometry";
  public static final String UNIT_METERS="m";
  
  
  //pagination default values
  public static final String FROM_KEY="from";
  public static final String SIZE_KEY="size";
  public static final int DEFAULT_SIZE_VALUE=5000;
  public static final int DEFAULT_FROM_VALUE=0;
  public static final String TOTAL_HITS_KEY="totalHits";
  
  public static final String DB_ERROR_2XX = "Status code is not 2xx";

  // SQL
  public static final String SQL_DELETE = "DELETE FROM file_server_token s where file_token = $1";
  public static final String SQL_INSERT =
      "INSERT INTO file_server_token (user_token, file_token, validity_date, server_id ) VALUES ($1, $2, $3,$4)";
  public static final String SQL_SELECT = "SELECT * FROM file_server_token WHERE user_token = $1";

  public static final String TERM_QUERY = "{\"term\":{\"$1\":\"$2\"}}";
  public static final String MUST_RANGE_QUERY =
      "{\"must\":[{\"range\":{\"startTime\":{\"lte\":\"$1\"}}},{\"range\":{\"endTime\":{\"gte\":\"$2\"}}}]}";
}

package iudx.file.server.utilities;

public class Constants {
  public static final long MAX_SIZE = 1073741824L; // 1GB = 1073741824 Bytes
  // api
  public static final String API_TEMPORAL = "/ngsi-ld/v1/temporal/entities";
  public static final String API_FILE_UPLOAD = "/iudx/v1/upload";
  public static final String API_FILE_DOWNLOAD = "/iudx/v1/download";
  public static final String API_FILE_DELETE = "/iudx/v1/delete";
  public static final String API_TOKEN = "/token";


  // header
  public static final String HEADER_TOKEN = "token";


  public static final String CONTENT_TYPE = "content-type";
  public static final String APPLICATION_JSON = "application/json";

  // json fields
  public static final String JSON_TYPE = "type";
  public static final String JSON_TITLE = "title";
  public static final String JSON_DETAIL = "detail";

  // SQL
  public static final String SQL_DELETE = "DELETE FROM file_server_token s where file_token = $1";
  public static final String SQL_INSERT =
      "INSERT INTO file_server_token (user_token, file_token, validity_date, server_id ) VALUES ($1, $2, $3,$4)";
  public static final String SQL_SELECT = "SELECT * FROM file_server_token WHERE user_token = $1";

  // cache
  public static final long CACHE_TIMEOUT = 30;


  // Form/Query Params
  public static final String PARAM_FILE = "file";
  public static final String PARAM_ID = "id";
  public static final String PARAM_SAMPLE = "isSample";
  public static final String PARAM_START_TIME = "startTime";
  public static final String PARAM_END_TIME = "endTime";
  public static final String PARAM_FILE_ID = "file-id";
  public static final String PARAM_TIME_REL = "timerel";

  public static final String API_ENDPOINT = "apiEndpoint";
  public static final String API_METHOD = "method";


  public static final String TERM_QUERY = "{\"term\":{\"$1\":\"$2\"}}";
  public static final String MUST_RANGE_QUERY =
      "{\"must\":[{\"range\":{\"startTime\":{\"lte\":\"$1\"}}},{\"range\":{\"endTime\":{\"gte\":\"$2\"}}}]}";

  public static final String ERROR = "Error";
  public static final String COUNT = "count";
  public static final String DETAIL = "detail";
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
  public static final String FILTER_PATH_VAL = "took,hits.hits._source";
  public static final String SEARCH_REQ_PARAM = "/_search";
  public static final String ID = "id";


}

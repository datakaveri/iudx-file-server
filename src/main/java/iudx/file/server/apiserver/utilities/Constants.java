package iudx.file.server.apiserver.utilities;

public class Constants extends iudx.file.server.common.Constants{
  public static final long MAX_SIZE = 1073741824L; // 1GB = 1073741824 Bytes
  // api
  public static final String API_TEMPORAL = "/ngsi-ld/v1/temporal/entities";
  public static final String API_SPATIAL="/ngsi-ld/v1/entities";
  public static final String API_FILE_UPLOAD = "/iudx/v1/upload";
  public static final String API_FILE_DOWNLOAD = "/iudx/v1/download";
  public static final String API_FILE_DELETE = "/iudx/v1/delete";
  public static final String API_LIST_METADATA = "/iudx/v1/list";
  public static final String API_APIS="/apis";
  public static final String API_API_SPECS="/apis/spec";


  // header
  public static final String HEADER_TOKEN = "token";
  public static final String HEADER_HOST = "Host";
  public static final String HEADER_ACCEPT = "Accept";
  public static final String HEADER_CONTENT_LENGTH = "Content-Length";
  public static final String HEADER_CONTENT_TYPE = "Content-Type";
  public static final String HEADER_ORIGIN = "Origin";
  public static final String HEADER_REFERER = "Referer";
  public static final String HEADER_ALLOW_ORIGIN = "Access-Control-Allow-Origin";
  public static final String HEADER_OPTIONS = "options";
  public static final String HEADER_EXTERNAL_STORAGE = "externalStorage";


  public static final String CONTENT_TYPE = "content-type";
  public static final String APPLICATION_JSON = "application/json";
  public static final String APPLICATION_HTML="text/html";

  // json fields
  public static final String JSON_TYPE = "type";
  public static final String JSON_TITLE = "title";
  public static final String JSON_DETAIL = "detail";
  public static final String ERROR_MESSAGE = "errorMessage";


  // Form/Query Params
  public static final String PARAM_FILE = "file";
  public static final String PARAM_ID = "id";
  public static final String PARAM_SAMPLE = "isSample";
  public static final String PARAM_START_TIME = "startTime";
  public static final String PARAM_TIME = "time";
  public static final String PARAM_END_TIME = "endTime";
  public static final String PARAM_END_TIME_LOWERCASE = "endtime";
  public static final String PARAM_FILE_ID = "file-id";
  public static final String PARAM_TIME_REL = "timerel";
  public static final String PARAM_GEOREL="georel";
  public static final String PARAM_GEOMETRY="geometry";
  public static final String PARAM_COORDINATES="coordinates";
  public static final String PARAM_GEOPROPERTY="geoproperty";
  public static final String PARAM_FILE_URL="file-download-url";

   public static final String MSG_BAD_QUERY = "Bad query";

  public static final String API_ENDPOINT = "apiEndpoint";
  public static final String API_METHOD = "method";
  
  public static final int VALIDATION_ALLOWED_COORDINATES=10;
  public static final int VALIDATION_COORDINATE_PRECISION_ALLOWED=6;

  public static final String RESPONSE_SIZE = "response_size";
}

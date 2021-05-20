package iudx.file.server.apiserver.utilities;

public class Constants extends iudx.file.server.common.Constants{
  public static final long MAX_SIZE = 1073741824L; // 1GB = 1073741824 Bytes
  // api
  public static final String API_TEMPORAL = "/ngsi-ld/v1/temporal/entities";
  public static final String API_FILE_UPLOAD = "/iudx/v1/upload";
  public static final String API_FILE_DOWNLOAD = "/iudx/v1/download";
  public static final String API_FILE_DELETE = "/iudx/v1/delete";
  public static final String API_LIST_METADATA = "/iudx/v1/list";


  // header
  public static final String HEADER_TOKEN = "token";


  public static final String CONTENT_TYPE = "content-type";
  public static final String APPLICATION_JSON = "application/json";

  // json fields
  public static final String JSON_TYPE = "type";
  public static final String JSON_TITLE = "title";
  public static final String JSON_DETAIL = "detail";


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


}

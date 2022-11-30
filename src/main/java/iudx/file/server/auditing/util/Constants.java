package iudx.file.server.auditing.util;

public class Constants {
  public static final String ID = "id";
  /* Errors */
  public static final String SUCCESS = "Success";
  public static final String FAILED = "Failed";
  public static final String DETAIL = "detail";
  public static final String ERROR_TYPE = "type";
  public static final String TITLE = "title";
  public static final String RESULTS = "results";
  public static final String MESSAGE = "message";

  /* Database */
  public static final String ERROR = "Error";
  public static final String QUERY_KEY = "query";
  public static final String DATA_NOT_FOUND="Required Data not Found";

  /*Auditing Service Constants*/
  public static final String USER_ID = "userID";
  public static final String API = "api";
  public static final String RESOURCE_ID = "resourceID";
  public static final String PROVIDER_ID = "providerID";

  public static final String RESPONSE_SIZE = "response_size";

  public static final String  DATABASE_TABLE_NAME = "databaseTableName";
  public static final String WRITE_QUERY =
          "INSERT INTO $0 (id,api,userid,epochtime,resourceid,isotime,providerid,size) VALUES ('$1','$2','$3',$4,'$5','$6','$7',$8)";
  public static final String ORIGIN_SERVER = "file-server";
  public static final String ORIGIN = "origin";
  public static final String PRIMARY_KEY= "primaryKey";
  public static final String EPOCH_TIME = "epochTime";
  public static final String ISO_TIME = "isoTime";
  public static final String EXCHANGE_NAME = "auditing";
  public static final String ROUTING_KEY = "##";

}

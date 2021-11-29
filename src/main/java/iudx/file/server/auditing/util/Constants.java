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
  public static final String EMPTY_RESPONSE = "Empty response";

  /* Database */
  public static final String ERROR = "Error";
  public static final String QUERY_KEY = "query";
  public static final String DATA_NOT_FOUND="Required Data not Found";
  public static final String USERID_NOT_FOUND = "User ID not found" ;
  public static final String START_TIME = "startTime";
  public static final String END_TIME = "endTime";
  public static final String ENDPOINT = "endPoint";
  public static final String TIME = "time";
  public static final String INVALID_DATE_TIME = "Date-Time not in correct format.";
  public static final String MISSING_START_TIME = "Start-Time not found.";
  public static final String MISSING_END_TIME = "End-Time not found.";
  public static final String INVALID_TIME = "End-Time cannot be before Start-Time.";

  /*Auditing Service Constants*/
  public static final String USER_ID = "userID";
  public static final String API = "api";
  public static final String RESOURCE_ID = "resourceID";
  public static final String PROVIDER_ID = "providerID";
  public static final String WRITE_QUERY =
          "INSERT INTO fsauditingtable2 (id, userId, api, resourceid, providerid, time, epochtime) VALUES ('$1','$2','$3','$4','$5','$6', $7)";
  public static final String READ_QUERY =
          "SELECT userId, api, resourceid, providerid, time from fsauditingtable2 where userId='$1'";
  public static final String START_TIME_QUERY = " and epochtime>=$2";
  public static final String END_TIME_QUERY = " and epochtime<=$3";
  public static final String API_QUERY = " and api='$4'";
  //TODO: populate these fields ->
  public static final String API_COLUMN_NAME = "(defaultdb.fsauditingtable2.api)";
  public static final String USERID_COLUMN_NAME = "(defaultdb.fsauditingtable2.userid)";
  public static final String RESOURCE_COLUMN_NAME = "(defaultdb.fsauditingtable2.resourceid)";
  public static final String PROVIDER_COLUMN_NAME = "(defaultdb.fsauditingtable2.providerid)";
  public static final String TIME_COLUMN_NAME = "(defaultdb.fsauditingtable2.time)";
}

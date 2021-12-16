package iudx.file.server.databroker.util;

public class Constants {
  // Registration response fields used for construction JSON
  public static final String SUCCESS = "success";
  public static final String QUEUE_NAME = "file-server-token-invalidation";
  public static final int CACHE_TIMEOUT_AMOUNT = 30;
  public static final String QUERY = "select userID, timestamp from databroker";
}

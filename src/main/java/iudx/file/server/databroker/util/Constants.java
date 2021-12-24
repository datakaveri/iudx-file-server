package iudx.file.server.databroker.util;

public class Constants {
  // Registration response fields used for construction JSON
  public static final String SUCCESS = "success";
  public static final String QUEUE_NAME = "file-server-token-invalidation";
  public static final int CACHE_TIMEOUT_AMOUNT = 12;
  public static final String QUERY = "SELECT _id,modified_at FROM revoke_tokens;";
}

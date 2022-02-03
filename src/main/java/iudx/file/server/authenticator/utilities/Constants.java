package iudx.file.server.authenticator.utilities;

import java.util.List;

public class Constants extends iudx.file.server.common.Constants {
  // cache
  public static final long CACHE_TIMEOUT = 30;
  public static final String FILE_SERVER_REGEX = "(.*)file(.*)";
  public static final List<String> OPEN_ENDPOINTS =
          List.of(
                  "/ngsi-ld/v1/temporal/entities",
                  "/ngsi-ld/v1/entities");

  public static final String JSON_IID = "iid";
  public static final String JSON_EXPIRY = "expiry";
  public static final String JSON_USERID = "userid";
  public static final String JSON_CONSUMER = "consumer"; 
}

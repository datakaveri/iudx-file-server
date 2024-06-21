package iudx.file.server.authenticator.utilities;

import java.util.List;

public class Constants extends iudx.file.server.common.Constants {
  // cache
  public static final long CACHE_TIMEOUT = 30;
  public static final String FILE_SERVER_REGEX = "(.*)file(.*)";
  public static final List<String> OPEN_ENDPOINTS = List.of("/download");

  public static final List<String> QUERY_ENDPOINTS =
      List.of("/temporal/entities", "/entities", "/list");

  public static final String JSON_IID = "iid";
  public static final String JSON_EXPIRY = "expiry";
  public static final String JSON_USERID = "userid";
  public static final String JSON_CONSUMER = "consumer";
  public static final String ROLE = "role";
  public static  final String ATTRS = "attrs";
  public static  final String ACCESS = "access";

  public static final String DRL = "drl";
  public static final String DID = "did";

  public static String DATA_ACCESS_RESTRICTION = "SELECT userid, COUNT(*) AS num_rows, SUM(size) AS total_data_downloaded FROM auditing_rs WHERE userid = '$1' GROUP BY userid " ;
}

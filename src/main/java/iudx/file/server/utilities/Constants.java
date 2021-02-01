package iudx.file.server.utilities;

public class Constants {
  public static final String CONFIG_FILE = "config.properties";
  public static final String KEYSTORE_FILE_NAME = "keystore";
  public static final String KEYSTORE_FILE_PASSWORD = "keystorePassword";
  public static final int PORT = 8443;
  public static final String TMP_DIR="/home/kailash/data/tmp/";
  public static final String DIR = "/home/kailash/data/uploads/";
  public static final long MAX_SIZE = 1073741824L; // 1GB = 1073741824 Bytes
  
  //api
  public static final String API_TEMPORAL="/ngsi-ld/v1/temporal/entities";
  public static final String API_FILE="/file";
  public static final String API_TOKEN="/token";
  
  //header
  public static final String HEADER_TOKEN = "token";
  
  
  public static final String CONTENT_TYPE = "content-type";
  public static final String APPLICATION_JSON = "application/json";
  
  //json fields
  public static final String JSON_TYPE = "type";
  public static final String JSON_TITLE = "title";
  public static final String JSON_DETAIL = "detail";
  
  //SQL
  public static final String SQL_DELETE="DELETE FROM file_server_token s where file_token = $1";
  public static final String SQL_INSERT="INSERT INTO file_server_token (user_token, file_token, validity_date, server_id ) VALUES ($1, $2, $3,$4)";
  public static final String SQL_SELECT="SELECT * FROM file_server_token WHERE user_token = $1";
      
}

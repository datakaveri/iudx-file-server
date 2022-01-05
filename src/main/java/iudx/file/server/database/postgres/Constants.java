package iudx.file.server.database.postgres;

public class Constants {

  public static String INSERT_REVOKE_TOKEN_SQL =
      "INSERT INTO revoked_tokens (_id, expiry) VALUES('$1','$2') "
          + "ON CONFLICT (_id) "
          + "DO UPDATE SET expiry = '$2'";

  public static String INSERT_UNIQUE_ATTR_SQL =
      "INSERT INTO unique_attributes(resource_id,unique_attribute) VALUES($1,$2)";

  public static String UPDATE_UNIQUE_ATTR_SQL =
      "UPDATE unique_attributes SET unique_attribute=$1 WHERE resource_id=$2";

  public static String DELETE_UNIQUE_ATTR_SQL =
      "DELETE FROM unique_attributes WHERE resource_id=$1";
}

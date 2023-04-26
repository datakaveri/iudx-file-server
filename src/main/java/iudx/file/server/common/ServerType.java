package iudx.file.server.common;

/**
 * QueryType.
 *
 * <h1>iudx QueryType </h1>
 */
public enum ServerType {
  FILE_SERVER("file-server"),
  RESOURCE_SERVER("rs-server");

  private String serverName;

  ServerType(String serverName) {
    this.serverName = serverName;
  }

  public String getserverName() {
    return this.serverName;
  }
}

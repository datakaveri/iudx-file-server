package iudx.file.server.apiserver.service;

public enum ServerType {
  FILE_SERVER("file-server"), RESOURCE_SERVER("rs-server");

  private String serverName;

  public String getserverName() {
    return this.serverName;
  }

  private ServerType(String serverName) {
    this.serverName = serverName;
  }
}

package iudx.file.server.authenticator.authorization;

import iudx.file.server.apiserver.utilities.Configuration;

import java.util.stream.Stream;

public enum Api {
  UPLOAD(Configuration.getBasePath(Configuration.IUDX_V1_BASE_PATH) + "/upload"),
  DOWNLOAD(Configuration.getBasePath(Configuration.IUDX_V1_BASE_PATH) + "/download"),
  DELETE_FILE(Configuration.getBasePath(Configuration.IUDX_V1_BASE_PATH) + "/delete"),
  QUERY(Configuration.getBasePath(Configuration.NGSILD_BASE_PATH) + "/temporal/entities"),
  SPATIAL(Configuration.getBasePath(Configuration.NGSILD_BASE_PATH) + "/entities"),
  LIST(Configuration.getBasePath(Configuration.IUDX_V1_BASE_PATH) + "/list"),
  API_SPECS("/apis/spec"),
  APIS("/apis");

  private final String endpoint;

  Api(String endpoint) {
    this.endpoint = endpoint;
  }

  public String getApiEndpoint() {
    return this.endpoint;
  }

  public static Api fromEndpoint(final String endpoint) {
    return Stream.of(values())
        .filter(v -> v.endpoint.equalsIgnoreCase(endpoint))
        .findAny()
        .orElse(null);
  }

}

package iudx.file.server.authenticator.authorization;

import java.util.stream.Stream;

public enum Api {
  UPLOAD("/iudx/v1/upload"),
  DOWNLOAD("/iudx/v1/download"),
  DELETE_FILE("/iudx/v1/delete"),
  QUERY("/ngsi-ld/v1/temporal/entities"),
  SPATIAL("/ngsi-ld/v1/entities"),
  LIST("/iudx/v1/list"),
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

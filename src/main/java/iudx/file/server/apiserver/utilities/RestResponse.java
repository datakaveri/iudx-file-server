package iudx.file.server.apiserver.utilities;

import io.vertx.core.json.JsonObject;

public class RestResponse {

  private final int type;
  private final String title;
  private final String details;

  private RestResponse(int status, String message, String details) {
    this.type = status;
    this.title = message;
    this.details = details;
  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    json.put("type", type);
    json.put("title", title);
    json.put("details", details);
    return json;
  }

  public String toJsonString() {
    return toJson().toString();
  }

  public static class Builder {
    private int type;
    private String title;
    private String details;

    public Builder() {}

    public Builder type(int code) {
      this.type = code;
      return this;
    }

    public Builder title(String message) {
      this.title = message;
      return this;
    }

    public Builder details(String message) {
      this.details = message;
      return this;
    }

    public RestResponse build() {
      return new RestResponse(type, title, details);
    }

  }

}

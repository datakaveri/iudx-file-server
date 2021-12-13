package iudx.file.server.apiserver.response;

import io.vertx.core.json.JsonObject;

public class RestResponse {

  private final String type;
  private final String title;
  private final String details;

  private RestResponse(String status, String message, String details) {
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
    private String type;
    private String title;
    private String details;

    public Builder() {}

    public Builder type(String code) {
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

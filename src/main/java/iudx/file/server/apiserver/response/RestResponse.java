package iudx.file.server.apiserver.response;

import io.vertx.core.json.JsonObject;

/**
 * <p>
 * Response Object can be used to pass URN based messages/responses between different verticles,
 * mostly in case of failures. where following parameters can be used
 *
 * <pre>
 *  type    : String representation of URN like urn:dx:rs:SomeErrorURN
 *  status  : HttpPstatus code (e.g 404,400 etc.)
 *  title   : brief error title
 *  detail  : detailed message
 * </pre>
 * </p>
 *
 */
public class RestResponse {

  private final String type;
  private final String title;
  private final String details;

  private RestResponse(String status, String message, String details) {
    this.type = status;
    this.title = message;
    this.details = details;
  }
  /** form a respone json. */

  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    json.put("type", type);
    json.put("title", title);
    json.put("detail", details);
    return json;
  }
  /** build string to json. */

  public String toJsonString() {
    return toJson().toString();
  }
  /** Builder class . */

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

package iudx.file.server.validations;

import io.vertx.core.json.JsonObject;

public class ContentTypeValidator {

  private JsonObject validContentType;
  
  public ContentTypeValidator(final JsonObject json) {
    this.validContentType=json;
  }

  public  boolean isValid(String contentType) {
    return validContentType.containsKey(contentType);
  }
}

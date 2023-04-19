package iudx.file.server.apiserver.validations;

import io.vertx.core.json.JsonObject;

/**
 * ContentTypeValidator.
 *
 * <h1>ContentTypeValidator</h1>
 */
public class ContentTypeValidator {

  private JsonObject validContentType;

  public ContentTypeValidator(final JsonObject json) {
    this.validContentType = json;
  }

  public boolean isValid(String contentType) {
    return validContentType.containsKey(contentType);
  }
}

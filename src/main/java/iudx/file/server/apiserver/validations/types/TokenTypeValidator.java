package iudx.file.server.apiserver.validations.types;

import iudx.file.server.apiserver.exceptions.DxRuntimeException;
import iudx.file.server.apiserver.response.ResponseUrn;

/**
 * TokenTypeValidator.
 *
 * <h1>TokenTypeValidator</h1>
 *
 * <p>it validate token
 */
public class TokenTypeValidator implements Validator {

  private final String value;
  private final boolean required;

  public TokenTypeValidator(String value, boolean required) {
    this.required = required;
    this.value = value;
  }

  @Override
  public boolean isValid() {
    if (required && (value == null || value.isBlank())) {
      throw new DxRuntimeException(
          failureCode(), ResponseUrn.INVALID_TOKEN, "Validation error : token is empty");
    } else {
      if (value == null || value.isBlank()) {
        return true;
      }
    }
    return true;
  }

  @Override
  public int failureCode() {
    return 400;
  }

  @Override
  public String failureMessage() {
    return "Invalid token";
  }
}

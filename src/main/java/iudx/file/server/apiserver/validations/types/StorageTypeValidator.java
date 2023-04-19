package iudx.file.server.apiserver.validations.types;

import iudx.file.server.apiserver.exceptions.DxRuntimeException;
import iudx.file.server.apiserver.response.ResponseUrn;

/**
 * StorageTypeValidator.
 *
 * <h1>StorageTypeValidator</h1>
 *
 * <p>it checks storage type value whether it's null or not
 */
public class StorageTypeValidator implements Validator {
  private final String value;
  private final boolean required;

  public StorageTypeValidator(String value, boolean required) {
    this.required = required;
    this.value = value;
  }

  @Override
  public boolean isValid() {
    if (required && (value == null || value.isBlank())) {
      throw new DxRuntimeException(
          failureCode(), ResponseUrn.INVALID_ATTR_PARAM, "Validation error : field is empty");
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

package iudx.file.server.apiserver.validations.types;

import iudx.file.server.apiserver.exceptions.DxRuntimeException;
import iudx.file.server.apiserver.response.ResponseUrn;

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
      throw new DxRuntimeException(failureCode(), ResponseUrn.YET_NOT_IMPLEMENTED, "Validation error : field is empty");
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

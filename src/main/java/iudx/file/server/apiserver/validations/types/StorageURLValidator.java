package iudx.file.server.apiserver.validations.types;

import iudx.file.server.apiserver.exceptions.DxRuntimeException;
import iudx.file.server.apiserver.response.ResponseUrn;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.MalformedURLException;
import java.net.URL;

public class StorageURLValidator implements Validator {
  private static final Logger LOGGER = LogManager.getLogger(GeomTypeValidator.class);

  private final String value;
  private final boolean required;

  public StorageURLValidator(String value, boolean required) {
    this.value = value;
    this.required = required;
  }

  @Override
  public boolean isValid() {
    if(required && (value == null || value.isBlank())) {
      throw new DxRuntimeException(failureCode(), ResponseUrn.INVALID_ATTR_VALUE, "Validation error: field is empty");
    } else {
      if(value == null || value.isBlank()) {
        return true;
      }
    }
    if(!isValidValue(value)) {
     LOGGER.error("Validation error: [ "+ value + " ]: url is not allowed");
     throw new DxRuntimeException(failureCode(), ResponseUrn.INVALID_ATTR_VALUE, "Validation error: url value is not allowed");
    }
    return true;
  }

  @Override
  public int failureCode() {
    return 400;
  }

  @Override
  public String failureMessage() {
    return "Invalid storage url type value";
  }

  private boolean isValidValue(String value) {

    try {
      URL url = new URL(value);
    } catch (MalformedURLException e) {
      LOGGER.error("MalformedURL : [ " + value + " ]");
      return false;
    }
    return true;
  }
}

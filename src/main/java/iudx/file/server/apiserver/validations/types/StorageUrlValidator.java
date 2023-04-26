package iudx.file.server.apiserver.validations.types;

import iudx.file.server.apiserver.exceptions.DxRuntimeException;
import iudx.file.server.apiserver.response.ResponseUrn;
import java.net.MalformedURLException;
import java.net.URL;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * StorageURLValidator.
 *
 * <h1>StorageURLValidator</h1>
 *
 * <p>it validate StorageURL
 */
public class StorageUrlValidator implements Validator {
  private static final Logger LOGGER = LogManager.getLogger(StorageUrlValidator.class);

  private final String value;
  private final boolean required;

  public StorageUrlValidator(String value, boolean required) {
    this.value = value;
    this.required = required;
  }

  @Override
  public boolean isValid() {
    if (required && (value == null || value.isBlank())) {
      throw new DxRuntimeException(
          failureCode(), ResponseUrn.INVALID_ATTR_VALUE, "Validation error: field is empty");
    } else {
      if (value == null || value.isBlank()) {
        return true;
      }
    }
    if (!isValidValue(value)) {
      LOGGER.error("Validation error: [ " + value + " ]: url is not allowed");
      throw new DxRuntimeException(
          failureCode(),
          ResponseUrn.INVALID_ATTR_VALUE,
          "Validation error: url value is not allowed");
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
      new URL(value);
    } catch (MalformedURLException e) {
      LOGGER.error("MalformedURL : [ " + value + " ]");
      return false;
    }
    return true;
  }
}

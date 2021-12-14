package iudx.file.server.apiserver.validations.types;

import iudx.file.server.apiserver.exceptions.DxRuntimeException;
import iudx.file.server.apiserver.response.ResponseUrn;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SampleTypeValidator implements Validator {

  private static final Logger LOGGER = LogManager.getLogger(SampleTypeValidator.class);

  private String value;
  private boolean required;

  public SampleTypeValidator(String value, boolean required) {
    this.value = value;
    this.required = required;
  }

  @Override
  public boolean isValid() {
    if (required && (value == null || value.isBlank())) {
      LOGGER.error("Validation error : null or blank value for required mandatory field");
      throw new DxRuntimeException(failureCode(), ResponseUrn.MANDATORY_FIELD, "Validation error : null or blank value for required mandatory field");
    } else {
      if (value == null || value.isBlank()) {
        return true;
      }
    }
    if (!value.equalsIgnoreCase("true")) {
      LOGGER.error("Validation error : Invalid isSample field value [ " + value + " ]");
      throw new DxRuntimeException(failureCode(), ResponseUrn.INVALID_ATTR_VALUE, "Validation error : Invalid isSample field value [ " + value + " ]");
    }
    return true;
  }

  @Override
  public int failureCode() {
    return 400;
  }

  @Override
  public String failureMessage() {
    return "invalid sample value provided.";
  }

}

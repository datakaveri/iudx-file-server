package iudx.file.server.apiserver.validations.types;

import iudx.file.server.apiserver.exceptions.DxRuntimeException;
import iudx.file.server.apiserver.response.ResponseUrn;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PaginationLimitTypeValidator implements Validator {

  private static final Logger LOGGER = LogManager.getLogger(PaginationLimitTypeValidator.class);

  private final String value;
  private final boolean required;

  public PaginationLimitTypeValidator(String value, boolean required) {
    this.value = value;
    this.required = required;
  }

  @Override
  public boolean isValid() {
    if (required && (value == null || value.isBlank())) {
      LOGGER.error("Validation error : null or blank value for required mandatory field");
      throw new DxRuntimeException(failureCode(), ResponseUrn.MANDATORY_FIELD, "Validation error : null or blank value for required mandatory field");
    } else {
      if (value == null) {
        return true;
      }
      if (value.isBlank()) {
        LOGGER.error("Validation error :  blank value passed");
        throw new DxRuntimeException(failureCode(), ResponseUrn.MANDATORY_FIELD, "Validation error : blank value passed");
      }
    }
    if (!isValidValue(value)) {
      LOGGER.error("Validation error : invalid pagination limit Value [ " + value + " ]");
      throw new DxRuntimeException(failureCode(), ResponseUrn.INVALID_ATTR_VALUE, "Validation error : invalid pagination limit value [ " + value + " ]");
    }
    return true;
  }

  private boolean isValidValue(String value) {
    try {
      int size = Integer.parseInt(value);
      if (size > 10000 || size < 0) {
        LOGGER.error(
            "Validation error : invalid pagination limit Value > 10000 or negative value passed [ "
                + value + " ]");
        throw new DxRuntimeException(failureCode(), ResponseUrn.REQUEST_LIMIT_EXCEED,
                "Validation error : invalid pagination limit Value > 10000 or negative value passed [ "  + value + " ]");
      }
      return true;
    } catch (Exception ex) {
      LOGGER.error("Validation error : invalid pagination limit Value [ " + value
          + " ] only integer expected");
      throw new DxRuntimeException(failureCode(), ResponseUrn.INVALID_ATTR_VALUE,
              "Validation error : invalid pagination limit Value [ " + value + " ] only integer expected" );
    }
  }

  @Override
  public int failureCode() {
    return 400;
  }

  @Override
  public String failureMessage() {
    return "bad query";
  }

}

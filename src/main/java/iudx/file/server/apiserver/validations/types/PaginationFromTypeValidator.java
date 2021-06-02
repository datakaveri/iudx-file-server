package iudx.file.server.apiserver.validations.types;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PaginationFromTypeValidator implements Validator {

  private static final Logger LOGGER = LogManager.getLogger(PaginationFromTypeValidator.class);

  private final String value;
  private final boolean required;

  public PaginationFromTypeValidator(String value, boolean required) {
    this.value = value;
    this.required = required;
  }

  @Override
  public boolean isValid() {
    if (required && (value == null || value.isBlank())) {
      LOGGER.error("Validation error : null or blank value for required mandatory field");
      return false;
    } else {
      if (value == null || value.isBlank()) {
        return true;
      }
    }
    if (!isValidValue(value)) {
      LOGGER.error("Validation error : invalid pagination from Value [ " + value + " ]");
      return false;
    }
    return true;
  }

  private boolean isValidValue(String value) {
    try {
      int from=Integer.parseInt(value);
      // TODO : currently we cannot control 'from' param value, but in future need to control this
      // since elastic performance will degrade after a certain threshold

      if (from > 50000) {
        LOGGER.error("Validation error : invalid pagination from Value > 50000 [ " + value + " ]");
        return false;
      }
      return true;
    } catch (Exception ex) {
      LOGGER.error("Validation error : invalid pagination from Value [ " + value
          + " ] only integer expected");
      return false;
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

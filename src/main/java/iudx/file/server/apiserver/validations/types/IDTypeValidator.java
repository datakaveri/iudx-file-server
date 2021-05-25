package iudx.file.server.apiserver.validations.types;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class IDTypeValidator implements Validator {

  private static final Logger LOGGER = LogManager.getLogger(IDTypeValidator.class);

  private final String value;
  private final boolean required;

  public IDTypeValidator(String value, boolean required) {
    this.value = value;
    this.required = required;
  }

  @Override
  public boolean isValid() {
    if (required && (value == null || value.isBlank())) {
      LOGGER.error("Validation error : null or blank value for required mandatory field");
      return false;
    } else {
      if (value == null) {
        return true;
      }
    }
    if (!isValidLength(value) || !isValidId(value)) {
      LOGGER.error("Validation error : Invalid id [ "+value+" ]");
      return false;
    }
    return true;
  }

  @Override
  public int failureCode() {
    return 400;
  }

  @Override
  public String failureMessage() {
    return "Invalid id";
  }


  private boolean isValidId(String id) {
    String[] idcomponents = id.split("/");
    return idcomponents.length >= 4;
  }

  private boolean isValidLength(String id) {
    return id.length() <= 512;
  }
}

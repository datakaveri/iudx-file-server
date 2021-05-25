package iudx.file.server.apiserver.validations.types;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TokenTypeValidator implements Validator {

  private static final Logger LOGGER = LogManager.getLogger(TokenTypeValidator.class);

  private final String value;
  private final boolean required;


  public TokenTypeValidator(String value, boolean required) {
    this.required = required;
    this.value = value;
  }

  @Override
  public boolean isValid() {
    if (required && (value == null || value.isBlank())) {
      return false;
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

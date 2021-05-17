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
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public String failureMessage() {
    // TODO Auto-generated method stub
    return null;
  }

  // public ParameterTypeValidator create() {
  // ParameterTypeValidator tokenValidator = new TokenValidator();
  // return tokenValidator;
  // }
  //
  //
  // class TokenValidator implements ParameterTypeValidator {
  // @Override
  // public RequestParameter isValid(String value) throws ValidationException {
  // // TODO match regex for token
  // if (value.isBlank()) {
  // throw ValidationException.ValidationExceptionFactory
  // .generateNotMatchValidationException("Empty values are not allowed in parameter.");
  // }
  // return RequestParameter.create(value);
  // }
  //
  // }

}

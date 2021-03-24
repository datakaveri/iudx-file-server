package iudx.file.server.validations.types;

import io.vertx.ext.web.api.RequestParameter;
import io.vertx.ext.web.api.validation.ParameterTypeValidator;
import io.vertx.ext.web.api.validation.ValidationException;

public class IDTypeValidator {

  private String pattern = ".*";

  public ParameterTypeValidator create() {
    ParameterTypeValidator idValidator = new IDValidator();
    return idValidator;
  }

  class IDValidator implements ParameterTypeValidator {

    // TODO : replace with a regex
    /**
     * 
     * @param id
     * @return TRUE for a valid id (at-least group level id), FALSE for invalid id
     */
    private boolean isValidId(String id) {
      String[] idcomponents = id.split("/");
      return idcomponents.length >= 4;
    }

    private boolean isValidLength(String id) {
      return id.length() <= 512;
    }

    @Override
    public RequestParameter isValid(String value) throws ValidationException {
      if (value.isBlank()) {
        throw ValidationException.ValidationExceptionFactory
            .generateNotMatchValidationException("Empty values are not allowed in parameter.");
      }
      if (!isValidLength(value)) {
        throw ValidationException.ValidationExceptionFactory
            .generateNotMatchValidationException("id length greater than 512 characters.");
      }
      if (!isValidId(value)) {
        throw ValidationException.ValidationExceptionFactory
            .generateNotMatchValidationException("Not a valid IUDX id");
      }

      return RequestParameter.create(value);
    }

  }
}

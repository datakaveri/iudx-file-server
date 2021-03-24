package iudx.file.server.validations.types;

import io.vertx.ext.web.api.RequestParameter;
import io.vertx.ext.web.api.validation.ParameterTypeValidator;
import io.vertx.ext.web.api.validation.ValidationException;

public class FileIdTypeValidator {

  private String pattern = ".*";

  public ParameterTypeValidator create() {
    ParameterTypeValidator idValidator = new FileIdValidator();
    return idValidator;
  }

  class FileIdValidator implements ParameterTypeValidator {

    // TODO : replace with a regex
    /**
     * 
     * @param id
     * @return TRUE for a valid id (at-least group level id), FALSE for invalid id
     */
    private boolean isValidId(String id) {
      String[] idcomponents = id.split("/");
      return idcomponents.length >= 5;
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
            .generateNotMatchValidationException("Not a valid IUDX file id");
      }

      return RequestParameter.create(value);
    }

  }
}

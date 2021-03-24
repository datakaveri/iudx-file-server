package iudx.file.server.validations.types;

import io.vertx.ext.web.api.RequestParameter;
import io.vertx.ext.web.api.validation.ParameterTypeValidator;
import io.vertx.ext.web.api.validation.ValidationException;

public class TemporalRelTypeValidator {

  public ParameterTypeValidator create() {
    ParameterTypeValidator relValidator = new TemporalRelValueValidator();
    return relValidator;
  }


  class TemporalRelValueValidator implements ParameterTypeValidator {

    private boolean isValidValue(String value) {
      return value.equals("during");
    }

    @Override
    public RequestParameter isValid(String value) throws ValidationException {
      // TODO Auto-generated method stub
      if (value.isBlank()) {
        throw ValidationException.ValidationExceptionFactory
            .generateNotMatchValidationException("Empty values are not allowed in parameter.");
      }

      if (!isValidValue(value)) {
        throw ValidationException.ValidationExceptionFactory
            .generateNotMatchValidationException("Not a valid value for timerel");
      }
      return RequestParameter.create(value);
    }

  }
}

package iudx.file.server.validations.types;

import io.vertx.ext.web.api.RequestParameter;
import io.vertx.ext.web.api.validation.ParameterTypeValidator;
import io.vertx.ext.web.api.validation.ValidationException;

public class SampleTypeValidator {

  public ParameterTypeValidator create() {
    ParameterTypeValidator sampleValueValidator = new SampleValueValidator();
    return sampleValueValidator;
  }


  class SampleValueValidator implements ParameterTypeValidator {
    @Override
    public RequestParameter isValid(String value) throws ValidationException {
      if (!value.equals("true")) {
        throw ValidationException.ValidationExceptionFactory
            .generateNotMatchValidationException("Value not recognized for field");
      }
      return RequestParameter.create(value);
    }
  }
}

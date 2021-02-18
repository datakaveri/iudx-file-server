package iudx.file.server.validations.types;

import io.vertx.ext.web.api.RequestParameter;
import io.vertx.ext.web.api.validation.ParameterTypeValidator;
import io.vertx.ext.web.api.validation.ValidationException;

public class TokenTypeValidator {

  public ParameterTypeValidator create() {
    ParameterTypeValidator tokenValidator = new TokenValidator();
    return tokenValidator;
  }


  class TokenValidator implements ParameterTypeValidator {
    @Override
    public RequestParameter isValid(String value) throws ValidationException {
      // TODO match regex for token
      return RequestParameter.create(value);
    }

  }

}

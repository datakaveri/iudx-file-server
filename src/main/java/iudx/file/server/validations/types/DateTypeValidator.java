package iudx.file.server.validations.types;

import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import io.vertx.ext.web.api.RequestParameter;
import io.vertx.ext.web.api.validation.ParameterTypeValidator;
import io.vertx.ext.web.api.validation.ValidationException;

public class DateTypeValidator {

  public ParameterTypeValidator create() {
    ParameterTypeValidator dateTypeValidator = new DateValidator();
    return dateTypeValidator;
  }

  class DateValidator implements ParameterTypeValidator {


    private boolean isValidDate(String value) {
      String dateString = value.trim().replaceAll("\\s", "+");// since + is treated as space in uri
      try {
        ZonedDateTime.parse(dateString);
        return true;
      } catch (DateTimeParseException e) {
        return false;
      }
    }


    @Override
    public RequestParameter isValid(String value) throws ValidationException {
      if (value.isBlank()) {
        throw ValidationException.ValidationExceptionFactory
            .generateNotMatchValidationException("Empty values are not allowed in parameter.");
      }
      if (!isValidDate(value)) {
        throw ValidationException.ValidationExceptionFactory
            .generateNotMatchValidationException("Invalid Date format.");
      }
      return RequestParameter.create(value);
    }

  }

}

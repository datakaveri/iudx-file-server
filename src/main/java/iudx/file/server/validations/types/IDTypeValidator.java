package iudx.file.server.validations.types;

import io.vertx.ext.web.api.validation.ParameterTypeValidator;

public class IDTypeValidator {

  private String pattern = ".*";
  private Integer minLength = 12;
  private Integer maxLength = 512;


  public ParameterTypeValidator create() {
    ParameterTypeValidator idTypeValidator =
        ParameterTypeValidator.createStringTypeValidator(pattern, minLength, maxLength, "");
    return ParameterTypeValidator.createArrayTypeValidator(idTypeValidator, "csv", 5, 1);
  }
}

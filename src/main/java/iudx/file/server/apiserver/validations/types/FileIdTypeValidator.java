package iudx.file.server.apiserver.validations.types;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

//TOOD : Regex for fileId
public class FileIdTypeValidator implements Validator {

  private static final Logger LOGGER = LogManager.getLogger(FileIdTypeValidator.class);
  
  private final String value;
  private final boolean required;

  public FileIdTypeValidator(String value, boolean required) {
    this.value = value;
    this.required = required;
  }

  @Override
  public boolean isValid() {
    if (required && (value == null || value.isBlank())) {
      LOGGER.error("Validation error : null or blank value for required mandatory field");
      return false;
    } else {
      if (value == null || value.isBlank()) {
        return true;
      }
    }
    if (!isValidLength(value) || !isValidId(value)) {
      LOGGER.error("Validation error : invalid file id [ "+value+" ]");
      return false;
    }
    return true;
  }

  @Override
  public int failureCode() {
    // TODO Auto-generated method stub
    return 400;
  }

  @Override
  public String failureMessage() {
    // TODO Auto-generated method stub
    return "Invalid file id";
  }

  private boolean isValidId(String id) {
    String[] idcomponents = id.split("/");
    return idcomponents.length >= 5;
  }

  private boolean isValidLength(String id) {
    return id.length() <= 512;
  }

  // private String pattern = ".*";
  //
  // public ParameterTypeValidator create() {
  // ParameterTypeValidator idValidator = new FileIdValidator();
  // return idValidator;
  // }
  //
  // class FileIdValidator implements ParameterTypeValidator {
  //
  // // TODO : replace with a regex
  // /**
  // *
  // * @param id
  // * @return TRUE for a valid id (at-least group level id), FALSE for invalid id
  // */
  // private boolean isValidId(String id) {
  // String[] idcomponents = id.split("/");
  // return idcomponents.length >= 5;
  // }
  //
  // private boolean isValidLength(String id) {
  // return id.length() <= 512;
  // }
  //
  // @Override
  // public RequestParameter isValid(String value) throws ValidationException {
  // if (value.isBlank()) {
  // throw ValidationException.ValidationExceptionFactory
  // .generateNotMatchValidationException("Empty values are not allowed in parameter.");
  // }
  // if (!isValidLength(value)) {
  // throw ValidationException.ValidationExceptionFactory
  // .generateNotMatchValidationException("id length greater than 512 characters.");
  // }
  // if (!isValidId(value)) {
  // throw ValidationException.ValidationExceptionFactory
  // .generateNotMatchValidationException("Not a valid IUDX file id");
  // }
  //
  // return RequestParameter.create(value);
  // }
  //
  // }
}

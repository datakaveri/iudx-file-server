package iudx.file.server.apiserver.validations.types;

import iudx.file.server.apiserver.exceptions.DxRuntimeException;
import iudx.file.server.apiserver.response.ResponseUrn;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

// TOOD : Regex for fileId
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
      throw new DxRuntimeException(failureCode(), ResponseUrn.MANDATORY_FIELD, "Validation error : null or blank value for required mandatory field");
    } else {
      if (value == null || value.isBlank()) {
        return true;
      }
    }
    if (!isValidLength(value) || !isValidId(value)) {
      LOGGER.error("Validation error : invalid file id [ " + value + " ]");
      throw new DxRuntimeException(failureCode(), ResponseUrn.INVALID_ATTR_VALUE, "Validation error : invalid file id [ " + value + " ]");
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
}

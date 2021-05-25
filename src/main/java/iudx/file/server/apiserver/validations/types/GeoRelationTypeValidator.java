package iudx.file.server.apiserver.validations.types;

import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class GeoRelationTypeValidator implements Validator {

  private static final Logger LOGGER = LogManager.getLogger(GeoRelationTypeValidator.class);
  
  private List<String> allowedValues = List.of("within", "intersects", "near");
  private final String value;
  private final boolean required;

  public GeoRelationTypeValidator(String value, boolean required) {
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
    if (!allowedValues.contains(value)) {
      LOGGER.error("Validation error : Invalid geo relation value passed [ " + value + " ]");
      return false;
    }
    return true;
  }

  @Override
  public int failureCode() {
    return 400;
  }

  @Override
  public String failureMessage() {
    return "Invalid geo rel";
  }
}

package iudx.file.server.apiserver.validations.types;

import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class GeomTypeValidator implements Validator {
  private static final Logger LOGGER = LogManager.getLogger(GeomTypeValidator.class);

  private List<Object> allowedValues =
      List.of("Point", "point", "Polygon", "polygon", "LineString", "linestring", "bbox");

  private String value;
  private boolean required;

  public GeomTypeValidator(String value, boolean required) {
    this.value = value;
    this.required = required;
  }

  @Override
  public boolean isValid() {
    LOGGER.debug("value : " + value + "required : " + required);
    if (required && (value == null || value.isBlank())) {
      LOGGER.error("Validation error : null or blank value for required mandatory field");
      return false;
    } else {
      if (value == null || value.isBlank()) {
        return true;
      }
    }
    if (!allowedValues.contains(value)) {
      LOGGER.error("Validation error : Invalid geom type value passed [ " + value + " ]");
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
    return "Invalid geo typee value";
  }
}

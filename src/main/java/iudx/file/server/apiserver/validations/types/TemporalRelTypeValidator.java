package iudx.file.server.apiserver.validations.types;

import java.util.Arrays;
import java.util.List;

import iudx.file.server.apiserver.exceptions.DxRuntimeException;
import iudx.file.server.apiserver.response.ResponseUrn;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TemporalRelTypeValidator implements Validator {

  private static final Logger LOGGER = LogManager.getLogger(TemporalRelTypeValidator.class);

  private final List<String> allowedTimeRel = Arrays.asList("after", "before", "during");

  private String value;
  private boolean required;

  public TemporalRelTypeValidator(String value, boolean required) {
    this.value = value;
    this.required = required;
  }

  @Override
  public boolean isValid() {
    LOGGER.debug("value : " + value + "required : " + required);
    if (required && (value == null || value.isBlank())) {
      throw new DxRuntimeException(failureCode(), ResponseUrn.MANDATORY_FIELD, "Validation error : null or blank value for required mandatory field");
    } else {
      if (value == null || value.isBlank()) {
        return true;
      }
    }
    if (!allowedTimeRel.contains(value)) {
      LOGGER.error("Validation error : Invalid temporal relation value passed [ " + value + " ]");
      throw new DxRuntimeException(failureCode(), ResponseUrn.INVALID_TEMPORAL_RELATION_VALUE, "Validation error : Invalid temporal relation value passed [ "+value+" ]");
    }
    return true;
  }

  @Override
  public int failureCode() {
    return 400;
  }

  @Override
  public String failureMessage() {
    return "invalid time relation value.";
  }
}


package iudx.file.server.apiserver.validations.types;

import iudx.file.server.apiserver.exceptions.DxRuntimeException;
import iudx.file.server.apiserver.response.ResponseUrn;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.List;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static iudx.file.server.apiserver.utilities.Constants.VALIDATION_ALLOWED_COORDINATES;
import static iudx.file.server.apiserver.utilities.Constants.VALIDATION_COORDINATE_PRECISION_ALLOWED;

public class CoordinatesTypeValidator implements Validator {

  private static final Logger LOGGER = LogManager.getLogger(CoordinatesTypeValidator.class);

  private static final String LATITUDE_PATTERN =
      "^(\\+|-)?(?:90(?:(?:\\.0{1,6})?)|(?:[0-9]|[1-8][0-9])(?:(?:\\.[0-9]{1,6})?))$";
  private static final String LONGITUDE_PATTERN =
      "^(\\+|-)?(?:180(?:(?:\\.0{1,6})?)|(?:[0-9]|[1-9][0-9]|1[0-7][0-9])(?:(?:\\.[0-9]{1,6})?))$";
  private static final Pattern pattern = Pattern.compile("[\\w]+[^\\,]*(?:\\.*[\\w])");
  private final int allowedMaxCoordinates = VALIDATION_ALLOWED_COORDINATES;
  private final String value;
  private final boolean required;
  private DecimalFormat df = new DecimalFormat("#.######");

  public CoordinatesTypeValidator(String value, boolean required) {
    this.value = value;
    this.required = required;
  }

  private boolean isValidLatitude(String latitude) {
    String message = "";
    try {
      Float latitudeValue = Float.parseFloat(latitude);
      if (!df.format(latitudeValue).matches(LATITUDE_PATTERN)) {
        message = "Validation error : invalid latitude value " + latitude;
      }
    } catch (Exception ex) {
      message = "Validation error : invalid latitude value " + latitude;
    }
    if (message.isBlank()) {
      return true;
    }
    throw new DxRuntimeException(failureCode(), ResponseUrn.INVALID_GEO_PARAM, message);
  }

  private boolean isValidLongitude(String longitude) {
    String message = "";
    try {
      Float longitudeValue = Float.parseFloat(longitude);
      if (!df.format(longitudeValue).matches(LONGITUDE_PATTERN)) {
        message = "Validation error : invalid longitude value " + longitude;
      }
    } catch (Exception ex) {
      message = "Validation error : invalid longitude value " + longitude;
    }
    if (message.isBlank()) {
      return true;
    }
    throw new DxRuntimeException(failureCode(), ResponseUrn.INVALID_GEO_PARAM, message);
  }

  private boolean isPricisonLengthAllowed(String value) {
    boolean result = false;
    try {
      result = new BigDecimal(value).scale() > VALIDATION_COORDINATE_PRECISION_ALLOWED;
    } catch (Exception ex) {
      throw new DxRuntimeException(
          failureCode(),
          ResponseUrn.INVALID_GEO_VALUE,
          "Validation error : invalid value " + value);
    }
    return result;
  }

  private boolean isValidCoordinates(String value) {
    String coordinates = value.replaceAll("\\[", "").replaceAll("\\]", "");
    String[] coordinatesArray = coordinates.split(",");
    boolean checkLongitudeFlag = false;
    for (String coordinate : coordinatesArray) {
      if (!coordinate.isBlank()) {
        coordinate = coordinate.trim();
      } else {
        LOGGER.error("invalid/empty coordinate value");
        return false;
      }
      if (checkLongitudeFlag && !isValidLatitude(coordinate)) {
        return false;
      } else if (!isValidLongitude(coordinate)) {
        return false;
      }
      checkLongitudeFlag = !checkLongitudeFlag;
      if (isPricisonLengthAllowed(coordinate)) {
        LOGGER.error("invalid coordinate (only 6 digits to precision allowed)");
        return false;
      }
    }
    return true;
  }

  private String getProbableGeomType(String coords) {
    String geom = null;
    if (coords.startsWith("[[[")) {
      geom = "polygon";
    } else if (coords.startsWith("[[")) {
      geom = "line";
    } else if (coords.startsWith("[")) {
      geom = "point";
    }
    return geom;
  }

  private boolean isValidCoordinateCount(String coordinates) {
    String geom = getProbableGeomType(coordinates);
    List<String> coordinatesList = getCoordinatesValues(coordinates);
    if (geom.equalsIgnoreCase("point")) {
      if (coordinatesList.size() != 2) {
        LOGGER.error("Invalid number of coordinates given for point");
        return false;
      }
    } else if (geom.equalsIgnoreCase("polygon")) {
      if (coordinatesList.size() > allowedMaxCoordinates * 2) {
        return false;
      }
    } else {
      if (coordinatesList.size() > allowedMaxCoordinates * 2) {
        return false;
      }
      // TODO : handle for line and bbox (since line and bbox share same [[][]] structure )
      return true;
    }
    return true;
  }

  private List<String> getCoordinatesValues(String coordinates) {
    Matcher matcher = pattern.matcher(coordinates);
    List<String> coordinatesValues =
        matcher.results().map(MatchResult::group).collect(Collectors.toList());
    return coordinatesValues;
  }

  @Override
  public boolean isValid() {
    String message = "";
    if (required && (value == null || value.isBlank())) {
      message = "Validation error : null or blank value for required mandatory field";
      throw new DxRuntimeException(failureCode(), ResponseUrn.INVALID_GEO_PARAM, message);
    } else {
      if (value == null || value.isBlank()) {
        return true;
      }
    }
    if (!isValidCoordinateCount(value)) {
      message =
          "Invalid numbers of coordinates supplied (Only 10 coordinates allowed for polygon and line & 1 coordinate for point)";
    }
    if (!isValidCoordinates(value)) {
      message = "invalid coordinate (only 6 digits to precision allowed)";
    }
    if (message.isBlank()) {
      return true;
    }
    throw new DxRuntimeException(failureCode(), ResponseUrn.INVALID_GEO_PARAM, message);
  }

  @Override
  public int failureCode() {
    // TODO Auto-generated method stub
    return 400;
  }

  @Override
  public String failureMessage() {
    // TODO Auto-generated method stub
    return null;
  }
}

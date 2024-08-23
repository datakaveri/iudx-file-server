package iudx.file.server.apiserver.response;

import java.util.stream.Stream;

/**
 * ResponseUrn.
 *
 * <h1>ResponseUrn</h1>
 *
 * <p>it helps to build http response according to urn with customized message
 */
public enum ResponseUrn {
  SUCCESS("urn:dx:rs:success", "Successful operation"),
  INVALID_TEMPORAL_PARAM("irn:dx:rs:InvalidTemporalParam", "Invalid temporal param"),
  INVALID_TEMPORAL_RELATION_VALUE(
      "urn:dx:rs:InvalidTemporalRelationValue", "Invalid temporal param value"),
  INVALID_TEMPORAL_DATE_FORMAT(
      "urn:dx:rs:InvalidTemporalDateFormat", "Invalid temporal param value date format"),
  INVALID_GEO_PARAM("urn:dx:rs:InvalidGeoParam", "Invalid geo param"),
  INVALID_GEO_VALUE("urn:dx:rs:InvalidGeoValue", "Invalid geo param value"),
  INVALID_ATTR_PARAM("urn:dx:rs:invalidAttributeParam", "Invalid attribute param"),
  INVALID_ATTR_VALUE("urn:dx:rs:invalidAttributeValue", "Invalid attribute value"),
  INVALID_OPERATION("urn:dx:rs:invalidOperation", "Invalid operation"),
  BAD_REQUEST_URN("urn:dx:rs:badRequest", "bad request parameter"),
  PAYLOAD_TOO_LARGE_URN("urn:dx:rs:payloadTooLarge", "Response size exceeds limit"),
  BACKING_SERVICE_FORMAT_URN(
      "urn:dx:rs:backend", "format error from backing service [cat,auth etc.]"),
  UNAUTHORIZED_ENDPOINT("urn:dx:rs:unauthorizedEndpoint", "Access to endpoint is not available"),
  UNAUTHORIZED_RESOURCE("urn:dx:rs:unauthorizedResource", "Access to resource is not available"),
  EXPIRED_TOKEN("urn:dx:rs:expiredAuthorizationToken", "Token has expired"),
  MISSING_TOKEN("urn:dx:rs:missingAuthorizationToken", "Token needed and not present"),
  INVALID_TOKEN("urn:dx:rs:invalidAuthorizationToken", "Token is invalid"),
  MANDATORY_FIELD(
      "urn:dx:rs:missingMandatoryField",
      "A mandatory field is missing"), // TODO: is this an acceptable response urn?

  RESOURCE_ALREADY_EXISTS("urn:dx:rs:resourceAlreadyExists", "Document of given ID already exists"),

  INVALID_PAYLOAD_FORMAT(
      "urn:dx:rs:invalidPayloadFormat", "Invalid json format in post request [schema mismatch]"),
  RESOURCE_NOT_FOUND("urn:dx:rs:resourceNotFound", "Document of given id does not exist"),
  METHOD_NOT_FOUND("urn:dx:rs:MethodNotAllowed", "Method not allowed for given endpoint"),
  UNSUPPORTED_MEDIA_TYPE(
      "urn:dx:rs:UnsupportedMediaType", "Requested/Presented media type not supported"),

  RESPONSE_PAYLOAD_EXCEED(
      "urn:dx:rs:responsePayloadLimitExceeded",
      "Search operations exceeds the default response payload limit"),
  REQUEST_PAYLOAD_EXCEED(
      "urn:dx:rs:requestPayloadLimitExceeded",
      "Operation exceeds the default request payload limit"),
  REQUEST_OFFSET_EXCEED(
      "urn:dx:rs:requestOffsetLimitExceeded", "Operation exceeds the default value of offset"),
  REQUEST_LIMIT_EXCEED(
      "urn:dx:rs:requestLimitExceeded", "Operation exceeds the default value of limit"),

  BACKING_SERVICE_FORMAT("urn:dx:rs:backend", "format error from backing service [cat,auth etc.]"),

  YET_NOT_IMPLEMENTED("urn:dx:rs:general", "urn yet not implemented in backend verticle."),
  DB_ERROR_URN("urn:dx:rs:databaseError", "database error"),
  UNAUTHORIZED_URN("urn:dx:rs:unauthorizedAccess" , "Access limit exceeded");

  private final String urn;
  private final String message;

  ResponseUrn(String urn, String message) {
    this.urn = urn;
    this.message = message;
  }
  /**
   * urnFromCode.
   *
   * @param urn urn data
   */

  public static ResponseUrn urnFromCode(final String urn) {
    return Stream.of(values())
        .filter(v -> v.urn.equalsIgnoreCase(urn))
        .findAny()
        .orElse(YET_NOT_IMPLEMENTED); /* If backend services don't respond with URN */
  }

  public String getUrn() {
    return urn;
  }

  public String getMessage() {
    return message;
  }

  public String toString() {
    return "[" + urn + " : " + message + " ]";
  }
}

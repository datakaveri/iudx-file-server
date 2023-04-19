package iudx.file.server.apiserver.utilities;

/**
 * HttpStatusCode.
 *
 * <h1>HttpStatusCode</h1>
 */
public enum HttpStatusCode {

  // 1xx: Informational
  CONTINUE(100, "Continue", "urn:dx:rs:continue"),
  SWITCHING_PROTOCOLS(101, "Switching Protocols", "urn:dx:rs:switchingProtocols"),
  PROCESSING(102, "Processing", "urn:dx:rs:processing"),
  EARLY_HINTS(103, "Early Hints", "urn:dx:rs:earlyHints"),

  // 2XX: codes
  SUCCESS(200, "Successful Operation", "urn:dx:rs:success"),
  NO_CONTENT(204, "No Content", "urn:dx:rs:noContent"),

  // 4xx: Client Error
  BAD_REQUEST(400, "Bad Request", "urn:dx:rs:badRequest"),
  UNAUTHORIZED(401, "Not Authorized", "urn:dx:rs:notAuthorized"),
  PAYMENT_REQUIRED(402, "Payment Required", "urn:dx:rs:paymentRequired"),
  FORBIDDEN(403, "Forbidden", "urn:dx:rs:forbidden"),
  NOT_FOUND(404, "Not Found", "urn:dx:rs:resourceNotFound"),
  METHOD_NOT_ALLOWED(405, "Method Not Allowed", "urn:dx:rs:methodNotAllowed"),
  NOT_ACCEPTABLE(406, "Not Acceptable", "urn:dx:rs:notAcceptable"),
  PROXY_AUTHENTICATION_REQUIRED(
      407, "Proxy Authentication Required", "urn:dx:rs:proxyAuthenticationRequired"),
  REQUEST_TIMEOUT(408, "Request Timeout", "urn:dx:rs:requestTimeout"),
  CONFLICT(409, "Conflict", "urn:dx:rs:conflict"),
  GONE(410, "Gone", "urn:dx:rs:gone"),
  LENGTH_REQUIRED(411, "Length Required", "urn:dx:rs:lengthRequired"),
  PRECONDITION_FAILED(412, "Precondition Failed", "urn:dx:rs:preconditionFailed"),
  REQUEST_TOO_LONG(413, "Payload Too Large", "urn:dx:rs:payloadTooLarge"),
  REQUEST_URI_TOO_LONG(414, "URI Too Long", "urn:dx:rs:uriTooLong"),
  UNSUPPORTED_MEDIA_TYPE(415, "Unsupported Media Type", "urn:dx:rs:unsupportedMediaType"),
  REQUESTED_RANGE_NOT_SATISFIABLE(416, "Range Not Satisfiable", "urn:dx:rs:rangeNotSatisfiable"),
  EXPECTATION_FAILED(417, "Expectation Failed", "urn:dx:rs:expectation Failed"),
  MISDIRECTED_REQUEST(421, "Misdirected Request", "urn:dx:rs:misdirected Request"),
  UNPROCESSABLE_ENTITY(422, "Unprocessable Entity", "urn:dx:rs:unprocessableEntity"),
  LOCKED(423, "Locked", "urn:dx:rs:locked"),
  FAILED_DEPENDENCY(424, "Failed Dependency", "urn:dx:rs:failedDependency"),
  TOO_EARLY(425, "Too Early", "urn:dx:rs:tooEarly"),
  UPGRADE_REQUIRED(426, "Upgrade Required", "urn:dx:rs:upgradeRequired"),
  PRECONDITION_REQUIRED(428, "Precondition Required", "urn:dx:rs:preconditionRequired"),
  TOO_MANY_REQUESTS(429, "Too Many Requests", "urn:dx:rs:tooManyRequests"),
  REQUEST_HEADER_FIELDS_TOO_LARGE(
      431, "Request Header Fields Too Large", "urn:dx:rs:requestHeaderFieldsTooLarge"),
  UNAVAILABLE_FOR_LEGAL_REASONS(
      451, "Unavailable For Legal Reasons", "urn:dx:rs:unavailableForLegalReasons"),

  // 5xx: Server Error
  INTERNAL_SERVER_ERROR(500, "Internal Server Error", "urn:dx:rs:internalServerError"),
  NOT_IMPLEMENTED(501, "Not Implemented", "urn:dx:rs:notImplemented"),
  BAD_GATEWAY(502, "Bad Gateway", "urn:dx:rs:badGateway"),
  SERVICE_UNAVAILABLE(503, "Service Unavailable", "urn:dx:rs:serviceUnavailable"),
  GATEWAY_TIMEOUT(504, "Gateway Timeout", "urn:dx:rs:gatewayTimeout"),
  HTTP_VERSION_NOT_SUPPORTED(
      505, "HTTP Version Not Supported", "urn:dx:rs:httpVersionNotSupported"),
  VARIANT_ALSO_NEGOTIATES(506, "Variant Also Negotiates", "urn:dx:rs:variantAlsoNegotiates"),
  INSUFFICIENT_STORAGE(507, "Insufficient Storage", "urn:dx:rs:insufficientStorage"),
  LOOP_DETECTED(508, "Loop Detected", "urn:dx:rs:loopDetected"),
  NOT_EXTENDED(510, "Not Extended", "urn:dx:rs:notExtended"),
  NETWORK_AUTHENTICATION_REQUIRED(
      511, "Network Authentication Required", "urn:dx:rs:networkAuthenticationRequired");

  private final int value;
  private final String description;
  private final String urn;

  HttpStatusCode(int value, String description, String urn) {
    this.value = value;
    this.description = description;
    this.urn = urn;
  }
  /** HttpStatusCode . */

  public static HttpStatusCode getByValue(int value) {
    for (HttpStatusCode status : values()) {
      if (status.value == value) {
        return status;
      }
    }
    throw new IllegalArgumentException("Invalid status code: " + value);
  }

  public int getValue() {
    return value;
  }

  public String getDescription() {
    return description;
  }

  public String getUrn() {
    return urn;
  }

  @Override
  public String toString() {
    return value + " " + description;
  }
}

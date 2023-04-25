package iudx.file.server.apiserver.exceptions;

import iudx.file.server.apiserver.response.ResponseUrn;

/** handle the runtime exceptions. */
public class DxRuntimeException extends RuntimeException {

  private static final long serialVersionUID = 1L; // TODO: ??

  private final int STatusCode;
  private final ResponseUrn urn;
  private final String message;
  /** handle the runtime exceptions. */

  public DxRuntimeException(final int statusCode, final ResponseUrn urn) {
    super();
    this.STatusCode = statusCode;
    this.urn = urn;
    this.message = urn.getMessage();
  }
  /** handle the runtime exceptions. */

  public DxRuntimeException(final int statusCode, final ResponseUrn urn, final String message) {
    super(message);
    this.STatusCode = statusCode;
    this.urn = urn;
    this.message = message;
  }
  /** handle the runtime exceptions. */

  public DxRuntimeException(final int statusCode, final ResponseUrn urn, final Throwable cause) {
    super(cause);
    this.STatusCode = statusCode;
    this.urn = urn;
    this.message = urn.getMessage();
  }

  public int getSTatusCode() {
    return STatusCode;
  }

  public ResponseUrn getUrn() {
    return urn;
  }

  public String getMessage() {
    return message;
  }
}

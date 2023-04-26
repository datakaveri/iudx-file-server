package iudx.file.server.apiserver.validations.types;

/**
 * Validator.
 *
 * <h1>Validator</h1>
 */

public interface Validator {

  boolean isValid();

  int failureCode();

  String failureMessage();

  default String failureMessage(final String value) {
    return failureMessage() + " [ " + value + " ] ";
  }
}

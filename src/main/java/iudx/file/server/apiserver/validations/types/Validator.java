package iudx.file.server.apiserver.validations.types;

public interface Validator {
  
  boolean isValid();

  int failureCode();

  String failureMessage();
}

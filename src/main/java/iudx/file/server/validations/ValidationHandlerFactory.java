package iudx.file.server.validations;


import static iudx.file.server.utilities.Constants.HEADER_TOKEN;
import static iudx.file.server.utilities.Constants.PARAM_END_TIME;
import static iudx.file.server.utilities.Constants.PARAM_FILE_ID;
import static iudx.file.server.utilities.Constants.PARAM_ID;
import static iudx.file.server.utilities.Constants.PARAM_SAMPLE;
import static iudx.file.server.utilities.Constants.PARAM_START_TIME;
import io.vertx.ext.web.api.validation.HTTPRequestValidationHandler;
import io.vertx.ext.web.api.validation.ParameterTypeValidator;
import iudx.file.server.validations.types.DateTypeValidator;
import iudx.file.server.validations.types.FileIdTypeValidator;
import iudx.file.server.validations.types.IDTypeValidator;
import iudx.file.server.validations.types.SampleTypeValidator;
import iudx.file.server.validations.types.TokenTypeValidator;

public class ValidationHandlerFactory {

  public HTTPRequestValidationHandler create(RequestType requestType) {
    HTTPRequestValidationHandler validator = null;
    switch (requestType) {
      case UPLOAD:
        validator = getUploadRequestValidations();
        break;
      case DOWNLOAD:
        validator = getDownloadRequestValidations();
        break;
      case DELETE:
        validator = getDeleteRequestValidations();
        break;
      default:
        break;
    }
    return validator;
  }

  private final ParameterTypeValidator idTypeValidator = new IDTypeValidator().create();
  private final ParameterTypeValidator fileIdValueValidator=new FileIdTypeValidator().create();
  private final ParameterTypeValidator sampleValueValidator = new SampleTypeValidator().create();
  private final ParameterTypeValidator dateTypeValidator = new DateTypeValidator().create();
  private final ParameterTypeValidator tokenTypeValidator = new TokenTypeValidator().create();

  private HTTPRequestValidationHandler getUploadRequestValidations() {
    final HTTPRequestValidationHandler validator = HTTPRequestValidationHandler.create()
        .addFormParamWithCustomTypeValidator(PARAM_ID, idTypeValidator, true, false)
        .addFormParamWithCustomTypeValidator(PARAM_START_TIME, dateTypeValidator, false, false)
        .addFormParamWithCustomTypeValidator(PARAM_SAMPLE, sampleValueValidator, false, false)
        .addFormParamWithCustomTypeValidator(PARAM_END_TIME, dateTypeValidator, false, false)
        .addHeaderParamWithCustomTypeValidator(HEADER_TOKEN, tokenTypeValidator, false, false);
    return validator;
  }

  private HTTPRequestValidationHandler getDownloadRequestValidations() {
    final HTTPRequestValidationHandler validator = HTTPRequestValidationHandler.create()
        .addQueryParamWithCustomTypeValidator(PARAM_FILE_ID, fileIdValueValidator, true, false)
        .addHeaderParamWithCustomTypeValidator(HEADER_TOKEN, tokenTypeValidator, false, false);
    return validator;
  }

  private HTTPRequestValidationHandler getDeleteRequestValidations() {
    final HTTPRequestValidationHandler validator = HTTPRequestValidationHandler.create()
        .addQueryParamWithCustomTypeValidator(PARAM_FILE_ID, fileIdValueValidator, true, false)
        .addHeaderParamWithCustomTypeValidator(HEADER_TOKEN, tokenTypeValidator, true, false);
    return validator;
  }
}

package iudx.file.server.apiserver.validations;


import static iudx.file.server.apiserver.utilities.Constants.*;
import java.util.ArrayList;
import java.util.List;

import iudx.file.server.apiserver.validations.types.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.vertx.core.MultiMap;

public class ValidationHandlerFactory {

  private static final Logger LOGGER = LogManager.getLogger(ValidationHandlerFactory.class);

  public List<Validator> create(RequestType requestType, MultiMap parameters, MultiMap headers) {
    LOGGER.debug("type :" + requestType);
    List<Validator> validator = null;
    switch (requestType) {
      case UPLOAD:
        validator = getUploadRequestValidations(parameters, headers);
        break;
      case DOWNLOAD:
        validator = getDownloadRequestValidations(parameters, headers);
        break;
      case DELETE:
        validator = getDeleteRequestValidations(parameters, headers);
        break;
      case TEMPORAL_QUERY:
        validator = getTemporalQueryRequestValidator(parameters);
        break;
      case LIST_QUERY:
        validator = getListQueryRequestValidator(parameters);
        break;
      case GEO_QUERY:
        validator = getGeoQueryRequestValidator(parameters);
        break;
      default:
        break;
    }
    return validator;
  }


  private List<Validator> getUploadRequestValidations(final MultiMap parameters,
      final MultiMap headers) {
    List<Validator> validators = new ArrayList<>();

    validators.add(new IDTypeValidator(parameters.get(PARAM_ID), true));
    validators.add(new DateTypeValidator(parameters.get(PARAM_START_TIME), false));
    validators.add(new DateTypeValidator(parameters.get(PARAM_END_TIME), false));
    validators.add(new SampleTypeValidator(parameters.get(PARAM_SAMPLE), false));
    validators.add(new TokenTypeValidator(headers.get(HEADER_TOKEN), false));

    //// geo (mandatory for upload)
    validators.add(new GeomTypeValidator(parameters.get(PARAM_GEOMETRY), false));
    validators.add(new CoordinatesTypeValidator(parameters.get(PARAM_COORDINATES), false));

    // external storage
    validators.add(new StorageTypeValidator(headers.get(HEADER_EXTERNAL_STORAGE),false));
    validators.add(new StorageURLValidator(parameters.get(PARAM_FILE_URL), Boolean.parseBoolean(headers.get(HEADER_EXTERNAL_STORAGE))));

    return validators;
  }

  private List<Validator> getDownloadRequestValidations(final MultiMap parameters,
      final MultiMap headers) {
    List<Validator> validators = new ArrayList<>();

    validators.add(new FileIdTypeValidator(parameters.get(PARAM_FILE_ID), true));
    validators.add(new TokenTypeValidator(headers.get(HEADER_TOKEN), false));

    return validators;
  }


  private List<Validator> getDeleteRequestValidations(final MultiMap parameters,
      final MultiMap headers) {
    List<Validator> validators = new ArrayList<>();

    validators.add(new FileIdTypeValidator(parameters.get(PARAM_FILE_ID), true));
    validators.add(new TokenTypeValidator(headers.get(HEADER_TOKEN), true));

    // external storage
    validators.add(new StorageTypeValidator(headers.get(HEADER_EXTERNAL_STORAGE),false));

    return validators;
  }


  private List<Validator> getTemporalQueryRequestValidator(final MultiMap parameters) {
    List<Validator> validators = new ArrayList<>();
    // temporal fields(mandatory)
    validators.add(new IDTypeValidator(parameters.get(PARAM_ID), true));
    validators.add(new TemporalRelTypeValidator(parameters.get(PARAM_TIME_REL), true));
    validators.add(new DateTypeValidator(parameters.get("time"), true));
    validators.add(new DateTypeValidator(parameters.get(PARAM_END_TIME), false));

    // pagination optional fields
    validators.add(new PaginationLimitTypeValidator(parameters.get(PARAM_LIMIT), false));
    validators.add(new PaginationOffsetTypeValidator(parameters.get(PARAM_OFFSET), false));

    return validators;
  }

  private List<Validator> getListQueryRequestValidator(final MultiMap parameters) {
    List<Validator> validators = new ArrayList<>();

    validators.add(new IDTypeValidator(parameters.get(PARAM_ID), true));

    return validators;
  }

  private List<Validator> getGeoQueryRequestValidator(final MultiMap parameters) {
    List<Validator> validators = new ArrayList<>();

    validators.add(new IDTypeValidator(parameters.get(PARAM_ID), true));
    validators.add(new GeoRelationTypeValidator(parameters.get(PARAM_GEOREL), true));
    validators.add(new GeomTypeValidator(parameters.get(PARAM_GEOMETRY), true));
    validators.add(new CoordinatesTypeValidator(parameters.get(PARAM_COORDINATES), true));

    return validators;
  }
}

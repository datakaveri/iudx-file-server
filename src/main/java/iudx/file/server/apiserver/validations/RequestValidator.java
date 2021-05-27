package iudx.file.server.apiserver.validations;

import static iudx.file.server.apiserver.utilities.Constants.*;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.Set;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.Promise;
import iudx.file.server.apiserver.utilities.RestResponse;

public class RequestValidator {

  private static final Logger LOGGER = LogManager.getLogger(RequestValidator.class);

  private static Set<String> validParams = new HashSet<String>();
  static {
    validParams.add(PARAM_TIME);
    validParams.add(PARAM_ID);
    validParams.add(PARAM_START_TIME);
    validParams.add(PARAM_END_TIME);
    validParams.add(PARAM_TIME_REL);
    validParams.add(PARAM_GEOREL);
    validParams.add(PARAM_GEOMETRY);
    validParams.add(PARAM_GEOPROPERTY);
    validParams.add(PARAM_COORDINATES);

  }


  public Future<Boolean> isValid(MultiMap map) {
    Promise<Boolean> promise = Promise.promise();
    if (isValidParams(map)) {
      LOGGER.debug("valid params");
      promise.complete(true);
    } else {
      LOGGER.error("Invalid query param found in request");
      promise.fail("Invalid query param.");
    }
    return promise.future();
  }


  private boolean isValidParams(MultiMap map) {
    final List<Entry<String, String>> entries = map.entries();
    for (final Entry<String, String> entry : entries) {
      if (!validParams.contains(entry.getKey())) {
        return false;
      }
    }
    return true;
  }


  private Future<Boolean> isValidGeoQuery(MultiMap map) {
    Promise<Boolean> promsie = Promise.promise();
    if (map.contains(PARAM_GEOREL) && map.contains(PARAM_COORDINATES)
        && map.contains(PARAM_GEOMETRY)) {
      promsie.complete(true);
    } else {
      LOGGER.error("Invalid geo query");
      promsie.fail("Invalid geo query");
    }
    return promsie.future();
  }

  public Future<Boolean> isValidArchiveRequest(MultiMap params) {
    Promise<Boolean> promise = Promise.promise();
    if (params.contains(PARAM_GEOMETRY) && params.contains(PARAM_COORDINATES)
        && params.contains(PARAM_START_TIME) && params.contains(PARAM_END_TIME)) {
      promise.complete(true);
    } else {
      promise.fail(new RestResponse.Builder()
          .type(400)
          .title("Bad query")
          .details("Bad request").build()
          .toJsonString());
      LOGGER.error("Invalid archieve request, All mandatory fields are required");
    }

    return promise.future();
  }

}

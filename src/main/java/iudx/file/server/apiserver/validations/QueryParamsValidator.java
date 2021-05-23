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

public class QueryParamsValidator {
  
  private static final Logger LOGGER = LogManager.getLogger(QueryParamsValidator.class);

  private static Set<String> validParams = new HashSet<String>();
  static {
    validParams.add("id");
    validParams.add("time");
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

}

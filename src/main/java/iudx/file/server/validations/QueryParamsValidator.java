package iudx.file.server.validations;

import static iudx.file.server.utilities.Constants.*;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.Promise;

public class QueryParamsValidator {

  private static Set<String> validParams = new HashSet<String>();
  static {
    validParams.add("id");
    validParams.add("time");
    validParams.add(PARAM_ID);
    validParams.add(PARAM_START_TIME);
    validParams.add(PARAM_END_TIME);
    validParams.add(PARAM_TIME_REL);
  }


  public Future<Boolean> isValid(MultiMap map) {
    Promise<Boolean> promise = Promise.promise();
    if (isValidParams(map)) {
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

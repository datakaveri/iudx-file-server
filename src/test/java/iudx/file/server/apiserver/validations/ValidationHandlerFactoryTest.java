package iudx.file.server.apiserver.validations;

import static iudx.file.server.apiserver.utilities.Constants.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.file.server.apiserver.validations.types.Validator;

@ExtendWith(VertxExtension.class)
public class ValidationHandlerFactoryTest {

  static ValidationHandlerFactory validationFactory;

  @BeforeAll
  static void init(Vertx vertx, VertxTestContext testContext) {
    validationFactory = new ValidationHandlerFactory();
    testContext.completeNow();
  }

  @Test
  @DisplayName("upload request validations")
  public void getUploadRequestValidations() {

    MultiMap params = MultiMap.caseInsensitiveMultiMap();
    MultiMap headers = MultiMap.caseInsensitiveMultiMap();

    params.set(PARAM_ID, "abcda");
    params.set(PARAM_START_TIME, LocalDateTime.now().minusDays(5).toString());
    params.set(PARAM_END_TIME, LocalDateTime.now().toString());
    params.set(PARAM_SAMPLE, "true");
    params.set(PARAM_GEOMETRY, "point");
    params.set(PARAM_COORDINATES, "[23,75]");

    headers.set(HEADER_TOKEN, "asdasd");

    List<Validator> validators = validationFactory.create(RequestType.UPLOAD, params, headers);
    assertEquals(7, validators.size());
  }


  @Test
  @DisplayName("download request validations")
  public void getDownloadRequestValidations() {
    MultiMap params = MultiMap.caseInsensitiveMultiMap();
    MultiMap headers = MultiMap.caseInsensitiveMultiMap();

    params.set(PARAM_FILE_ID, "asda/asd/aasd/d");
    params.set(HEADER_TOKEN, "asd.asd.asd");

    List<Validator> validators = validationFactory.create(RequestType.DOWNLOAD, params, headers);
    assertEquals(2, validators.size());
  }

  @Test
  @DisplayName("download request validations")
  public void getDeleteRequestValidations() {
    MultiMap params = MultiMap.caseInsensitiveMultiMap();
    MultiMap headers = MultiMap.caseInsensitiveMultiMap();

    params.set(PARAM_FILE_ID, "asda/asd/aasd/d");
    headers.set(HEADER_TOKEN, "asd.asd.asd");

    List<Validator> validators = validationFactory.create(RequestType.DELETE, params, headers);
    assertEquals(2, validators.size());
  }

  @Test
  @DisplayName("temporal query request validations")
  public void getTemporalQueryRequestValidations() {
    MultiMap params = MultiMap.caseInsensitiveMultiMap();
    MultiMap headers = MultiMap.caseInsensitiveMultiMap();

    params.set(PARAM_ID, "asda/asd/aasd/d");
    params.set(PARAM_TIME_REL, "within");
    params.set("time", LocalDateTime.now().minusDays(5).toString());
    params.set(PARAM_END_TIME, LocalDateTime.now().toString());
    params.set(PARAM_LIMIT, "100");
    params.set(PARAM_OFFSET, "0");

    List<Validator> validators = validationFactory.create(RequestType.TEMPORAL_QUERY, params, headers);
    assertEquals(6, validators.size());
  }

  @Test
  @DisplayName("geo query request validations")
  public void getGeoQueryRequestValidations() {
    MultiMap params = MultiMap.caseInsensitiveMultiMap();
    MultiMap headers = MultiMap.caseInsensitiveMultiMap();

    params.set(PARAM_ID, "asda/asd/aasd/d");
    params.set(PARAM_GEOREL, "within");
    params.set(PARAM_GEOMETRY, "point");
    params.set(PARAM_COORDINATES, "[23,75]");

    List<Validator> validators = validationFactory.create(RequestType.GEO_QUERY, params, headers);
    assertEquals(4, validators.size());
  }

  @Test
  @DisplayName("geo query request validations")
  public void getListQueryRequestValidations() {
    MultiMap params = MultiMap.caseInsensitiveMultiMap();
    MultiMap headers = MultiMap.caseInsensitiveMultiMap();

    params.set(PARAM_ID, "asda/asd/aasd/d");

    List<Validator> validators = validationFactory.create(RequestType.LIST_QUERY, params, headers);
    assertEquals(1, validators.size());
  }



}

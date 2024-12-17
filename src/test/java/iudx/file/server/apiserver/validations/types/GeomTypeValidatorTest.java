package iudx.file.server.apiserver.validations.types;

import static org.junit.Assert.assertTrue;
import java.util.stream.Stream;

import iudx.file.server.apiserver.exceptions.DxRuntimeException;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import io.vertx.core.Vertx;
import io.vertx.core.cli.annotations.Description;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;

@ExtendWith(VertxExtension.class)
public class GeomTypeValidatorTest {

  private GeomTypeValidator geomTypeValidator;

  @BeforeEach
  public void setup(Vertx vertx, VertxTestContext testContext) {
    testContext.completeNow();
  }

  static Stream<Arguments> allowedValues() {
    // Add any valid value which will pass successfully.
    return Stream.of(
        Arguments.of("Point", true),
        Arguments.of("point", true),
        Arguments.of("Polygon", true),
        Arguments.of("polygon", true),
        Arguments.of("LineString", true),
        Arguments.of("linestring", true),
        Arguments.of(null, false),
        Arguments.of(" ", false));
  }

  @ParameterizedTest
  @MethodSource("allowedValues")
  @Description("geom type type parameter allowed values.")
  public void testValidGeomTypeValue(String value, boolean required, Vertx vertx,
      VertxTestContext testContext) {
    geomTypeValidator = new GeomTypeValidator(value, required);
    assertTrue(geomTypeValidator.isValid());
    testContext.completeNow();
  }


  static Stream<Arguments> invalidValues() {
    // Add any valid value which will pass successfully.
    return Stream.of(
        Arguments.of("false", true),
        Arguments.of(null, true),
        Arguments.of("   ", true),
        Arguments.of(RandomStringUtils.random(6), true),
        Arguments.of("false", false),
        Arguments.of(RandomStringUtils.random(6), false),
        Arguments.of("1=1", true),
        Arguments.of("ABC=ABC", true));
  }

  @ParameterizedTest
  @MethodSource("invalidValues")
  @Description("geom type parameter invalid values.")
  public void testInvalidGeomTypeValue(String value, boolean required, Vertx vertx,
      VertxTestContext testContext) {
    geomTypeValidator = new GeomTypeValidator(value, required);
    Assertions.assertThrows(DxRuntimeException.class, () -> geomTypeValidator.isValid());
    testContext.completeNow();
  }
}

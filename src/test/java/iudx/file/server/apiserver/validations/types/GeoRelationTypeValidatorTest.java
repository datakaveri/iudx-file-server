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
public class GeoRelationTypeValidatorTest {
  
  private GeoRelationTypeValidator geoRelationValidator;
  
  @BeforeEach
  public void setup(Vertx vertx, VertxTestContext testContext) {
    testContext.completeNow();
  }
  
  static Stream<Arguments> allowedValues() {
    // Add any valid value which will pass successfully.
    return Stream.of(
        Arguments.of("near", true),
        Arguments.of("within", true),
        Arguments.of("intersects", true),
        Arguments.of(null, false),
        Arguments.of(" ", false));
  }
  
  @ParameterizedTest
  @MethodSource("allowedValues")
  @Description("georel type parameter allowed values.")
  public void testValidGeoRelTypeValue(String value, boolean required, Vertx vertx,
      VertxTestContext testContext) {
    geoRelationValidator = new GeoRelationTypeValidator(value, required);
    assertTrue(geoRelationValidator.isValid());
    testContext.completeNow();
  }
  
  
  static Stream<Arguments> invalidValues() {
    // Add any valid value which will pass successfully.
    return Stream.of(
        Arguments.of(null, true),
        Arguments.of("   ", true),
        Arguments.of(RandomStringUtils.random(6), true),
        Arguments.of("ABC", false),
        Arguments.of(RandomStringUtils.random(6), false),
        Arguments.of("1=1", true),
        Arguments.of("ABC=ABC", true));
  }
  
  @ParameterizedTest
  @MethodSource("invalidValues")
  @Description("geom type parameter invalid values.")
  public void testInvalidGeoRelValue(String value, boolean required, Vertx vertx,
      VertxTestContext testContext) {
    geoRelationValidator = new GeoRelationTypeValidator(value, required);
    Assertions.assertThrows(DxRuntimeException.class, () -> geoRelationValidator.isValid());
    testContext.completeNow();
  }

}

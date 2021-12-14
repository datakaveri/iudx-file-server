package iudx.file.server.apiserver.validations.types;

import static org.junit.Assert.*;
import java.util.stream.Stream;

import iudx.file.server.apiserver.exceptions.DxRuntimeException;
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
public class CoordinatesTypeValidatorTest {

  private CoordinatesTypeValidator coordsTypeValidator;

  @BeforeEach
  public void setup(Vertx vertx, VertxTestContext testContext) {
    testContext.completeNow();
  }

  static Stream<Arguments> allowedValues() {
    // Add any valid value which will pass successfully.
    return Stream.of(
        Arguments.of("[21.178,72.834]", true),
        Arguments.of(
            "[[[72.719,21],[72.842,21.2],[72.923,20.8],[72.74,20.34],[72.9,20.1],[72.67,20],[72.719,21]]]",
            true),
        Arguments.of("[[72.8296,21.2],[72.8297,21.15]]", true),
        Arguments.of("[[72.842,21.2],[72.923,20.8],[72.74,20.34],[72.9,20.1],[72.67,20]]", true),
        Arguments.of(null, false),
        Arguments.of(" ", false));
  }

  @ParameterizedTest
  @MethodSource("allowedValues")
  @Description("coordinates type parameter allowed values.")
  public void testValidCoordinatesTypeValue(String value, boolean required,
      VertxTestContext testContext) {
    coordsTypeValidator = new CoordinatesTypeValidator(value, required);
    assertTrue(coordsTypeValidator.isValid());
    testContext.completeNow();
  }

  static Stream<Arguments> invalidValues() {
    return Stream.of(
        Arguments.of(null, true),
        Arguments.of(" ", true),
        Arguments.of("[21.1784567787,72.834]", true),
        Arguments.of("[21.17,72.83467867874564]", true),
        Arguments.of("[21.178,72.834,23.5678]", true),
        Arguments.of("[21178345635353535353534521312,72.8342]", true),
        Arguments.of("[21.17834,7283425675567567567567567567567]", true),
        Arguments.of("[21.17834,72.83425675567567567567567567567]", true),
        Arguments.of(
            "[[[73.8444,18.5307],[73.84357,18.52820],[73.8492,18.52836],[73.84632,18.52250],[73.83816,18.52934],[73.83576,18.52063],[73.84357,18.51631],[73.84992,18.51338],[73.85833,18.51672],[73.86065,18.52185],[73.861770,18.52567],[73.85928,18.52966],[73.85447,18.53414],[73.8444,18.53072]]]",
            true),
        Arguments.of(
            "[[73.8444,18.5307],[73.84357,18.52820],[73.8492,18.52836],[73.84632,18.52250],[73.83816,18.52934],[73.83576,18.52063],[73.84357,18.51631],[73.84992,18.51338],[73.85833,18.51672],[73.86065,18.52185],[73.861770,18.52567],[73.85928,18.52966],[73.85447,18.53414],[73.8444,18.53072]]",
            true),
        Arguments.of(
            "[[[73.8425,18.52791],[73.8411,18.5194],[73.8541,18.51880],[73.85892,18.5275],[73.84250,18.5279235684]]]",
            true),
        Arguments.of("[21.1784,72.834,12.566]", true),
        Arguments.of("[21.1784,72.834A]", true),
        Arguments.of("[21.178A,72.834A]", true)
        );
  }
  
  @ParameterizedTest
  @MethodSource("invalidValues")
  @Description("coordinates type parameter invalid values.")
  public void testInvalidCoordinatesTypeValue(String value, boolean required,
      VertxTestContext testContext) {
    coordsTypeValidator = new CoordinatesTypeValidator(value, required);
    Assertions.assertThrows(DxRuntimeException.class,() -> coordsTypeValidator.isValid());
    testContext.completeNow();
  }
}

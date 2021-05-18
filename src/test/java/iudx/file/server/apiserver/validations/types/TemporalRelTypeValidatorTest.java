package iudx.file.server.apiserver.validations.types;

import static org.junit.Assert.*;
import java.util.stream.Stream;
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
public class TemporalRelTypeValidatorTest {
  private TemporalRelTypeValidator temporalRelTypeValidator;

  @BeforeEach
  public void setup(Vertx vertx, VertxTestContext testContext) {
    testContext.completeNow();
  }

  static Stream<Arguments> allowedValues() {
    // Add any valid value which will pass successfully.
    return Stream.of(
        Arguments.of("after", true),
        Arguments.of("before", true),
        Arguments.of("during", true),
        Arguments.of("after", false),
        Arguments.of("before", false),
        Arguments.of("during", false),
        Arguments.of(null, false),
        Arguments.of(" ", false));
  }

  @ParameterizedTest
  @MethodSource("allowedValues")
  @Description("temporal rel type parameter allowed values.")
  public void testValidTemporalRelTypeValue(String value, boolean required, Vertx vertx,
      VertxTestContext testContext) {
    temporalRelTypeValidator = new TemporalRelTypeValidator(value, required);
    assertTrue(temporalRelTypeValidator.isValid());
    testContext.completeNow();
  }
  
  
  static Stream<Arguments> invalidValues() {
    // Add any valid value which will pass successfully.
    return Stream.of(
        Arguments.of("After", true),
        Arguments.of("Before", true),
        Arguments.of("During", true),
        Arguments.of("After", false),
        Arguments.of("Before", false),
        Arguments.of("During", false),
        Arguments.of(null, true),
        Arguments.of(" ", true));
  }

  @ParameterizedTest
  @MethodSource("invalidValues")
  @Description("temporal rel type parameter invalid values.")
  public void testInvalidTemporalRelTypeValue(String value, boolean required, Vertx vertx,
      VertxTestContext testContext) {
    temporalRelTypeValidator = new TemporalRelTypeValidator(value, required);
    assertFalse(temporalRelTypeValidator.isValid());
    testContext.completeNow();
  }
  
  
}

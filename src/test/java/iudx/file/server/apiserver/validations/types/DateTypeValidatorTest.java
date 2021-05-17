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
public class DateTypeValidatorTest {

  private DateTypeValidator dateTypeValidator;

  @BeforeEach
  public void setup(Vertx vertx, VertxTestContext testContext) {
    testContext.completeNow();
  }


  static Stream<Arguments> allowedValues() {
    // Add any valid value which will pass successfully.
    return Stream.of(
        Arguments.of("2020-09-15T00:00:00Z", true),
        Arguments.of("2020-09-15T00:00:00Z", false),
        Arguments.of("2020-09-15T00:00:00+05:30", true),
        Arguments.of(null, false),
        Arguments.of("  ", false));
  }

  @ParameterizedTest
  @MethodSource("allowedValues")
  @Description("date type parameter allowed values.")
  public void testValidDateTypeValue(String value, boolean required, Vertx vertx,
      VertxTestContext testContext) {
    dateTypeValidator = new DateTypeValidator(value, required);
    assertTrue(dateTypeValidator.isValid());
    testContext.completeNow();
  }


  static Stream<Arguments> invalidValues() {
    // Add any valid value which will pass successfully.
    return Stream.of(
        Arguments.of("2020-09-15X00:00:00Z", true),
        Arguments.of("ABC$", true),
        Arguments.of("-- ! 1=1", true),
        Arguments.of("ABC==ABC", true),
        Arguments.of("-- ! 1=1", false),
        Arguments.of("ABC==ABC", false),
        Arguments.of(null, true),
        Arguments.of("  ", true));
  }

  @ParameterizedTest
  @MethodSource("invalidValues")
  @Description("date type parameter invalid values.")
  public void testInvalidDateTypeValue(String value, boolean required, Vertx vertx,
      VertxTestContext testContext) {
    dateTypeValidator = new DateTypeValidator(value, required);
    assertFalse(dateTypeValidator.isValid());
    testContext.completeNow();
  }
}

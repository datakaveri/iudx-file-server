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
public class TokenTypeValidatorTest {

  private TokenTypeValidator tokenTypeValidator;

  @BeforeEach
  public void setup(Vertx vertx, VertxTestContext testContext) {
    testContext.completeNow();
  }

  static Stream<Arguments> allowedValues() {
    return Stream.of(
        Arguments.of("eysds.aweawea.sas", true),
        Arguments.of(null, false),
        Arguments.of(" ", false));
  }

  @ParameterizedTest
  @MethodSource("allowedValues")
  @Description("coordinates type parameter allowed values.")
  public void testValidCoordinatesTypeValue(String value, boolean required,
      VertxTestContext testContext) {
    tokenTypeValidator = new TokenTypeValidator(value, required);
    assertTrue(tokenTypeValidator.isValid());
    testContext.completeNow();
  }

  static Stream<Arguments> invalidValues() {
    return Stream.of(
        Arguments.of(null, true),
        Arguments.of(" ", true));
  }

  @ParameterizedTest
  @MethodSource("invalidValues")
  @Description("coordinates type parameter invalid values.")
  public void testinValidCoordinatesTypeValue(String value, boolean required,
      VertxTestContext testContext) {
    tokenTypeValidator = new TokenTypeValidator(value, required);
    assertFalse(tokenTypeValidator.isValid());
    testContext.completeNow();
  }

}

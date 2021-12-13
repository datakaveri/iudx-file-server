package iudx.file.server.apiserver.validations.types;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
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
public class PaginationLimitTypeValidatorTest {

  private PaginationLimitTypeValidator paginationLimitTypeValidator;

  @BeforeEach
  public void setup(Vertx vertx, VertxTestContext testContext) {
    testContext.completeNow();
  }

  static Stream<Arguments> allowedValues() {
    // Add any valid value which will pass successfully.
    return Stream.of(
        Arguments.of(null, false),
        Arguments.of("1000", false),
        Arguments.of("5000", false),
        Arguments.of("7500", false),
        Arguments.of("10000", false),
        Arguments.of("0", false));
  }

  @ParameterizedTest
  @MethodSource("allowedValues")
  @Description("pagination limit type parameter allowed values.")
  public void testValidLimitTypeValue(String value, boolean required, Vertx vertx,
      VertxTestContext testContext) {
    paginationLimitTypeValidator = new PaginationLimitTypeValidator(value, required);
    assertTrue(paginationLimitTypeValidator.isValid());
    testContext.completeNow();
  }


  static Stream<Arguments> invalidValues() {
    // Add any valid value which will pass successfully.
    return Stream.of(
        Arguments.of(" ", true),
        Arguments.of("-1", false),
        Arguments.of("10001", false),
        Arguments.of("   ", false),
        Arguments.of("7896541233568796313611634", false),
        Arguments.of("false", false),
        Arguments.of("kajlksdjloasknfdlkanslodnmalsdasd", false));
  }

  @ParameterizedTest
  @MethodSource("invalidValues")
  @Description("pagination limit type parameter invalid values.")
  public void testInvalidLimitTypeValue(String value, boolean required, Vertx vertx,
      VertxTestContext testContext) {
    paginationLimitTypeValidator = new PaginationLimitTypeValidator(value, required);
    Assertions.assertThrows(DxRuntimeException.class, () -> paginationLimitTypeValidator.isValid());
    testContext.completeNow();
  }
}

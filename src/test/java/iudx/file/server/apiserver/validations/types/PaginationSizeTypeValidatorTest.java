package iudx.file.server.apiserver.validations.types;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
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
public class PaginationSizeTypeValidatorTest {

private PaginationSizeTypeValidator paginationSizeTypeValidator;
  
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
  @Description("pagination from type parameter allowed values.")
  public void testValidFromTypeValue(String value, boolean required, Vertx vertx,
      VertxTestContext testContext) {
    paginationSizeTypeValidator = new PaginationSizeTypeValidator(value, required);
    assertTrue(paginationSizeTypeValidator.isValid());
    testContext.completeNow();
  }
  
  
  static Stream<Arguments> invalidValues() {
    // Add any valid value which will pass successfully.
    return Stream.of(
        Arguments.of("-1", false),
        Arguments.of("10001", false),
        Arguments.of("   ", false),
        Arguments.of("7896541233568796313611634", false),
        Arguments.of("false", false),
        Arguments.of("kajlksdjloasknfdlkanslodnmalsdasd", false));
  }
  
  @ParameterizedTest
  @MethodSource("invalidValues")
  @Description("pagination from type parameter invalid values.")
  public void testInvalidFromTypeValue(String value, boolean required, Vertx vertx,
      VertxTestContext testContext) {
    paginationSizeTypeValidator = new PaginationSizeTypeValidator(value, required);
    assertFalse(paginationSizeTypeValidator.isValid());
    testContext.completeNow();
  }
}

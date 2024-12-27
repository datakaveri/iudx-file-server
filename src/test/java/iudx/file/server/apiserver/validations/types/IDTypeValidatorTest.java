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

//TODO : regex for id type
@ExtendWith(VertxExtension.class)
public class IDTypeValidatorTest {

  private iudx.file.server.apiserver.validations.types.IdTypeValidator IdTypeValidator;

  @BeforeEach
  public void setup(Vertx vertx, VertxTestContext testContext) {
    testContext.completeNow();
  }

  static Stream<Arguments> allowedValues() {
    // Add any valid value which will pass successfully.
    return Stream.of(
        Arguments.of("b58da193-23d9-43eb-b98a-a103d4b6103c", true),
        Arguments.of("5b7556b5-0779-4c47-9cf2-3f209779aa22", false),
        Arguments.of("b58da193-23d9-43eb-b98a-a103d4b6103c", true),
        Arguments.of("5b7556b5-0779-4c47-9cf2-3f209779aa22", false),
        Arguments.of(null, false));
  }

  @ParameterizedTest
  @MethodSource("allowedValues")
  @Description("id type parameter allowed values.")
  public void testValidIdTypeValue(String value, boolean required, Vertx vertx,
      VertxTestContext testContext) {
    IdTypeValidator = new IdTypeValidator(value, required);
    assertTrue(IdTypeValidator.isValid());
    testContext.completeNow();
  }


  static Stream<Arguments> invalidValues() {
    // Add any valid value which will pass successfully.
    return Stream.of(
        Arguments.of("iisc.ac.in/sample.txt",true),
        Arguments.of("iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/sample.txt",true),
        Arguments.of("ABC=ABC",true),
        Arguments.of(RandomStringUtils.random(513),true),
        Arguments.of("  ", true),
        Arguments.of(null, true));
  }

  @ParameterizedTest
  @MethodSource("invalidValues")
  @Description("id type parameter allowed values.")
  public void testInvalidIdTypeValue(String value, boolean required, Vertx vertx,
      VertxTestContext testContext) {
    IdTypeValidator = new IdTypeValidator(value, required);
    Assertions.assertThrows(DxRuntimeException.class, () -> IdTypeValidator.isValid());
    testContext.completeNow();
  }
}

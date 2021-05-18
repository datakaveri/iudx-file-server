package iudx.file.server.apiserver.validations.types;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.util.stream.Stream;
import org.apache.commons.lang.RandomStringUtils;
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

  private IDTypeValidator IdTypeValidator;

  @BeforeEach
  public void setup(Vertx vertx, VertxTestContext testContext) {
    testContext.completeNow();
  }

  static Stream<Arguments> allowedValues() {
    // Add any valid value which will pass successfully.
    return Stream.of(
        Arguments.of(
            "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/file.iudx.io/surat-itms-realtime-information/surat-itms-live-eta",
            true),
        Arguments.of(
            "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/file.iudx.io/surat-itms-realtime-information",
            false),
        Arguments.of(
            "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/file.iudx.io/surat-itms-realtime-information/surat-itms-live-eta",
            true),
        Arguments.of(
            "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/file.iudx.io/surat-itms-realtime-information",
            false),
        Arguments.of(null, false));
  }

  @ParameterizedTest
  @MethodSource("allowedValues")
  @Description("id type parameter allowed values.")
  public void testValidIdTypeValue(String value, boolean required, Vertx vertx,
      VertxTestContext testContext) {
    IdTypeValidator = new IDTypeValidator(value, required);
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
    IdTypeValidator = new IDTypeValidator(value, required);
    assertFalse(IdTypeValidator.isValid());
    testContext.completeNow();
  }
}

package iudx.file.server.apiserver.validations.types;

import static org.junit.Assert.*;
import java.util.stream.Stream;

import iudx.file.server.apiserver.exceptions.DxRuntimeException;
import org.apache.commons.lang.RandomStringUtils;
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
public class FileIdTypeValidatorTest {

  private FileIdTypeValidator fileIdTypeValidator;

  @BeforeEach
  public void setup(Vertx vertx, VertxTestContext testContext) {
    testContext.completeNow();
  }

  static Stream<Arguments> allowedValues() {
    // Add any valid value which will pass successfully.
    return Stream.of(
        Arguments.of(
            "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/file.iudx.io/surat-itms-realtime-information/surat-itms-live-eta/sample.txt",
            true),
        Arguments.of(
            "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/file.iudx.io/surat-itms-realtime-information/sample.txt",
            false),
        Arguments.of(
            "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/file.iudx.io/surat-itms-realtime-information/surat-itms-live-eta/asdad-aasd8ad.txt",
            true),
        Arguments.of(
            "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/file.iudx.io/surat-itms-realtime-information/ada-89ad.txt",
            false),
        Arguments.of(null, false));
  }

  @ParameterizedTest
  @MethodSource("allowedValues")
  @Description("fileid type parameter allowed values.")
  public void testValidFileIdTypeValue(String value, boolean required, Vertx vertx,
      VertxTestContext testContext) {
    fileIdTypeValidator = new FileIdTypeValidator(value, required);
    assertTrue(fileIdTypeValidator.isValid());
    testContext.completeNow();
  }


  static Stream<Arguments> invalidValues() {
    // Add any valid value which will pass successfully.
    return Stream.of(
        Arguments.of("iisc.ac.in/sample.txt",true),
        Arguments.of("iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/sample.txt",true),
        Arguments.of("asdad-aasd8ad.txt",true),
        Arguments.of(RandomStringUtils.random(513),true),
        Arguments.of("  ", true),
        Arguments.of(null, true));
  }

  @ParameterizedTest
  @MethodSource("invalidValues")
  @Description("fileId type parameter allowed values.")
  public void testInvalidFileIdTypeValue(String value, boolean required, Vertx vertx,
      VertxTestContext testContext) {
    fileIdTypeValidator = new FileIdTypeValidator(value, required);
    Assertions.assertThrows(DxRuntimeException.class,() -> fileIdTypeValidator.isValid());
    testContext.completeNow();
  }
}

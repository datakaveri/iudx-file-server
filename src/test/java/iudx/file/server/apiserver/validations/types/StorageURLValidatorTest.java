package iudx.file.server.apiserver.validations.types;

import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.file.server.apiserver.exceptions.DxRuntimeException;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(VertxExtension.class)

public class StorageURLValidatorTest {
    private static StorageUrlValidator storageURLValidator;

    public static Stream<Arguments> validValues() {
        return Stream.of(
                Arguments.of(null, false),
                Arguments.of("", false),
                Arguments.of(
                        "https://rs.iudx.org.in/ngsi-ld/v1/entities/suratmunicipal.org/6db486cb4f720e8585ba1f45a931c63c25dbbbda/rs.iudx.org.in/surat-itms-realtime-info/surat-itms-live-eta",
                        true));
    }

    public static Stream<Arguments> inValidValues() {
        return Stream.of(
                Arguments.of(null, true),
                Arguments.of("", true),
                Arguments.of("Dummy URL Value", true)

        );
    }

    @ParameterizedTest(name = "{index}) value = {0}, required = {1}")
    @DisplayName("Test isValid method with valid values")
    @MethodSource("validValues")
    public void testIsValidWithValidValues(String value, boolean required, VertxTestContext vertxTestContext) {
        storageURLValidator = new StorageUrlValidator(value, required);
        assertTrue(storageURLValidator.isValid());
        vertxTestContext.completeNow();

    }

    @ParameterizedTest(name = "{index}) value = {0}, required = {1}")
    @DisplayName("Test isValid method with invalid values")
    @MethodSource("inValidValues")
    public void testIsValidWhenRequiredTrue(String value, boolean required, VertxTestContext vertxTestContext) {
        storageURLValidator = new StorageUrlValidator(value, required);
        assertThrows(DxRuntimeException.class, () -> storageURLValidator.isValid());
        vertxTestContext.completeNow();
    }

    @DisplayName("Test failureMessage method")
    @Test
    public void testFailureMessage(VertxTestContext vertxTestContext) {

        assertEquals("Invalid storage url type value", storageURLValidator.failureMessage());
        vertxTestContext.completeNow();
    }

    @DisplayName("Test failureCode method")
    @Test
    public void testFailureCode(VertxTestContext vertxTestContext) {
        assertEquals(400, storageURLValidator.failureCode());
        vertxTestContext.completeNow();
    }

}

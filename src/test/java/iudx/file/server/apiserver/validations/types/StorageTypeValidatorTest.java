package iudx.file.server.apiserver.validations.types;

import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.file.server.apiserver.exceptions.DxRuntimeException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(VertxExtension.class)
@ExtendWith(MockitoExtension.class)
public class StorageTypeValidatorTest {
    private StorageTypeValidator storageTypeValidator;

    private static Stream<Arguments> validData() {
        return Stream.of(
                Arguments.of(null, false),
                Arguments.of("", false),
                Arguments.of("abcd", false),
                Arguments.of("abcd", true)
        );
    }

    private static Stream<Arguments> invalidData() {
        return Stream.of(
                Arguments.of(null, true),
                Arguments.of("", true)
        );
    }

    @ParameterizedTest(name = "{index}) value = {0}, required = {1}")
    @MethodSource("validData")
    @DisplayName("Test isValid method")
    public void testIsValid(String value, boolean required, VertxTestContext vertxTestContext) {

        storageTypeValidator = new StorageTypeValidator(value, required);
        assertTrue(storageTypeValidator.isValid());
        vertxTestContext.completeNow();
    }

    @ParameterizedTest(name = "{index}) value = {0}, required = {1}")
    @MethodSource("invalidData")
    @DisplayName("Test isValid method for DxRuntimeException")
    public void testIsValidForDxRuntimeException(String value, boolean required, VertxTestContext vertxTestContext) {
        storageTypeValidator = new StorageTypeValidator(value, required);
        assertThrows(DxRuntimeException.class, () -> storageTypeValidator.isValid());
        vertxTestContext.completeNow();
    }

    @DisplayName("Test failureCode method")
    @Test
    public void testFailureCode(VertxTestContext vertxTestContext) {
        storageTypeValidator = new StorageTypeValidator(null, false);
        assertEquals(400, storageTypeValidator.failureCode());
        vertxTestContext.completeNow();
    }

    @DisplayName("Test failureMessage method")
    @Test
    public void testFailureMessage(VertxTestContext vertxTestContext) {
        storageTypeValidator = new StorageTypeValidator(null, false);
        assertEquals("Invalid token", storageTypeValidator.failureMessage());
        vertxTestContext.completeNow();
    }
}

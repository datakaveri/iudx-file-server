package iudx.file.server.apiserver.validations.types;

import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.file.server.apiserver.exceptions.DxRuntimeException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(VertxExtension.class)
public class ShortageURLValidatorTest {

    private static String value;
    private static boolean required;
    private static StorageURLValidator storageURLValidator;

    @DisplayName("Test isValid method when value is null and required is false ")
    @Test
    public void testIsValidWhenRequiredFalse(VertxTestContext vertxTestContext) {
        storageURLValidator = new StorageURLValidator(null, required);
        assertTrue(storageURLValidator.isValid());
        vertxTestContext.completeNow();
    }

    @DisplayName("Test isValid method when value is null and required is true")
    @Test
    public void testIsValidWhenRequiredTrue(VertxTestContext vertxTestContext) {
        storageURLValidator = new StorageURLValidator(null, true);
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

    @DisplayName("Test isValidValue method with Invalid URL value")
    @Test
    public void isValidValueWithInvalidValue(VertxTestContext vertxTestContext) {
        value = "Dummy URL Value";
        storageURLValidator = new StorageURLValidator(value, true);
        assertThrows(DxRuntimeException.class,
                () -> storageURLValidator.isValid());
        vertxTestContext.completeNow();
    }

    @DisplayName("Test is isValidValue with valid URL value")
    @Test
    public void isValidWithValidValue(VertxTestContext vertxTestContext) {
        value = "https://rs.iudx.org.in/ngsi-ld/v1/entities/suratmunicipal.org/6db486cb4f720e8585ba1f45a931c63c25dbbbda/rs.iudx.org.in/surat-itms-realtime-info/surat-itms-live-eta";
        storageURLValidator = new StorageURLValidator(value, true);
        assertTrue(storageURLValidator.isValid());
        vertxTestContext.completeNow();
    }

}

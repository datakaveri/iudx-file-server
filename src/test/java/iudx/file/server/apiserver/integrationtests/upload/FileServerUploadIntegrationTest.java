package iudx.file.server.apiserver.integrationtests.upload;

import io.restassured.http.ContentType;
import iudx.file.server.apiserver.integrationtests.RestAssuredConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import java.io.*;
import static org.hamcrest.Matchers.equalTo;
import static io.restassured.RestAssured.given;
import static iudx.file.server.apiserver.integrationtests.TokenHelper.delegateToken;
import static org.hamcrest.Matchers.notNullValue;

@ExtendWith(RestAssuredConfiguration.class)
public class FileServerUploadIntegrationTest {

    private File createTempFileWithContent() {
        // Create a temporary file
        File tempFile;
        try {
            tempFile = File.createTempFile("test", ".txt");
        } catch (IOException e) {
            throw new RuntimeException("Failed to create a temporary file", e);
        }

        // Write content to the file
        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write("This is the content of the file for testing purposes.");
        } catch (IOException e) {
            throw new RuntimeException("Failed to write content to the temporary file", e);
        }

        return tempFile;
    }
    // Create a temporary file and get its reference
    File tempFile = createTempFileWithContent();
    String id ="83c2e5c2-3574-4e11-9530-2b1fbdfce832";
    boolean isSample=true;
    String invalidToken ="abc";
    String fileDownloadURL = "https://docs.google.com/document/d/19f6oOIxHVjC3twcRHQATjrEXJsDO0rLixoFgLV7xMxk/edit?usp=sharing";

    @Test
    @DisplayName("200 (Success) DX file upload - Resource level (sample)")
    public void fileUploadSuccessTest() {

        given()
                .multiPart("file", tempFile, "text/plain")
                .formParam("id", id)
                .formParam("isSample", isSample)
                .header("token", delegateToken)
                .when()
                .post("/upload")
                .then()
                .statusCode(200)
                .log().body()
                .contentType(ContentType.JSON)
                .body("results", notNullValue())
                .body("results[0].fileId", notNullValue());
    }
    @Test
    @DisplayName("401 (not authorized) DX file upload - Resource level (sample)")
    public void unauthorisedFileUploadTest() {

        given()
                .multiPart("file", tempFile, "text/plain")
                .formParam("id", id)
                .formParam("isSample", isSample)
                .header("token", invalidToken)
                .when()
                .post("/upload")
                .then()
                .statusCode(401)
                .log().body()
                .contentType(ContentType.JSON)
                .body("type", equalTo("urn:dx:rs:invalidAuthorizationToken"))
                .body("title", equalTo("Not authorized"))
                .body("detail", equalTo("Token is invalid"));
    }
    @Test
    @DisplayName("200 (Success) - Archive Resource Level")
    public void successArchiveTest() {
        // Create a temporary file and get its reference
        //File tempFile = createTempFileWithContent();
        given()
                .multiPart("file", tempFile, "text/plain")
                .formParam("id", id)
                .formParam("startTime", "2020-09-05T00:00:00Z")
                .formParam("endTime", "2020-09-15T00:00:00Z")
                .formParam("geometry", "point")
                .formParam("coordinates", "[72.81,21.16]")
                .header("token", delegateToken)
                .when()
                .post("/upload")
                .then()
                .statusCode(200)
                .log().body()
                .contentType(ContentType.JSON)
                .body("type", equalTo("urn:dx:rs:success"))
                .body("title", equalTo("Success"))
                .body("results", notNullValue())
                .body("results[0].fileId", notNullValue());
    }
    @Test
    @DisplayName("200 (Success) - Archive Resource Level")
    public void unauthorisedArchiveTest() {
        // Create a temporary file and get its reference
        //File tempFile = createTempFileWithContent();

        given()
                .multiPart("file", tempFile, "text/plain")
                .formParam("id", id)
                .formParam("startTime", "2020-09-05T00:00:00Z")
                .formParam("endTime", "2020-09-15T00:00:00Z")
                .formParam("geometry", "point")
                .formParam("coordinates", "[72.81,21.16]")
                .header("token",invalidToken)
                .when()
                .post("/upload")
                .then()
                .statusCode(401)
                .log().body()
                .contentType(ContentType.JSON)
                .body("type", equalTo("urn:dx:rs:invalidAuthorizationToken"))
                .body("title", equalTo("Not authorized"))
                .body("detail", equalTo("Token is invalid"));
    }
    @Test
    @DisplayName("400 (No id param in request) DX file upload")
    public void invalidParamFileUploadTest() {
        // Create a temporary file and get its reference
        //File tempFile = createTempFileWithContent();

        given()
                .multiPart("file", tempFile, "text/plain")
                .formParam("id1", id)
                .formParam("isSample", isSample)
                .header("token",delegateToken)
                .when()
                .post("/upload")
                .then()
                .statusCode(400)
                .log().body()
                .contentType(ContentType.JSON)
                .body("type", equalTo("urn:dx:rs:invalidPayloadFormat"))
                .body("title", equalTo("Bad Request"))
                .body("detail", equalTo("Validation error : null or blank value for required mandatory field"));
    }
    @Test
    @DisplayName("400 (Invalid isSample value) DX file upload")
    public void invalidIsSampleFileUploadTest() {
        // Create a temporary file and get its reference
        //File tempFile = createTempFileWithContent();
        given()
                .multiPart("file", tempFile, "text/plain")
                .formParam("id", id)
                .formParam("isSample", "true1")
                .header("token",delegateToken)
                .when()
                .post("/upload")
                .then()
                .statusCode(400)
                .log().body()
                .contentType(ContentType.JSON)
                .body("type", equalTo("urn:dx:rs:invalidAttributeValue"))
                .body("title", equalTo("Bad Request"))
                .body("detail", equalTo("Validation error : Invalid isSample field value [ true1 ]"));
    }
    @Test
    @DisplayName("200 (Success) DX file upload - Resource level (External Storage)")
    public void fileUploadExternalStorageTest() {
        // Create a temporary file and get its reference
        //File tempFile = createTempFileWithContent();
        given()
                .multiPart("file", tempFile, "text/plain")
                .formParam("id", id)
                .formParam("startTime", "2020-09-05T00:00:00Z")
                .formParam("endTime", "2020-09-15T00:00:00Z")
                .formParam("geometry", "point")
                .formParam("coordinates", "[72.81,21.16]")
                .formParam("file-download-url",fileDownloadURL)
                .header("token", delegateToken)
                .when()
                .post("/upload")
                .then()
                .statusCode(200)
                .log().body()
                .contentType(ContentType.JSON)
                .body("type", equalTo("urn:dx:rs:success"))
                .body("title", equalTo("Success"))
                .body("results", notNullValue())
                .body("results[0].fileId", notNullValue());
    }

}

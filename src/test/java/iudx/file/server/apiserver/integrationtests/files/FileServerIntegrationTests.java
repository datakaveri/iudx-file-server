package iudx.file.server.apiserver.integrationtests.files;

import io.restassured.http.ContentType;
import io.vertx.core.json.JsonObject;
import iudx.file.server.apiserver.integrationtests.RestAssuredConfiguration;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import java.io.*;


import static iudx.file.server.authenticator.TokensForITs.*;
import static org.hamcrest.Matchers.equalTo;
import static io.restassured.RestAssured.given;

import static org.hamcrest.Matchers.notNullValue;

@ExtendWith(RestAssuredConfiguration.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class FileServerIntegrationTests {

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

    private static String sampleFileId;
    private static String archiveFileId;
    private static String externalStorageFileId;
    String invalidFileId = "_abced";
    String nonExistingArchiveId ="83c2e5c2-3574-4e11-9530-2b1fbdfce832/8185010f-705d-4966-ac44-2050887c68f3_invalid.txt";

    boolean isSample=true;
    String invalidToken ="abc";
    String fileDownloadURL = "https://docs.google.com/document/d/19f6oOIxHVjC3twcRHQATjrEXJsDO0rLixoFgLV7xMxk/edit?usp=sharing";

    //File Upload
    @Test
    @Order(1)
    @DisplayName("200 (Success) DX file upload - Resource level (sample)")
    public void sampleFileUploadSuccessTest() {

        JsonObject respJson = new JsonObject(given()
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
                .body("results[0].fileId", notNullValue())
                .extract()
                .asString());
        sampleFileId = respJson.getJsonArray("results").getJsonObject(0).getString("fileId");

    }
    @Test
    @Order(2)
    @DisplayName("401 (not authorized) DX file upload - Resource level (sample)")
    public void unauthorisedSampleFileUploadTest() {

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
    @Order(3)
    @DisplayName("200 (Success) - Archive Resource Level")
    public void archiveFileUploadSuccessTest() {
        // Create a temporary file and get its reference
        //File tempFile = createTempFileWithContent();
        JsonObject respJson = new JsonObject(given()
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
                .body("results[0].fileId", notNullValue())
                .extract()
                .asString());
        archiveFileId = respJson.getJsonArray("results").getJsonObject(0).getString("fileId");

    }
    @Test
    @Order(4)
    @DisplayName("401 (not authorized) DX file upload - Resource level (Archive)")
    public void unauthorisedArchiveFileTest() {
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
    @Order(5)
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
    @Order(6)
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
    @Order(7)
    @DisplayName("200 (Success) DX file upload - Resource level (External Storage)")
    public void externalStorageFileUploadSuccessTest() {
        // Create a temporary file and get its reference
        //File tempFile = createTempFileWithContent();
        JsonObject respJson = new JsonObject(given()
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
                .body("results[0].fileId", notNullValue())
                .extract()
                .asString());
        externalStorageFileId = respJson.getJsonArray("results").getJsonObject(0).getString("fileId");
    }

    // File Download
    @Test
    @Order(8)
    @DisplayName("200 (Success) DX file download - RL (Sample file )")
    public void sampleFileDownloadSuccessTest() {
        given()
                .param("file-id", sampleFileId)
                .header("token", openResourceToken)
                .when()
                .get("/download")
                .then()
                .log().body()
                .statusCode(200);
    }
    @Test
    @Order(9)
    @DisplayName("400 (invalid file id) DX file download")
    public void invalidIdSampleFileDownloadTest() {
        given()
                .header("token", openResourceToken)
                .param("file-id", invalidFileId)
                .when()
                .get("/download")
                .then()
                .statusCode(400)
                .log().body()
                .contentType(ContentType.JSON)
                .body("type", equalTo("urn:dx:rs:invalidAttributeValue"))
                .body("title", equalTo("Bad Request"))
                .body("detail", equalTo("Validation error : invalid file id [ " + invalidFileId + " ]"));
    }
    @Test
    @Order(10)
    @DisplayName("200 (Success) DX file download -Resource level (Archive file )")
    public void archiveFileDownloadSuccessTest(){
        given()
                .param("file-id", archiveFileId)
                .header("token", secureResourceToken)
                .when()
                .get("/download")
                .then()
                .log().body()
                .statusCode(200);
    }
    @Test
    @Order(11)
    @DisplayName("401 (not authorized) DX file download - RL (Archive file )")
    public void unauthorisedArchiveFileDownloadTest() {
        given()
                .header("token", invalidToken)
                .param("file-id", archiveFileId)
                .when()
                .get("/download")
                .then()
                .statusCode(401)
                .log().body()
                .contentType(ContentType.JSON)
                .body("type", equalTo("urn:dx:rs:invalidAuthorizationToken"))
                .body("title", equalTo("Not authorized"))
                .body("detail", equalTo("Token is invalid"));
    }
    @Test
    @Order(12)
    @DisplayName("404 (Not Found) DX file download -Resource level (Archive file )")
    public void nonExistingArchiveFileDownloadTest() {
        given()
                .header("token", secureResourceToken)
                .param("file-id", nonExistingArchiveId)
                .when()
                .get("/download")
                .then()
                .statusCode(404)
                .log().body()
                .contentType(ContentType.JSON)
                .body("type", equalTo("urn:dx:rs:resourceNotFound"))
                .body("title", equalTo("Not Found"))
                .body("detail", equalTo("Document of given id does not exist"));
    }


    @AfterEach
    public void tearDown() {
        // Introduce a delay
        try {
            Thread.sleep(1000); // 1 second delay
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

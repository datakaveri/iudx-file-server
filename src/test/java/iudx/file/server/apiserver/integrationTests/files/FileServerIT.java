package iudx.file.server.apiserver.integrationTests.files;

import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import io.vertx.core.json.JsonObject;
import iudx.file.server.apiserver.integrationTests.RestAssuredConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import static io.restassured.RestAssured.baseURI;
import static io.restassured.RestAssured.given;
import static iudx.file.server.authenticator.TokensForITs.*;
import static org.hamcrest.Matchers.*;

@ExtendWith(RestAssuredConfiguration.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class FileServerIT {
  private static final Logger LOGGER = LogManager.getLogger(FileServerIT.class);
  private static final String rsId = RestAssuredConfiguration.getRsId();
  private static final String openRsId = RestAssuredConfiguration.getOpenRsId();
  private static final String openRsGroupId = RestAssuredConfiguration.getOpenRsGroupId();
  private static final String nonExistingArchiveId = RestAssuredConfiguration.getNonExistingArchiveId();
  private static final String fileDownloadURL = RestAssuredConfiguration.getFileDownloadURL();
  private static String sampleFileId;
  private static String archiveFileId;
  private static String externalStorageFileId;
  // Create a temporary file and get its reference
  File tempFile = createTempFileWithContent();
  String invalidFileId = "_abced";
  boolean isSample = true;
  String invalidToken = "abc";

  private File createTempFileWithContent() {
    // Create a temporary file
    tempFile = new File("./file_upload_test.txt");

    // Write content to the file
    try (FileWriter writer = new FileWriter(tempFile)) {
      writer.write("This is the content of the file for testing purposes.");
    } catch (IOException e) {
      throw new RuntimeException("Failed to write content to the temporary file", e);
    }

    return tempFile;
  }

  //File Upload
  @Test
  @Order(1)
  @DisplayName("200 (Success) DX file upload - Resource level (sample)")
  public void sampleFileUploadSuccessTest() {
    JsonObject respJson = new JsonObject(
        given().relaxedHTTPSValidation()
        .multiPart("file", tempFile, "text/plain")
        .formParam("id", rsId)
        .formParam("isSample", isSample)
        .header("token", delegateToken)
        .when()
        .post("/upload")
        .then()
        .statusCode(200)
        //.log().body()
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

    given().relaxedHTTPSValidation()
        .multiPart("file", tempFile, "text/plain")
        .formParam("id", rsId)
        .formParam("isSample", isSample)
        .header("token", invalidToken)
        .when()
        .post("/upload")
        .then()
        .statusCode(401)
        //.log().body()
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
    JsonObject respJson = new JsonObject(given().relaxedHTTPSValidation()
        .multiPart("file", tempFile, "text/plain")
        .formParam("id", rsId)
        .formParam("startTime", "2020-09-05T00:00:00Z")
        .formParam("endTime", "2020-09-15T00:00:00Z")
        .formParam("geometry", "point")
        .formParam("coordinates", "[72.81,21.16]")
        .header("token", delegateToken)
        .when()
        .post("/upload")
        .then()
        .statusCode(200)
        //.log().body()
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

    given().relaxedHTTPSValidation()
        .multiPart("file", tempFile, "text/plain")
        .formParam("id", rsId)
        .formParam("startTime", "2020-09-05T00:00:00Z")
        .formParam("endTime", "2020-09-15T00:00:00Z")
        .formParam("geometry", "point")
        .formParam("coordinates", "[72.81,21.16]")
        .header("token", invalidToken)
        .when()
        .post("/upload")
        .then()
        .statusCode(401)
        //.log().body()
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

    given().relaxedHTTPSValidation()
        .multiPart("file", tempFile, "text/plain")
        .formParam("id1", rsId)
        .formParam("isSample", isSample)
        .header("token", delegateToken)
        .when()
        .post("/upload")
        .then()
        .statusCode(400)
        //.log().body()
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
    given().relaxedHTTPSValidation()
        .multiPart("file", tempFile, "text/plain")
        .formParam("id", rsId)
        .formParam("isSample", "true1")
        .header("token", delegateToken)
        .when()
        .post("/upload")
        .then()
        .statusCode(400)
        //.log().body()
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
    JsonObject respJson = new JsonObject(given().relaxedHTTPSValidation()
        .multiPart("file", tempFile, "text/plain")
        .formParam("id", rsId)
        .formParam("startTime", "2020-09-05T00:00:00Z")
        .formParam("endTime", "2020-09-15T00:00:00Z")
        .formParam("geometry", "point")
        .formParam("coordinates", "[72.81,21.16]")
        .formParam("file-download-url", fileDownloadURL)
        .header("token", delegateToken)
        .when()
        .post("/upload")
        .then()
        .statusCode(200)
        //.log().body()
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
    given().relaxedHTTPSValidation()
        .param("file-id", sampleFileId)
        .header("token", openResourceToken)
        .when()
        .get("/download")
        .then()
        //.log().body()
        .statusCode(200);
  }

  @Test
  @Order(9)
  @DisplayName("400 (invalid file id) DX file download")
  public void invalidIdSampleFileDownloadTest() {
    given().relaxedHTTPSValidation()
        .header("token", openResourceToken)
        .param("file-id", invalidFileId)
        .when()
        .get("/download")
        .then()
        .statusCode(400)
        //.log().body()
        .contentType(ContentType.JSON)
        .body("type", equalTo("urn:dx:rs:invalidAttributeValue"))
        .body("title", equalTo("Bad Request"))
        .body("detail", equalTo("Validation error : invalid file id [ " + invalidFileId + " ]"));
  }

  @Test
  @Order(10)
  @DisplayName("200 (Success) DX file download -Resource level (Archive file )")
  public void archiveFileDownloadSuccessTest() {
    given().relaxedHTTPSValidation()
        .param("file-id", archiveFileId)
        .header("token", secureResourceToken)
        .when()
        .get("/download")
        .then()
        //.log().body()
        .statusCode(200);
  }

  @Test
  @Order(11)
  @DisplayName("401 (not authorized) DX file download - RL (Archive file )")
  public void unauthorisedArchiveFileDownloadTest() {
    given().relaxedHTTPSValidation()
        .header("token", invalidToken)
        .param("file-id", archiveFileId)
        .when()
        .get("/download")
        .then()
        .statusCode(401)
        //.log().body()
        .contentType(ContentType.JSON)
        .body("type", equalTo("urn:dx:rs:invalidAuthorizationToken"))
        .body("title", equalTo("Not authorized"))
        .body("detail", equalTo("Token is invalid"));
  }

  @Test
  @Order(12)
  @DisplayName("404 (Not Found) DX file download -Resource level (Archive file )")
  public void nonExistingArchiveFileDownloadTest() {
    given().relaxedHTTPSValidation()
        .header("token", secureResourceToken)
        .param("file-id", nonExistingArchiveId)
        .when()
        .get("/download")
        .then()
        .statusCode(404)
        //.log().body()
        .contentType(ContentType.JSON)
        .body("type", equalTo("urn:dx:rs:resourceNotFound"))
        .body("title", equalTo("Not Found"))
        .body("detail", equalTo("Document of given id does not exist"));
  }

  //For query APIs
  @Test
  @Order(13)
  @DisplayName("200 (Success) Search for Files of an open resource (using openToken)")
  void GetResourceLevelSample() {
    given().relaxedHTTPSValidation()
        .header("token", openResourceToken)
        .param("id", openRsId)
        .param("time", "2020-09-10T00:00:00Z")
        .param("endTime", "2020-09-15T00:00:00Z")
        .param("timerel", "between")
        .when()
        .get(baseURI + "/ngsi-ld/v1/temporal/entities")
        .then()
        .statusCode(200)
        .body("type", is(200))
        .body("title", is("urn:dx:rs:success"));
  }

  @Test
  @Order(14)
  @DisplayName("200 (Success) Search for Files of an open resource group (using openToken)")
  void GetResourceGroupSample() {
    given().relaxedHTTPSValidation()
        .header("token", openResourceToken)
        .param("id", openRsGroupId)
        .param("time", "2020-09-10T00:00:00Z")
        .param("endTime", "2020-09-15T00:00:00Z")
        .param("timerel", "between")
        .when()
        .get(baseURI + "/ngsi-ld/v1/temporal/entities")
        .then()
        .statusCode(200)
        .body("type", is(200))
        .body("title", is("urn:dx:rs:success"));
  }

  @Test
  @Order(15)
  @DisplayName("401 (Not Authorized) Search for Files")
  void SearchForFilesUnAuth() {
    given().relaxedHTTPSValidation()
        .param("id", "8b95ab80-2aaf-4636-a65e-7f2563d0d371")
        .param("time", "2020-09-10T00:00:00Z")
        .param("endTime", "2020-09-15T00:00:00Z")
        .param("timerel", "between")
        .when()
        .get(baseURI + "/ngsi-ld/v1/temporal/entities")
        .then()
        .statusCode(401)
        .body("type", is("urn:dx:rs:missingAuthorizationToken"))
        .body("title", is("Not authorized"))
        .body("detail", is("Token needed and not present"));
  }

  @Test
  @Order(16)
  @DisplayName("200 (Success) Search for Files using Spatial [geo(circle)]")
  void searchForFilesUsingSpatialGeoCircle() {
    String georel = "near;maxDistance=10000";
    String geometry = "point";
    String coordinates = "[72.79,21.16]";
    RequestSpecification requestSpec = given().relaxedHTTPSValidation().urlEncodingEnabled(false);
    given().relaxedHTTPSValidation()
        .header("token", secureResourceToken)
        .spec(requestSpec)
        .queryParam("id", rsId)
        .queryParam("georel", georel)
        .queryParam("geometry", geometry)
        .queryParam("coordinates", coordinates)
        .when()
        .get(baseURI + "/ngsi-ld/v1/entities")
        .then()
        .statusCode(200)
        .body("type", is(200))
        .body("title", is("urn:dx:rs:success"))
        .extract()
        .response();
  }

  @Test
  @Order(17)
  @DisplayName("200 (Success) Search for Files using Spatial [geo(Polygon)]")
  void SearchForFilesUsingSpatialGeoPolygon() {
    given().relaxedHTTPSValidation()
        .header("token", secureResourceToken)
        .param("id", rsId)
        .param("georel", "within")
        .param("geometry", "polygon")
        .param("coordinates", "[[[72.7815,21.1726],[72.7856,21.1519],[72.807,21.1527],[72.8170,21.1680],[72.800,21.1808],[72.7815,21.1726]]]")
        .when()
        .get(baseURI + "/ngsi-ld/v1/entities")
        .then()
        .statusCode(200)
        .body("type", is(200))
        .body("title", is("urn:dx:rs:success"));
  }

  @Test
  @Order(18)
  @DisplayName("200 (Success) Complex Search [temporal+geo(Circle) search]")
  void ComplexSearchForFilesUsingTemporalPlusGeoPolygon() {
    given().relaxedHTTPSValidation()
        .header("token", secureResourceToken)
        .param("id", rsId)
        .param("time", "2020-09-10T00:00:00Z")
        .param("endTime", "2020-09-15T00:00:00Z")
        .param("timerel", "between")
        .param("georel", "near;maxDistance=10000")
        .param("geometry", "point")
        .param("coordinates", "[72.79,21.16]")
        .when()
        .get(baseURI + "/ngsi-ld/v1/temporal/entities")
        .then()
        .statusCode(200)
        .body("type", is(200))
        .body("title", is("urn:dx:rs:success"));
  }

  @Test
  @Order(19)
  @DisplayName("401 (not authorized) Complex Search [temporal+geo(Circle) search]")
  void ComplexSearchForFilesUsingTemporalPlusGeoPolygonUnAuth() {
    given().relaxedHTTPSValidation()
        .param("id", rsId)
        .param("time", "2020-09-10T00:00:00Z")
        .param("endTime", "2020-09-15T00:00:00Z")
        .param("timerel", "between")
        .param("georel", "near;maxDistance=10000")
        .param("geometry", "point")
        .param("coordinates", "[72.79,21.16]")
        .when()
        .get(baseURI + "/ngsi-ld/v1/temporal/entities")
        .then()
        .statusCode(401)
        .body("type", is("urn:dx:rs:missingAuthorizationToken"))
        .body("title", is("Not authorized"))
        .body("detail", is("Token needed and not present"));
  }

  @Test
  @Order(20)
  @DisplayName("200 (Success) List metadata of an open resource (using openToken)")
  void ListMetaDataOfOpenResource() {
    given().relaxedHTTPSValidation()
        .header("token", openResourceToken)
        .param("id", openRsId)
        .when()
        .get("/list")
        .then()
        .statusCode(200)
        .body("type", is(200))
        .body("title", is("urn:dx:rs:success"));
  }

  @Test
  @Order(21)
  @DisplayName("200 (Success) List metadata of an open resource group (using openToken)")
  void ListMetaDataOfOpenResourceGroup() {
    given().relaxedHTTPSValidation()
        .header("token", openResourceToken)
        .param("id", openRsGroupId)
        .when()
        .get("/list")
        .then()
        .statusCode(200)
        .body("type", is(200))
        .body("title", is("urn:dx:rs:success"));
  }

  @Test
  @Order(22)
  @DisplayName("200 (Success) List metadata of a secure resource (using secureToken)")
  void ListMetaDataOfSecureResource() {
    given().relaxedHTTPSValidation()
        .header("token", secureResourceToken)
        .param("id", rsId)
        .when()
        .get("/list")
        .then()
        .statusCode(200)
        .body("type", is(200))
        .body("title", is("urn:dx:rs:success"));
  }

  @Test
  @Order(23)
  @DisplayName("401 (Not authorized) List metadata ")
  void ListMetaDataUnAuth() {
    given().relaxedHTTPSValidation()
        .param("id", rsId)
        .when()
        .get("/list")
        .then()
        .statusCode(401)
        .body("type", is("urn:dx:rs:missingAuthorizationToken"))
        .body("title", is("Not authorized"))
        .body("detail", is("Token needed and not present"));
  }

  @Test
  @Order(24)
  @DisplayName("200 (Success) DX file delete -RL  (sample file)")
  void DeleteRLSampleFile() {
    LOGGER.debug("rl sample..." + sampleFileId);
    given().relaxedHTTPSValidation()
        .header("token", delegateToken)
        .param("file-id", sampleFileId)
        .when()
        .delete("/delete")
        .then()
        .statusCode(200)
        .body("type", is("urn:dx:rs:success"))
        .body("title", is("Successful Operation"));
  }

  @Test
  @Order(25)
  @DisplayName("404 (Not Found) DX file delete - RL  (sample file)")
  void DeleteRLSampleFileNotFound() {
    given().relaxedHTTPSValidation()
        .header("token", delegateToken)
        .param("file-id", nonExistingArchiveId)
        .when()
        .delete("/delete")
        .then()
        .statusCode(404)
        .body("type", is("urn:dx:rs:resourceNotFound"))
        .body("title", is("Not Found"))
        .body("detail", is("Document of given id does not exist"));
  }

  @Test
  @Order(26)
  @DisplayName("200 (Success) DX file delete - RL  (Archive file)")
  void DeleteRLSampleArchiveFile() {
    LOGGER.debug("rl archive file id..." + archiveFileId);
    given().relaxedHTTPSValidation()
        .header("token", delegateToken)
        .param("file-id", archiveFileId)
        .when()
        .delete("/delete")
        .then()
        .statusCode(200)
        .body("type", is("urn:dx:rs:success"))
        .body("title", is("Successful Operation"));
  }

  @Test
  @Order(27)
  @DisplayName("404 (Not Found) DX file delete - RL (Archive file)")
  void DeleteRLSampleArchiveFileNotFound() {
    given().relaxedHTTPSValidation()
        .header("token", delegateToken)
        .param("file-id", nonExistingArchiveId)
        .when()
        .delete("/delete")
        .then()
        .statusCode(404)
        .body("type", is("urn:dx:rs:resourceNotFound"))
        .body("title", is("Not Found"))
        .body("detail", is("Document of given id does not exist"));
  }

  @Test
  @Order(28)
  @DisplayName("200 (Success) DX file delete - RL  (External Storage)")
  void DeleteRLSampleExternalFile() {
    LOGGER.debug("rl external storage file id..." + externalStorageFileId);
    given().relaxedHTTPSValidation()
        .header("token", delegateToken)
        .param("file-id", externalStorageFileId)
        .when()
        .delete("/delete")
        .then()
        .statusCode(200)
        .body("type", is("urn:dx:rs:success"))
        .body("title", is("Successful Operation"));
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

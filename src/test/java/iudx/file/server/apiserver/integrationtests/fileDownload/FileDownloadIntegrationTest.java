package iudx.file.server.apiserver.integrationtests.fileDownload;

import iudx.file.server.apiserver.integrationtests.RestAssuredConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static io.restassured.RestAssured.given;
import static iudx.file.server.apiserver.integrationtests.TokenHelper.openResourceToken;
import static org.hamcrest.Matchers.equalTo;

@ExtendWith(RestAssuredConfiguration.class)
public class FileDownloadIntegrationTest {
    @Test
    @DisplayName("200 (Success) DX file download - RL (Sample file )")
    public void fileDownloadSuccessTest() {
        String fileId="83c2e5c2-3574-4e11-9530-2b1fbdfce832/sample.txt";
        given()
                .queryParam("id", fileId)
                .header("token",openResourceToken)
                .when()
                .get("/consumer/ratings")
                .then()
                .statusCode(200)
                .log().body();
                //.body("type", equalTo("urn:dx:cat:Success"));
    }
}

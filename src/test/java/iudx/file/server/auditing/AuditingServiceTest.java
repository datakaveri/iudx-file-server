package iudx.file.server.auditing;

import static iudx.file.server.auditing.util.Constants.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import iudx.file.server.configuration.Configuration;
@ExtendWith({VertxExtension.class})
public class AuditingServiceTest {

  private static final Logger LOGGER = LogManager.getLogger(AuditingServiceTest.class);
  private static AuditingService auditingService;
  private static Configuration config;
  private static Vertx vertxObj;
  private static JsonObject dbConfig;
  private static String databaseIP;
  private static Integer databasePort;
  private static String databaseName;
  private static String databaseUserName;
  private static String databasePassword;
  private static Integer databasePoolSize;

  @BeforeAll
  @DisplayName("Deploying Verticle")
  static void startVertex(Vertx vertx, VertxTestContext vertxTestContext) {
    vertxObj = vertx;
    config = new Configuration();
    dbConfig = config.configLoader(3, vertx);
    databaseIP = dbConfig.getString("auditingDatabaseIP");
    databasePort = dbConfig.getInteger("auditingDatabasePort");
    databaseName = dbConfig.getString("auditingDatabaseName");
    databaseUserName = dbConfig.getString("auditingDatabaseUserName");
    databasePassword = dbConfig.getString("auditingDatabasePassword");
    databasePoolSize = dbConfig.getInteger("auditingPoolSize");
    auditingService = new AuditingServiceImpl(dbConfig, vertxObj);
    vertxTestContext.completeNow();
  }

  private JsonObject writeRequest() {
    JsonObject jsonObject = new JsonObject();
    jsonObject
            .put(API,"/ngsi-ld/v1/temporal/entities")
            .put(USER_ID,"pranav-testing-stuff")
            .put(PROVIDER_ID, "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86")
            .put(RESOURCE_ID,"iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs.iudx.io/surat-itms-realtime-information/surat-itms-live-eta");
    return jsonObject;
  }

  private JsonObject readRequest() {
    JsonObject jsonObject = new JsonObject();
    jsonObject
            .put(USER_ID,"pranav-testing-stuff")
            .put(START_TIME,"1970-01-01T05:30:00+05:30[Asia/Kolkata]")
            .put(END_TIME,"2021-11-30T20:00:00+05:30[Asia/Kolkata]");
    return jsonObject;
  }

  @Test
  @DisplayName("Testing write query w/o endpoint")
  void writeForMissingEndpoint(VertxTestContext vertxTestContext){
    JsonObject request = writeRequest();
    request.remove(API);
    auditingService.executeWriteQuery(
      request,
      vertxTestContext.failing(
        response ->
          vertxTestContext.verify(
            () -> {
              LOGGER.info("RESPONSE" + new JsonObject(response.getMessage()).getString(DETAIL));
              assertEquals(DATA_NOT_FOUND, new JsonObject(response.getMessage()).getString(DETAIL));
              vertxTestContext.completeNow();
            })));
  }

  @Test
  @DisplayName("Testing write query w/o user ID")
  void writeForMissingUserID(VertxTestContext vertxTestContext){
    JsonObject request = writeRequest();
    request.remove(USER_ID);
    auditingService.executeWriteQuery(
      request,
      vertxTestContext.failing(
        response ->
          vertxTestContext.verify(
            () -> {
              LOGGER.info("RESPONSE" + new JsonObject(response.getMessage()).getString(DETAIL));
              assertEquals(DATA_NOT_FOUND, new JsonObject(response.getMessage()).getString(DETAIL));
              vertxTestContext.completeNow();
            })));
  }

  @Test
  @DisplayName("Testing write query w/o RESOURCE ID")
  void writeForMissingResourceid(VertxTestContext vertxTestContext){
    JsonObject request = writeRequest();
    request.remove(RESOURCE_ID);
    auditingService.executeWriteQuery(
      request,
      vertxTestContext.failing(
        response ->
          vertxTestContext.verify(
          () -> {
            LOGGER.info("RESPONSE" + new JsonObject(response.getMessage()).getString(DETAIL));
            assertEquals(DATA_NOT_FOUND, new JsonObject(response.getMessage()).getString(DETAIL));
            vertxTestContext.completeNow();
          })));
  }

  @Test
  @DisplayName("Testing write query w/o PROVIDER ID")
  void writeForMissingProviderid(VertxTestContext vertxTestContext){
    JsonObject request = writeRequest();
    request.remove(PROVIDER_ID);
    auditingService.executeWriteQuery(
            request,
            vertxTestContext.failing(
                    response ->
                            vertxTestContext.verify(
                                    () -> {
                                      LOGGER.info("RESPONSE" + new JsonObject(response.getMessage()).getString(DETAIL));
                                      assertEquals(DATA_NOT_FOUND, new JsonObject(response.getMessage()).getString(DETAIL));
                                      vertxTestContext.completeNow();
                                    })));
  }

  @Test
  @DisplayName("Testing Write Query")
  void writeData(VertxTestContext vertxTestContext) {
    JsonObject request = writeRequest();
    auditingService.executeWriteQuery(
    request,
    vertxTestContext.succeeding(
      response ->
        vertxTestContext.verify(
          () -> {
            LOGGER.info("RESPONSE" + response.getString("title"));
            assertEquals("Success", response.getString("title"));
            vertxTestContext.completeNow();
          })));
  }

  @Test
  @DisplayName("Failure-testing Read query for missing userId")
  void readForMissingUserId(VertxTestContext vertxTestContext) {
    JsonObject request = readRequest();
    request.remove(USER_ID);

    auditingService.executeReadQuery(
      request,
      vertxTestContext.failing(
        response ->
          vertxTestContext.verify(
            () -> {
              assertEquals(
                USERID_NOT_FOUND,
                new JsonObject(response.getMessage()).getString(DETAIL));
              vertxTestContext.completeNow();
            })));
  }

  @Test
  @DisplayName("Failure-testing Read query for missing start time")
  void readForMissingStartTime(VertxTestContext vertxTestContext) {
    JsonObject request = readRequest();
    request.remove(START_TIME);

    auditingService.executeReadQuery(
      request,
      vertxTestContext.failing(
        response ->
          vertxTestContext.verify(
            () -> {
              assertEquals(
                MISSING_START_TIME,
                new JsonObject(response.getMessage()).getString(DETAIL));
              vertxTestContext.completeNow();
            })));
  }

  @Test
  @DisplayName("Failure-testing Read query for missing end time")
  void readForMissingEndTime(VertxTestContext vertxTestContext) {
    JsonObject request = readRequest();
    request.remove(END_TIME);

    auditingService.executeReadQuery(
      request,
      vertxTestContext.failing(
        response ->
          vertxTestContext.verify(
            () -> {
              assertEquals(
                MISSING_END_TIME,
                new JsonObject(response.getMessage()).getString(DETAIL));
              vertxTestContext.completeNow();
            })));
  }

  @Test
  @DisplayName("Failure-testing Read query when end time is before start time")
  void readForEndTimeBeforeStartTime(VertxTestContext vertxTestContext) {
    JsonObject request = readRequest();
    String temp = request.getString(START_TIME);
    request
      .put(START_TIME, request.getString(END_TIME))
      .put(END_TIME, temp);

    auditingService.executeReadQuery(
      request,
      vertxTestContext.failing(
        response ->
          vertxTestContext.verify(
            () -> {
              assertEquals(
                INVALID_TIME, new JsonObject(response.getMessage()).getString(DETAIL));
              vertxTestContext.completeNow();
            })));
  }

  @Test
  @DisplayName("Failure-testing Read query for Invalid start time date time format")
  void readForInvalidStartDateTimeFormat(VertxTestContext vertxTestContext) {
    JsonObject request = readRequest();
    request.put(START_TIME, "1970-01-0105:30:00+05:30[Asia/Kolkata]");

    auditingService.executeReadQuery(
      request,
      vertxTestContext.failing(
        response ->
          vertxTestContext.verify(
            () -> {
              assertEquals(
                INVALID_DATE_TIME,
                new JsonObject(response.getMessage()).getString(DETAIL));
              vertxTestContext.completeNow();
            })));
  }

  @Test
  @DisplayName("Failure-testing Read query for Invalid end time date time format")
  void readForInvalidEndDateTimeFormat(VertxTestContext vertxTestContext) {
    JsonObject request = readRequest();
    request.put(END_TIME, "2021-11-2405:30:00+05:30[Asia/Kolkata]");

    auditingService.executeReadQuery(
            request,
            vertxTestContext.failing(
                    response ->
                            vertxTestContext.verify(
                                    () -> {
                                      assertEquals(
                                              INVALID_DATE_TIME,
                                              new JsonObject(response.getMessage()).getString(DETAIL));
                                      vertxTestContext.completeNow();
                                    })));
  }
  @Test
  @DisplayName("Failure-testing Read query for empty response")
  void readforEmptyResponse(VertxTestContext vertxTestContext) {
    JsonObject request = readRequest();
    request.put(END_TIME,"1970-01-01T05:30:00+05:30[Asia/Kolkata]");

    auditingService.executeReadQuery(
      request,
      vertxTestContext.failing(
        response ->
          vertxTestContext.verify(
            () -> {
              LOGGER.debug(response);
              assertEquals(EMPTY_RESPONSE, new JsonObject(response.getMessage()).getString(DETAIL));
              vertxTestContext.completeNow();
            })));
  }

  @Test
  @DisplayName("Testing Read Query")
  void readData(VertxTestContext vertxTestContext) {
    JsonObject request = readRequest();
    auditingService.executeReadQuery(
      request,
      vertxTestContext.succeeding(
        response ->
          vertxTestContext.verify(
            () -> {
              LOGGER.debug(response);
              assertEquals(SUCCESS, response.getString(TITLE));
              vertxTestContext.completeNow();
            })));
  }
  @Test
  @DisplayName("Testing Read Query for a specific endpoint")
  void readDataForSpecificAPI(VertxTestContext vertxTestContext) {
    JsonObject request = readRequest();
    request.put(API,"/ngsi-ld/v1/temporal/entities"); //TODO: add any endpoint here
    auditingService.executeReadQuery(
            request,
            vertxTestContext.succeeding(
                    response ->
                            vertxTestContext.verify(
                                    () -> {
                                      LOGGER.debug(response);
                                      assertEquals(SUCCESS, response.getString(TITLE));
                                      vertxTestContext.completeNow();
                                    })));
  }
}

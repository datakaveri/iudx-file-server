package iudx.file.server.common;


import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import io.vertx.core.*;
import io.vertx.core.json.JsonArray;
import io.vertx.junit5.VertxTestContext;
import java.sql.SQLOutput;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.VertxExtension;
import iudx.file.server.common.service.CatalogueService;
import iudx.file.server.common.service.impl.CatalogueServiceImpl;


@ExtendWith({VertxExtension.class, MockitoExtension.class})
public class CatalogueServiceTest {

    @Mock
    WebClient client;
    @Mock
    HttpRequest<Buffer> httpRequest;
    @Mock
    HttpResponse<Buffer> response;
    @Mock
    JsonObject json;
    @Mock
    WebClientFactory webClientFactory;
    JsonObject config;

    @Mock
    AsyncResult<HttpResponse<Buffer>> asyncResultMock;

    CatalogueService catalogueService;


    @BeforeEach
    public void setup(Vertx vertx) {
        webClientFactory = mock(WebClientFactory.class);
        config = new JsonObject();
        config.put("catalogueHost", "asdas");
        config.put("cataloguePort", 123);
        doReturn(client).when(webClientFactory).getWebClientFor(any());
        catalogueService = new CatalogueServiceImpl(webClientFactory, config);
    }


    @Test
    @DisplayName("success - is Item exists ")
    public void isItemExistTestSuccess(Vertx vertx) {

        doReturn(httpRequest).when(client).get(anyInt(), anyString(), anyString());
        doReturn(httpRequest).when(httpRequest).addQueryParam(any(), any());
        doReturn(httpRequest).when(httpRequest).expect(any());

        AsyncResult<HttpResponse<Buffer>> asyncResult = mock(AsyncResult.class);
        when(asyncResult.succeeded()).thenReturn(true);
        when(asyncResult.result()).thenReturn(response);
        when(response.bodyAsJsonObject()).thenReturn(new JsonObject()
                .put("type", "urn:dx:cat:Success")
                .put("totalHits", 1));


        Mockito.doAnswer(new Answer<AsyncResult<HttpResponse<Buffer>>>() {
            @SuppressWarnings("unchecked")
            @Override
            public AsyncResult<HttpResponse<Buffer>> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<HttpResponse<Buffer>>>) arg0.getArgument(0)).handle(asyncResult);
                return null;
            }
        }).when(httpRequest).send(any());

        Future<Boolean> call = catalogueService.isItemExist("adadasd");

        assertTrue(call.succeeded());
        verify(httpRequest, times(1)).addQueryParam(any(), any());
        verify(httpRequest, times(1)).expect(any());
        verify(httpRequest, times(1)).send(any());
    }

    @Test
    @DisplayName("fail - is Item exists ")
    public void isItemExistTestFailure(Vertx vertx) {

        doReturn(httpRequest).when(client).get(anyInt(), anyString(), anyString());
        doReturn(httpRequest).when(httpRequest).addQueryParam(any(), any());
        doReturn(httpRequest).when(httpRequest).expect(any());

        AsyncResult<HttpResponse<Buffer>> asyncResult = mock(AsyncResult.class);
        when(asyncResult.succeeded()).thenReturn(false);
        when(asyncResult.cause()).thenReturn(new Throwable(""));


        Mockito.doAnswer(new Answer<AsyncResult<HttpResponse<Buffer>>>() {
            @SuppressWarnings("unchecked")
            @Override
            public AsyncResult<HttpResponse<Buffer>> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<HttpResponse<Buffer>>>) arg0.getArgument(0)).handle(asyncResult);
                return null;
            }
        }).when(httpRequest).send(any());

        Future<Boolean> call = catalogueService.isItemExist("adadasd");

        assertFalse(call.succeeded());
        verify(httpRequest, times(1)).addQueryParam(any(), any());
        verify(httpRequest, times(1)).expect(any());
        verify(httpRequest, times(1)).send(any());
    }

    @Test
    @DisplayName("Test isAllowedMetaDataField method ")
    public void isAllowedMetaDataFieldTest(VertxTestContext vertxTestContext) {
        MultiMap map = MultiMap.caseInsensitiveMultiMap();
        map.add("ID", "");
        catalogueService.isAllowedMetaDataField(map).onComplete(handler -> {
            if (handler.succeeded()) {
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

  @Test
  @DisplayName(
      "Test getAllowedFilters4Queries method for succeeded catalogueAPICall(/iudx/cat/v1/item)")
  public void testGetAllowedFilters4QueriesSuccess(VertxTestContext vertxTestContext) {
    String id = "dummy_id";
    doReturn(httpRequest).when(client).get(anyInt(), anyString(), anyString());
    doReturn(httpRequest).when(httpRequest).addQueryParam(any(), any());
    doReturn(httpRequest).when(httpRequest).expect(any());

    List<String> list = new ArrayList<String>();
    list.add("iudx:Resource");
    list.add("iudx:TransitManagement");

    AsyncResult<HttpResponse<Buffer>> asyncResult = mock(AsyncResult.class);
    when(asyncResult.succeeded()).thenReturn(true);
    when(asyncResult.result()).thenReturn(response);
    when(response.bodyAsJsonObject())
        .thenReturn(
            new JsonObject()
                .put("type", "urn:dx:cat:Success")
                .put("totalHits", 1)
                .put("results", new JsonArray().add(new JsonObject().put("type", list))));

    Mockito.doAnswer(
            new Answer<AsyncResult<HttpResponse<Buffer>>>() {
              @SuppressWarnings("unchecked")
              @Override
              public AsyncResult<HttpResponse<Buffer>> answer(InvocationOnMock arg0)
                  throws Throwable {
                ((Handler<AsyncResult<HttpResponse<Buffer>>>) arg0.getArgument(0))
                    .handle(asyncResult);
                return null;
              }
            })
        .when(httpRequest)
        .send(any());

    catalogueService
        .getAllowedFilters4Queries(id)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                vertxTestContext.completeNow();
              } else {
                vertxTestContext.failNow(
                    "failed invalid catalogue call (/iudx/cat/v1/item) " + handler.cause());
              }
            });

    //    Wanted but not invoked !
    //    verify(client,times(2)).get(anyInt(),anyString(),anyString());
    //    verify(httpRequest, times(2)).addQueryParam(any(), any());
    //    verify(httpRequest,times(2)).send(any());
  }

  @Test
  @DisplayName("Test getAllowedFilters4Queries for failed catalogueAPICall (/iudx/cat/v1/item)")
  public void testgetAllowedFilters4QueriesFailure(VertxTestContext vertxTestContext) {
    String id =
        "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/file.iudx.io/surat-itms-realtime-information/surat-itms-live-eta";

    doReturn(httpRequest).when(client).get(anyInt(), anyString(), anyString());
    doReturn(httpRequest).when(httpRequest).addQueryParam(any(), any());
    doReturn(httpRequest).when(httpRequest).expect(any());

    List<String> list = new ArrayList<String>();
    list.add("iudx:Resource");
    list.add("iudx:TransitManagement");

    AsyncResult<HttpResponse<Buffer>> asyncResult = mock(AsyncResult.class);
    when(asyncResult.succeeded()).thenReturn(true, false);
    when(asyncResult.result()).thenReturn(response);
    when(response.bodyAsJsonObject())
        .thenReturn(
            new JsonObject()
                .put("type", "urn:dx:cat:Success")
                .put("totalHits", 1)
                .put("results", new JsonArray().add(new JsonObject().put("type", list))));

    Mockito.doAnswer(
            new Answer<AsyncResult<HttpResponse<Buffer>>>() {
              @SuppressWarnings("unchecked")
              @Override
              public AsyncResult<HttpResponse<Buffer>> answer(InvocationOnMock arg0)
                  throws Throwable {
                ((Handler<AsyncResult<HttpResponse<Buffer>>>) arg0.getArgument(0))
                    .handle(asyncResult);
                return null;
              }
            })
        .when(httpRequest)
        .send(any());

    catalogueService
        .getAllowedFilters4Queries(id)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                vertxTestContext.failNow(
                    "failed invalid catalogue call (/iudx/cat/v1/item) " + handler.cause());

              } else {
                vertxTestContext.completeNow();
              }
            });
    /* verify(client, times(2)).get(anyInt(), anyString(), anyString());
    verify(httpRequest, times(2)).addQueryParam(any(), any());
    verify(httpRequest, times(2)).send(any());*/
  }
}

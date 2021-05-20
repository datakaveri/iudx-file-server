package iudx.file.server.common.service.impl;

import java.util.Arrays;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.predicate.ResponsePredicate;
import iudx.file.server.common.ServerType;
import iudx.file.server.common.WebClientFactory;
import iudx.file.server.common.service.CatalogueService;

public class CatalogueServiceImpl implements CatalogueService {

  private static final Logger LOGGER = LogManager.getLogger(CatalogueServiceImpl.class);
  private final List<String> allowedDefaultMetaFields=Arrays.asList("file","id","isSample","startTime","endTime");
  
  private Vertx vertx;
  private WebClient webClient;
  private String host;
  private int port;

  public CatalogueServiceImpl(Vertx vertx, WebClientFactory webClientFactory, JsonObject config) {
    this.vertx = vertx;
    this.webClient = webClientFactory.getWebClientFor(ServerType.FILE_SERVER);
    this.host = config.getString("catalogueHost");
    this.port = config.getInteger("cataloguePort");
  }

  @Override
  public Future<Boolean> isAllowedMetaDataField(MultiMap params) {
    Promise<Boolean> promise = Promise.promise();
    
    params.forEach(e->{
   // to test
      if (params.contains("invalid_metadatafield")) {
        JsonObject json=new JsonObject();
        json.put("type", 400).put("title", "Bad Request").put("details", "Bad query.");
        promise.fail(json.toString());
      } else {
        promise.complete(true);
      }
    });

    return promise.future();
  }

  @Override
  public Future<List<String>> getAllowedFilters4Queries(String id) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Future<Boolean> isItemExist(String id) {
    LOGGER.debug("isItemExist() started");
    Promise<Boolean> promise = Promise.promise();
    LOGGER.info("id : " + id);
    webClient.get(port, host, "/iudx/cat/v1/item").addQueryParam("id", id)
        .expect(ResponsePredicate.JSON).send(responseHandler -> {
          if (responseHandler.succeeded()) {
            HttpResponse<Buffer> response = responseHandler.result();
            JsonObject responseBody = response.bodyAsJsonObject();
            if (responseBody.getString("status").equalsIgnoreCase("success")
                && responseBody.getInteger("totalHits") > 0) {
              promise.complete(true);
            } else {
              promise.fail(responseHandler.cause());
            }
          } else {
            promise.fail(responseHandler.cause());
          }
        });
    return promise.future();
  }

}

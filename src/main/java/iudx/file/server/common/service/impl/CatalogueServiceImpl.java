package iudx.file.server.common.service.impl;

import static iudx.file.server.common.Constants.*;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.vertx.core.*;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.predicate.ResponsePredicate;
import iudx.file.server.common.ServerType;
import iudx.file.server.common.WebClientFactory;
import iudx.file.server.common.service.CatalogueService;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CatalogueServiceImpl implements CatalogueService {

  private static final Logger LOGGER = LogManager.getLogger(CatalogueServiceImpl.class);
  private final Cache<String, List<String>> applicableFilterCache =
      CacheBuilder.newBuilder()
          .maximumSize(1000)
          .expireAfterAccess(CACHE_TIMEOUT_AMOUNT, TimeUnit.MINUTES)
          .build();
  private WebClient webClient;
  private String host;
  private int port;
  private String catBasePath;
  private String catItemPath;
  private String catSearchPath;

  public CatalogueServiceImpl(WebClientFactory webClientFactory, JsonObject config) {
    this.webClient = webClientFactory.getWebClientFor(ServerType.FILE_SERVER);
    this.host = config.getString("catalogueHost");
    this.port = config.getInteger("cataloguePort");
    catBasePath = config.getString("dxCatalogueBasePath");
    catItemPath = catBasePath + CAT_ITEM_PATH;
    catSearchPath = catBasePath + CAT_SEARCH_PATH;
  }

  @Override
  public Future<Boolean> isAllowedMetaDataField(MultiMap params) {
    Promise<Boolean> promise = Promise.promise();
    promise.complete(true);
    return promise.future();
  }

  @Override
  public Future<List<String>> getAllowedFilters4Queries(String id) {
    Promise<List<String>> promise = Promise.promise();

    getRelItem(id)
        .onSuccess(
            relHandler -> {
              String groupId = null;
              List<String> filters = null;
              if (relHandler.getString("type").equalsIgnoreCase(ITEM_TYPE_RESOURCE)) {
                filters = applicableFilterCache.getIfPresent(id);
                groupId = relHandler.getString("resourceGroup");
              } else {
                groupId = relHandler.getString("id");
              }
              LOGGER.debug("groupId: {} ", groupId);
              if (filters == null) {
                // check for group if not present by item key.
                filters = applicableFilterCache.getIfPresent(groupId + "/*");
              }
              if (filters == null) {
                fetchFilters4Item(id, groupId)
                    .onComplete(
                        handler -> {
                          if (handler.succeeded()) {
                            promise.complete(handler.result());
                          } else {
                            promise.fail("failed to fetch filters.");
                          }
                        });
              } else {

                promise.complete(filters);
              }



            });

    return promise.future();

  }

  private Future<List<String>> fetchFilters4Item(String id, String groupId) {
    Promise<List<String>> promise = Promise.promise();
    Future<List<String>> getItemFilters = getFilterFromId(id);
    Future<List<String>> getGroupFilters = getFilterFromId(groupId);
    getItemFilters.onComplete(
        itemHandler -> {
          if (itemHandler.succeeded()) {
            List<String> filters4Item = itemHandler.result();
            if (filters4Item.isEmpty()) {
              getGroupFilters.onComplete(
                  groupHandler -> {
                    if (groupHandler.succeeded()) {
                      List<String> filters4Group = groupHandler.result();
                      applicableFilterCache.put(groupId + "/*", filters4Group);
                      promise.complete(filters4Group);
                    } else {
                      LOGGER.error(
                          "Failed to fetch applicable filters for id: "
                              + id
                              + "or group id : "
                              + groupId);
                    }
                  });
            } else {
              applicableFilterCache.put(id, filters4Item);
              promise.complete(filters4Item);
            }
          } else {
            promise.fail(itemHandler.cause());
          }
        });
    return promise.future();
  }

  private Future<List<String>> getFilterFromId(String id) {
    Promise<List<String>> promise = Promise.promise();
    callCatalogueApi(
        id,
        handler -> {
          if (handler.succeeded()) {
            promise.complete(handler.result());
          } else {
            promise.fail("failed to fetch filters for id : " + id);
          }
        });
    return promise.future();
  }

  private void callCatalogueApi(String id, Handler<AsyncResult<List<String>>> handler) {
    List<String> filters = new ArrayList<String>();
    webClient
        .get(port, host, catItemPath)
        .addQueryParam("id", id)
        .send(
            catHandler -> {
              if (catHandler.succeeded()) {
                JsonArray response = catHandler.result().bodyAsJsonObject().getJsonArray("results");
                response.forEach(
                    json -> {
                      JsonObject res = (JsonObject) json;
                      if (res.containsKey("iudxResourceAPIs")) {
                        filters.addAll(toList(res.getJsonArray("iudxResourceAPIs")));
                      }
                    });
                handler.handle(Future.succeededFuture(filters));
              } else {
                LOGGER.info("catalogue call (" + catItemPath + ") failed for id" + id);
                handler.handle(
                    Future.failedFuture("catalogue call(" + catItemPath + ") failed for id" + id));
              }
            });
  }

  private <T> List<T> toList(JsonArray arr) {
    if (arr == null) {
      return null;
    } else {
      return (List<T>) arr.getList();
    }
  }

  @Override
  public Future<Boolean> isItemExist(String id) {
    LOGGER.debug("isItemExist() started");
    Promise<Boolean> promise = Promise.promise();
    webClient
        .get(port, host, catItemPath)
        .addQueryParam("id", id)
        .expect(ResponsePredicate.JSON)
        .send(
            responseHandler -> {
              if (responseHandler.succeeded()) {
                HttpResponse<Buffer> response = responseHandler.result();
                JsonObject responseBody = response.bodyAsJsonObject();
                if (responseBody.getString("type").equalsIgnoreCase("urn:dx:cat:Success")
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

  @Override
  public Future<JsonObject> getRelItem(String id) {
    LOGGER.debug("get item for id: {} ", id);
    Promise<JsonObject> promise = Promise.promise();

    webClient
        .get(port, host, catSearchPath)
        .addQueryParam("property", "[id]")
        .addQueryParam("value", "[[" + id + "]]")
        .addQueryParam("filter", "[id,provider,resourceGroup,type,accessPolicy]")
        .expect(ResponsePredicate.JSON)
        .send(
            relHandler -> {
              if (relHandler.succeeded()
                  && relHandler.result().bodyAsJsonObject().getInteger("totalHits") > 0) {
                JsonArray resultArray =
                    relHandler.result().bodyAsJsonObject().getJsonArray("results");
                JsonObject response = resultArray.getJsonObject(0);

                Set<String> type = new HashSet<String>(new JsonArray().getList());
                type = new HashSet<String>(response.getJsonArray("type").getList());

                Set<String> itemTypeSet =
                    type.stream().map(e -> e.split(":")[1]).collect(Collectors.toSet());
                itemTypeSet.retainAll(ITEM_TYPES);

                String itemType =
                    itemTypeSet.toString().replaceAll("\\[", "").replaceAll("\\]", "");
                LOGGER.info("itemType: {} ", itemType);
                response.put("type", itemType);

                promise.complete(response);

              } else {
                LOGGER.error("catalogue call search api failed: " + relHandler.cause());
                promise.fail("catalogue call search api failed");
              }
            });

    return promise.future();
  }
}

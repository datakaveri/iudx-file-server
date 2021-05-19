package iudx.file.server.apiserver.service;

import java.util.List;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;

public interface CatalogueService {


  public Future<Boolean> isAllowedMetaDataField(MultiMap params);

  public Future<List<String>> getAllowedFilters4Queries(String id);

  public Future<Boolean> isItemExist(String id);

}

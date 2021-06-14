package iudx.file.server.common.service;

import java.util.List;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;

public interface CatalogueService {


  Future<Boolean> isAllowedMetaDataField(MultiMap params);

  Future<List<String>> getAllowedFilters4Queries(String id);

  Future<Boolean> isItemExist(String id);

}

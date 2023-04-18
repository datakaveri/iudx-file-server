package iudx.file.server.apiserver.utilities;

import iudx.file.server.apiserver.query.QueryParams;
import iudx.file.server.common.QueryType;

public class Utilities {

  /**
   *
   *
   * <pre>
   * Retreives a query type based on the attributes present in the query json.
   * </pre>
   *
   * @param query
   * @return QueryType
   */
  public static QueryType getQueryType(QueryParams params) {
    QueryType queryType = null;
    if (params.isGeoParamsPresent() && params.isTemporalParamsPresent()) {
      queryType = QueryType.TEMPORAL_GEO;
    } else if (params.isGeoParamsPresent()) {
      queryType = QueryType.GEO;
    } else if (params.isTemporalParamsPresent()) {
      queryType = QueryType.TEMPORAL;
    } else {
      queryType = QueryType.LIST;
    }
    return queryType;
  }

  /**
   *
   *
   * <pre>
   * retreive components from id index :
   *            0 - Domain
   *            1 - user SHA
   *            2 - File Server
   *            3 - File group
   *            4 - Resource/ File id(for Group level file, index 4 represent file Id(optional))
   *            5 - File id(optional)
   * </pre>
   *
   * @param fileId
   * @return array of file id after splitting by "/".
   */
  public static String[] getFileIdComponents(String fileId) {
    return fileId.split("/");
  }
}

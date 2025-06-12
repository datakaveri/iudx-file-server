package iudx.file.server.common;

import static iudx.file.server.apiserver.utilities.Constants.*;

/**
 * This class is used to get complete endpoint by appending configurable base path with the APIs.
 */
public class Api {
  private static volatile Api apiInstance;
  //    public static final String NGSILD_BASE_PATH = "dxApiBasePath";
  //    public static final String IUDX_V1_BASE_PATH = "iudxApiBasePath";
  private String dxApiBasePath;
  private String iudxApiBasePath;
  private StringBuilder temporalEndpoint;
  private StringBuilder spatialEndpoint;
  private StringBuilder fileUploadEndpoint;
  private StringBuilder fileDownloadEndpoint;
  private StringBuilder fileDeleteEndpoint;
  private StringBuilder listMetaDataEndpoint;
  private StringBuilder fileGtfsUploadEndpoint;
  private StringBuilder fileGtfsDownloadEndpoint;

  private Api(String dxApiBasePath, String iudxApiBasePath) {
    this.dxApiBasePath = dxApiBasePath;
    this.iudxApiBasePath = iudxApiBasePath;
    buildEndpoints();
  }
  /** This method is used to getI nstance. */

  public static Api getInstance(String dxApiBasePath, String iudxApiBasePath) {
    if (apiInstance == null) {
      synchronized (Api.class) {
        if (apiInstance == null) {
          apiInstance = new Api(dxApiBasePath, iudxApiBasePath);
        }
      }
    }
    return apiInstance;
  }
  /** This method is used to build complete endpoint. */

  public void buildEndpoints() {
    temporalEndpoint = new StringBuilder(dxApiBasePath).append(API_TEMPORAL);
    spatialEndpoint = new StringBuilder(dxApiBasePath).append(API_SPATIAL);
    fileUploadEndpoint = new StringBuilder(iudxApiBasePath).append(API_FILE_UPLOAD);
    fileDownloadEndpoint = new StringBuilder(iudxApiBasePath).append(API_FILE_DOWNLOAD);
    fileDeleteEndpoint = new StringBuilder(iudxApiBasePath).append(API_FILE_DELETE);
    listMetaDataEndpoint = new StringBuilder(iudxApiBasePath).append(API_LIST_METADATA);
    fileGtfsUploadEndpoint = new StringBuilder(iudxApiBasePath).append(API_GTFS_UPLOAD);
    fileGtfsDownloadEndpoint = new StringBuilder(iudxApiBasePath).append(API_GTFS_DOWNLOAD);
  }

  public String getApiTemporal() {
    return temporalEndpoint.toString();
  }

  public String getApiSpatial() {
    return spatialEndpoint.toString();
  }

  public String getApiFileUpload() {
    return fileUploadEndpoint.toString();
  }

  public String getApiGtfsUpload() {
    return fileGtfsUploadEndpoint.toString();
  }

  public String getApiGtfsDownload() {
    return fileGtfsDownloadEndpoint.toString();
  }

  public String getApiFileDownload() {
    return fileDownloadEndpoint.toString();
  }

  public String getApiFileDelete() {
    return fileDeleteEndpoint.toString();
  }

  public String getListMetaData() {
    return listMetaDataEndpoint.toString();
  }
}

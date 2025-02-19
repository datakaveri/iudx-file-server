package iudx.file.server.apiserver.service;

import io.vertx.core.Future;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.FileUpload;
import java.util.List;
import com.google.transit.realtime.GtfsRealtime.FeedMessage;


/**
 * The FileService.
 *
 * <h1>FileService</h1>
 *
 * <p>it helps the upload download and delete the file in server
 */
public interface FileService {

  /**
   * upload file to server.
   *
   * @param file set of file (although there will always be a single file to upload)
   * @param filePath path for the upload file
   */
  Future<JsonObject> upload(final List<FileUpload> file, String filePath);

  /**
   * upload file to server.
   *
   * @param file set of file (although there will always be a single file to upload)
   * @param fileName uploaded filename
   * @param filePath path for uploaded file
   */
  Future<JsonObject> upload(final List<FileUpload> file, String fileName, String filePath);

  /**
   * download file from server.
   *
   * @param fileName name of file to be downloaded.
   * @param response response object to send file as Content-Disposition header
   */
  Future<JsonObject> download(
      final String fileName, String filePath, final HttpServerResponse response);

  /**
   * delete file from server.
   *
   * @param fileName name of file to be deleted
   * @param filePath path of deleted file
   */
  Future<JsonObject> delete(final String fileName, String filePath);

  Future<JsonObject> uploadGtfsRealtime(final FileUpload file, String fileName, String filePath);

  Future<JsonObject> downloadGtfsRealtime(final String fileName, String filePath, final HttpServerResponse response);


}

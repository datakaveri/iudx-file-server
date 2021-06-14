package iudx.file.server.apiserver.service;

import java.util.Set;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.FileUpload;

public interface FileService {

  /**
   * upload file to server.
   * 
   * @param file set of file (although there will always be a single file to upload)
   * @param handler
   */
  Future<JsonObject> upload(final Set<FileUpload> file, String filePath);

  /**
   * upload file to server
   * 
   * @param file set of file (although there will always be a single file to upload)
   * @param fileName uploaded filename
   * @param filePath path for uploaded file
   * @param handler
   */
  Future<JsonObject> upload(final Set<FileUpload> file, String fileName, String filePath);

  /**
   * download file from server
   * 
   * @param fileName name of file to be downloaded.
   * @param response response object to send file as Content-Disposition header
   * @param handler Async handler
   */
  Future<JsonObject> download(final String fileName, String filePath,
      final HttpServerResponse response);

  /**
   * delete file from server
   * 
   * @param fileName name of file to be deleted
   * @param handler Async handler
   */
  Future<JsonObject> delete(final String fileName, String filePath);
}

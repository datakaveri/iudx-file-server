package iudx.file.server.service;

import java.util.Set;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.FileUpload;

public interface FileService {

  /**
   * upload file to server.
   * 
   * @param Set of files to be uploaded.
   * @param handler
   */
  public void upload(final Set<FileUpload> file, final Handler<AsyncResult<JsonObject>> handler);

  /**
   * download file from server
   * 
   * @param fileName name of file to be downloaded.
   * @param response response object to send file as Content-Disposition header
   * @param handler Async handler
   */
  public void download(final String fileName, final HttpServerResponse response,
      final Handler<AsyncResult<JsonObject>> handler);

  /**
   * delete file from server
   * 
   * @param fileName name of file to be deleted
   * @param handler Async handler
   */
  public void delete(final String fileName, final Handler<AsyncResult<JsonObject>> handler);
}

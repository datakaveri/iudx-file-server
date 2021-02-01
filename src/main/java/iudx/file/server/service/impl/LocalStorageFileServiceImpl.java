package iudx.file.server.service.impl;

import static iudx.file.server.utilities.Constants.DIR;
import static iudx.file.server.utilities.Constants.MAX_SIZE;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;
import org.apache.commons.compress.utils.FileNameUtils;
import org.apache.commons.io.FileUtils;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.file.AsyncFile;
import io.vertx.core.file.CopyOptions;
import io.vertx.core.file.FileSystem;
import io.vertx.core.file.OpenOptions;
import io.vertx.core.http.HttpServerFileUpload;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.core.streams.Pump;
import io.vertx.core.streams.ReadStream;
import io.vertx.ext.web.FileUpload;
import iudx.file.server.service.FileService;
import iudx.file.server.utilities.DefaultAsyncResult;
import io.vertx.core.buffer.Buffer;

// TODO : uniform response messages.
public class LocalStorageFileServiceImpl implements FileService {

  private static final Logger LOGGER = LogManager.getLogger(LocalStorageFileServiceImpl.class);

  private final SimpleDateFormat sf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSS");
  private final FileSystem fileSystem;

  public LocalStorageFileServiceImpl(FileSystem fileSystem) {
    this.fileSystem = fileSystem;
  }

  /**
   * {@inheritDoc}
   */
  // @Override
  @Deprecated
  public void upload1(HttpServerRequest request, Handler<AsyncResult<JsonObject>> handler) {
    final String basePath = DIR;
    final Long maxSize = MAX_SIZE;
    // request.pause();
    final JsonObject res = new JsonObject();
    final JsonObject mimeTypeObj = new JsonObject();
    mimeTypeObj.put("text/plain", "txt").put("text/csv", "csv").put("application/pdf", "pdf")
        .put("video/mp4", "mp4").put("application/zip", "zip")
        .put("application/x-7z-compressed", "7z")
        .put("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "xlsx")
        .put("application/vnd.openxmlformats-officedocument.wordprocessingml.document", "docx");

    request.setExpectMultipart(true);
    // request.resume();
    request.uploadHandler(new Handler<HttpServerFileUpload>() {
      @Override
      public void handle(final HttpServerFileUpload upload) {
        final JsonObject metadata = new JsonObject();
        metadata.put("name", upload.name()).put("filename", upload.filename()).put("content-type",
            upload.contentType());
        metadata.put("content-transfer-encoding", upload.contentTransferEncoding())
            .put("charset", upload.charset()).put("size", upload.size());
        String content_type = upload.contentType().toLowerCase();
        if (!mimeTypeObj.containsKey(content_type)) {
          JsonObject response =
              new JsonObject().put("status", "error").put("message", "invalid file content type");
          handler.handle(Future.failedFuture(response.toString()));
        } else {
          String uploadedPath = basePath + UUID.randomUUID().toString() + "+" + upload.filename();
          metadata.put("uploadedPath", uploadedPath);
          LOGGER
              .info("Now streaming file content at path :  " + metadata.getString("uploadedPath"));
          upload.streamToFileSystem(uploadedPath);
          doUpload(upload, metadata);
        }
      }

      private void doUpload(final HttpServerFileUpload upload, final JsonObject metadata) {
        upload.endHandler(new Handler<Void>() {
          @Override
          public void handle(Void event) {
            if (null != metadata.getString("status")
                && metadata.getString("status").equals("exception")) {
              JsonObject response = new JsonObject().put("status", "exception");
              handler.handle(Future.failedFuture(response.toString()));
            } else {
              metadata.put("size", upload.size());
              if (maxSize != null && maxSize < metadata.getLong("size", 0l)) {
                fileSystem.delete(metadata.getString("uploadedPath"),
                    new Handler<AsyncResult<Void>>() {
                      @Override
                      public void handle(AsyncResult<Void> event) {
                        LOGGER.info("file size [ " + metadata.getLong("size", 0l)
                            + " ] exceeded to maxSize [ " + maxSize + " ]. Hence deleted");
                        if (event.failed()) {
                          LOGGER.error(event.cause().getMessage(), event.cause());
                        }
                      }
                    });
                JsonObject response =
                    new JsonObject().put("status", "error").put("message", "file.too.large");
                handler.handle(Future.failedFuture(response.toString()));
              } else {
                // get uploaded file last modified date and put into metadata
                getLastModifiedDate(metadata.getString("uploadedPath"))
                    .onComplete(resultHandler -> {
                      metadata.put("lastModifiedDate",
                          resultHandler.succeeded() ? resultHandler.result() : null);
                      JsonObject response =
                          new JsonObject().put("uploadedPath", metadata.getString("uploadedPath"))
                              .put("status", "ok").put("message", "file uploaded")
                              .put("metadata", metadata);
                      handler.handle(Future.succeededFuture(response));
                    });
              }
            }
          }
        });

        upload.exceptionHandler(new Handler<Throwable>() {
          @Override
          public void handle(Throwable event) {
            metadata.put("status", "exception");
            LOGGER.error("Exception occured : " + event.getMessage(), event);
            fileSystem.delete(metadata.getString("uploadedPath"), new Handler<AsyncResult<Void>>() {
              @Override
              public void handle(AsyncResult<Void> event) {
                LOGGER.info("Partially uploaded file deleted");
                if (event.failed()) {
                  LOGGER.error(event.cause().getMessage(), event.cause());
                }
              }
            });
          }
        });

      }

      private Future<String> getLastModifiedDate(final String uploadedPath) {
        Promise<String> promise = Promise.promise();
        fileSystem.props(uploadedPath, ar -> {
          if (ar.succeeded()) {
            promise.complete(sf.format(new Date(ar.result().lastModifiedTime())));
          }
          if (ar.failed()) {
            promise.complete(null);
          }
        });

        return promise.future();
      }

    });
  }

  @Override
  public void upload(Set<FileUpload> files, String filePath,
      Handler<AsyncResult<JsonObject>> handler) {
    LOGGER.debug("uploading.. files to file system.");
    final JsonObject metadata = new JsonObject();
    fileSystem.mkdirsBlocking(DIR + filePath);
    Iterator<FileUpload> fileUploadIterator = files.iterator();
    while (fileUploadIterator.hasNext()) {
      FileUpload fileUpload = fileUploadIterator.next();
      LOGGER.debug("uploading... " + fileUpload.fileName());
      String uuid = UUID.randomUUID().toString();
      String fileExtension = FileNameUtils.getExtension(fileUpload.fileName());
      String fileUploadPath = DIR + "/" + filePath + "/" + uuid + "." + fileExtension;
      CopyOptions copyOptions = new CopyOptions();
      copyOptions.setReplaceExisting(true);
      fileSystem.move(fileUpload.uploadedFileName(), fileUploadPath, copyOptions,
          fileMoveHandler -> {
            if (fileMoveHandler.succeeded()) {
              metadata.put("fileName", fileUpload.fileName());
              metadata.put("content-type", fileUpload.contentType());
              metadata.put("content-transfer-encoding", fileUpload.contentTransferEncoding());
              metadata.put("char-set", fileUpload.charSet());
              metadata.put("size", fileUpload.size() + " Bytes");
              metadata.put("uploaded path", fileUploadPath);
              metadata.put("file-id", uuid + "." + fileExtension);
              handler.handle(Future.succeededFuture(metadata));
            } else {
              LOGGER.debug("failed :" + fileMoveHandler.cause());
              handler.handle(Future.failedFuture(fileMoveHandler.cause()));
            }
          });
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void download(String fileName, String uploadDir, HttpServerResponse response,
      Handler<AsyncResult<JsonObject>> handler) {
    JsonObject finalResponse = new JsonObject();
    Promise<JsonObject> promise = Promise.promise();
    String path = DIR + uploadDir + "/" + fileName;
    response.setChunked(true);
    fileSystem.exists(path, existHandler -> {
      if (existHandler.succeeded()) {
        if (existHandler.result()) {
          fileSystem.open(path, new OpenOptions().setCreate(true), readEvent -> {
            if (readEvent.failed()) {
              finalResponse.put("statusCode", HttpStatus.SC_BAD_REQUEST);
              promise.complete(finalResponse);
            }
            LOGGER.debug("seding file : " + fileName + " to client");
            ReadStream<Buffer> asyncFile = readEvent.result();
            response.putHeader("Content-Disposition", "attachment; filename=" + fileName);
            asyncFile.pipeTo(response, pipeHandler -> {
              asyncFile.endHandler(avoid -> {
                // response.putHeader("Content-Disposition", "attachment; filename=" + fileName);
                promise.complete();
              });
            });
          });
        } else {
          finalResponse.put("statusCode", HttpStatus.SC_NOT_FOUND);
          finalResponse.put("statusMessage", "File does not exist");
          LOGGER.error("File does not exist");
          promise.complete(finalResponse);

        }
      } else {
        LOGGER.error(existHandler.cause());
        finalResponse.put("statusCode", HttpStatus.SC_INTERNAL_SERVER_ERROR);
        finalResponse.put("statusMessage", "");
        promise.complete(finalResponse);
      }
    });
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void delete(String fileName, String filePath, Handler<AsyncResult<JsonObject>> handler) {
    JsonObject finalResponse = new JsonObject();
    // String filePath = DIR + "/" + fileName;
    LOGGER.info("filePath : " + filePath);
    fileSystem.exists(filePath, existHandler -> {
      if (existHandler.succeeded()) {
        if (existHandler.result()) {
          fileSystem.delete(filePath, readEvent -> {
            if (readEvent.failed()) {
              finalResponse.put("statusCode", HttpStatus.SC_INTERNAL_SERVER_ERROR);
              finalResponse.put("statusMessage", readEvent.cause().toString());
              handler.handle(Future.failedFuture(finalResponse.toString()));
            } else {
              LOGGER.info("File deleted");
              finalResponse.put("statusCode", HttpStatus.SC_OK);
              finalResponse.put("statusMessage", "File Deleted");
              handler.handle(Future.succeededFuture(finalResponse));
            }
          });
        } else {
          LOGGER.error("File does not exist");
          finalResponse.put("statusCode", HttpStatus.SC_NOT_FOUND);
          finalResponse.put("statusMessage", "File does not exist");
          handler.handle(Future.failedFuture(finalResponse.toString()));
        }
      } else {
        LOGGER.error(existHandler.cause());
        handler.handle(Future.failedFuture(existHandler.cause()));
      }
    });
  }

}

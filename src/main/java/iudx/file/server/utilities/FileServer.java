package iudx.file.server.utilities;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.file.FileSystem;
import io.vertx.core.http.HttpServerFileUpload;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;


public class FileServer {

  private static final Logger log = LoggerFactory.getLogger(FileServer.class);
  private final String basePath;
  private final FileSystem fs;
  private final SimpleDateFormat sf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSS");

  public FileServer(Vertx vertx, String basePath) {
    // flat storage used for uploading a file
    this.basePath = basePath != null && !basePath.endsWith("/") ? basePath + "/" : basePath;
    this.fs = vertx.fileSystem();
  }


  public void writeUploadFile(final HttpServerRequest request, final Long maxSize,
      final Handler<JsonObject> handler) {
    log.info("writeUploadFile initiated - request paused");
    request.pause();
    final JsonObject res = new JsonObject();
    final JsonObject mimeTypeObj = new JsonObject();
    mimeTypeObj.put("text/plain", "txt").put("text/csv", "csv").put("application/pdf", "pdf")
        .put("video/mp4", "mp4").put("application/zip", "zip")
        .put("application/x-7z-compressed", "7z")
        .put("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "xlsx")
        .put("application/vnd.openxmlformats-officedocument.wordprocessingml.document", "docx");

    request.setExpectMultipart(true);

    mkdirsIfNotExists(basePath, new Handler<AsyncResult<Void>>() {
      @Override
      public void handle(AsyncResult<Void> event) {
        if (event.succeeded()) {
          log.info("mkdirsIfNotExists's handler - request resumed");
          request.resume();
        } else {
          handler.handle(res.put("status", "error"));
          log.error(event.cause().getMessage(), event.cause());
        }
      }
    });

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
          handler.handle(res.put("status", "error").put("message", "invalid file content type"));
        } else {
          final String uploadedPath =
              basePath + UUID.randomUUID().toString() + "." + mimeTypeObj.getString(content_type);
          metadata.put("uploadedPath", uploadedPath);
          log.info("Now streaming file content at path :  " + metadata.getString("uploadedPath"));
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
              handler.handle(res.put("status", "exception"));
            } else {
              metadata.put("size", upload.size());
              if (maxSize != null && maxSize < metadata.getLong("size", 0l)) {
                fs.delete(metadata.getString("uploadedPath"), new Handler<AsyncResult<Void>>() {
                  @Override
                  public void handle(AsyncResult<Void> event) {
                    log.info("file size [ " + metadata.getLong("size", 0l)
                        + " ] exceeded to maxSize [ " + maxSize + " ]. Hence deleted");
                    if (event.failed()) {
                      log.error(event.cause().getMessage(), event.cause());
                    }
                  }
                });

                handler.handle(res.put("status", "error").put("message", "file.too.large"));
              } else {
                // get uploaded file last modified date and put into metadata
                getLastModifiedDate(metadata.getString("uploadedPath"))
                    .onComplete(resultHandler -> {
                      metadata.put("lastModifiedDate",
                          resultHandler.succeeded() ? resultHandler.result() : null);
                      handler.handle(res.put("uploadedPath", metadata.getString("uploadedPath"))
                          .put("status", "ok").put("message", "file uploaded")
                          .put("metadata", metadata));
                    });
              }
            }
          }
        });

        upload.exceptionHandler(new Handler<Throwable>() {
          @Override
          public void handle(Throwable event) {
            metadata.put("status", "exception");
            log.error("Exception occured : " + event.getMessage(), event);
            fs.delete(metadata.getString("uploadedPath"), new Handler<AsyncResult<Void>>() {
              @Override
              public void handle(AsyncResult<Void> event) {
                log.info("Partially uploaded file deleted");
                if (event.failed()) {
                  log.error(event.cause().getMessage(), event.cause());
                }
              }
            });
          }
        });

      }

      private Future<String> getLastModifiedDate(final String uploadedPath) {
        Promise<String> promise = Promise.promise();
        fs.props(uploadedPath, ar -> {
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

  private void mkdirsIfNotExists(String basePath, final Handler<AsyncResult<Void>> h) {
    fs.exists(basePath, new Handler<AsyncResult<Boolean>>() {
      @Override
      public void handle(AsyncResult<Boolean> event) {
        if (event.succeeded()) {
          if (Boolean.FALSE.equals(event.result())) {
            fs.mkdirs(basePath, new Handler<AsyncResult<Void>>() {
              @Override
              public void handle(AsyncResult<Void> event) {
                h.handle(event);
              }
            });
          } else {
            h.handle(new DefaultAsyncResult<>((Void) null));
          }
        } else {
          h.handle(new DefaultAsyncResult<Void>(event.cause()));
        }
      }
    });
  }

}

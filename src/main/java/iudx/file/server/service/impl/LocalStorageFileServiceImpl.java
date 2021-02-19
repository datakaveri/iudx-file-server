package iudx.file.server.service.impl;

import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;
import org.apache.commons.compress.utils.FileNameUtils;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.CopyOptions;
import io.vertx.core.file.FileSystem;
import io.vertx.core.file.OpenOptions;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.core.streams.ReadStream;
import io.vertx.ext.web.FileUpload;
import iudx.file.server.service.FileService;

public class LocalStorageFileServiceImpl implements FileService {

  private static final Logger LOGGER = LogManager.getLogger(LocalStorageFileServiceImpl.class);

  private final SimpleDateFormat sf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSS");
  private FileSystem fileSystem;
  private final String directory;

  public LocalStorageFileServiceImpl(FileSystem fileSystem,String directory) {
    this.fileSystem = fileSystem;
    this.directory=directory;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void upload(Set<FileUpload> files, String filename, String filePath,
      Handler<AsyncResult<JsonObject>> handler) {
    LOGGER.debug("uploading.. files to file system.");
    final JsonObject metadata = new JsonObject();
    final JsonObject finalResponse = new JsonObject();
    System.out.println(directory+filePath);
    fileSystem=fileSystem.mkdirsBlocking(directory + filePath);
    Iterator<FileUpload> fileUploadIterator = files.iterator();
    while (fileUploadIterator.hasNext()) {
      FileUpload fileUpload = fileUploadIterator.next();
      LOGGER.debug("uploading... " + fileUpload.fileName());
      String uuid = filename;
      String fileExtension = FileNameUtils.getExtension(fileUpload.fileName());
      String fileUploadPath = directory + "/" + filePath + "/" + uuid + "." + fileExtension;
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
              finalResponse.put("type", HttpStatus.SC_INTERNAL_SERVER_ERROR);
              finalResponse.put("title", "failed to upload file.");
              finalResponse.put("detail", fileMoveHandler.cause());
              handler.handle(Future.failedFuture(finalResponse.toString()));
            }
          });
    }
  }

  @Override
  public void upload(Set<FileUpload> files, String filePath,
      Handler<AsyncResult<JsonObject>> handler) {
    upload(files, UUID.randomUUID().toString(), filePath, handler);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void download(String fileName, String uploadDir, HttpServerResponse response,
      Handler<AsyncResult<JsonObject>> handler) {
    JsonObject finalResponse = new JsonObject();
    Promise<JsonObject> promise = Promise.promise();
    String path = directory + uploadDir + "/" + fileName;
    System.out.println(path);
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
            response.putHeader("content-type", "application/octet-stream");
            response.putHeader("Content-Disposition", "attachment; filename=" + fileName);
            asyncFile.pipeTo(response, pipeHandler -> {
              asyncFile.endHandler(avoid -> {
                handler.handle(Future.succeededFuture());
              });
            });
          });
        } else {
          finalResponse.put("type", HttpStatus.SC_NOT_FOUND);
          finalResponse.put("title", "File does not exist");
          finalResponse.put("detail", "File does not exist");
          LOGGER.error("File does not exist");
          handler.handle(Future.failedFuture(finalResponse.toString()));
        }
      } else {
        LOGGER.error(existHandler.cause());
        finalResponse.put("type", HttpStatus.SC_INTERNAL_SERVER_ERROR);
        finalResponse.put("title", "Something went wrong while downloading file.");
        finalResponse.put("detail", existHandler.cause());
        handler.handle(Future.failedFuture(finalResponse.toString()));
      }
    });
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void delete(String fileName, String filePath, Handler<AsyncResult<JsonObject>> handler) {
    JsonObject finalResponse = new JsonObject();
    String path = directory + filePath + "/" + fileName;
    LOGGER.info("filePath : " + path);
    fileSystem.exists(path, existHandler -> {
      if (existHandler.succeeded()) {
        if (existHandler.result()) {
          fileSystem.delete(path, readEvent -> {
            if (readEvent.failed()) {
              finalResponse.put("type", HttpStatus.SC_INTERNAL_SERVER_ERROR);
              finalResponse.put("title", readEvent.cause().toString());
              handler.handle(Future.failedFuture(finalResponse.toString()));
            } else {
              LOGGER.info("File deleted");
              finalResponse.put("type", HttpStatus.SC_OK);
              finalResponse.put("title", "File Deleted");
              finalResponse.put("detail", "File Deleted");
              handler.handle(Future.succeededFuture(finalResponse));
            }
          });
        } else {
          LOGGER.error("File does not exist");
          finalResponse.put("type", HttpStatus.SC_NOT_FOUND);
          finalResponse.put("title", "File does not exist");
          finalResponse.put("detail", "File does not exist");
          handler.handle(Future.failedFuture(finalResponse.toString()));
        }
      } else {
        LOGGER.error(existHandler.cause());
        handler.handle(Future.failedFuture(existHandler.cause()));
      }
    });
  }

}

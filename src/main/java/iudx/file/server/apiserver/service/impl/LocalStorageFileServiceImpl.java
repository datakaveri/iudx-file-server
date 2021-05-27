package iudx.file.server.apiserver.service.impl;

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
import iudx.file.server.apiserver.service.FileService;

public class LocalStorageFileServiceImpl implements FileService {

  private static final Logger LOGGER = LogManager.getLogger(LocalStorageFileServiceImpl.class);

  private final SimpleDateFormat sf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSS");
  private FileSystem fileSystem;
  private final String directory;

  public LocalStorageFileServiceImpl(FileSystem fileSystem, String directory) {
    this.fileSystem = fileSystem;
    this.directory = directory;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Future<JsonObject> upload(Set<FileUpload> files, String filename, String filePath) {
    Promise<JsonObject> promise = Promise.promise();
    LOGGER.debug("uploading.. files to file system.");
    final JsonObject metadata = new JsonObject();
    final JsonObject finalResponse = new JsonObject();
    LOGGER.info(directory + filePath);
    fileSystem = fileSystem.mkdirsBlocking(directory + filePath);
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
              metadata.put("uploaded_path", fileUploadPath);
              metadata.put("file-id", uuid + "." + fileExtension);
              promise.complete(metadata);
            } else {
              LOGGER.debug("failed :" + fileMoveHandler.cause());
              finalResponse.put("type", HttpStatus.SC_INTERNAL_SERVER_ERROR);
              finalResponse.put("title", "failed to upload file.");
              finalResponse.put("details", fileMoveHandler.cause());
              promise.fail(finalResponse.toString());
            }
          });
    }
    return promise.future();
  }

  @Override
  public Future<JsonObject> upload(Set<FileUpload> files, String filePath) {
    return upload(files, UUID.randomUUID().toString(), filePath);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Future<JsonObject> download(String fileName, String uploadDir,
      HttpServerResponse response) {
    Promise<JsonObject> promise = Promise.promise();
    JsonObject finalResponse = new JsonObject();
    String path = directory + uploadDir + "/" + fileName;
    LOGGER.info(path);
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
                promise.complete();
              });
            });
          });
        } else {
          finalResponse.put("type", HttpStatus.SC_NOT_FOUND);
          finalResponse.put("title", "File does not exist");
          finalResponse.put("details", "File does not exist");
          LOGGER.error("File does not exist");
          promise.fail(finalResponse.toString());
        }
      } else {
        LOGGER.error(existHandler.cause());
        finalResponse.put("type", HttpStatus.SC_INTERNAL_SERVER_ERROR);
        finalResponse.put("title", "Something went wrong while downloading file.");
        finalResponse.put("details", existHandler.cause());
        promise.fail(finalResponse.toString());
      }
    });
    return promise.future();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Future<JsonObject> delete(String fileName, String filePath) {
    Promise<JsonObject> promise = Promise.promise();
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
              promise.fail(finalResponse.toString());
            } else {
              LOGGER.info("File deleted");
              finalResponse.put("type", HttpStatus.SC_OK);
              finalResponse.put("title", "File Deleted");
              finalResponse.put("details", "File Deleted");
              promise.complete(finalResponse);
            }
          });
        } else {
          LOGGER.error("File does not exist");
          finalResponse.put("type", HttpStatus.SC_NOT_FOUND);
          finalResponse.put("title", "File does not exist");
          finalResponse.put("details", "File does not exist");
          promise.fail(finalResponse.toString());
        }
      } else {
        LOGGER.error(existHandler.cause());
        promise.fail(existHandler.cause());
      }
    });
    return promise.future();
  }

}

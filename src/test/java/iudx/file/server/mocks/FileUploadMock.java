package iudx.file.server.mocks;

import io.vertx.ext.web.FileUpload;

public class FileUploadMock implements FileUpload{

  @Override
  public String name() {
    return "mockFile.txt";
  }

  @Override
  public String uploadedFileName() {
    return "mockFile.txt";
  }

  @Override
  public String fileName() {
    return "mockFile.txt";
  }

  @Override
  public long size() {
    return 0;
  }

  @Override
  public String contentType() {
    return "text/plain";
  }

  @Override
  public String contentTransferEncoding() {
    return null;
  }

  @Override
  public String charSet() {
    return "UTF-8";
  }

  @Override
  public boolean cancel() {
    return false;
  }
}

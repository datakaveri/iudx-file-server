package iudx.file.server.util;

import io.vertx.core.Vertx;
import io.vertx.core.file.FileSystem;

public class FileCheckUtil {
  
  private FileSystem fs;
  private String directory;
  
  public FileCheckUtil(Vertx vertx,String directory) {
    this.fs=vertx.fileSystem();
    this.directory=directory;
  }

  /**
   * Check file exist in system
   * @param filePath
   * @return
   */
  public boolean isFileExist(String filePath) {
    String[] idComponents=filePath.split("/");
    int totalComponents=idComponents.length;
    StringBuilder fileId=new StringBuilder(directory);
    boolean isSampleFile=idComponents[totalComponents-1].contains("sample");
    fileId.append(idComponents[1]).append("/").append(idComponents[3]);
    if(totalComponents==6 ) { //Resource level  file
      if(isSampleFile) {
        fileId.append("/").append(idComponents[4]);
      }
    }else if(totalComponents==5 ) { //Group level  file
      //group level sample and archive file are saved in same location.
    }
    fileId.append("/").append(idComponents[totalComponents-1]);
    System.out.println("file to check from system ::: "+fileId.toString());
    return fs.existsBlocking(fileId.toString());
  }
}

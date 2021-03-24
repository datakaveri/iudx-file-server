package iudx.file.server.validations;

import java.util.HashMap;
import java.util.Map;

public class ContentTypeValidator {

  private static Map<String, String> validContentType;

  static{
    validContentType=new HashMap<>();
    validContentType.put("text/plain", "txt");
    validContentType.put("text/csv", "csv");
    validContentType.put("application/pdf", "pdf");
    validContentType.put("video/mp4", "mp4");
    validContentType.put("application/zip", "zip");
    validContentType.put("application/x-7z-compressed", "7z");
    validContentType.put("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "xlsx");
    validContentType.put("application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "docx");
  }

  public static boolean isValid(String contentType) {
    return validContentType.containsKey(contentType);
  }
}

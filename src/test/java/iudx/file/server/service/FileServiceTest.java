package iudx.file.server.service;

import java.util.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;

import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.impl.BodyHandlerImpl;
import io.vertx.ext.web.impl.RoutingContextImpl;
import iudx.file.server.service.impl.LocalStorageFileServiceImpl;

//TODO : create test cases for FileServiceImpl
@ExtendWith(VertxExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class FileServiceTest {

  private static FileService fileService;

  @BeforeAll
  @DisplayName("Deploy a verticle")
  static void init(Vertx vertx, io.vertx.reactivex.core.Vertx vertx2,
      VertxTestContext testContext) {
    fileService = new LocalStorageFileServiceImpl(vertx.fileSystem());
  }


  //@Test
  @Order(1)
  @DisplayName("save file")
  void saveFile(VertxTestContext testContext) {
    
    /*
     * RoutingContext routingContext=new RoutingContextImpl("", router, request, routes);
     * BodyHandlerImpl bodyHandler=new BodyHandlerImpl(); bodyHandler.handle(routingContext);
     * Set<FileUpload> files=routingContext.fileUploads();
     */
    testContext.completeNow();
    
  }

}

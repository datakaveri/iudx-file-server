package iudx.file.server.apiserver.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.AsyncFile;
import io.vertx.core.file.FileSystem;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.core.streams.ReadStream;
import io.vertx.ext.web.FileUpload;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.file.server.apiserver.service.impl.LocalStorageFileServiceImpl;
import iudx.file.server.mocks.FileUploadMock;
import net.bytebuddy.implementation.bytecode.assign.primitive.VoidAwareAssigner;

@ExtendWith({VertxExtension.class, MockitoExtension.class})
public class LocalFileStorageTest {

  static LocalStorageFileServiceImpl fileService;
  static FileUpload file;
  static FileSystem fs;

  @BeforeAll
  static void setup(Vertx vertx, VertxTestContext testContext) {

    testContext.completeNow();
  }

  @Test
  @DisplayName("success - upload file")
  public void uploadFileSuccessTest(Vertx vertx, VertxTestContext testContext) {

    fs = mock(FileSystem.class);
    fileService = new LocalStorageFileServiceImpl(fs, "/abc");
    file = new FileUploadMock();

    lenient().when(fs.mkdirBlocking(any())).thenReturn(fs);
    AsyncResult<JsonObject> asyncResult = Mockito.mock(AsyncResult.class);

    Mockito.doAnswer(new Answer<AsyncResult<JsonObject>>() {
      @SuppressWarnings("unchecked")
      @Override
      public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
        ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(3)).handle(asyncResult);
        return null;
      }
    }).when(fs).move(any(), any(), any(), any());
    lenient().when(asyncResult.succeeded()).thenReturn(true);
    lenient().when(asyncResult.result()).thenReturn(new JsonObject().put("fileName", file.fileName()));

    System.out.println(file.uploadedFileName());
    Set<FileUpload> set = new HashSet<>();
    set.add(file);

    Future<JsonObject> fut = fileService.upload(set, "mockFile.txt", "/abc");
    fut.onComplete(handler -> {

      JsonObject res = handler.result();
      assertEquals(file.fileName(), res.getString("fileName"));

      assertTrue(handler.succeeded());
      verify(fs, times(1)).move(any(), any(), any(), any());
      verify(fs, times(1)).mkdirsBlocking(any());
      testContext.completeNow();
    });

  }

  @Test
  @DisplayName("fail - upload file")
  public void uploadFileFailTest(Vertx vertx, VertxTestContext testContext) {
    fs = mock(FileSystem.class);
    fileService = new LocalStorageFileServiceImpl(fs, "/abc");
    file = new FileUploadMock();

    lenient().when(fs.mkdirBlocking(any())).thenReturn(fs);
    AsyncResult<JsonObject> asyncResult = Mockito.mock(AsyncResult.class);

    Mockito.doAnswer(new Answer<AsyncResult<JsonObject>>() {
      @SuppressWarnings("unchecked")
      @Override
      public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
        ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(3)).handle(asyncResult);
        return null;
      }
    }).when(fs).move(any(), any(), any(), any());
    lenient().when(asyncResult.failed()).thenReturn(true);

    System.out.println(file.uploadedFileName());
    Set<FileUpload> set = new HashSet<>();
    set.add(file);

    Future<JsonObject> fut = fileService.upload(set, "mockFile.txt", "/abc");
    fut.onComplete(handler -> {
      assertTrue(handler.failed());
      verify(fs, times(1)).move(any(), any(), any(), any());
      verify(fs, times(1)).mkdirsBlocking(any());
      testContext.completeNow();
    });

  }

  @Test
  @DisplayName("success - get file")
  public void downloadFileSuccessTest(Vertx vertx, VertxTestContext testContext) {
    fs = mock(FileSystem.class);
    fileService = new LocalStorageFileServiceImpl(fs, "/abc");
    file = new FileUploadMock();

    HttpServerResponse response = mock(HttpServerResponse.class);
    AsyncFile asyncFile = mock(AsyncFile.class);


    AsyncResult<Boolean> existAsyncResult = mock(AsyncResult.class);
    AsyncResult<AsyncFile> openAsyncResult = mock(AsyncResult.class);
    AsyncResult<ReadStream<Buffer>> pipeToAsyncFileResult = mock(AsyncResult.class);

    lenient().when(existAsyncResult.succeeded()).thenReturn(true);
    lenient().when(existAsyncResult.result()).thenReturn(true);

    lenient().when(openAsyncResult.succeeded()).thenReturn(true);
    lenient().when(openAsyncResult.failed()).thenReturn(false);
    lenient().when(openAsyncResult.result()).thenReturn(asyncFile);

    Mockito.doAnswer(new Answer<AsyncResult<Boolean>>() {
      @SuppressWarnings("unchecked")
      @Override
      public AsyncResult<Boolean> answer(InvocationOnMock arg0) throws Throwable {
        ((Handler<AsyncResult<Boolean>>) arg0.getArgument(1)).handle(existAsyncResult);
        return null;
      }
    }).when(fs).exists(any(), any());

    Mockito.doAnswer(new Answer<AsyncResult<AsyncFile>>() {
      @SuppressWarnings("unchecked")
      @Override
      public AsyncResult<AsyncFile> answer(InvocationOnMock arg0) throws Throwable {
        ((Handler<AsyncResult<AsyncFile>>) arg0.getArgument(2)).handle(openAsyncResult);
        return null;
      }
    }).when(fs).open(any(), any(), any());

    Mockito.doAnswer(new Answer<AsyncResult<ReadStream<Buffer>>>() {
      @SuppressWarnings("unchecked")
      @Override
      public AsyncResult<ReadStream<Buffer>> answer(InvocationOnMock arg0) throws Throwable {
        ((Handler<AsyncResult<ReadStream<Buffer>>>) arg0.getArgument(1)).handle(pipeToAsyncFileResult);
        return null;
      }
    }).when(asyncFile).pipeTo(any(), any());

//    Mockito.doAnswer(new Answer<Handler<Void>>() {
//      @SuppressWarnings("unchecked")
//      @Override
//      public Handler<Void> answer(InvocationOnMock arg0) throws Throwable {
//        ((Handler<Void>) arg0.getArgument(0)).handle(null);
//        return null;
//      }
//    }).when(asyncFile).endHandler(any());

    Future<JsonObject> fut = fileService.download(file.fileName(), "/abc", response);
    fut.onComplete(handler -> {
      assertTrue(handler.succeeded());
      verify(fs, times(1)).exists(any(), any());
      verify(fs, times(1)).open(any(), any(), any());
      testContext.completeNow();
    });

  }

  @Test
  @DisplayName("fail - get file")
  public void downloadFileFailTest(Vertx vertx, VertxTestContext testContext) {
    fs = mock(FileSystem.class);
    fileService = new LocalStorageFileServiceImpl(fs, "/abc");
    file = new FileUploadMock();

    HttpServerResponse response = mock(HttpServerResponse.class);
    AsyncFile asyncFile = mock(AsyncFile.class);


    AsyncResult<Boolean> existAsyncResult = mock(AsyncResult.class);
    AsyncResult<AsyncFile> openAsyncResult = mock(AsyncResult.class);
    AsyncResult<ReadStream<Buffer>> pipeToAsyncFileResult = mock(AsyncResult.class);

    lenient().when(existAsyncResult.succeeded()).thenReturn(true);
    lenient().when(existAsyncResult.result()).thenReturn(true);

    lenient().when(openAsyncResult.succeeded()).thenReturn(false);
    lenient().when(openAsyncResult.failed()).thenReturn(true);
    lenient().when(openAsyncResult.result()).thenReturn(asyncFile);

    Mockito.doAnswer(new Answer<AsyncResult<Boolean>>() {
      @SuppressWarnings("unchecked")
      @Override
      public AsyncResult<Boolean> answer(InvocationOnMock arg0) throws Throwable {
        ((Handler<AsyncResult<Boolean>>) arg0.getArgument(1)).handle(existAsyncResult);
        return null;
      }
    }).when(fs).exists(any(), any());

    Mockito.doAnswer(new Answer<AsyncResult<AsyncFile>>() {
      @SuppressWarnings("unchecked")
      @Override
      public AsyncResult<AsyncFile> answer(InvocationOnMock arg0) throws Throwable {
        ((Handler<AsyncResult<AsyncFile>>) arg0.getArgument(2)).handle(openAsyncResult);
        return null;
      }
    }).when(fs).open(any(), any(), any());

    Future<JsonObject> fut = fileService.download(file.fileName(), "/abc", response);
    fut.onComplete(handler -> {
      assertTrue(handler.succeeded());
      verify(fs, times(1)).exists(any(), any());
      verify(fs, times(1)).open(any(), any(), any());
      testContext.completeNow();
    });

  }

  @Test
  @DisplayName("fail - get file [not exist]")
  public void downloadFileNotExistTest(Vertx vertx, VertxTestContext testContext) {
    fs = mock(FileSystem.class);
    fileService = new LocalStorageFileServiceImpl(fs, "/abc");
    file = new FileUploadMock();

    HttpServerResponse response = mock(HttpServerResponse.class);
    AsyncResult<Boolean> existAsyncResult = mock(AsyncResult.class);

    lenient().when(existAsyncResult.succeeded()).thenReturn(true);
    lenient().when(existAsyncResult.result()).thenReturn(false);

    Mockito.doAnswer(new Answer<AsyncResult<Boolean>>() {
      @SuppressWarnings("unchecked")
      @Override
      public AsyncResult<Boolean> answer(InvocationOnMock arg0) throws Throwable {
        ((Handler<AsyncResult<Boolean>>) arg0.getArgument(1)).handle(existAsyncResult);
        return null;
      }
    }).when(fs).exists(any(), any());

    Future<JsonObject> fut = fileService.download(file.fileName(), "/abc", response);
    fut.onComplete(handler -> {
      assertTrue(handler.failed());
      verify(fs, times(1)).exists(any(), any());
      verify(fs, times(0)).open(any(), any(), any());
      testContext.completeNow();
    });
  }

  @Test
  @DisplayName("fail - get file [not exist]")
  public void downloadFileNotExistTest2(Vertx vertx, VertxTestContext testContext) {
    fs = mock(FileSystem.class);
    fileService = new LocalStorageFileServiceImpl(fs, "/abc");
    file = new FileUploadMock();

    HttpServerResponse response = mock(HttpServerResponse.class);
    AsyncResult<Boolean> existAsyncResult = mock(AsyncResult.class);

    lenient().when(existAsyncResult.succeeded()).thenReturn(false);
    lenient().when(existAsyncResult.result()).thenReturn(false);

    Mockito.doAnswer(new Answer<AsyncResult<Boolean>>() {
      @SuppressWarnings("unchecked")
      @Override
      public AsyncResult<Boolean> answer(InvocationOnMock arg0) throws Throwable {
        ((Handler<AsyncResult<Boolean>>) arg0.getArgument(1)).handle(existAsyncResult);
        return null;
      }
    }).when(fs).exists(any(), any());

    Future<JsonObject> fut = fileService.download(file.fileName(), "/abc", response);
    fut.onComplete(handler -> {
      assertTrue(handler.failed());
      verify(fs, times(1)).exists(any(), any());
      verify(fs, times(0)).open(any(), any(), any());
      testContext.completeNow();
    });
  }


  @Test
  @DisplayName("success - delete file")
  public void deleteFileTest(Vertx vertx, VertxTestContext testContext) {
    fs = mock(FileSystem.class);
    fileService = new LocalStorageFileServiceImpl(fs, "/abc");
    file = new FileUploadMock();

    AsyncResult<Boolean> existAsyncResult = mock(AsyncResult.class);
    AsyncResult<Void> deleteAsyncResult = mock(AsyncResult.class);

    lenient().when(existAsyncResult.succeeded()).thenReturn(true);
    lenient().when(existAsyncResult.result()).thenReturn(true);

    lenient().when(deleteAsyncResult.succeeded()).thenReturn(true);
    lenient().when(deleteAsyncResult.failed()).thenReturn(false);

    Mockito.doAnswer(new Answer<AsyncResult<Boolean>>() {
      @SuppressWarnings("unchecked")
      @Override
      public AsyncResult<Boolean> answer(InvocationOnMock arg0) throws Throwable {
        ((Handler<AsyncResult<Boolean>>) arg0.getArgument(1)).handle(existAsyncResult);
        return null;
      }
    }).when(fs).exists(any(), any());


    Mockito.doAnswer(new Answer<AsyncResult<Void>>() {
      @SuppressWarnings("unchecked")
      @Override
      public AsyncResult<Void> answer(InvocationOnMock arg0) throws Throwable {
        ((Handler<AsyncResult<Void>>) arg0.getArgument(1)).handle(deleteAsyncResult);
        return null;
      }
    }).when(fs).delete(any(), any());

    Future<JsonObject> fut = fileService.delete(file.fileName(), "/abc");
    fut.onComplete(handler -> {
      assertTrue(handler.succeeded());
      verify(fs, times(1)).exists(any(), any());
      verify(fs, times(1)).delete(any(), any());
      testContext.completeNow();
    });
  }


  @Test
  @DisplayName("fail - delete file")
  public void deleteFileFailedTest(Vertx vertx, VertxTestContext testContext) {
    fs = mock(FileSystem.class);
    fileService = new LocalStorageFileServiceImpl(fs, "/abc");
    file = new FileUploadMock();

    AsyncResult<Boolean> existAsyncResult = mock(AsyncResult.class);
    AsyncResult<Void> deleteAsyncResult = mock(AsyncResult.class);

    lenient().when(existAsyncResult.succeeded()).thenReturn(true);
    lenient().when(existAsyncResult.result()).thenReturn(true);

    lenient().when(deleteAsyncResult.succeeded()).thenReturn(false);
    lenient().when(deleteAsyncResult.failed()).thenReturn(true);
    lenient().when(deleteAsyncResult.cause()).thenReturn(new Throwable("failed"));

    Mockito.doAnswer(new Answer<AsyncResult<Boolean>>() {
      @SuppressWarnings("unchecked")
      @Override
      public AsyncResult<Boolean> answer(InvocationOnMock arg0) throws Throwable {
        ((Handler<AsyncResult<Boolean>>) arg0.getArgument(1)).handle(existAsyncResult);
        return null;
      }
    }).when(fs).exists(any(), any());


    Mockito.doAnswer(new Answer<AsyncResult<Void>>() {
      @SuppressWarnings("unchecked")
      @Override
      public AsyncResult<Void> answer(InvocationOnMock arg0) throws Throwable {
        ((Handler<AsyncResult<Void>>) arg0.getArgument(1)).handle(deleteAsyncResult);
        return null;
      }
    }).when(fs).delete(any(), any());

    Future<JsonObject> fut = fileService.delete(file.fileName(), "/abc");
    fut.onComplete(handler -> {
      assertTrue(handler.failed());
      verify(fs, times(1)).exists(any(), any());
      verify(fs, times(1)).delete(any(), any());
      testContext.completeNow();
    });
  }
  
  @Test
  @DisplayName("fail - delete file [not found]")
  public void deleteFileFailednotExistsTest(Vertx vertx, VertxTestContext testContext) {
    fs = mock(FileSystem.class);
    fileService = new LocalStorageFileServiceImpl(fs, "/abc");
    file = new FileUploadMock();

    AsyncResult<Boolean> existAsyncResult = mock(AsyncResult.class);

    lenient().when(existAsyncResult.succeeded()).thenReturn(true);
    lenient().when(existAsyncResult.result()).thenReturn(false);

    Mockito.doAnswer(new Answer<AsyncResult<Boolean>>() {
      @SuppressWarnings("unchecked")
      @Override
      public AsyncResult<Boolean> answer(InvocationOnMock arg0) throws Throwable {
        ((Handler<AsyncResult<Boolean>>) arg0.getArgument(1)).handle(existAsyncResult);
        return null;
      }
    }).when(fs).exists(any(), any());

    Future<JsonObject> fut = fileService.delete(file.fileName(), "/abc");
    fut.onComplete(handler -> {
      assertTrue(handler.failed());
      verify(fs, times(1)).exists(any(), any());
      verify(fs, times(0)).delete(any(), any());
      testContext.completeNow();
    });
  }
  
  @Test
  @DisplayName("fail - delete file [not found]")
  public void deleteFileFailednotExistsTest2(Vertx vertx, VertxTestContext testContext) {
    fs = mock(FileSystem.class);
    fileService = new LocalStorageFileServiceImpl(fs, "/abc");
    file = new FileUploadMock();

    AsyncResult<Boolean> existAsyncResult = mock(AsyncResult.class);

    lenient().when(existAsyncResult.succeeded()).thenReturn(false);
    lenient().when(existAsyncResult.result()).thenReturn(false);

    Mockito.doAnswer(new Answer<AsyncResult<Boolean>>() {
      @SuppressWarnings("unchecked")
      @Override
      public AsyncResult<Boolean> answer(InvocationOnMock arg0) throws Throwable {
        ((Handler<AsyncResult<Boolean>>) arg0.getArgument(1)).handle(existAsyncResult);
        return null;
      }
    }).when(fs).exists(any(), any());

    Future<JsonObject> fut = fileService.delete(file.fileName(), "/abc");
    fut.onComplete(handler -> {
      assertTrue(handler.failed());
      verify(fs, times(1)).exists(any(), any());
      verify(fs, times(0)).delete(any(), any());
      testContext.completeNow();
    });
  }

}

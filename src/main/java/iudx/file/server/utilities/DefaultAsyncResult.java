/**
 * 
 */
package iudx.file.server.utilities;

import io.vertx.core.AsyncResult;

/**
 * @author Umesh.Pacholi
 *
 */
public class DefaultAsyncResult<T> implements AsyncResult<T> {

  private final T object;
  private final Throwable exception;

  public DefaultAsyncResult(T object) {
    this.object = object;
    this.exception = null;
  }

  public DefaultAsyncResult(Throwable exception) {
    this.object = null;
    this.exception = exception;
  }

  @Override
  public T result() {
    return object;
  }

  @Override
  public Throwable cause() {
    return exception;
  }

  @Override
  public boolean succeeded() {
    return exception == null;
  }

  @Override
  public boolean failed() {
    return exception != null;
  }

}

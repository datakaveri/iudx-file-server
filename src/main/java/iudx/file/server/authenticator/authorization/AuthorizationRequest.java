package iudx.file.server.authenticator.authorization;

import java.util.Objects;

public final class AuthorizationRequest {

  private final Method method;
  private final String api;

  public AuthorizationRequest(final Method method, final String api) {
    this.method = method;
    this.api = api;
  }

  public Method getMethod() {
    return method;
  }

  public String getApi() {
    return api;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AuthorizationRequest that = (AuthorizationRequest) o;
    return getMethod() == that.getMethod() && getApi().equals(that.getApi());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getMethod(), getApi());
  }
}

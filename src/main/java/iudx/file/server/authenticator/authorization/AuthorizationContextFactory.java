package iudx.file.server.authenticator.authorization;

import iudx.file.server.common.Api;

public class AuthorizationContextFactory {

  public static AuthorizationStrategy create(String role, Api api) {

    switch (role) {
      case "consumer":
        return ConsumerAuthStrategy.getInstance(api);
      case "provider":
        return ProviderAuthStrategy.getInstance(api);
      case "delegate":
        return DelegateAuthStrategy.getInstance(api);
      default:
        throw new IllegalArgumentException(role + "role is not defined in IUDX");
    }
  }
}

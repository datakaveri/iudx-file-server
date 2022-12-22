package iudx.file.server.authenticator.authorization;

import iudx.file.server.common.Api;

public class AuthorizationContextFactory {

  public static AuthorizationStrategy create(String role, Api api) {
    
    switch (role) {
      case "consumer": {
        return new ConsumerAuthStrategy(api);
      }
      case "provider": {
        return new ProviderAuthStrategy(api);
      }
      case "delegate": {
        return new DelegateAuthStrategy(api);
      }
      default:
        throw new IllegalArgumentException(role + "role is not defined in IUDX");
    }
  }

}

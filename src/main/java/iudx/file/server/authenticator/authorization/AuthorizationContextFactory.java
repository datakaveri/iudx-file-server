package iudx.file.server.authenticator.authorization;

public class AuthorizationContextFactory {

  public static AuthorizationStrategy create(String role) {
    
    switch (role) {
      case "consumer": {
        return new ConsumerAuthStrategy();
      }
      case "provider": {
        return new ProviderAuthStrategy();
      }
      case "delegate": {
        return new DelegateAuthStrategy();
      }
      default:
        throw new IllegalArgumentException(role + "role is not defined in IUDX");
    }
  }

}

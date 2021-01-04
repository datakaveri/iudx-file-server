package iudx.file.server.service;

import java.util.Map;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

public interface TokenStore {

  /**
   * 
   * @param token
   * @return Map
   */
  public Future<Map<String,String>> getTokenDetails(String token);

  /**
   * 
   * @param authToken a valid auth tooken
   * @param filetoken a valid file token
   * @param serverId  file server Id
   * @return JsonObject containing token and validity of token
   */
  public Future<JsonObject> put(String authToken,String fileToken,String serverId);
  
  /**
   * delete a token from store.
   * @param token
   * @return
   */
  public Future<Boolean> delete(String token);

}

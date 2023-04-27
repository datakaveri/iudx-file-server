package iudx.file.server.authenticator.utilities;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

@DataObject(generateConverter = true, publicConverter = false)
public final class JwtData {

  private String accessToken;
  private String sub;
  private String iss;
  private String aud;
  private long exp;
  private long iat;
  private String iid;
  private String role;
  private JsonObject cons;

  public JwtData() {
    super();
  }

  public JwtData(JsonObject json) {
    JwtDataConverter.fromJson(json, this);
    setAccessToken(json.getString("access_token"));
  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    JwtDataConverter.toJson(this, json);
    return json;
  }

  public String getAccessToken() {
    return accessToken;
  }

  public void setAccessToken(String accessToken) {
    this.accessToken = accessToken;
  }

  public String getSub() {
    return sub;
  }

  public void setSub(String sub) {
    this.sub = sub;
  }

  public String getIss() {
    return iss;
  }

  public void setIss(String iss) {
    this.iss = iss;
  }

  public String getAud() {
    return aud;
  }

  public void setAud(String aud) {
    this.aud = aud;
  }

  public String getIid() {
    return iid;
  }

  public void setIid(String iid) {
    this.iid = iid;
  }

  public String getRole() {
    return role;
  }

  public void setRole(String role) {
    this.role = role;
  }

  public JsonObject getCons() {
    return cons;
  }

  public void setCons(JsonObject cons) {
    this.cons = cons;
  }

  public long getExp() {
    return exp;
  }

  public void setExp(long exp) {
    this.exp = exp;
  }

  public long getIat() {
    return iat;
  }

  public void setIat(long iat) {
    this.iat = iat;
  }
}

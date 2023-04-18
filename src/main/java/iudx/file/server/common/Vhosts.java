package iudx.file.server.common;

/**
 * This enum contains a mapping for IUDX vhosts available with config json Keys in databroker
 * verticle.
 *
 */
public enum Vhosts {


  IUDX_INTERNAL("internalVhost");

  public String value;

  Vhosts(String value) {
    this.value = value;
  }

}

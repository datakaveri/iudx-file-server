package iudx.file.server.common;

/**
 * This enum contains a mapping for IUDX vhosts available with config json Keys in databroker
 * verticle.
 *
 */
public enum VHosts {


  IUDX_INTERNAL("internalVhost");

  public String value;

  VHosts(String value) {
    this.value = value;
  }

}

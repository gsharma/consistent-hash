package com.github.consistenthash.ring;

/**
 * Allow for representation of a physical server/node on a ring.
 * 
 * @author gaurav
 */
public final class PhysicalNode implements Node {
  private final String key = keyProvider().key();

  @Override
  public String getKey() {
    return key;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("PhysicalNode [key:");
    builder.append(key);
    builder.append("]");
    return builder.toString();
  }

}

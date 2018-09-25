package com.github.consistenthash.ring;

import java.util.UUID;

/**
 * A random key provider relying on random Type 4 UUIDs.
 * 
 * @author gaurav
 */
public class RandomKeyProvider implements KeyProvider {

  @Override
  public String key() {
    return UUID.randomUUID().toString();
  }

}

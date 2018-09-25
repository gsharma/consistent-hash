package com.github.consistenthash.hasher;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * MD5 hash implementation.
 * 
 * @author gaurav
 */
public final class MD5HashFunction implements HashFunction {
  private MessageDigest instance;

  public MD5HashFunction() {
    try {
      instance = MessageDigest.getInstance("MD5");
    } catch (NoSuchAlgorithmException exception) {
    }
  }

  @Override
  public Long hash(final String toHash) {
    instance.reset();
    instance.update(toHash.getBytes());
    byte[] digest = instance.digest();
    long md5 = 0;
    for (int iter = 0; iter < 4; iter++) {
      md5 <<= 8;
      md5 |= ((int) digest[iter]) & 0xFF;
    }
    return md5;
  }

}

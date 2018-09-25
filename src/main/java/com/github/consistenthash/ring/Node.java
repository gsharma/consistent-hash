package com.github.consistenthash.ring;

/**
 * Basic node skeleton to represent potentially physical or virtual nodes that will live as points
 * on the ring.
 * 
 * @author gaurav
 */
public interface Node {
  // return the key which will be used for hash mapping
  String getKey();

  default KeyProvider keyProvider() {
    return new RandomKeyProvider();
  }

}

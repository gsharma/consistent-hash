package com.github.consistenthash.hasher;

import java.util.function.Function;

/**
 * Base skeleton for pluggable hash functions.
 * 
 * @author gaurav
 */
public interface HashFunction extends Function<String, Long> {
  @Override
  default Long apply(final String toHash) {
    return hash(toHash);
  }

  Long hash(final String toHash);
}

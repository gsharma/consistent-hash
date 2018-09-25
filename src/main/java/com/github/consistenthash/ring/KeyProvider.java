package com.github.consistenthash.ring;

import java.util.function.Function;

/**
 * A skeleton key provider
 * 
 * @author gaurav
 */
public interface KeyProvider extends Function<Void, String> {
  @Override
  default String apply(Void blah) {
    return key();
  }

  String key();

}

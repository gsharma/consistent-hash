package com.github.consistenthash.ring;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

import com.github.consistenthash.hasher.MD5HashFunction;

/**
 * Tests for ensuring correctness of ring consistent hasher.
 * 
 * @author gaurav
 */
public class RingConsistentHashTest {
  private static final Logger logger =
      LogManager.getLogger(RingConsistentHash.class.getSimpleName());

  {
    // System.setProperty("log4j2.debug", "true");
  }

  @Test
  public void testRingConsistentHash() {
    int physicalNodeCount = 5;
    int virtualNodeCount = 50;

    // hydrate the ring later
    final RingConsistentHash<PhysicalNode> consistentHash = new RingConsistentHash<>(new MD5HashFunction());

    final Map<PhysicalNode, Integer> physicalNodeToKeys = new HashMap<>(physicalNodeCount);
    for (int iter = 0; iter < physicalNodeCount; iter++) {
      PhysicalNode physicalNode = new PhysicalNode();
      physicalNodeToKeys.put(physicalNode, 0);
      assertTrue(consistentHash.addNode(physicalNode, virtualNodeCount));
    }

    final int keysToHash = 400;
    for (int iter = 0; iter < keysToHash; iter++) {
      final String key = UUID.randomUUID().toString();
      final PhysicalNode chosenNode = consistentHash.chooseNode(key);
      for (PhysicalNode physicalNode : physicalNodeToKeys.keySet()) {
        if (chosenNode.getKey().equals(physicalNode.getKey())) {
          int nodeCount = physicalNodeToKeys.get(physicalNode);
          physicalNodeToKeys.put(physicalNode, ++nodeCount);
          break;
        }
      }
      logger.info(String.format("key:%s, %s", key, chosenNode.toString()));
    }
    StringBuilder builder = new StringBuilder(String.format("%d keys hashedTo::[", keysToHash));
    builder.append("nodes").append(":").append(physicalNodeToKeys.values());
    builder.append("]");
    logger.info(builder.toString());

    assertEquals(physicalNodeCount, consistentHash.getTotalPhysicalNodeCount());
    assertEquals(physicalNodeCount * virtualNodeCount, consistentHash.getTotalVirtualNodeCount());
    for (PhysicalNode physicalNode : physicalNodeToKeys.keySet()) {
      assertEquals(virtualNodeCount, consistentHash.getVirtualNodeCount(physicalNode));
    }

    for (PhysicalNode physicalNode : physicalNodeToKeys.keySet()) {
      assertTrue(consistentHash.removeNode(physicalNode));
    }
    assertEquals(0, consistentHash.getTotalPhysicalNodeCount());
    assertEquals(0, consistentHash.getTotalVirtualNodeCount());
    for (PhysicalNode physicalNode : physicalNodeToKeys.keySet()) {
      assertEquals(0, consistentHash.getVirtualNodeCount(physicalNode));
    }
  }

}

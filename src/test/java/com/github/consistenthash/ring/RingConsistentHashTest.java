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
    final RingConsistentHash<PhysicalNode> consistentHash =
        new RingConsistentHash<>(new MD5HashFunction());

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
      logger.debug(String.format("key:%s, %s", key, chosenNode.toString()));
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

  @Test
  public void testRingHashMembershipChanges() {
    // 1a. Initial ring setup
    int physicalNodeCount = 20;
    int virtualNodeCount = 500;
    int keysToHash = 400_000;

    // hydrate the ring later
    final RingConsistentHash<PhysicalNode> consistentHash =
        new RingConsistentHash<>(new MD5HashFunction());

    final Map<PhysicalNode, Integer> physicalNodeToKeys = new HashMap<>();
    for (int iter = 0; iter < physicalNodeCount; iter++) {
      PhysicalNode physicalNode = new PhysicalNode();
      physicalNodeToKeys.put(physicalNode, 0);
      assertTrue(consistentHash.addNode(physicalNode, virtualNodeCount));
    }

    // 1b. Add keys, figure distribution
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
      logger.debug(String.format("key:%s, %s", key, chosenNode.toString()));
    }
    StringBuilder builder = new StringBuilder(
        String.format("%d keys hashed to %d servers::", keysToHash, physicalNodeToKeys.size()));
    for (Map.Entry<PhysicalNode, Integer> entry : physicalNodeToKeys.entrySet()) {
      builder.append("\n\t[").append(entry.getKey().getKey()).append("::").append(entry.getValue())
          .append("]");
    }
    logger.info(builder.toString());

    assertEquals(physicalNodeCount, consistentHash.getTotalPhysicalNodeCount());
    assertEquals(physicalNodeCount * virtualNodeCount, consistentHash.getTotalVirtualNodeCount());
    for (PhysicalNode physicalNode : physicalNodeToKeys.keySet()) {
      assertEquals(virtualNodeCount, consistentHash.getVirtualNodeCount(physicalNode));
    }

    // 1c. Prepare a new physicalNodeToKeys map to compare previous key distribution with the new
    // distribution with the additional server in the ring
    final Map<PhysicalNode, Integer> physicalNodeToKeysAfter = new HashMap<>();
    for (PhysicalNode physicalNode : physicalNodeToKeys.keySet()) {
      physicalNodeToKeysAfter.put(physicalNode, 0);
    }

    // 2a. Add a few new servers to the ring
    int newServersToAdd = 5;
    for (int iter = 0; iter < newServersToAdd; iter++) {
      PhysicalNode physicalNode = new PhysicalNode();
      physicalNodeToKeysAfter.put(physicalNode, 0);
      assertTrue(consistentHash.addNode(physicalNode, virtualNodeCount));
      physicalNodeCount++;
    }

    // 2b. Add keys, figure distribution again
    for (int iter = 0; iter < keysToHash; iter++) {
      final String key = UUID.randomUUID().toString();
      final PhysicalNode chosenNode = consistentHash.chooseNode(key);
      for (PhysicalNode node : physicalNodeToKeysAfter.keySet()) {
        if (chosenNode.getKey().equals(node.getKey())) {
          int nodeCount = physicalNodeToKeysAfter.get(node);
          physicalNodeToKeysAfter.put(node, ++nodeCount);
          break;
        }
      }
      // logger.info(String.format("key:%s, %s", key, chosenNode.toString()));
    }
    builder = new StringBuilder(String.format("%d keys hashed to %d servers::", keysToHash,
        physicalNodeToKeysAfter.size()));
    for (Map.Entry<PhysicalNode, Integer> entry : physicalNodeToKeysAfter.entrySet()) {
      builder.append("\n\t[").append(entry.getKey().getKey()).append("::").append(entry.getValue())
          .append("]");
    }
    logger.info(builder.toString());

    assertEquals(physicalNodeCount, consistentHash.getTotalPhysicalNodeCount());
    assertEquals(physicalNodeCount * virtualNodeCount, consistentHash.getTotalVirtualNodeCount());
    for (PhysicalNode node : physicalNodeToKeysAfter.keySet()) {
      assertEquals(virtualNodeCount, consistentHash.getVirtualNodeCount(node));
    }

    builder = new StringBuilder(String.format("%% keys moved after going from %d->%d servers::",
        physicalNodeToKeys.size(), physicalNodeToKeysAfter.size()));
    for (Map.Entry<PhysicalNode, Integer> entry : physicalNodeToKeysAfter.entrySet()) {
      builder.append("\n\t[").append(entry.getKey().getKey()).append("::");
      if (physicalNodeToKeys.containsKey(entry.getKey())) {
        builder
            .append(percentageKeysMoved(physicalNodeToKeys.get(entry.getKey()), entry.getValue()));
      } else {
        builder.append(0);
      }
      builder.append("%]");
    }
    logger.info(builder.toString());

    // cleanup
    for (PhysicalNode node : physicalNodeToKeysAfter.keySet()) {
      assertTrue(consistentHash.removeNode(node));
    }
    assertEquals(0, consistentHash.getTotalPhysicalNodeCount());
    assertEquals(0, consistentHash.getTotalVirtualNodeCount());
    for (PhysicalNode node : physicalNodeToKeysAfter.keySet()) {
      assertEquals(0, consistentHash.getVirtualNodeCount(node));
    }

  }

  private static int percentageKeysMoved(Integer previous, Integer current) {
    int difference = Math.abs(previous - current);
    return (difference * 100) / previous;
  }

}

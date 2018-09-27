package com.github.consistenthash.ring;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.consistenthash.hasher.HashFunction;

/**
 * A ring based consistent hash.
 * 
 * Guidelines:<br>
 * 1. Replicate nodes as virtual nodes to improve key distribution<br>
 * 2. Consider using a red-black tree<br>
 * 3. Don't worry about read misses, just re-hydrate from client->server<br>
 * 4. Keep application on client, don't apply to storage stratum<br>
 * 5. Tunables:<br>
 * a) virtual node count (can also vary by node size to change node weight in the ring)<br>
 * b) choice of hashing algorithm (fast and perfect, as much as possible)<br>
 * 
 * There exist many open questions one of which is efficient hot node handling.
 * 
 * @author gaurav
 */
public final class RingConsistentHash<N extends Node> {
  private static final Logger logger =
      LogManager.getLogger(RingConsistentHash.class.getSimpleName());
  private final SortedMap<Long, VirtualNode<N>> ring = new TreeMap<Long, VirtualNode<N>>();
  private final HashFunction hashFunction;

  // somewhat pessimistic attempt at locking; we can do better with some more time investment
  private final ReentrantReadWriteLock superLock = new ReentrantReadWriteLock(true);
  private final WriteLock writeLock = superLock.writeLock();
  private final ReadLock readLock = superLock.readLock();

  /**
   * Seed the ring with a "good" hash function. The definition of what's good depends on what you
   * are trying to do and is domain specific but we strive for a balance between an instantaneous
   * (blazing fast), pure hash function.
   */
  public RingConsistentHash(final HashFunction hashFunction) {
    this.hashFunction = hashFunction;
  }

  /**
   * Insert a physical node into the hash ring - this will essentially hydrate the ring with
   * virtualNodeCount number of virtual nodes associated with the provided physical node.
   * 
   * Typically, in order to lower the standard deviation from mean of the spread of keys around the
   * ring, pick a good hash function (fast, does not easily collide, mixes well) plus a sufficiently
   * large virtualNodeCount value.
   */
  public boolean addNode(final N physicalNode, final int virtualNodeCount) {
    boolean added = false;
    if (virtualNodeCount < 0) {
      throw new IllegalArgumentException(
          String.format("%d is not a valid virtual node count", virtualNodeCount));
    }
    logger.info(
        String.format("Hydrating %s with virtual node count %d", physicalNode, virtualNodeCount));
    int existingVirtualNodeCount = getVirtualNodeCount(physicalNode);
    if (writeLock.tryLock()) {
      try {
        for (int iter = 0; iter < virtualNodeCount; iter++) {
          final VirtualNode<N> virtualNode =
              new VirtualNode<N>(physicalNode, iter + existingVirtualNodeCount);
          long hash = hashFunction.hash(virtualNode.getKey());
          ring.put(hash, virtualNode);
          logger.debug(String.format("  Inserted %s at hash %d", virtualNode, hash));
          added = true; // overwrite away
        }
      } finally {
        writeLock.unlock();
      }
    }
    return added;
  }

  /**
   * Drop the physical node from the ring - this will drop all its associated virtual nodes from the
   * hash ring. As expected, this will result in a small percentage of existing keys "moving to" or
   * hashing to the remaining nodes in the ring.
   * 
   * A key goal of consistent hashing is to minimize this percentage on ring membership changes via
   * removeNode() or addNode()
   */
  public boolean removeNode(final N physicalNode) {
    boolean removed = false;
    logger.info(String.format("Dropping %s and all its virtual nodes", physicalNode));
    // this is silly, we can easily do better
    if (writeLock.tryLock()) {
      try {
        Iterator<Long> iterator = ring.keySet().iterator();
        while (iterator.hasNext()) {
          final Long key = iterator.next();
          final VirtualNode<N> virtualNode = ring.get(key);
          if (virtualNode.isVirtualNodeOf(physicalNode)) {
            iterator.remove();
            logger.debug(String.format("  Removed %s", virtualNode));
            removed = true; // overwrite away
          }
        }
      } finally {
        writeLock.unlock();
      }
    }
    return removed;
  }

  /**
   * Choose a node in the ring to store the value keyed by the provided key. Note that this, along
   * with other functions for ring based consistent hashing can purely be computed on client side of
   * a set of storage servers virtually represented by the ring of servers as seen by the clients.
   * 
   * The idea is to pick a node on the client side and then independently deal with the storage of
   * the key on that server. Storage is a concern completely disjoint with the server selection
   * algorithm.
   */
  public N chooseNode(final String key) {
    N node = null;
    if (readLock.tryLock()) {
      try {
        if (ring.isEmpty()) {
          return node;
        }
        // a. compute incoming key's hash
        final Long hash = hashFunction.hash(key);

        // b. look for all virtual nodes with hashValue >= hash
        final SortedMap<Long, VirtualNode<N>> tailMap = ring.tailMap(hash);

        // c. if not empty, select the hash for the first virtual node. Else, for wrap-around case,
        // pick the first hash (lowest hash value) from the ring
        final Long nodeHash = !tailMap.isEmpty() ? tailMap.firstKey() : ring.firstKey();

        // d. lookup hash->virtualNode->physicalNode
        node = ring.get(nodeHash).getPhysicalNode();
      } finally {
        readLock.unlock();
      }
    }
    return node;
  }

  /**
   * "Compute" total number of virtual nodes in the ring.
   */
  public int getTotalVirtualNodeCount() {
    int totalVirtualNodeCount = Integer.MIN_VALUE;
    if (readLock.tryLock()) {
      try {
        totalVirtualNodeCount = ring.values().size();
      } finally {
        readLock.unlock();
      }
    }
    return totalVirtualNodeCount;
  }

  /**
   * "Compute" total number of physical nodes in the ring.
   */
  public int getTotalPhysicalNodeCount() {
    int physicalNodeSize = Integer.MIN_VALUE;
    if (readLock.tryLock()) {
      try {
        final Set<N> physicalNodes = new HashSet<>();
        for (VirtualNode<N> virtualNode : ring.values()) {
          physicalNodes.add(virtualNode.getPhysicalNode());
        }
        physicalNodeSize = physicalNodes.size();
      } finally {
        readLock.unlock();
      }
    }
    return physicalNodeSize;
  }

  /**
   * "Compute" total number of virtual nodes in the ring belonging to/associated with the given
   * physicalNode.
   */
  public int getVirtualNodeCount(final N physicalNode) {
    int virtualNodeCount = Integer.MIN_VALUE;
    if (readLock.tryLock()) {
      virtualNodeCount = 0;
      try {
        for (final VirtualNode<N> virtualNode : ring.values()) {
          if (virtualNode.isVirtualNodeOf(physicalNode)) {
            virtualNodeCount++;
          }
        }
      } finally {
        readLock.unlock();
      }
    }
    return virtualNodeCount;
  }

}

package com.github.consistenthash.ring;

/**
 * Allow for representation of v number of Virtual Nodes on a ring such that if Physical Node Count
 * is represented by 'p' and Virtual Node Count is represented by 'v', v >> p
 * 
 * This is typically done to allow a wider spread of points on the ring.
 * 
 * @author gaurav
 */
final class VirtualNode<N extends Node> implements Node {
  // to provide a better spread
  private final char separator = '_';
  private final N physicalNode;
  private final int virtualNodeIndex;
  private final String key;

  VirtualNode(final N physicalNode, final int virtualNodeIndex) {
    this.virtualNodeIndex = virtualNodeIndex;
    this.physicalNode = physicalNode;

    // pre-compute away
    this.key = physicalNode.getKey() + separator + virtualNodeIndex;
  }

  @Override
  public String getKey() {
    return key;
  }

  // get physical node associated with this virtual node
  N getPhysicalNode() {
    return physicalNode;
  }

  // check if the provided physical node has an association with this virtual node
  boolean isVirtualNodeOf(final N physicalNode) {
    return this.physicalNode.getKey().equals(physicalNode.getKey());
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("VirtualNode [key=");
    builder.append(getKey());
    builder.append(", ");
    builder.append(physicalNode);
    builder.append(", virtualNodeIndex=");
    builder.append(virtualNodeIndex);
    builder.append("]");
    return builder.toString();
  }

}


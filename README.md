[![Build Status](https://img.shields.io/travis/gsharma/consistent-hash/master.svg)](https://travis-ci.org/gsharma/consistent-hash)
[![Test Coverage](https://img.shields.io/codecov/c/github/gsharma/consistent-hash/master.svg)](https://codecov.io/github/gsharma/consistent-hash?branch=master)
[![Quality Gate](https://sonarcloud.io/api/project_badges/measure?project=com.github.consistenthash:consistenthash&metric=alert_status)](https://sonarcloud.io/dashboard?id=com.github.consistenthash:consistenthash)
[![Licence](https://img.shields.io/hexpm/l/plug.svg)](https://github.com/gsharma/consistent-hash/blob/master/LICENSE)

# Consistent Hashing

Description coming soon - this project is wip
Karger et al. introduced the concept of consistent hashing and gave an algorithm to implement it. Consistent hashing specifies a distribution of data among servers in such a way that servers can be added or removed without having to totally reorganize the data. It was originally proposed for web caching on the Internet, in order to address the problem that clients may not be aware of the entire set of cache servers.

## Ring Consistent Hashing
https://www.akamai.com/es/es/multimedia/documents/technical-publication/consistent-hashing-and-random-trees-distributed-caching-protocols-for-relieving-hot-spots-on-the-world-wide-web-technical-publication.pdf

## Jump Consistent Hashing
https://arxiv.org/abs/1406.2294

## Rendezvous Consistent Hashing
https://www.eecs.umich.edu/techreports/cse/96/CSE-TR-316-96.pdf

## Maglev Consistent Hashing
https://ai.google/research/pubs/pub44824 (section 3.4)

## Rough Design Notes

0. Start with a circle in line with Karger et al
1. N nodes can be replicated R times to improve shard distribution. Replicated nodes to be termed Virtual nodes.
2. Shard replicated nodes' hashes to angles on the cicle
3. Add sharded nodes' hashes to a sorted map - key (angle) : value (node id)
4. Circle is now primed
5. Operations provided:
   a) data ops: add(), get(), remove() - key (angle) : value (kv pair)
   b) node ops: addNode(), removeNode()
6. Don't deal with get() misses. get() misses should be rehydrated, responsibility of clients to rehydrate from permanent storage
7. Don't apply algorithmic operations to storage stratum server-side, rather apply only client-side
8. Tunables available:
   a) replication factor (by cluster size, by hardware homogeneity)
   b) choice of hashing algorithm
9. Open questions:
   a) no perfect hashing algorithms - how to cheaply deal with collisions?
   b) how to handle hot replicas that show a major K/N skew?
   c) can we do better than Krager?
   d) what is a good value for the upper-bound of N times R?
10. Consider improvements afforded by HRW / Rendezvous Hashing.
11. Is support for CAS operations needed?

## Consistent Hashing as a library
Add mvn dependency:
```xml
<dependency>
  <groupId>com.github.consistenthash</groupId>
  <artifactId>consistenthash</artifactId>
  <version>1.0-SNAPSHOT</version>
</dependency>
```

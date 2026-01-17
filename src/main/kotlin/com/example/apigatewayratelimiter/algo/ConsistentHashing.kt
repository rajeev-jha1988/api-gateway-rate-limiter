package com.example.apigatewayratelimiter.algo

import java.util.TreeMap

/**
 *  you have multiple servers and you want to distribute incoming requests
 *  1. Hashing Technique
 *  2. Ordered Ring where we have find next celing server for a given hash
 *  3. Virtual Nodes to avoid load imbalance
 *  4. Consistent Hashing to minimize re-distribution when servers are added/removed
 *  5. Implementation using TreeMap in Kotlin
 *
 *  @param TreeMap to maintain ordered ring of servers
 *  @param MutableMap to track server locations and assigned keys
 *  @param Hash function to compute hash values for servers and keys
 *  @param Methods to add/remove servers and assign keys based on consistent hashing
 *  @param MutableMap to tract assigned keys and their hashes
 *
 *
 * */

interface HashingStrategy {
    fun calculate(
        key: String,
        keyHash: Int,
    ): Int
}

class UserHashingStrategy : HashingStrategy {
    override fun calculate(
        key: String,
        keyHash: Int,
    ): Int {
        val p = keyHash.toLong()
        val n = 360L
        var hashCode = 0L
        var pPow = 1L

        for (character in key) {
            val charVal = (character - 'A' + 1).toLong()
            hashCode = (hashCode + (charVal * pPow)) % n
            pPow = (pPow * p) % n
        }
        return hashCode.toInt()
    }
}

class HashRing(
    private val hashingStrategy: HashingStrategy,
) {
    private val ring = TreeMap<Int, MutableList<String>>()
    private val serverLocs = mutableMapOf<String, Int>()
    private val keyToAssignedServer = mutableMapOf<String, String>()
    private val keyToHash = mutableMapOf<String, Int>()

    fun addServer(
        serverId: String, // name of server
        hashKey: Int, // aditional hash key
    ): Int {
        val hVal = hashingStrategy.calculate(serverId, hashKey)
        serverLocs[serverId] = hVal
        ring.computeIfAbsent(hVal) { mutableListOf() }.add(serverId)

        var reassignCount = 0

        // Re-evaluate all keys to see if they shift to this new server
        for ((kName, keyHash) in keyToHash) {
            val (currentSrv, _) = getTargetServer(keyHash)
            if (currentSrv == serverId && keyToAssignedServer[kName] != serverId) {
                // assignedKeys[kName] = serverId
                keyToAssignedServer[kName] = serverId
                reassignCount++
            }
        }
      /*
        val serverFrom = ring.higherKey(hVal)?: ring.firstKey()
        migrateData(from = serverFrom, to= serverId, targetHas = hVal) // now we will find the lower key
        // and this will be higheykey and migrate all the data form server to server*/

        return reassignCount
    }

    fun removeServer(serverId: String): Int {
        val hVal = serverLocs[serverId] ?: return 0

        ring[hVal]?.remove(serverId)

        if (ring[hVal]?.isEmpty() == true) {
            ring.remove(hVal)
        }
        serverLocs.remove(serverId)

        var reassignCount = 0
        // Re-evaluate all keys to see if they need reassignment which assiged to server
        for ((kName, keyHash) in keyToHash) {
            if (keyToAssignedServer[kName] == serverId) {
                val (newSrv, _) = getTargetServer(keyHash)
                keyToAssignedServer[kName] = newSrv
                reassignCount++
            }
        }

        return reassignCount
    }

    fun assignKey(
        keyName: String,
        extraHashKey: Int,
    ): Int {
        val keyHash = hashingStrategy.calculate(keyName, extraHashKey)
        keyToHash[keyName] = keyHash

        val (serverId, serverHash) = getTargetServer(keyHash)
        keyToAssignedServer[keyName] = serverId

        return serverHash
    }

    fun getTargetServer(hash: Int): Pair<String, Int> {
        val targetedHash = ring.ceilingKey(hash) ?: ring.firstKey()
        val servername = ring[targetedHash]!!.last() // as on ring for given hash we take last added server
        return Pair(servername, targetedHash)
    }

    /**
     * GET Operation: Locates the physical server responsible for a key.
     *
     * @param keyName The name of the record to find.
     * @param hashKey The value used for the hash function.
     * @return The ID of the server where this key's data is stored.
     */
    fun get(
        keyName: String,
        hashKey: Int,
    ): String {
        val keyDegree = hashingStrategy.calculate(keyName, hashKey)

        // Use the same clockwise logic: find first server degree >= keyDegree
        val targetDegree =
            ring.ceilingKey(keyDegree) ?: ring.firstKey()
                ?: throw IllegalStateException("No servers available in the ring.")

        // Rule: If multiple servers share a degree, return the latest added (last in list)
        return ring[targetDegree]!!.last()
    }
}

// Example Usage
fun main() {
    val hashingStrategy = UserHashingStrategy()
    val hashRing = HashRing(hashingStrategy)
    val operation =
        arrayOf(
            "ADD",
            "ASSIGN",
            "ASSIGN",
            "ASSIGN",
            "ADD",
            "ASSIGN",
            "ASSIGN",
            "ASSIGN",
            "ADD",
            "ASSIGN",
            "ASSIGN",
            "ASSIGN",
            "ADD",
            "ASSIGN",
            "ASSIGN",
            "ASSIGN",
            "REMOVE",
            "ASSIGN",
            "ASSIGN",
            "ASSIGN",
            "REMOVE",
            "ASSIGN",
            "ASSIGN",
            "ASSIGN",
            "REMOVE",
            "ASSIGN",
            "ASSIGN",
        )
    val serverAndKeys =
        arrayOf(
            "INDIA",
            "VLVL",
            "OXXV",
            "HHGN",
            "RUSSIA",
            "AWNF",
            "SPHI",
            "FXKT",
            "CHINA",
            "JXZU",
            "BWPK",
            "JYWN",
            "GERMANY",
            "ZKYK",
            "HLQZ",
            "BRMS",
            "INDIA",
            "FMVA",
            "NPJO",
            "GACA",
            "RUSSIA",
            "ZMWM",
            "XVUA",
            "IDUW",
            "CHINA",
            "EHWW",
            "KROX",
        )
    val extraHashing =
        intArrayOf(
            431,
            563,
            223,
            761,
            197,
            409,
            31,
            223,
            769,
            619,
            991,
            613,
            139,
            797,
            547,
            821,
            -1,
            131,
            577,
            269,
            -1,
            499,
            599,
            29,
            -1,
            13,
            337,
        )

    val results = IntArray(operation.size)

    for (i in operation.indices) {
        results[i] =
            when (operation[i]) {
                "ADD" -> hashRing.addServer(serverAndKeys[i], extraHashing[i])
                "REMOVE" -> hashRing.removeServer(serverAndKeys[i])
                "ASSIGN" -> hashRing.assignKey(serverAndKeys[i], extraHashing[i])
                else -> 0
            }
    }

    println(results.joinToString(", "))
}

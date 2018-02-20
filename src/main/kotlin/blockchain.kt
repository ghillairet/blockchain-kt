import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.kittinunf.fuel.httpGet
import com.google.common.hash.Hashing
import java.net.URL
import java.util.*

data class Block(val index: Long,
                 val timestamp: Long,
                 val transactions: Set<Transaction>,
                 val proof: Long,
                 val previousHash: String)

data class Transaction(val sender: String,
                       val recipient: String,
                       val amount: Int)

data class Payload(val length: Int = 0, val chain: List<Block> = emptyList())

class Blockchain {

    private val chain: MutableList<Block> = mutableListOf()
    private var currentTransactions: Set<Transaction> = emptySet()
    private val nodes: MutableSet<URL> = mutableSetOf()

    init {
        newBlock(100, "1")
    }

    /**
     * Returns the current list of blocks
     */
    fun chain() = chain

    /**
     * Returns the list of nodes registered by this blockchain
     */
    fun nodes() = nodes

    /**
     * Registers a server node to the current list of known nodes.
     *
     * @param node a valid URL
     */
    fun register(node: URL) {
        nodes.add(node)
    }

    /**
     * Determines if the given block chain is valid.
     *
     * @param chain Blockchain to validate
     * @return true if valid, false otherwise
     */
    fun validChain(chain: List<Block>): Boolean {
        var lastBlock = chain[0]
        var currentIndex = 1

        while (currentIndex < chain.size) {
            val block = chain[currentIndex]

            // Check that the hash of the block is correct
            if (block.previousHash != hash(lastBlock))
                return false

            // Check that the Proof of Work is correct
            if (!isValidProof(lastBlock.proof, block.proof))
                return false

            lastBlock = block
            currentIndex++
        }
        return true
    }

    /**
     * This is our Consensus Algorithm, it resolves conflicts by replacing
     * our chain with the longest one in the network.
     *
     * @return true if our chain has been replaced, false otherwise
     */
    fun resolveConflicts(): Boolean {
        val maxLength = chain.size

        val chains = nodes
                .map { it.httpGet().response().third }
                .map { value -> value.fold({ mapper.readValue(it, Payload::class.java) }, { Payload() }) }
                .filter { it != null && it.length > maxLength && validChain(it.chain) }
                .sortedByDescending { it.length }
                .map { it.chain }

        return if (chains.isNotEmpty()) {
            chain.clear()
            chain.addAll(chains[0])
            true
        } else {
            false
        }
    }


    private fun URL.httpGet() =
            this.toString().httpGet()

    fun newBlock(proof: Long, previousHash: String): Block {
        val block = Block(
                index = chain.count() + 1L,
                timestamp = Date().time,
                transactions = currentTransactions,
                proof = proof,
                previousHash = previousHash)

        currentTransactions = emptySet()
        chain.add(block)
        return block
    }

    fun newTransaction(sender: String, recipient: String, amount: Int): Long {
        currentTransactions += Transaction(sender, recipient, amount)
        return lastBlock().index + 1
    }

    fun lastBlock() = chain.last()

    fun proofOfWork(lastProof: Long): Long {
        var proof = 0L
        while (!isValidProof(lastProof, proof))
            proof += 1
        return proof
    }

    private fun isValidProof(lastProof: Long, proof: Long): Boolean =
            Hashing.sha256()
                    .hashString("$lastProof$proof", charset("UTF-8"))
                    .toString()
                    .endsWith("0000")

    companion object {

        private val mapper = jacksonObjectMapper()

        fun hash(block: Block): String =
                Hashing.sha256()
                        .hashString(mapper.writeValueAsString(block), charset("UTF-8"))
                        .toString()
    }
}
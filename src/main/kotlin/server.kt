import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import spark.Spark.get
import spark.Spark.post
import java.net.URL
import java.util.*

fun main(args: Array<String>) {

    val blockchain = Blockchain()
    val mapper = jacksonObjectMapper()
    val nodeIdentifier = UUID.randomUUID().toString().replace("-", "")

    println("Starting node $nodeIdentifier")

    get("/mine") { _, resp ->
        val lastBlock = blockchain.lastBlock()
        val lastProof = lastBlock.proof
        val proof = blockchain.proofOfWork(lastProof)

        // We must receive a reward for finding the proof.
        // The sender is "0" to signify that this node has mined a new coin.
        blockchain.newTransaction("0", nodeIdentifier, 1)

        // Forge the new Block by adding it to the chain
        val previousHash = Blockchain.hash(lastBlock)
        val block = blockchain.newBlock(proof, previousHash)

        resp.status(200)
        resp.type("application/json")

        mapper.writeValueAsString(mapper.createObjectNode()
                .put("message", "New Block forged")
                .put("index", block.index)
                .putPOJO("transactions", block.transactions)
                .put("proof", block.proof)
                .put("previous_hash", block.previousHash)
        )
    }

    get("/chain") { _, resp ->
        resp.status(200)
        resp.type("application/json")

        mapper.writeValueAsString(mapper.createObjectNode()
                .putPOJO("chain", blockchain.chain())
                .put("length", blockchain.chain().size))
    }

    post("/transactions/new") { req, resp ->
        val sender = req.queryParams("sender")
        val recipient = req.queryParams("recipient")
        val amount = Integer.parseInt(req.queryParams("amount"))

        val index = blockchain.newTransaction(sender, recipient, amount)

        resp.status(201)
        resp.type("application/json")

        mapper.writeValueAsString(mapper.createObjectNode()
                .put("message", "Transaction will be added to Block $index"))
    }

    post("/nodes/register") { req, resp ->
        val nodes = req.queryParamsValues("nodes")

        nodes.forEach { blockchain.register(URL(it)) }

        resp.status(201)
        resp.type("application/json")

        mapper.writeValueAsString(mapper.createObjectNode()
                .put("message", "New nodes have been added")
                .putPOJO("nodes", blockchain.nodes())
        )
    }

    get("/nodes/resolve") { _, resp ->
        val replaced = blockchain.resolveConflicts()
        val message = if (replaced) "Our chain was replaced" else "Our chain is authoritative"

        resp.status(200)
        resp.type("application/json")

        mapper.writeValueAsString(mapper.createObjectNode()
                .put("message", message)
                .putPOJO("chain", blockchain.chain()))
    }
}
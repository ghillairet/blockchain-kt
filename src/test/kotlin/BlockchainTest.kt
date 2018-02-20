import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.util.*

class BlockchainTest {

    @Test
    fun testAddBlock() {
        val blockchain = Blockchain()
        val block = blockchain.newBlock(10, Blockchain.hash(blockchain.lastBlock()))

        assertThat(block).isNotNull()
        assertThat(block.index).isEqualTo(2)

        val b2 = blockchain.newBlock(10, Blockchain.hash(blockchain.lastBlock()))
        assertThat(b2.previousHash).isEqualTo(Blockchain.hash(block))
    }

    @Test
    fun testHash() {
        val hash = Blockchain.hash(Block(
                index = 1,
                timestamp = Date().time,
                transactions = setOf(Transaction("me", "you", 22)),
                proof = 10,
                previousHash = "1"))

        println(hash)
    }

}
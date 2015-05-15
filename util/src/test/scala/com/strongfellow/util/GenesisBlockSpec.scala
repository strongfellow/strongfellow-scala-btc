
import java.nio.file.{Files, Paths}
import org.scalatest.FlatSpec

class GenesisBlockSpec extends FlatSpec {

  val bytes = Files.readAllBytes(
      Paths.get(getClass.getResource("com/strongfellow/util/genesis.bin").getPath()))

  "uint" should "function correctly" in {
    val bs = Array[Byte](97, -5, 100, 114, 101, 119)
    assert(BlockParser.uint(bs, 0, bs.length) == 131277594622817L)
  }

  "The genesis block" should "have 293 bytes" in {
    assert(bytes.length == 293)
  }

  it should "have magic number f9beb4d9" in {
    assert(BlockParser.magic(bytes) == "f9beb4d9")
  }
  it should "have length 285" in {
    assert(BlockParser.blockLength(bytes) == 285)
  }

  it should "have block hash 000000000019d6689c085ae165831e934ff763ae46a2a6c172b3f1b60a8ce26f" in {
    assert(BlockParser.blockHash(bytes) == "000000000019d6689c085ae165831e934ff763ae46a2a6c172b3f1b60a8ce26f")
  }
  
  it should "have version 1" in {
    assert(BlockParser.version(bytes) == 1L)
  }

  it should "have previous block hash 0000000000000000000000000000000000000000000000000000000000000000" in {
    assert(BlockParser.hashPrevBlock(bytes) == "0000000000000000000000000000000000000000000000000000000000000000")
  }
  it should "have merkle root hash 4a5e1e4baab89f3a32518a88c31bc87f618f76673e2cc77ab2127b7afdeda33b" in {
    assert(BlockParser.hashMerkleRoot(bytes) == "4a5e1e4baab89f3a32518a88c31bc87f618f76673e2cc77ab2127b7afdeda33b")
  }

  it should "have time 1231006505" in {
    assert(BlockParser.time(bytes) == 1231006505L)
  }
  it should "have bits ffff001d" in {
    assert(BlockParser.bits(bytes) == "ffff001d")
  }

  it should "have nonce 2083236893" in {
    assert(BlockParser.nonce(bytes) == 2083236893)
  }

  it should "have 1 transaction" in {
    assert(BlockParser.transactions(bytes).length == 1)
  }

  it should "have transaction hash 4a5e1e4baab89f3a32518a88c31bc87f618f76673e2cc77ab2127b7afdeda33b" in {
    var tx = BlockParser.transactions(bytes)(0)
    assert(BlockParser.doubleSha(tx) == "4a5e1e4baab89f3a32518a88c31bc87f618f76673e2cc77ab2127b7afdeda33b")
  }

  it should "have merkleRoot 4a5e1e4baab89f3a32518a88c31bc87f618f76673e2cc77ab2127b7afdeda33b" in {
    assert(BlockParser.computedMerkleRoot(bytes) == "4a5e1e4baab89f3a32518a88c31bc87f618f76673e2cc77ab2127b7afdeda33b")
  }


}

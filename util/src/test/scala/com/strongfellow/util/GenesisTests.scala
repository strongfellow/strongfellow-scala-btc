
import java.nio.file.{Files, Paths}
import org.scalatest.FlatSpec

class GenesisBlockSpec extends FlatSpec {

  val bytes = Files.readAllBytes(
      Paths.get(getClass.getResource("com/strongfellow/util/genesis.bin").getPath()))

  "The genesis block" should "have 293 bytes" in {
    assert(bytes.length == 293)
  }
}

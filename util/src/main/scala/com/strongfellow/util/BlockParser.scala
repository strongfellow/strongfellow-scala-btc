
import java.security.MessageDigest

object BlockParser {
  def magic(block:Seq[Byte]) : String = hex(block.take(4))
  def blockLength(block:Seq[Byte]) : Long = uint32(block.drop(4))

  def hashHex(bytes:Seq[Byte]) = {
    bytes.take(32).reverseIterator.map(byteToHex).mkString
  }

  def uint(bytes:Seq[Byte]) : Long = {
    bytes.zipWithIndex.map({ case (x:Byte, i:Int) => ((x & 0xffL) << (8*i))}).sum
  }

  def uint8(bytes:Seq[Byte]) : Long = uint(bytes.take(1))
  def uint16(bytes:Seq[Byte]) : Long = uint(bytes.take(2))
  def uint32(bytes:Seq[Byte]) : Long = uint(bytes.take(4))
  def uint64(bytes:Seq[Byte]) : Long = uint(bytes.take(8))

  def byteToHex(b:Byte) = "%02x".format(b & 0xff)
  def hex(bytes:Seq[Byte]) : String = bytes.map(byteToHex).mkString

  def dHash(byteSeqs:Seq[Byte]*) = {
    val md = MessageDigest.getInstance("SHA-256")
    for (bytes <- byteSeqs) {
      bytes.foreach { md.update(_) }
    }
    val d1 = md.digest()
    md.reset()
    md.update(d1)
    md.digest()
  }

  def reverseHex(bytes:Seq[Byte]) = bytes.reverseIterator.map(byteToHex).mkString

  def doubleSha(bytes:Seq[Byte]) : String = reverseHex(dHash(bytes))

  def blockHash(bytes:Seq[Byte], offset:Int = 0) : String = {
    doubleSha(bytes.slice(offset + 8, offset + 88))
  }

  def version(bytes:Seq[Byte], offset:Int = 0) : Long = uint32(bytes.drop(offset + 8))
  def hashPrevBlock(bytes:Seq[Byte], offset:Int = 0) : String = hashHex(bytes.drop(offset + 12))
  def hashMerkleRoot(bytes:Seq[Byte], offset:Int = 0) : String = hashHex(bytes.drop(offset + 44))
  def time(block:Seq[Byte]) : Long = uint32(block.drop(76))
  def bits(block:Seq[Byte]) : String = hex(block.drop(80).take(4))
  def nonce(bytes:Seq[Byte], offset:Int = 0) : Long = uint32(bytes.drop(84))

  def varInt(bytes:Seq[Byte]) : (Long, Int) = {
    val n = uint8(bytes)
    if (n < 0xfd)
      (n, 1)
    else if (n == 0xfd)
      (uint16(bytes.drop(1)), 3)
    else if (n == 0xfe)
      (uint32(bytes.drop(1)), 5)
    else
      (uint64(bytes.drop(1)), 9)
  }

  def transactionLength(bytes:Seq[Byte]) : Int = {
    var index = 0
    index += 4 // version
    val (nIn, nInBytes) = varInt(bytes.drop(index))
    index += nInBytes
    for (j <- 1L to nIn) {
      index += 32 // hash
      index += 4 // index
      val (nScript, nBytes) = varInt(bytes.drop(index))
      index += nBytes
      index += nScript.toInt
      index += 4 // sequenceNo
    }
    val (nOut, nOutBytes) = varInt(bytes.drop(index))
    index += nOutBytes
    for (j <- 1L to nOut) {
      index += 8 // value
      val (nScript, nScriptBytes) = varInt(bytes.drop(index))
      index += nScriptBytes
      index += nScript.toInt
    }
    index += 4 // lock time
    index
  }

  def transactions(block:Seq[Byte]) = {
    val (n, b) = varInt(block.drop(88))
    var start = 88 + b
    val xs = 1.to(n.toInt).scanLeft(start)((s,_) => s + transactionLength(block.drop(s)))
    (xs zip xs.tail).map { case(s, e) => block.drop(s).take(e - s) }
  }

  def computedMerkleRoot(bytes:Seq[Byte]) : String = {
    var shas = transactions(bytes).map(dHash(_)).toArray
    var len = shas.length
    while (len > 1) {
      for (i <- (0 until len by 2)) {
        val a = shas(i)
        val b = if (i + 1 < len) shas(i + 1) else shas(i)
        shas(i / 2) = dHash(a, b)
      }
      len = ((len / 2) + (len % 2))
    }
    reverseHex(shas(0))
  }

}

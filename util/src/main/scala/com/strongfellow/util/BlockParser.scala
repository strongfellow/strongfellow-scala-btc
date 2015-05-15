
import java.security.MessageDigest

object BlockParser {

  def hashHex(bytes:Array[Byte], offset:Int) = {
    bytes.drop(offset).take(32).reverseIterator.map(byteToHex).mkString
  }

  def uint(bytes:Array[Byte], offset:Int, n:Int) : Long = {
    bytes.drop(offset).take(n).zipWithIndex.map({ case (x:Byte, i:Int) => ((x & 0xffL) << (8*i))}).sum
  }

  def uint8(bytes:Array[Byte], offset:Int) : Long = uint(bytes, offset, 1)
  def uint16(bytes:Array[Byte], offset:Int) : Long = uint(bytes, offset, 2)
  def uint32(bytes:Array[Byte], offset:Int) : Long = uint(bytes, offset, 4)
  def uint64(bytes:Array[Byte], offset:Int) : Long = uint(bytes, offset, 8)

  def byteToHex(b:Byte) : String = "%02x".format(b & 0xff)

  def hex(bytes:Array[Byte], offset:Int, length:Int) : String = bytes.drop(offset).take(length).map(byteToHex).mkString
  def magic(bytes:Array[Byte], offset:Int = 0) : String = hex(bytes, offset, 4)
  def blockLength(bytes:Array[Byte], offset:Int = 0) : Long = uint32(bytes, offset + 4)

  def dHash(byteSeqs:Seq[Byte]*) : Array[Byte] = {
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

  def blockHash(bytes:Array[Byte], offset:Int = 0) : String = {
    doubleSha(bytes.slice(offset + 8, offset + 88))
  }

  def version(bytes:Array[Byte], offset:Int = 0) : Long = uint32(bytes, offset + 8)
  def hashPrevBlock(bytes:Array[Byte], offset:Int = 0) : String = hashHex(bytes, offset + 12)
  def hashMerkleRoot(bytes:Array[Byte], offset:Int = 0) : String = hashHex(bytes, offset + 44)
  def time(bytes:Array[Byte], offset:Int = 0) : Long = uint32(bytes, offset + 76)
  def bits(bytes:Array[Byte], offset:Int = 0) : String = hex(bytes, offset + 80, 4)
  def nonce(bytes:Array[Byte], offset:Int = 0) : Long = uint32(bytes, offset + 84)

  def varInt(bytes:Array[Byte], offset:Int) : (Long, Int) = {
    val n = uint8(bytes, offset)
    if (n < 0xfd)
      (n, 1)
    else if (n == 0xfd)
      (uint16(bytes, offset + 1), 3)
    else if (n == 0xfe)
      (uint32(bytes, offset + 1), 5)
    else
      (uint64(bytes, offset + 1), 9)
  }

  def endTransaction(bytes:Array[Byte], start:Int) : Int = {
    var index = start
    index += 4 // version
    val (nIn, nInBytes) = varInt(bytes, index)
    index += nInBytes
    for (j <- 1L to nIn) {
      index += 32 // hash
      index += 4 // index
      val (nScript, nBytes) = varInt(bytes, index)
      index += nBytes
      index += nScript.toInt
      index += 4 // sequenceNo
    }
    val (nOut, nOutBytes) = varInt(bytes, index)
    index += nOutBytes
    for (j <- 1L to nOut) {
      index += 8 // value
      val (nScript, nScriptBytes) = varInt(bytes, index)
      index += nScriptBytes
      index += nScript.toInt
    }
    index += 4 // lock time
    index
  }

  def transactions(bytes:Array[Byte], offset:Int = 0) = {
    val (n, b) = varInt(bytes, offset + 88)
    var start = offset + 88 + b
    val xs = 1.to(n.toInt).scanLeft(start)((s,_) => endTransaction(bytes,s))
    (xs zip xs.tail).map { case(s, e) => bytes.view.slice(s, e)}
  }

  def computedMerkleRoot(bytes:Array[Byte]) : String = {
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

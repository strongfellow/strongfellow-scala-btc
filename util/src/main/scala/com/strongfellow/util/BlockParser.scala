
package com.strongfellow.util

import java.security.MessageDigest

object BlockParser {
  private def varString(bs:Seq[Byte]) = {
    val (n,b) = varInt(bs)
    hex(bs.drop(b).take(n.toInt))
  }

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

  private def subSeqGenerator(bytes:Seq[Byte], f: Seq[Byte] => Int) = {
    val (n,b) = varInt(bytes)
    var start = b
    val xs = 1.to(n.toInt).scanLeft(start)((s,_) => s + f(bytes.drop(s)))
    (xs zip xs.tail).map { case(s, e) => bytes.drop(s).take(e - s) }
  }

  def transactions(block:Seq[Byte]) = subSeqGenerator(block.drop(88), transactionLength)

  def txVersion(tx:Seq[Byte]) : Long = uint8(tx)
  def txIns(tx:Seq[Byte]) : Seq[Seq[Byte]] = subSeqGenerator(tx.drop(4), txInLength)
  def txOuts(tx:Seq[Byte]) : Seq[Seq[Byte]] = {
    var start = 4 
    val (n, b) = varInt(tx.drop(4))
    start += b
    start += txIns(tx).map(_.length).sum
    subSeqGenerator(tx.drop(start), txOutLength)
  }

  def txInLength(bytes:Seq[Byte]) : Int = {
    var index = 32 // hash
    index += 4 // index
    val (nScript, nBytes) = varInt(bytes.drop(index))
    index += nBytes
    index += nScript.toInt
    index += 4 // sequenceNo
    index
  }

  def txInHash(txin:Seq[Byte]) = hashHex(txin)
  def txInIndex(txin:Seq[Byte]) = uint32(txin.drop(32))
  def txInScript(txin:Seq[Byte]) = varString(txin.drop(36))
  def txInSequenceNo(txin:Seq[Byte]) = uint32(txin.takeRight(4))

  def txOutLength(bytes:Seq[Byte]) = {
    var index = 8 // value
    val (nScript, nScriptBytes) = varInt(bytes.drop(index))
    index += nScriptBytes
    index += nScript.toInt
    index
  }
  def txOutValue(txout:Seq[Byte]) : Long = uint64(txout)
  def txOutScript(txout:Seq[Byte]) = varString(txout.drop(8))

  def computedMerkleRoot(block:Seq[Byte]) : String = {
    var shas = transactions(block).map(dHash(_)).toArray
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

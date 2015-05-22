
package com.strongfellow.awsutil

import java.io.InputStream
import java.io.ByteArrayInputStream
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.ObjectMetadata
import java.security.MessageDigest

class Cache {
  val s3:AmazonS3 = new AmazonS3Client()

  def md5(bs:Array[Byte]):String = {
    MessageDigest.getInstance("MD5").digest(bs).map("%02x".format(_)).mkString("")
  }

  def remoteMd5(bucket:String, key:String):Option[String] = {
    try {
      val remote = s3.getObjectMetadata(bucket, key).getETag()
      Some(remote)
    } catch {
      case _:com.amazonaws.services.s3.model.AmazonS3Exception => None
    }
  }

  def put(bs:Array[Byte], bucket:String, key:String):Unit = {
    val stream:InputStream = new ByteArrayInputStream(bs)
    val meta:ObjectMetadata = new ObjectMetadata()
    meta.setContentLength(bs.length)
    s3.putObject(bucket, key, stream, meta);
  }
}

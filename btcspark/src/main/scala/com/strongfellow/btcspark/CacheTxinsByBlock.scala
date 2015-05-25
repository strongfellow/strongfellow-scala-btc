
package com.strongfellow.btcspark

import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import org.apache.hadoop.io._
import com.strongfellow.util._
import com.strongfellow.awsutil._

object CacheTxinsByBlock {

  def main(args: Array[String]) {

    object CacheWrapper extends Serializable {
      lazy val cache = new Cache()
    }

    val bucket = "strongfellow.com"    
    val sc = new SparkContext()
    val output = args.head
    val inputs = args.tail

    val rdd:RDD[(String,BytesWritable)] = inputs.map(sc.sequenceFile[String,BytesWritable](_)).reduceLeft(_ ++ _)

    rdd.foreach({ case (block, bytes) => 
      val key = "networks/f9beb4d9/blocks/%s/txins".format(block)
      CacheWrapper.cache.cache(bytes.copyBytes(), bucket, key)
    })
  }
}

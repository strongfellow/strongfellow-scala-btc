import org.apache.spark.rdd.RDD
import org.apache.hadoop.io._
import com.strongfellow.util._
import scala.math.Ordering.Implicits._

val rdd:RDD[(NullWritable,BytesWritable)] = sc.sequenceFile("projects/strongfellow/mrutils/shuffle-out")

def f(data:Array[Byte]) = {
  val blockHash = BlockParser.blockHash(data.view)
  BlockParser.transactions(data.view).zipWithIndex.flatMap({ case (tx, i) =>
    val txHash = BlockParser.doubleSha(tx)

    val a = BlockParser.txIns(tx).zipWithIndex.map({ case (txin, j) =>
      ((BlockParser.txInHash(txin), BlockParser.txInIndex(txin)),
       (1, -1L, blockHash, i, j))
    })
    val b = BlockParser.txOuts(tx).zipWithIndex.map({ case (txout, j) =>
      ((txHash, j),
       (0, BlockParser.txOutValue(txout), blockHash, -1, -1))
    })
    a ++ b
  })
}

val m1 = rdd.flatMap(pair => f(pair._2.getBytes()))
val r1 = m1.groupByKey().flatMap({ case ((tx, index), tuples) =>
  val tups = tuples.toArray.sortWith(_._1 < _._1)
  val value = tups.head._2
  tups.tail.map({
    case (_, _, block, i, j) => {
      (block, (i, j, value))
    }
  })
})

def bs(n:Long) : Seq[Byte] = (0 to 3).map(i => (n >>> (8 * i) & 0xff).byteValue())

val r2 = r1.groupByKey().map({ case (block, tuples) => 
  (block, tuples.toArray.sortWith(_ < _).map({case (i, j, value) => (i, j,value)}).mkString("|"))
})

val r3 = r1.groupByKey().map({ case (block, tuples) => 
  (block, tuples.toArray.sortWith(_ < _).flatMap({case (i, j, value) => bs(value)}))
})

r2.saveAsTextFile("block6")

r1.saveAsTextFile("tmpmagic2")



import org.apache.spark.rdd.RDD
import org.apache.hadoop.io._

val inputPath = "s3://emr.strongfellow.com/crawl/outputs/2015-05-07"
val outputPath = "s3://emr.strongfellow.com/crawl/outputs/2015-05-07-shuffled"

val in:RDD[(NullWritable,BytesWritable)] = sc.sequenceFile(inputPath)
val ps = in.map(_._2.copyBytes()).repartition(101).map(a => (NullWritable.get(), new BytesWritable(a)))
ps.saveAsSequenceFile(outputPath, None)

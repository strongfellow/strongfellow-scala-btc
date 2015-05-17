import org.apache.spark.rdd.RDD
import org.apache.hadoop.io._
import com.strongfellow.util._

val rdd:RDD[(NullWritable,BytesWritable)] = sc.sequenceFile("projects/strongfellow/mrutils/shuffle-out")

def f(data:Array[Byte]) : (String,String) = (BlockParser.blockHash(data.view), BlockParser.computedMerkleRoot(data.view))
val summaries = rdd.map(pair => f(pair._2.getBytes()))
summarise.saveAsTextFile("tmpsummaries")

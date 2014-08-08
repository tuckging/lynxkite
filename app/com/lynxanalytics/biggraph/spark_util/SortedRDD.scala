package com.lynxanalytics.biggraph.spark_util

import org.apache.spark.{ Partition, TaskContext }
import org.apache.spark.rdd._
import org.apache.spark.SparkContext.rddToPairRDDFunctions
import scala.reflect.ClassTag

import scala.collection.mutable.ArrayBuffer
import scala.reflect.ClassTag

import com.lynxanalytics.biggraph.{ bigGraphLogger => log }
import com.lynxanalytics.biggraph.spark_util.Implicits._

object SortedRDD {
  // Creates a SortedRDD from an unsorted RDD.
  def fromUnsorted[K: Ordering, V](rdd: RDD[(K, V)]): SortedRDD[K, V] =
    new SortedRDD(
      rdd.mapPartitions(_.toIndexedSeq.sortBy(_._1).iterator, preservesPartitioning = true))

  // Wraps an already sorted RDD.
  def fromSorted[K: Ordering, V](rdd: RDD[(K, V)]): SortedRDD[K, V] =
    new SortedRDD(rdd)
}
// An RDD with each partition sorted by the key. "self" must already be sorted.
class SortedRDD[K: Ordering, V] private[spark_util] (self: RDD[(K, V)]) extends RDD[(K, V)](self) {
  assert(self.partitioner.isDefined)
  override def getPartitions: Array[Partition] = self.partitions
  override val partitioner = self.partitioner
  override def compute(split: Partition, context: TaskContext) = self.compute(split, context)

  private def merge[K, V, W](
    bi1: collection.BufferedIterator[(K, V)],
    bi2: collection.BufferedIterator[(K, W)])(implicit ord: Ordering[K]): Stream[(K, (V, W))] = {
    if (bi1.hasNext && bi2.hasNext) {
      val (k1, v1) = bi1.head
      val (k2, v2) = bi2.head
      if (ord.equiv(k1, k2)) {
        bi1.next; //bi2.next; // uncomment to disallow multiple keys on the left side
        (k1, (v1, v2)) #:: merge(bi1, bi2)
      } else if (ord.lt(k1, k2)) {
        bi1.next
        merge(bi1, bi2)
      } else {
        bi2.next
        merge(bi1, bi2)
      }
    } else {
      Stream()
    }
  }

  private def leftOuterMerge[K, V, W](
    bi1: collection.BufferedIterator[(K, V)],
    bi2: collection.BufferedIterator[(K, W)])(implicit ord: Ordering[K]): Stream[(K, (V, Option[W]))] = {
    if (bi1.hasNext && bi2.hasNext) {
      val (k1, v1) = bi1.head
      val (k2, v2) = bi2.head
      if (ord.equiv(k1, k2)) {
        bi1.next; //bi2.next; // uncomment to disallow multiple keys on the left side
        (k1, (v1, Some(v2))) #:: leftOuterMerge(bi1, bi2)
      } else if (ord.lt(k1, k2)) {
        bi1.next
        (k1, (v1, None)) #:: leftOuterMerge(bi1, bi2)
      } else {
        bi2.next
        leftOuterMerge(bi1, bi2)
      }
    } else if (bi1.hasNext) {
      bi1.toStream.map { case (k, v) => (k, (v, None)) }
    } else {
      Stream()
    }
  }

  /* 
   * Differs from Spark's join implementation as this allows multiple keys only on the left side
   * the keys of 'other' must be unique!
   */
  def sortedJoin[W](other: SortedRDD[K, W]): SortedRDD[K, (V, W)] = {
    assert(self.partitions.size == other.partitions.size, s"Size mismatch between $self and $other")
    assert(self.partitioner == other.partitioner, s"Partitioner mismatch between $self and $other")
    val zipped = this.zipPartitions(other, true) { (it1, it2) => merge(it1.buffered, it2.buffered).iterator }
    new SortedRDD(zipped)
  }

  /* 
   * Differs from Spark's join implementation as this allows multiple keys only on the left side
   * the keys of 'other' must be unique!
   */
  def sortedLeftOuterJoin[W](other: SortedRDD[K, W]): SortedRDD[K, (V, Option[W])] = {
    assert(self.partitions.size == other.partitions.size, s"Size mismatch between $self and $other")
    assert(self.partitioner == other.partitioner, s"Partitioner mismatch between $self and $other")
    val zipped = this.zipPartitions(other, true) { (it1, it2) => leftOuterMerge(it1.buffered, it2.buffered).iterator }
    new SortedRDD(zipped)
  }

  def mapValues[U](f: V => U)(implicit ck: ClassTag[K], cv: ClassTag[V]): SortedRDD[K, U] = {
    val mapped = self.mapValues(x => f(x))
    new SortedRDD(mapped)
  }

  // This version takes a Key-Value tuple as argument.
  def mapValuesWithKeys[U](f: ((K, V)) => U): SortedRDD[K, U] = {
    val mapped = this.mapPartitions({ it =>
      it.map { case (k, v) => (k, f(k, v)) }
    }, preservesPartitioning = true)
    new SortedRDD(mapped)
  }

  override def filter(f: ((K, V)) => Boolean): SortedRDD[K, V] = {
    new SortedRDD(super.filter(f))
  }

  override def distinct: SortedRDD[K, V] = {
    val distinct = this.mapPartitions({ it =>
      val bi = it.buffered
      new Iterator[(K, V)] {
        def hasNext = bi.hasNext
        def next() = {
          val n = bi.next
          while (bi.hasNext && bi.head == n) bi.next
          n
        }
      }
    }, preservesPartitioning = true)
    new SortedRDD(distinct)
  }

  // `createCombiner`, which turns a V into a C (e.g., creates a one-element list)
  // `mergeValue`, to merge a V into a C (e.g., adds it to the end of a list)
  def combineByKey[C](createCombiner: V => C,
                      mergeValue: (C, V) => C): SortedRDD[K, C] = {
    val combined = this.mapPartitions({ it =>
      val bi = it.buffered
      new Iterator[(K, C)] {
        def hasNext = bi.hasNext
        def next() = {
          val n = bi.next
          var res = createCombiner(n._2)
          while (bi.hasNext && bi.head._1 == n._1) res = mergeValue(res, bi.next._2)
          n._1 -> res
        }
      }
    }, preservesPartitioning = true)
    new SortedRDD(combined)
  }

  def groupByKey(): SortedRDD[K, ArrayBuffer[V]] = {
    val createCombiner = (v: V) => ArrayBuffer(v)
    val mergeValue = (buf: ArrayBuffer[V], v: V) => buf += v
    combineByKey(createCombiner, mergeValue)
  }

  def collectFirstNValuesOrSo(n: Int)(implicit ct: ClassTag[V]): Seq[V] = {
    val numPartitions = partitions.size
    val div = n / numPartitions
    val mod = n % numPartitions
    this
      .mapPartitionsWithIndex((pid, it) => {
        val elementsFromThisPartition = if (pid < mod) (div + 1) else div
        it.take(elementsFromThisPartition)
      })
      .map { case (k, v) => v }
      .collect
  }
}

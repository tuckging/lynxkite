// Creates the right Bucketer for an attribute based on the AxisOptions.
package com.lynxanalytics.biggraph.controllers

import scala.reflect.runtime.universe._

import com.lynxanalytics.biggraph.graph_api._
import com.lynxanalytics.biggraph.graph_operations._
import com.lynxanalytics.biggraph.graph_util._
import com.lynxanalytics.biggraph.graph_api.Scripting._

object FEBucketers {
  def bucketedAttribute[T](
    metaManager: MetaGraphManager,
    dataManager: DataManager,
    attr: Attribute[T],
    numBuckets: Int,
    axisOptions: AxisOptions): BucketedAttribute[T] = {

    implicit val tt = attr.typeTag
    implicit val mm = metaManager
    implicit val dm = dataManager

    val bucketer = if (typeOf[T] =:= typeOf[String]) {
      val op = ComputeTopValues[String](numBuckets + 1, 10000)
      val topVals = op(op.attribute, attr.runtimeSafeCast[String]).result.topValues.value.map(_._1)
      val hasOther = topVals.size == numBuckets + 1
      (if (hasOther) StringBucketer(topVals.takeRight(numBuckets - 1), true)
      else StringBucketer(topVals, false))
        .asInstanceOf[Bucketer[T]]
    } else if (typeOf[T] =:= typeOf[Double]) {
      val stats = {
        val op = ComputeMinMaxDouble()
        op(op.attribute, attr.runtimeSafeCast[Double]).result
      }
      val min = if (axisOptions.logarithmic) stats.minPositive.value else stats.min.value
      val max = stats.max.value
      val actualNumBuckets = if (min == max) 1 else numBuckets
      if (axisOptions.logarithmic)
        DoubleLogBucketer(min, max, actualNumBuckets).asInstanceOf[Bucketer[T]]
      else
        DoubleLinearBucketer(min, max, actualNumBuckets).asInstanceOf[Bucketer[T]]
    } else ???

    BucketedAttribute(attr, bucketer)
  }
}

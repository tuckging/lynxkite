package com.lynxanalytics.biggraph.graph_operations

import org.apache.spark.SparkContext.rddToPairRDDFunctions

import com.lynxanalytics.biggraph.graph_api._

object ReverseEdges {
  class Input extends MagicInputSignature {
    val vsA = vertexSet
    val vsB = vertexSet
    val esAB = edgeBundle(vsA, vsB)
  }
  class Output(implicit instance: MetaGraphOperationInstance,
               inputs: Input) extends MagicOutput(instance) {
    val esBA = edgeBundle(inputs.vsB.entity, inputs.vsA.entity)
  }
}
import ReverseEdges._
case class ReverseEdges() extends TypedMetaGraphOp[Input, Output] {
  @transient override lazy val inputs = new Input()

  def outputMeta(instance: MetaGraphOperationInstance) = new Output()(instance, inputs)

  def execute(inputDatas: DataSet,
              o: Output,
              output: OutputBuilder,
              rc: RuntimeContext): Unit = {
    implicit val id = inputDatas
    val esBA = inputs.esAB.rdd.mapValues(e => Edge(e.dst, e.src))
    output(o.esBA, esBA)
  }
}

package com.lynxanalytics.biggraph.graph_operations

import org.scalatest.FunSuite

import com.lynxanalytics.biggraph.graph_api._
import com.lynxanalytics.biggraph.graph_api.Scripting._
import com.lynxanalytics.biggraph.graph_operations._

import com.lynxanalytics.biggraph.JavaScript

class ShortestPathTest extends FunSuite with TestGraphOp {

  test("big random graph") {
    val graph = CreateVertexSet(100)().result
    val vs = graph.vs
    val ordinal = graph.ordinal
    val es = {
      val op = FastRandomEdgeBundle(0, 6.0)
      op(op.vs, vs).result.es
    }
    val seed = {
      val op = DeriveJSDouble(
        JavaScript("ordinal < 3 ? 1000.0 : undefined"),
        Seq("ordinal"))
      op(
        op.attrs,
        VertexAttributeToJSValue.seq(ordinal)).result.attr
    }
    val weights = AddConstantAttribute.run(es.idSet, 1.0)
    val distance = {
      val op = ShortestPath(3)
      op(op.vs, vs)(op.es, es)(op.weights, weights)(op.seed, seed).result.distance
    }
    val tooFarAway = distance.rdd.filter { case (_, distance) => distance > 1003.0 }.count()
    val farAway = distance.rdd.filter { case (_, distance) => distance > 1002.0 }.count()
    assert(tooFarAway == 0, s"${tooFarAway} nodes are further than max iterations")
    assert(farAway > 0, s"${farAway} nodes are found at distance 3")
  }

  test("one-line graph") {
    val graph = SmallTestGraph(
      Map(
        0 -> Seq(1),
        1 -> Seq(2),
        2 -> Seq(3),
        3 -> Seq(4),
        4 -> Seq(5),
        5 -> Seq())).result
    val seed = {
      val op = AddDoubleVertexAttribute(Map(0 -> 100.0))
      op(op.vs, graph.vs).result.attr
    }
    val weights = AddConstantAttribute.run(graph.es.idSet, 2.0)
    val distance = {
      val op = ShortestPath(10)
      op(op.vs, graph.vs)(op.es, graph.es)(op.weights, weights)(op.seed, seed).result.distance
    }.rdd.collect
    assert(distance.toSeq.size == 6)
    assert(distance.toMap == Map(
      0 -> 100.0,
      1 -> 102.0,
      2 -> 104.0,
      3 -> 106.0,
      4 -> 108.0,
      5 -> 110.0))
  }

  test("graph with two paths with different weights") {
    val graph = SmallTestGraph(
      Map(
        0 -> Seq(1, 5),
        1 -> Seq(2),
        2 -> Seq(3),
        3 -> Seq(4),
        5 -> Seq(6),
        6 -> Seq(4))).result
    val seed = {
      val op = AddDoubleVertexAttribute(Map(0 -> 0.0))
      op(op.vs, graph.vs).result.attr
    }
    val weights = {
      val op = AddDoubleVertexAttribute(Map(
        0 -> 1.0, 1 -> 1.0, 2 -> 1.0, 3 -> 1.0, 5 -> 10.0, 6 -> 10.0))
      val vertexWeights = op(op.vs, graph.vs).result.attr
      VertexToEdgeAttribute.srcAttribute(vertexWeights, graph.es)
    }
    val distance = {
      val op = ShortestPath(10)
      op(op.vs, graph.vs)(op.es, graph.es)(op.weights, weights)(op.seed, seed).result.distance
    }.rdd.collect

    assert(distance.toMap.get(4).get == 4.0)
    assert(distance.toMap.get(6).get == 11.0)
  }

  test("example graph") {
    val graph = ExampleGraph()().result
    val vs = graph.vertices
    val es = graph.edges
    val name = graph.name
    val seed = {
      val op = DeriveJSDouble(
        JavaScript("name === 'Bob' ? 1000.0 : undefined"),
        Seq("name"))
      op(
        op.attrs,
        VertexAttributeToJSValue.seq(name)).result.attr
    }
    val distance = {
      val op = ShortestPath(10)
      op(op.vs, vs)(op.es, es)(op.weights, graph.weight)(op.seed, seed).result.distance
    }
    val nameAndDistance = name.rdd.join(distance.rdd).collect
    assert(nameAndDistance.size == 3)
    assert(nameAndDistance.toMap == Map(
      0 -> ("Adam", 1003.0),
      1 -> ("Eve", 1004.0),
      2 -> ("Bob", 1000.0)))
  }
}

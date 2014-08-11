package com.lynxanalytics.biggraph.graph_operations

import scala.util.{ Failure, Success, Try }
import com.lynxanalytics.biggraph.graph_api._
import com.lynxanalytics.biggraph.graph_util.Filename
import com.lynxanalytics.biggraph.spark_util.SortedRDD
import com.lynxanalytics.biggraph.spark_util.Implicits._
import com.lynxanalytics.biggraph.{ bigGraphLogger => log }
import org.apache.spark.rdd.RDD
import org.apache.spark.Partitioner
import org.apache.spark.SparkContext.rddToPairRDDFunctions
import org.apache.spark.SparkContext

// Functions for looking at CSV files. The frontend can use these when
// constructing the import operation.
object ImportUtil {
  def header(file: Filename): String =
    file.reader.readLine

  def fields(file: Filename, delimiter: String): Seq[String] =
    split(header(file), delimiter)

  private[graph_operations] def splitter(delimiter: String): String => Seq[String] = {
    val delim = java.util.regex.Pattern.quote(delimiter)
    def oneOf(options: String*) = options.mkString("|")
    def any(p: String) = capture(p) + "*"
    def capture(p: String) = "(" + p + ")"
    def oneField(p: String) = oneOf(p + delim, p + "$") // Delimiter or line end.
    val quote = "\""
    val nonQuote = "[^\"]"
    val doubleQuote = quote + quote
    val quotedString = capture(quote + any(oneOf(nonQuote, doubleQuote)) + quote)
    val anyString = capture(".*?")
    val r = oneOf(oneField(quotedString), oneField(anyString)).r
    val splitter = { line: String =>
      val matches = r.findAllMatchIn(line)
      // Find the top-level group that has matched in each field.
      val fields = matches.map(_.subgroups.find(_ != null).get).toList
      val l = fields.length
      // The last field may be a mistake. (Sorry, I couldn't write a better regex.)
      val fixed = if (l < 2 || fields(l - 2).endsWith(delimiter)) fields else fields.take(l - 1)
      // Remove quotes and unescape double-quotes in quoted fields.
      fixed.map { field =>
        if (field.startsWith(quote) && field.endsWith(quote)) {
          field.slice(1, field.length - 1).replace(doubleQuote, quote)
        } else field
      }
    }
    return splitter
  }

  private val splitters = collection.mutable.Map[String, String => Seq[String]]()

  // Splits a line by the delimiter. Delimiters inside quoted fields are ignored. (They become part
  // of the string.) Quotes inside quoted fields must be escaped by doubling them (" -> "").
  // TODO: Maybe we should use a CSV library.
  private[graph_operations] def split(line: String, delimiter: String): Seq[String] = {
    // Cache the regular expressions.
    if (!splitters.contains(delimiter)) {
      splitters(delimiter) = splitter(delimiter)
    }
    return splitters(delimiter)(line)
  }
}

case class Javascript(expression: String) {
  def isEmpty = expression.isEmpty
  def nonEmpty = expression.nonEmpty

  def isTrue(mapping: (String, String)*): Boolean = isTrue(mapping.toMap)
  def isTrue(mapping: Map[String, String]): Boolean = {
    if (isEmpty) {
      return true
    }
    val bindings = Javascript.engine.createBindings
    for ((key, value) <- mapping) {
      bindings.put(key, value)
    }
    return Try(Javascript.engine.eval(expression, bindings)) match {
      case Success(result: java.lang.Boolean) =>
        result
      case Success(result) =>
        throw Javascript.Error(s"JS expression ($expression) returned $result instead of a Boolean")
      case Failure(e) =>
        throw Javascript.Error(s"Could not evaluate JS: $expression", e)
    }
  }
}
object Javascript {
  val engine = new javax.script.ScriptEngineManager().getEngineByName("JavaScript")
  case class Error(msg: String, cause: Throwable = null) extends Exception(msg, cause)
}

case class CSV(file: Filename,
               delimiter: String,
               header: String,
               filter: Javascript = Javascript("")) {
  val fields = ImportUtil.split(header, delimiter)

  def lines(sc: SparkContext): RDD[Seq[String]] = {
    val lines = file.loadTextFile(sc)
    return lines
      .filter(_ != header)
      .map(ImportUtil.split(_, delimiter))
      .filter(jsFilter(_))
  }

  def jsFilter(line: Seq[String]): Boolean = {
    if (line.length != fields.length) {
      log.info(s"Input line cannot be parsed: $line")
      return false
    }
    return filter.isTrue(fields.zip(line).toMap)
  }
}

trait ImportCommon {
  type Columns = Map[String, SortedRDD[ID, String]]

  val csv: CSV

  protected def mustHaveField(field: String) = {
    assert(csv.fields.contains(field), s"No such field: $field in ${csv.fields}")
  }

  protected def readColumns(rc: RuntimeContext, csv: CSV): Columns = {
    val lines = csv.lines(rc.sparkContext)
    val numbered = lines.randomNumbered(rc.defaultPartitioner.numPartitions).cache
    return csv.fields.zipWithIndex.map {
      case (field, idx) => field -> numbered.mapValues(line => line(idx))
    }.toMap
  }
}
object ImportCommon {
  def toSymbol(field: String) = Symbol("csv_" + field)
}

object ImportVertexList {
  class Output(implicit instance: MetaGraphOperationInstance,
               fields: Seq[String]) extends MagicOutput(instance) {
    val vertices = vertexSet
    val attrs = fields.map {
      f => f -> vertexAttribute[String](vertices, ImportCommon.toSymbol(f))
    }.toMap
  }
}
case class ImportVertexList(csv: CSV) extends ImportCommon
    with TypedMetaGraphOp[NoInput, ImportVertexList.Output] {
  import ImportVertexList._
  @transient override lazy val inputs = new NoInput()
  def outputMeta(instance: MetaGraphOperationInstance) = new Output()(instance, csv.fields)
  override val isHeavy = true

  def execute(inputDatas: DataSet,
              o: Output,
              output: OutputBuilder,
              rc: RuntimeContext): Unit = {
    val columns = readColumns(rc, csv)
    for ((field, rdd) <- columns) {
      output(o.attrs(field), rdd)
    }
    output(o.vertices, columns.values.head.mapValues(_ => ()))
  }
}

trait ImportEdges extends ImportCommon {
  val src: String
  val dst: String
  mustHaveField(src)
  mustHaveField(dst)

  def putEdgeAttributes(columns: Columns,
                        oattr: Map[String, EntityContainer[EdgeAttribute[String]]],
                        output: OutputBuilder): Unit = {
    for ((field, rdd) <- columns) {
      output(oattr(field), rdd)
    }
  }

  def putEdgeBundle(columns: Columns,
                    srcToId: SortedRDD[String, ID],
                    dstToId: SortedRDD[String, ID],
                    oeb: EdgeBundle,
                    output: OutputBuilder,
                    partitioner: Partitioner): Unit = {
    val edgeSrcDst = columns(src).sortedJoin(columns(dst))
    val bySrc = edgeSrcDst.map {
      case (edgeId, (src, dst)) => src -> (edgeId, dst)
    }.toSortedRDD(partitioner)
    val byDst = bySrc.sortedJoin(srcToId).map {
      case (src, ((edgeId, dst), sid)) => dst -> (edgeId, sid)
    }.toSortedRDD(partitioner)
    val edges = byDst.sortedJoin(dstToId).map {
      case (dst, ((edgeId, sid), did)) => edgeId -> Edge(sid, did)
    }.toSortedRDD(partitioner)
    output(oeb, edges)
  }
}

object ImportEdgeList {
  class Output(implicit instance: MetaGraphOperationInstance,
               fields: Seq[String])
      extends MagicOutput(instance) {
    val (vertices, edges) = graph
    val attrs = fields.map {
      f => f -> edgeAttribute[String](edges, ImportCommon.toSymbol(f))
    }.toMap
    val stringID = vertexAttribute[String](vertices)
  }
}
case class ImportEdgeList(csv: CSV, src: String, dst: String)
    extends ImportEdges
    with TypedMetaGraphOp[NoInput, ImportEdgeList.Output] {
  import ImportEdgeList._
  @transient override lazy val inputs = new NoInput()
  def outputMeta(instance: MetaGraphOperationInstance) = new Output()(instance, csv.fields)

  def execute(inputDatas: DataSet,
              o: Output,
              output: OutputBuilder,
              rc: RuntimeContext): Unit = {
    val partitioner = rc.defaultPartitioner
    val columns = readColumns(rc, csv)
    putEdgeAttributes(columns, o.attrs, output)
    val names = (columns(src).values ++ columns(dst).values).distinct
    val idToName = names.randomNumbered(partitioner.numPartitions)
    val nameToId = idToName.map { case (id, name) => (name, id) }
      .toSortedRDD(partitioner)
    putEdgeBundle(columns, nameToId, nameToId, o.edges, output, partitioner)
    output(o.vertices, idToName.mapValues(_ => ()))
    output(o.stringID, idToName)
  }

  override val isHeavy = true
}

object ImportEdgeListForExistingVertexSet {
  class Input extends MagicInputSignature {
    val sources = vertexSet
    val destinations = vertexSet
    val srcVidAttr = vertexAttribute[String](sources)
    val dstVidAttr = vertexAttribute[String](destinations)
  }
  class Output(implicit instance: MetaGraphOperationInstance,
               inputs: Input,
               fields: Seq[String])
      extends MagicOutput(instance) {
    val edges = edgeBundle(inputs.sources.entity, inputs.destinations.entity)
    val attrs = fields.map {
      f => f -> edgeAttribute[String](edges, ImportCommon.toSymbol(f))
    }.toMap
  }

  def checkIdMapping(rdd: RDD[(String, ID)], partitioner: Partitioner): SortedRDD[String, ID] =
    rdd.groupBySortedKey(partitioner)
      .mapValues { id =>
        assert(id.size == 1, "VertexId mapping is ambiguous, check supplied VertexAttributes")
        id.head
      }
}
case class ImportEdgeListForExistingVertexSet(csv: CSV, src: String, dst: String)
    extends ImportEdges
    with TypedMetaGraphOp[ImportEdgeListForExistingVertexSet.Input, ImportEdgeListForExistingVertexSet.Output] {
  import ImportEdgeListForExistingVertexSet._
  @transient override lazy val inputs = new Input()
  def outputMeta(instance: MetaGraphOperationInstance) = new Output()(instance, inputs, csv.fields)

  def execute(inputDatas: DataSet,
              o: Output,
              output: OutputBuilder,
              rc: RuntimeContext): Unit = {
    implicit val id = inputDatas
    val partitioner = rc.defaultPartitioner
    val columns = readColumns(rc, csv)
    putEdgeAttributes(columns, o.attrs, output)
    val srcToId = checkIdMapping(inputs.srcVidAttr.rdd.map { case (k, v) => v -> k }, partitioner)
    val dstToId = {
      if (inputs.srcVidAttr.data.gUID == inputs.dstVidAttr.data.gUID)
        srcToId
      else
        checkIdMapping(inputs.dstVidAttr.rdd.map { case (k, v) => v -> k }, partitioner)
    }
    putEdgeBundle(columns, srcToId, dstToId, o.edges, output, partitioner)
  }

  override val isHeavy = true
}

// Kryo registration and the creation of the SparkContext.
package com.lynxanalytics.biggraph.spark_util

import com.esotericsoftware.kryo.Kryo
import com.google.cloud.hadoop.fs.gcs
import com.lynxanalytics.biggraph.controllers.LogController
import com.lynxanalytics.biggraph.graph_util.LoggedEnvironment
import com.lynxanalytics.biggraph.graph_util.KiteInstanceInfo
import org.apache.spark
import org.apache.spark.serializer.KryoRegistrator
import scala.collection.mutable
import scala.reflect.ClassTag

import com.lynxanalytics.biggraph.{ bigGraphLogger => log }
import com.lynxanalytics.biggraph.graph_api
import com.lynxanalytics.biggraph.graph_operations
import com.lynxanalytics.biggraph.spark_util

// Placeholders for deleted classes.
class DeadClass1
class DeadClass2
class DeadClass3
class DeadClass4
class DeadClass5
class DeadClass6
class DeadClass7

class BigGraphKryoRegistrator extends KryoRegistrator {
  override def registerClasses(kryo: Kryo) {
    // Uncomment this if you are debugging some Kryo issue.
    // import com.esotericsoftware.minlog.Log
    // Log.set(Log.LEVEL_TRACE);

    // Adding one more line? Do it at the bottom!
    // Deleting a line? Do not.
    // Types will change IDs otherwise.
    kryo.register(classOf[scala.Tuple2[_, _]])
    kryo.register(classOf[Array[Any]])
    kryo.register(classOf[mutable.WrappedArray$ofRef])
    kryo.register(classOf[mutable.ArrayBuffer[_]])
    kryo.register(classOf[Array[mutable.ArrayBuffer[_]]])
    kryo.register(classOf[Array[Int]])
    kryo.register(classOf[Array[Long]])
    kryo.register(classOf[Array[Double]])
    kryo.register(classOf[Array[Tuple2[_, _]]])
    kryo.register(classOf[Array[Tuple3[_, _, _]]])
    kryo.register(classOf[Array[String]])
    kryo.register(classOf[scala.runtime.BoxedUnit])
    kryo.register(classOf[graph_api.CompactUndirectedGraph])
    kryo.register(classOf[::[_]])
    kryo.register(Nil.getClass)
    kryo.register(None.getClass)
    kryo.register(Set.empty[Int].getClass)
    kryo.register(classOf[mutable.ArrayBuffer[Any]])
    kryo.register(classOf[graph_api.Edge])
    kryo.register(classOf[Array[Seq[_]]])
    kryo.register(classOf[Array[graph_api.Edge]])
    kryo.register((0L, 0.0).getClass)
    kryo.register(Class.forName("org.apache.spark.util.BoundedPriorityQueue")) // SPARK-2306
    kryo.register(classOf[graph_operations.ComputeTopValues.PairOrdering[_]])
    kryo.register(classOf[collection.immutable.Range])
    kryo.register(classOf[DeadClass1])
    kryo.register(classOf[Array[DeadClass1]])
    kryo.register(classOf[mutable.WrappedArray$ofInt])
    kryo.register(('x', 'x').getClass)
    kryo.register(classOf[collection.mutable.Map[_, _]])
    kryo.register(classOf[scala.Tuple2[Double, Double]])
    kryo.register(classOf[Array[Tuple2[Long, Int]]])
    kryo.register(classOf[Option[_]])
    kryo.register(classOf[Array[Option[_]]])
    kryo.register(classOf[Vector[_]])
    kryo.register(classOf[graph_operations.DynamicValue])
    kryo.register(classOf[Array[graph_operations.DynamicValue]])
    kryo.register(ClassTag(Class.forName("org.apache.spark.util.collection.CompactBuffer")).wrap.runtimeClass)
    kryo.register(classOf[collection.mutable.Map$WithDefault])
    kryo.register(classOf[collection.mutable.Map$$anonfun$withDefaultValue$1])
    kryo.register(classOf[spark_util.IDBuckets[_]])
    kryo.register(classOf[graph_operations.Stats])
    kryo.register(classOf[Array[graph_operations.Stats]])
    kryo.register((0L, 0).getClass)
    kryo.register(classOf[Array[org.apache.spark.mllib.linalg.Vector]])
    kryo.register(classOf[org.apache.spark.mllib.linalg.DenseVector])
    kryo.register(breeze.linalg.DenseVector(Array[Double](0)).getClass)
    kryo.register(classOf[DeadClass2])
    kryo.register(classOf[DeadClass3])
    kryo.register((0.0, 0.0).getClass)
    kryo.register(math.Numeric.LongIsIntegral.getClass) // For using NumericRanges with sc.parallelize.
    kryo.register(classOf[DeadClass5])
    kryo.register(classOf[DeadClass6])
    kryo.register(classOf[DeadClass7])
    // The next three are required by some operations after the Spark 1.3.0 upgrade. (SPARK-6497)
    kryo.register(classOf[scala.reflect.ManifestFactory$$anon$10])
    kryo.register(classOf[scala.reflect.ClassTag$$anon$1])
    kryo.register(classOf[Class[_]])
    // === #1518 / SPARK-5949 ===
    kryo.register(classOf[org.roaringbitmap.RoaringBitmap])
    kryo.register(classOf[org.roaringbitmap.RoaringArray])
    kryo.register(classOf[DeadClass4])
    kryo.register(classOf[Array[DeadClass4]])
    kryo.register(classOf[org.roaringbitmap.BitmapContainer])
    kryo.register(classOf[org.roaringbitmap.ArrayContainer])
    kryo.register(classOf[Array[Short]])
    // ==========================
    kryo.register(classOf[Array[Array[Long]]]) // #1612
    kryo.register(classOf[com.lynxanalytics.biggraph.spark_util.CountOrdering[_]])
    kryo.register(classOf[com.lynxanalytics.biggraph.graph_util.HadoopFile])
    // More classes for SPARK-6497.
    kryo.register(classOf[scala.reflect.ManifestFactory$$anon$8])
    kryo.register(classOf[scala.reflect.ManifestFactory$$anon$9])
    kryo.register(classOf[scala.reflect.ManifestFactory$$anon$12])

    kryo.register(classOf[org.apache.spark.sql.types.StructType])
    kryo.register(classOf[org.apache.spark.sql.types.StructField])
    kryo.register(classOf[Array[org.apache.spark.sql.types.StructField]])
    kryo.register(classOf[org.apache.spark.sql.types.Metadata])
    kryo.register(org.apache.spark.sql.types.BinaryType.getClass)
    kryo.register(org.apache.spark.sql.types.ByteType.getClass)
    kryo.register(org.apache.spark.sql.types.DecimalType.getClass)
    kryo.register(org.apache.spark.sql.types.DoubleType.getClass)
    kryo.register(org.apache.spark.sql.types.FloatType.getClass)
    kryo.register(org.apache.spark.sql.types.IntegerType.getClass)
    kryo.register(org.apache.spark.sql.types.LongType.getClass)
    kryo.register(org.apache.spark.sql.types.MapType.getClass)
    kryo.register(org.apache.spark.sql.types.NullType.getClass)
    kryo.register(org.apache.spark.sql.types.ShortType.getClass)
    kryo.register(org.apache.spark.sql.types.StringType.getClass)
    kryo.register(org.apache.spark.sql.types.TimestampType.getClass)
    kryo.register(scala.collection.immutable.Map().getClass)
    kryo.register(classOf[Array[org.apache.spark.sql.catalyst.InternalRow]])
    kryo.register(classOf[org.apache.spark.sql.catalyst.expressions.GenericInternalRow])
    kryo.register(classOf[org.apache.spark.unsafe.types.UTF8String])
    kryo.register(classOf[Array[scala.collection.immutable.Map[_, _]]])
    kryo.register(classOf[collection.immutable.Set[_]])
    // Scala uses different actual classes for immutable sets up to size 5. Yay!
    kryo.register(Set().getClass)
    kryo.register(Set(1).getClass)
    kryo.register(Set(1, 2).getClass)
    kryo.register(Set(1, 2, 3).getClass)
    kryo.register(Set(1, 2, 3, 4).getClass)
    kryo.register(Set(1, 2, 3, 4, 5).getClass)
    kryo.register(classOf[org.apache.hadoop.io.BytesWritable])
    kryo.register(classOf[org.apache.spark.mllib.stat.MultivariateOnlineSummarizer])
    kryo.register(classOf[org.apache.spark.mllib.classification.NaiveBayesModel])
    kryo.register(classOf[Array[Array[Double]]])
    kryo.register(classOf[org.apache.spark.mllib.linalg.DenseMatrix])
    kryo.register(classOf[org.apache.spark.mllib.regression.LabeledPoint])
    kryo.register(classOf[Array[org.apache.spark.mllib.regression.LabeledPoint]])
    kryo.register(Class.forName("org.apache.spark.mllib.tree.impl.DTStatsAggregator"))
    kryo.register(Class.forName("org.apache.spark.mllib.tree.impurity.VarianceAggregator"))
    kryo.register(Class.forName("org.apache.spark.mllib.tree.impl.DecisionTreeMetadata"))
    kryo.register(org.apache.spark.mllib.tree.impurity.Variance.getClass)
    kryo.register(classOf[Enumeration$Val])
    kryo.register(org.apache.spark.mllib.tree.configuration.QuantileStrategy.getClass)
    kryo.register(classOf[org.apache.spark.mllib.tree.model.Split])
    kryo.register(org.apache.spark.mllib.tree.configuration.FeatureType.getClass)
    kryo.register(classOf[org.apache.spark.mllib.tree.model.InformationGainStats])
    kryo.register(classOf[org.apache.spark.mllib.tree.model.Predict])

    kryo.register(classOf[Array[collection.immutable.HashSet[_]]])
    kryo.register(classOf[collection.immutable.HashSet$HashSet1])

    kryo.register(classOf[Array[org.apache.spark.sql.Row]])
    kryo.register(classOf[org.apache.spark.sql.catalyst.expressions.GenericRowWithSchema])
    kryo.register(classOf[graph_operations.SegmentByEventSequence.EventListSegmentId])
    kryo.register(classOf[org.apache.spark.sql.catalyst.expressions.UnsafeRow])
    kryo.register(classOf[Array[org.apache.spark.mllib.tree.model.Split]])
    kryo.register(Class.forName("org.apache.spark.mllib.tree.model.Bin"))
    kryo.register(Class.forName("[Lorg.apache.spark.mllib.tree.model.Bin;"))
    kryo.register(Class.forName("org.apache.spark.mllib.tree.model.DummyLowSplit"))
    kryo.register(Class.forName("org.apache.spark.mllib.tree.model.DummyHighSplit"))

    kryo.register(Class.forName("[Lorg.apache.spark.mllib.regression.impl.GLMRegressionModel$SaveLoadV1_0$Data;"))
    kryo.register(Class.forName("org.apache.spark.mllib.regression.impl.GLMRegressionModel$SaveLoadV1_0$Data"))
    kryo.register(Class.forName("[Lorg.apache.spark.sql.types.StructType;"))
    kryo.register(Class.forName("org.apache.spark.mllib.linalg.VectorUDT"))
    kryo.register(Class.forName("org.apache.spark.sql.catalyst.util.GenericArrayData"))
    kryo.register(classOf[com.clearspring.analytics.stream.cardinality.HyperLogLogPlus])
    kryo.register(classOf[com.clearspring.analytics.stream.cardinality.RegisterSet])
    kryo.register(Class.forName("com.clearspring.analytics.stream.cardinality.HyperLogLogPlus$Format"))
    kryo.register(classOf[Array[org.apache.spark.sql.types.DataType]])
    kryo.register(classOf[java.sql.Timestamp])
    // Add new stuff just above this line! Thanks.
    // Adding Foo$mcXXX$sp? It is a type specialization. Register the decoded type instead!
    // Z = Boolean, B = Byte, C = Char, D = Double, F = Float, I = Int, J = Long, S = Short.
  }
}

class BigGraphKryoForcedRegistrator extends BigGraphKryoRegistrator {
  override def registerClasses(kryo: Kryo) {
    kryo.setRegistrationRequired(true)
    super.registerClasses(kryo)
  }
}

object BigGraphSparkContext {
  def createKryoWithForcedRegistration(): Kryo = {
    val myKryo = new Kryo()
    myKryo.setInstantiatorStrategy(new org.objenesis.strategy.StdInstantiatorStrategy());
    new BigGraphKryoForcedRegistrator().registerClasses(myKryo)
    myKryo
  }
  def setupMonitoring(conf: spark.SparkConf): spark.SparkConf = {
    val graphiteHostName = LoggedEnvironment.envOrElse("GRAPHITE_MONITORING_HOST", "")
    val graphitePort = LoggedEnvironment.envOrElse("GRAPHITE_MONITORING_PORT", "")
    if (graphiteHostName == "" || graphitePort == "") {
      conf
    } else {
      // Set the keys normally defined in metrics.properties here.
      // This way it's easier to make sure that executors receive the
      // settings.
      conf
        .set("spark.metrics.conf.*.sink.graphite.class", "org.apache.spark.metrics.sink.GraphiteSink")
        .set("spark.metrics.conf.*.sink.graphite.host", graphiteHostName)
        .set("spark.metrics.conf.*.sink.graphite.port", graphitePort)
        .set("spark.metrics.conf.*.sink.graphite.period", "1")
        .set("spark.metrics.conf.*.sink.graphite.unit", "seconds")
        .set("spark.metrics.conf.master.source.jvm.class", "org.apache.spark.metrics.source.JvmSource")
        .set("spark.metrics.conf.worker.source.jvm.class", "org.apache.spark.metrics.source.JvmSource")
        .set("spark.metrics.conf.driver.source.jvm.class", "org.apache.spark.metrics.source.JvmSource")
        .set("spark.metrics.conf.executor.source.jvm.class", "org.apache.spark.metrics.source.JvmSource")
    }
  }

  def apply(
    appName: String,
    useKryo: Boolean = true,
    forceRegistration: Boolean = false,
    master: String = ""): spark.SparkContext = {
    val versionFound = KiteInstanceInfo.sparkVersion
    val versionRequired = scala.io.Source.fromURL(getClass.getResource("/SPARK_VERSION")).mkString.trim
    assert(versionFound == versionRequired,
      s"Needs Apache Spark version $versionRequired. Found $versionFound.")

    // Don't forget to review spark.memory.useLegacyMode before upgrading Spark to 2.x
    // Without that setting, Spark 1.6.0 slows down when the cache is full. Other flags can
    // also fix that issue:
    //   spark.memory.fraction 0.66
    // Or:
    //   spark.executor.extraJavaOptions -XX:NewRatio=3
    assert(versionFound.startsWith("1."), "Spark 2.0 is not yet supported.")

    var sparkConf = new spark.SparkConf()
      .setAppName(appName)
      .set("spark.memory.useLegacyMode", "true")
      .set("spark.io.compression.codec", "lz4")
      .set("spark.rdd.compress", "true")
      .set("spark.locality.wait", "2m")
      .set("spark.eventLog.enabled", "true")
      .set("spark.executor.memory",
        LoggedEnvironment.envOrElse("EXECUTOR_MEMORY", "1700m"))
      .set("spark.akka.threads",
        LoggedEnvironment.envOrElse("AKKA_THREADS", "4")) // set it to number of cores on master
      .set("spark.local.dir", LoggedEnvironment.envOrElse("KITE_LOCAL_TMP", "/tmp"))
      // Speculative execution will start extra copies of tasks to eliminate long tail latency.
      .set("spark.speculation", "false") // Speculative execution is disabled, see #1907.
      .set("spark.speculation.interval", "1000") // (Milliseconds.) How often to check.
      .set("spark.speculation.quantile", "0.90") // (Fraction.) This much of the stage has to complete first.
      .set("spark.speculation.multiplier", "2") // (Ratio.) Task has to be this much slower than the median.
      .set(
        // Enables fair scheduling, that is tasks of all running jobs are scheduled round-robin
        // instead of one job finishes completely first. See:
        // http://spark.apache.org/docs/latest/job-scheduling.html
        "spark.scheduler.mode",
        "FAIR")
      .set("spark.core.connection.ack.wait.timeout", "240")
      // Combines shuffle output into a single file which improves shuffle performance and reduces
      // number of open files for jobs with many reduce tasks. It only has some bad side effects
      // on ext3 with >8 cores, so I think we can enable this for our usecases.
      .set("spark.shuffle.consolidateFiles", "true")
      .set(
        "spark.executor.cores",
        LoggedEnvironment.envOrElse("NUM_CORES_PER_EXECUTOR", "4"))
      // We need a higher akka.frameSize (the Spark default is 10) as when the number of
      // partitions gets into the hundreds of thousands the map output statuses exceed this limit.
      .setIfMissing(
        "spark.akka.frameSize", "1000")
      .set("spark.sql.runSQLOnFiles", "false")
      // Configure Spark event logging:
      .set(
        "spark.eventLog.dir",
        "file://" + LogController.getLogDir.getAbsolutePath)
      .set("spark.eventLog.enabled", "true")
      .set("spark.eventLog.compress", "true")
    sparkConf = setupMonitoring(sparkConf)
    if (useKryo) {
      sparkConf = sparkConf
        .set(
          "spark.serializer",
          "org.apache.spark.serializer.KryoSerializer")
        .set(
          "spark.kryo.registrator",
          if (forceRegistration)
            "com.lynxanalytics.biggraph.spark_util.BigGraphKryoForcedRegistrator"
          else "com.lynxanalytics.biggraph.spark_util.BigGraphKryoRegistrator")
    }
    if (master != "") {
      sparkConf = sparkConf.setMaster(master)
    }
    log.info("Creating Spark Context with configuration: " + sparkConf.toDebugString)
    val sc = new spark.SparkContext(sparkConf)
    sc.addSparkListener(new BigGraphSparkListener(sc))
    sc
  }
}

class BigGraphSparkListener(sc: spark.SparkContext) extends spark.scheduler.SparkListener {
  val maxStageFailures = LoggedEnvironment.envOrElse("KITE_STAGE_MAX_FAILURES", "4").toInt
  val stageFailures = collection.mutable.Map[Int, Int]()

  override def onStageCompleted(
    stageCompleted: spark.scheduler.SparkListenerStageCompleted): Unit = synchronized {
    val stage = stageCompleted.stageInfo
    if (stage.failureReason.nonEmpty) {
      stageFailures(stage.stageId) = stageFailures.getOrElse(stage.stageId, 0) + 1
    }
  }

  override def onStageSubmitted(
    stageSubmitted: spark.scheduler.SparkListenerStageSubmitted): Unit = synchronized {
    val stage = stageSubmitted.stageInfo
    val failures = stageFailures.getOrElse(stage.stageId, 0)
    if (failures >= maxStageFailures) {
      log.warn(s"Stage ${stage.stageId} has failed $failures times." +
        " Cancelling all jobs to prevent infinite retries. (#2001)")
      sc.cancelAllJobs()
    }
  }
}

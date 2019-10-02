package com.epam.bigdata.training.spark.streaming

import java.sql.Timestamp

import org.apache.spark.sql.streaming.OutputMode
import org.apache.spark.sql.{DataFrame, SparkSession}

/**
  * Entry point to the streaming application.
  */
object StreamingApplication {

  private val usage = "Streaming application expects the following parameters to be passed: \n" +
    " - Kafka Bootstrap Servers - e.g. 172.19.0.2:6667, \n" +
    " - Kafka Topic to read data from - e.g. kafka-topic-in, \n" +
    " - Kafka Topic to write data into - e.g. kafka-topic-out, \n" +
    " - Checkpointing Location - e.g. hdfs://namenode:8020/admin/spark/hashtags-counts. \n" +
    "So, the application can be submitted like this: \n" +
    "spark/bin/spark-submit \\" +
    " --master=spark://spark-master:7077 \\" +
    " --class com.epam.bigdata.training.kafka.hdfs.spark.connector.ConnectorApplication \\" +
    " --conf spark.default.parallelism=8 \\" +
    " --conf spark.driver.memory=1G \\" +
    " --conf spark.executor.memory=2G \\" +
    " --conf spark.serializer=org.apache.spark.serializer.KryoSerializer \\" +
    " --conf spark.memory.fraction=0.7 \\" +
    " --conf spark.memory.offHeap.enabled=true \\" +
    " --conf spark.memory.offHeap.size=1G \\" +
    " --conf spark.sql.shuffle.partitions=8 \\" +
    " --packages org.apache.spark:spark-sql-kafka-0-10_2.11:2.4.0,com.sksamuel.avro4s:avro4s-core_2.11:2.0.2 \\" +
    " kafka-to-hdfs-spark-connector.jar \\" +
    " kafka-1:9091,kafka-2:9092,kafka-3:9093 \\" +
    " big-data-tweets \\" +
    " big-data-hashtags-count \\" +
    " hdfs://namenode:8020/admin/spark/hashtags-counts"

  def main(args: Array[String]): Unit = {

    if (args.length < 4)
      throw new IllegalArgumentException(usage)

    val kafkaServers = args(0)
    val inTopic = args(1)
    val outTopic = args(2)
    val checkpointLocation = args(3)

    val spark = SparkSession
      .builder
      .appName("Tweets spark streaming application")
      .config("spark.sql.streaming.checkpointLocation", checkpointLocation)
      .getOrCreate()

    // subscribe to kafka topic
    val df = readKafkaDataFrame(spark, kafkaServers, inTopic)

    // count hashtags by time window
    val ds = countByTimeWindow(spark, df)

    // finally, output data to output kafka topic
    outputRecords(spark, ds, kafkaServers, outTopic)
  }

  /**
    * Connects to the specified kafka brokers and subsribes to the specified kafka topic.
    * @param spark        Spark session
    * @param kafkaServers Kafka bootstrap servers
    * @param kafkaTopic   Kafka topic to read the data from.
    * @return
    */
  def readKafkaDataFrame(spark: SparkSession, kafkaServers: String, kafkaTopic: String): DataFrame = {
    spark
      .readStream
      .format("kafka")
      .option("kafka.bootstrap.servers", kafkaServers)
      .option("subscribe", kafkaTopic)
      .load()
  }

  /**
    * Watermark by dt filed for 1.5 hours to support late records and
    * group by 1-hour window.
    *
    * @param spark  Spark session.
    * @param df     Incoming untyped data frame to transform.
    * @return
    */
  def countByTimeWindow(spark: SparkSession, df: DataFrame): DataFrame = {
    import org.apache.spark.sql.functions._
    import spark.implicits._

    df
      .select($"key".cast("string"), $"value")
      .as[(String, Array[Byte])]
      .map{ case (k, v) => (k, AvroUtils.readKafkaSerializedTweet(v)) }
      .flatMap{ case (user, tweet) => tweet.hashtags.map(hashtag => (new Timestamp(tweet.dt), hashtag))}
      .toDF()
      .select($"_1" as "dt", $"_2" as "hashtag")
      .withWatermark("dt", "90 minutes")
      .groupBy(
        window($"dt", "1 hour"),
        $"hashtag"
      )
      .count()
  }

  /**
    * Opens write stream to kafka.
    *
    * @param spark    Spark session
    * @param df       Dataframe to write
    * @param servers  Kafka bootstrap servers
    * @param topic    Kafka topic
    */
  def outputRecords(spark: SparkSession, df: DataFrame, servers: String, topic: String): Unit = {
    import org.apache.spark.sql.functions.{struct, to_json}
    import spark.implicits._

    df
      .withColumn("value", to_json(struct($"hashtag", $"window", $"count")))
      .select($"hashtag" as "key", $"value")
      .writeStream
      .format("kafka")
      .option("kafka.bootstrap.servers", servers)
      .option("topic", topic)
      .outputMode(OutputMode.Update())
      .start()
      .awaitTermination()
  }

}

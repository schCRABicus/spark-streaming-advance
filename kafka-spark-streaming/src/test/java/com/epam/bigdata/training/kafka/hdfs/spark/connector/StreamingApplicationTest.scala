package com.epam.bigdata.training.kafka.hdfs.spark.connector

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.sql.Timestamp
import java.time._

import com.epam.bigdata.training.spark.streaming.{StreamingApplication, Tweet}
import com.github.mrpowers.spark.fast.tests.DatasetComparer
import com.sksamuel.avro4s.AvroSchema
import org.apache.avro.io.{BinaryEncoder, EncoderFactory}
import org.apache.avro.reflect.{ReflectData, ReflectDatumWriter}
import org.apache.spark.sql.SparkSession
import org.scalatest.{FeatureSpec, GivenWhenThen, Matchers}

import scala.util.Random

class StreamingApplicationTest extends FeatureSpec with GivenWhenThen with DatasetComparer with Matchers {

  lazy val spark: SparkSession = {
    SparkSession.builder().master("local").appName("spark session").getOrCreate()
  }

  feature("Hashtags counting across time windows") {
    scenario("Correctly counts hashtags across different partitions") {
      import spark.implicits._

      Given("Dataframe with tweets")
      val df = Seq(
        ("user-A", toAvro(Tweet(dateAt(2019, 2, 26, 14), 1, "text-A", Array("tag-1", "tag-2")))),
        ("user-A", toAvro(Tweet(dateAt(2019, 2, 26, 13), 2, "text-B", Array("tag-1", "tag-2")))),
        ("user-A", toAvro(Tweet(dateAt(2019, 2, 26, 14), 3, "text-C", Array("tag-1", "tag-2")))),
        ("user-A", toAvro(Tweet(dateAt(2019, 2, 26, 13), 4, "text-D", Array("tag-1", "tag-3"))))
      ).toDF("key", "value")

      When("calculating counts")
      val resultDF = StreamingApplication.countByTimeWindow(spark, df).toDF("time window", "hashtag", "count")

      Then("assert equals the expected one")
      val expected = Seq(
        (tsAt(2019, 2, 26, 14, 0), tsAt(2019, 2, 26, 15, 0), "tag-1", 2),
        (tsAt(2019, 2, 26, 14, 0), tsAt(2019, 2, 26, 15, 0), "tag-2", 2),
        (tsAt(2019, 2, 26, 13, 0), tsAt(2019, 2, 26, 14, 0), "tag-1", 2),
        (tsAt(2019, 2, 26, 13, 0), tsAt(2019, 2, 26, 14, 0), "tag-2", 1),
        (tsAt(2019, 2, 26, 13, 0), tsAt(2019, 2, 26, 14, 0), "tag-3", 1)
      )

      val collectedResult = resultDF
          .select($"time window.start".cast("timestamp"), $"time window.end".cast("timestamp"), $"hashtag".cast("string"), $"count".cast("int"))
          .as[(Timestamp, Timestamp, String, Int)]
          .collect()

      collectedResult should contain theSameElementsAs expected
    }
  }

  private def dateAt(year: Int, month: Int, day: Int, hour: Int) = {
    LocalDate.of(year, month, day).atTime(hour, Random.nextInt(59)).toInstant(ZoneOffset.ofHours(0)).toEpochMilli
  }

  private def tsAt(year: Int, month: Int, day: Int, hour: Int, minute: Int) = {
    new Timestamp(
      LocalDate.of(year, month, day).atTime(hour, minute).toInstant(ZoneOffset.ofHours(0)).toEpochMilli
    )
  }

  private def toAvro(tweet: Tweet): Array[Byte] = {

    val out = new ByteArrayOutputStream
    out.write(0)
    out.write(ByteBuffer.allocate(4).putInt(1).array) // write schema version

    val encoder = EncoderFactory.get.directBinaryEncoder(out, null.asInstanceOf[BinaryEncoder])
    val writer = ReflectData.get().createDatumWriter(AvroSchema[Tweet]).asInstanceOf[ReflectDatumWriter[Tweet]]

    writer.write(tweet, encoder)
    encoder.flush()

    val bytes = out.toByteArray
    out.close()

    bytes
  }

}

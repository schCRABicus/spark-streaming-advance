package com.epam.bigdata.training.spark.streaming

import java.nio.ByteBuffer

import com.sksamuel.avro4s.AvroSchema
import org.apache.avro.generic.{GenericData, GenericDatumReader, GenericRecord}
import org.apache.avro.io.DecoderFactory

/**
  * Avro SerDe Utils.
  *
  * Out-of-the-box org.apache.spark.sql.avro.from_avro() method, introduced since spark 2.4.0
  * in library "org.apache.spark" %% "spark-avro" is not compatible with avro message
  * format serialized by AbstractKafkaAvroSerializer since kafka prepends version id and magic byte
  * in front of message.
  *
  * That's why the solution like
  * <code>
  *   <pre>
  *    import org.apache.spark.sql.avro._
  *    df
  *      .select($"key".cast("string"), from_avro($"value", schema))
  *      .as[(String, Tweet)]
  *   </pre>
  * </code>
  * fails to deserialize the message because of extra bytes.
  **/
object AvroUtils {

  /**
    * Messsages serizlied with kafka avro include magic byte and schema version id
    * in front of message. That being said, the byte array buffer must be shifted 5 bytes ahead to
    * consume serizlied message.
    *
    * See {@link io.confluent.kafka.serializers.AbstractKafkaAvroSerializer#serializeImpl()}
    * <code>
    *   <pre>
    *       ByteArrayOutputStream out = new ByteArrayOutputStream();
    *       out.write(MAGIC_BYTE);
    *       out.write(ByteBuffer.allocate(idSize).putInt(id).array());
    *   </pre>
    * </code>
    *
    * @param bytes Incoming byte array.
    * @return
    */
  def readKafkaSerializedTweet(bytes: Array[Byte]): Tweet = {
    try {
      val buffer = ByteBuffer.wrap(bytes)
      if (buffer.get != 0x0) throw new RuntimeException("Unknown magic byte!")
      val id = buffer.getInt()
      val idSize = 4

      val length = buffer.limit() - 1 - idSize
      val start = buffer.position() + buffer.arrayOffset()
      val reader = new GenericDatumReader[GenericRecord](AvroSchema[Tweet])

      val record = reader.read(null, DecoderFactory.get.binaryDecoder(buffer.array(), start, length, null))

      Tweet(
        record.get("dt").asInstanceOf[Long],
        record.get("id").asInstanceOf[Long],
        record.get("text").asInstanceOf[org.apache.avro.util.Utf8].toString,
        transformAvroArrayToObjectArray(record.get("hashtags").asInstanceOf[GenericData.Array[org.apache.avro.util.Utf8]])
      )

    } catch {
      case any : Throwable => {
        println(any)
        Tweet(-1, -1, any.getMessage, Array())
      }
    }
  }

  private def transformAvroArrayToObjectArray(arr: GenericData.Array[org.apache.avro.util.Utf8]): Array[String] = {
    if (arr == null) return new Array[String](0)
    val ret = new Array[String](arr.size)
    val iterator = arr.iterator
    var i = 0
    while ( {
      iterator.hasNext
    }) {
      val value = iterator.next.toString
      ret({
        i += 1; i - 1
      }) = value
    }
    ret
  }
}

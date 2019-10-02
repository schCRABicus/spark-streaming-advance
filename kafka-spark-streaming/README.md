# Application description

The goal of the application is to consumer avro-serialized tweets from kafka,
group them by 1-hour time window, and calculate counts for each tag. 
Results are then written to a different kafka topic in update output mode.

# Launch instructions

First of all, the application jar has to be copied to spark-master node from where it will be submitted later:

```
docker cp target/scala-2.11/kafka-spark-streaming_2.11-0.1.jar spark-master:kafka-spark-streaming.jar
```

Once the jar is copied, the spark job can be submitted the following way from the spark-master node:
```
spark/bin/spark-submit \
--master=spark://spark-master:7077 \
--class com.epam.bigdata.training.spark.streaming.StreamingApplication \
--conf spark.default.parallelism=8 \
--conf spark.driver.memory=1G \
--conf spark.executor.memory=2G \
--conf spark.serializer=org.apache.spark.serializer.KryoSerializer \
--conf spark.memory.fraction=0.7 \
--conf spark.memory.offHeap.enabled=true \
--conf spark.memory.offHeap.size=1G \
--conf spark.sql.shuffle.partitions=8 \
--packages org.apache.spark:spark-sql-kafka-0-10_2.11:2.4.0,com.sksamuel.avro4s:avro4s-core_2.11:2.0.2 \
kafka-spark-streaming.jar \
kafka-1:9091,kafka-2:9092,kafka-3:9093 \
    big-data-tweets \
    big-data-hashtags-count \
    hdfs://namenode:8020/admin/spark/hashtags-counts
```


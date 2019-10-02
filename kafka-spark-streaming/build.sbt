name := "kafka-spark-streaming"

version := "0.1"

scalaVersion := "2.11.12"

resolvers += "Spark Packages Repo" at "http://dl.bintray.com/spark-packages/maven"

// https://mvnrepository.com/artifact/org.apache.spark/spark-core
libraryDependencies += "org.apache.spark" %% "spark-core" % "2.4.0"
libraryDependencies += "org.apache.spark" %% "spark-sql" % "2.4.0"
libraryDependencies += "org.apache.spark" %% "spark-streaming" % "2.4.0"

libraryDependencies += "org.apache.spark" %% "spark-sql-kafka-0-10" % "2.4.0"
libraryDependencies += "com.sksamuel.avro4s" %% "avro4s-core" % "2.0.2"

// https://mvnrepository.com/artifact/com.holdenkarau/spark-testing-base
libraryDependencies += "com.holdenkarau" %% "spark-testing-base" % "2.4.0_0.11.0" % "test" exclude ("org.apache.spark", "spark-streaming-kafka-0-8_2.11")
//libraryDependencies += "org.apache.kafka" %% "kafka-clients" % "2.0.0" % "test"
// https://mvnrepository.com/artifact/com.101tec/zkclient
libraryDependencies += "com.101tec" % "zkclient" % "0.11" % "test"
// https://mvnrepository.com/artifact/org.apache.kafka/kafka
//libraryDependencies += "org.apache.kafka" %% "kafka" % "0.8.2.1" % "test"
libraryDependencies += "org.apache.kafka" %% "kafka" % "0.10.2.2" % "test"

excludeDependencies += "org.apache.spark" % "spark-streaming-kafka-0-8_2.11"

// test dependencies
libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.5" % "test"
libraryDependencies += "MrPowers" % "spark-fast-tests" % "2.3.1_0.15.0" % "test"
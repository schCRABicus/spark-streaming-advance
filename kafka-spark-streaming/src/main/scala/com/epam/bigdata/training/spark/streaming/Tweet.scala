package com.epam.bigdata.training.spark.streaming

/**
  * Tweet domain model
  */
case class Tweet(dt: Long, id: Long, text: String, hashtags: Array[String]) {}
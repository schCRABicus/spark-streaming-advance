# Environment

The Kafka environment consists of zookeeper instance, 3 kafka brokers and
schema registry. The environment is configured in `docker-compose.yaml` file
and can be deployed via
```
docker-compose up --build
```

The kafka brokers will be available at
`kafka-1:9091`, `kafka-2:9092`, `kafka-3:9093`.

Schema registry is available at port `8081`.


# Application description

The first step is to package the application.
`avro-maven-plugin` will generate TweetRecord entity from
`tweet.avsc` schema file, located under `src/main/resources/avro` directory.

Kafka `auto.register.schemas` property is set to `true` which means that
on publish the producer will register the schema automatically.

After that, the schema can be accessed on `http://localhost:8081/subjects/big-data-tweets-value/versions/1`.

To start the application, the following command can be invoked from project root:
```
java -cp target/twitter-to-kafka-connector-1.0-SNAPSHOT-jar-with-dependencies.jar \
    com.epam.bigdata.training.kafka.tweets.Application \
    --bootstrap_servers kafka-1:9091,kafka-2:9092,kafka-3:9093 \
    --schema_registry_url http://localhost:8081 \
    --topic big-data-tweets \
    --consumer_key jmNrxfuTzspBmrBf9qSEnmp9w \
    --consumer_secret qnI7MTufqtKcwIMDlGKRxbySWEO8Q3GjuopnfMn2t0SSeNvj7x \
    --access_token 1091327668262453248-ACpYxysAvMk9URdquysYDS72oMfnTy \
    --access_token_secret jJcUksP5QSP0kCNprbjczoqVRGjmtlW8pGFT9Mkl5rWn8
```

# JMX-Exporter configuration

Sources:
* [https://medium.com/@danielmrosa/monitoring-kafka-b97d2d5a5434](https://medium.com/@danielmrosa/monitoring-kafka-b97d2d5a5434)
* [https://alex.dzyoba.com/blog/jmx-exporter/](https://alex.dzyoba.com/blog/jmx-exporter/)

The environment can be started via
```
docker-compose -f docker-compose-jmx-exporter.yaml up --build
```
command and includes zookeeper, 3 kafka brokers, schema registry, attached jmx-exporter to
each kafka broker, prometheus gathering metrics from jmx-exporter 
and grafana visualizing the data.

## JMX Exporter java agent configuration and integration to Kafka.

jmx-exporter is a program that reads JMX data from JVM based applications (e.g. Java and Scala) and 
exposes it via HTTP in a simple text format that Prometheus understand and can scrape.

JMX is a common technology in Java world for exporting statistics of running application and also to 
control it (you can trigger GC with JMX, for example).

To enable JMX in kafka image, the following environment variables should be added:
```
 -e KAFKA_JMX_PORT=49999 \
  -e KAFKA_JMX_HOSTNAME=`docker-machine ip confluent`
```
See [Launching Kafka and Zookeeper with JMX Enabled](https://docs.confluent.io/3.2.0/cp-docker-images/docs/operations/monitoring.html#launching-kafka-and-zookeeper-with-jmx-enabled)
for more details.

To run jmx-exporter within Kafka, you should set `KAFKA_OPTS` environment variable like this:

```
KAFKA_OPTS='-javaagent:/opt/jmx-exporter/jmx-exporter.jar=7071:/etc/jmx-exporter/kafka.yml'
```

The corresponding environment variables where added in dedicated docker-compose-jmx-exporter.yml
image.

In addition to that, the `jmx-exporter` directory, containing java agent jar and configuration, where mounted to images.

Now that the java agent configured, the broker metrics are collected by JMX Exporter and
can be accessed by `localhost:8199/metrics` endpoint.

## Prometheus configuration.
The next step is to configure `Prometheus` to collect these metrics and store in it´s time series database:


## Configuring grafana
Finally, the `grafana` instance is started on port 3000.
First, add prometheus datasource, 
![](./outputs/prometheus-datasource.png)
and provide the url as `http;//prometheus:9090`.
![](./outputs/configured-prometheus-datasource.png)

Next import Kafka dashboard from `https://grafana.com/dashboards/721`
![](./outputs/import-butoon.png)
![](./outputs/kafka-import-url.png)

,selecting previously configured Prometheus datasource
![](./outputs/kafka-import-settings.png)

and observe the metrics collected when the application is running:
![](./outputs/kafka-dashboard.png)
.
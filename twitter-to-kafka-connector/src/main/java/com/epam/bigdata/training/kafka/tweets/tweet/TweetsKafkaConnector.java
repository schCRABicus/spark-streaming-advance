package com.epam.bigdata.training.kafka.tweets.tweet;

import avro.shaded.com.google.common.collect.ImmutableMap;
import io.confluent.kafka.schemaregistry.avro.AvroCompatibilityLevel;
import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.DescribeTopicsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Class responsible for Kafka operations like streaming data, establishing connections, etc.
 */
public class TweetsKafkaConnector implements Closeable {

    private static final Logger log = LoggerFactory.getLogger(TweetsKafkaConnector.class);

    private static final String TWEETS_PRODUCER_CLIENT_ID = "tweets-producer";

    private final KafkaProducer<String, TweetRecord> tweetsKafkaProducer;
    private final String topic;
    private final AtomicLong counter = new AtomicLong(0);

    /**
     * Public constructor. Initializes kafka producer.
     * @param servers           Kafka bootstrap servers list.
     * @param schemaRegistry    Avro schema registry.
     * @param topic             Kafka topic to write data into.
     */
    public TweetsKafkaConnector(final String servers, final String schemaRegistry, final String topic, final int replicas, final int partitions) {
        final Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, servers);
        props.put(KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaRegistry);
        props.put(KafkaAvroSerializerConfig.AUTO_REGISTER_SCHEMAS, true);
        props.put(ProducerConfig.CLIENT_ID_CONFIG, TWEETS_PRODUCER_CLIENT_ID);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class.getName());

        setupSchemaCompatibilityPolicy(schemaRegistry, topic);
        setupTopic(props, topic, replicas, partitions);

        this.tweetsKafkaProducer = new KafkaProducer<>(props);
        this.topic = topic;

        log.info("Initialized kafka connector to stream messages to topic {}", topic);
    }

    private void setupSchemaCompatibilityPolicy(final String schemaRegistry, final String topic) {
        final SchemaRegistryClient registryClient = new CachedSchemaRegistryClient(schemaRegistry, 1);
        final KafkaAvroSerializerConfig config = new KafkaAvroSerializerConfig(
                ImmutableMap.of(KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaRegistry)
        );

        final String subject = config.valueSubjectNameStrategy().getSubjectName(topic, false, new TweetRecord());
        log.info("Updating schema compatibility level for subject {} to level {}", subject, AvroCompatibilityLevel.NONE);
        try {
            registryClient.updateCompatibility(subject, AvroCompatibilityLevel.NONE.name);
        } catch (Exception e) {
            log.warn("Failed to relax registry compatibility check", e);
        }
    }

    private void setupTopic(final Properties props, final String topic, final int replicas, final int partitions) {
        AdminClient adminClient = AdminClient.create(props);
        DescribeTopicsResult describeTopicsResult = adminClient.describeTopics(Collections.singletonList(topic));

        boolean exists = false;
        try {
            Map<String, TopicDescription> topics = describeTopicsResult.all().get();

            log.info("Describe topics result for topic {} : {}", topic, describeTopicsResult);

            exists = topics.containsKey(topic);
        } catch (InterruptedException | ExecutionException e) {
            log.warn("Failed to fetch topic {} description, trying to create", topic, e);
        }

        if (!exists) {
            log.info("Topic {} does not exist, going to create it", topic);

            NewTopic newTopic = new NewTopic(topic, partitions, (short) replicas);
            try {
                adminClient.createTopics(Collections.singletonList(newTopic)).all().get();
            } catch (InterruptedException | ExecutionException e) {
                log.warn("Failed to create topic {}", topic, e);
            }

            log.info("Topic {} successfully created", topic);
        }
    }

    /**
     * Sends the specified stream to kafka.
     * @param username Tweet author.
     * @param tweet    Tweet record to send to kafka.
     */
    public void write(final String username, final TweetRecord tweet) {
        final ProducerRecord<String, TweetRecord> record = new ProducerRecord<>(topic, username, tweet);

        this.tweetsKafkaProducer.send(record);

        if (counter.incrementAndGet() % 100 == 0) {
            log.info("Recorded next 100 records");
        }
    }

    /**
     * Autocloseable - flushes producer and closes it.
     */
    @Override
    public void close() {
        log.info("Closing kafka connector...");
        tweetsKafkaProducer.flush();
        tweetsKafkaProducer.close();
    }
}

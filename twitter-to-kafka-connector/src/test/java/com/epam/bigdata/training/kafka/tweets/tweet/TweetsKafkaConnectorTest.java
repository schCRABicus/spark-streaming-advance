package com.epam.bigdata.training.kafka.tweets.tweet;

import com.salesforce.kafka.test.junit4.SharedKafkaTestResource;
import io.confluent.kafka.schemaregistry.client.MockSchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.rest.entities.requests.RegisterSchemaResponse;
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import io.confluent.kafka.serializers.KafkaAvroDecoder;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.hamcrest.core.Is;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpResponse;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.mockserver.model.HttpRequest.request;

public class TweetsKafkaConnectorTest {

    /**
     * We have a single embedded Kafka server that gets started when this test class is initialized.
     *
     * It's automatically started before any methods are run via the @ClassRule annotation.
     * It's automatically stopped after all of the tests are completed via the @ClassRule annotation.
     */
    @ClassRule
    public static final SharedKafkaTestResource sharedKafkaTestResource = new SharedKafkaTestResource();

    /**
     * Mock Server for schema registry
     */
    private static ClientAndServer mockSchemaRegistry;

    @BeforeClass
    public static void startServer() {
        mockSchemaRegistry = ClientAndServer.startClientAndServer(18081);
    }

    @AfterClass
    public static void stopServer() {
        mockSchemaRegistry.stop();
    }

    private String topic;

    private TweetsKafkaConnector connector;

    @Before
    public void setUp() throws Exception {
        topic = "test-tweets-topic-" + UUID.randomUUID().toString();
        connector = new TweetsKafkaConnector(
                sharedKafkaTestResource.getKafkaConnectString(),
                "http://localhost:18081",
                topic, 1, 4
        );

        RegisterSchemaResponse schemaResposne = new RegisterSchemaResponse();
        schemaResposne.setId(1);
        mockSchemaRegistry
                .when(
                        request()
                                .withMethod("POST")
                                .withPath("/subjects/" + topic + "-value/versions")
                )
                .respond(
                        HttpResponse.response(schemaResposne.toJson())
                );
    }

    /**
     * Clean up topic and close connection to Kafka.
     * @throws Exception if any exception occurs.
     */
    @After
    public void tearDown() throws Exception {
        try {
            connector.close();
        } catch (Exception e) { }
    }

    @Test
    public void write() {
        // given
        final String username = "account";
        final TweetRecord tweet = TweetRecord.newBuilder().setId(1L).setDt(new Date().getTime()).setText("Test msg").setHashtags(Collections.singletonList("hashtag-1")).build();
        mockSchemaRegistry
                .when(
                        request()
                                .withMethod("GET")
                                .withPath("/subjects/" + topic + "-value/ids/1")
                )
                .respond(
                        HttpResponse.response(TweetRecord.getClassSchema().toString())
                );

        // when
        connector.write(username, tweet);

        // then verify message serialized and written
        List<ConsumerRecord<byte[], byte[]>> records = sharedKafkaTestResource.getKafkaTestUtils().consumeAllRecordsFromTopic(topic);
        Assert.assertThat(records.size(), Is.is(1));
    }

    @Test
    public void testWriteAvroSerialization() throws IOException, RestClientException {
        // given
        final String username = "account";
        final TweetRecord tweet = TweetRecord.newBuilder().setId(1L).setDt(new Date().getTime()).setText("Test msg").setHashtags(Collections.singletonList("hashtag-1")).build();
        mockSchemaRegistry
                .when(
                        request()
                                .withMethod("GET")
                                .withPath("/subjects/" + topic + "-value/ids/1")
                )
                .respond(
                        HttpResponse.response(TweetRecord.getClassSchema().toString())
                );

        // when
        connector.write(username, tweet);

        // then verify message serialized and written
        List<ConsumerRecord<byte[], byte[]>> records = sharedKafkaTestResource.getKafkaTestUtils().consumeAllRecordsFromTopic(topic);
        Assert.assertThat(records.get(0).key(), Is.is(username.getBytes()));

        MockSchemaRegistryClient schemaRegistryClient = new MockSchemaRegistryClient();
        schemaRegistryClient.register(topic + "-value", TweetRecord.getClassSchema());
        KafkaAvroDecoder decoder = new KafkaAvroDecoder(schemaRegistryClient);
        Assert.assertThat(decoder.fromBytes(records.get(0).value()).toString(), Is.is(tweet.toString()));
    }
}
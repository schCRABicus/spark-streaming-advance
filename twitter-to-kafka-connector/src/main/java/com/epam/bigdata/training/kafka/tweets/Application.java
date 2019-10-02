package com.epam.bigdata.training.kafka.tweets;

import com.epam.bigdata.training.kafka.tweets.tweet.TweetsKafkaConnector;
import com.epam.bigdata.training.kafka.tweets.tweet.TweetsStreamer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Application entry point.
 *
 * <p />
 * Starts streamer and kafka connector and binds them together.
 */
public class Application {

    private static final Logger log = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) {
        final ApplicationOptions options = ApplicationOptions.resolve(args);
        final String[] track = new String[] {
                "big", "data", "ai", "machine", "learning", "course"
        };

        try (
                final TweetsKafkaConnector connector = new TweetsKafkaConnector(options.kafkaServers(), options.kafkaSchemaRegistry(), options.kafkaTopic(), 3, 4);
                final TweetsStreamer streamer = new TweetsStreamer(options.consumerKey(), options.consumerSecret(), options.accessToken(), options.accessTokenSecret())
        ){
            streamer.subscribe(connector::write);
            streamer.start(track);

            Thread.currentThread().join();
        } catch (IOException | InterruptedException e) {
            log.warn("Caught exception while streaming tweets to Kafka, closing stream", e);
        } finally {
            log.info("Application closed");
        }
    }
}

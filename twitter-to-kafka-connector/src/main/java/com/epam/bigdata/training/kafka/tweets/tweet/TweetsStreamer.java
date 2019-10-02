package com.epam.bigdata.training.kafka.tweets.tweet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import twitter4j.FilterQuery;
import twitter4j.HashtagEntity;
import twitter4j.Status;
import twitter4j.StatusAdapter;
import twitter4j.TwitterStream;
import twitter4j.TwitterStreamFactory;
import twitter4j.conf.ConfigurationBuilder;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Subscribes to twitter tweets stream and produces the continuous twitters stream.
 */
public class TweetsStreamer implements Closeable {

    private static final Logger log = LoggerFactory.getLogger(TweetsStreamer.class);

    private final String consumerKey;
    private final String consumerSecret;
    private final String accessToken;
    private final String accessTokenSecret;

    private final TweetsListener listener;
    private final TwitterStream stream;

    public TweetsStreamer(String consumerKey, String consumerSecret, String accessToken, String accessTokenSecret) {
        this.consumerKey = consumerKey;
        this.consumerSecret = consumerSecret;
        this.accessToken = accessToken;
        this.accessTokenSecret = accessTokenSecret;

        this.listener = new TweetsListener();
        this.stream = initStream();

        log.info("Initialized tweets streamer");
    }

    /**
     * Initializes twitter stream with the provided configuration
     * and filters out tweets by the specified tracks.
     * @return  Initialized stream.
     */
    private TwitterStream initStream() {
        final ConfigurationBuilder cb = new ConfigurationBuilder();
        cb.setDebugEnabled(true)
                .setOAuthConsumerKey(consumerKey)
                .setOAuthConsumerSecret(consumerSecret)
                .setOAuthAccessToken(accessToken)
                .setOAuthAccessTokenSecret(accessTokenSecret);

        return new TwitterStreamFactory(cb.build()).getInstance()
                .addListener(listener);
    }

    /**
     * Subscribes to tweets stream and consumes if any occur.
     * @param consumer  Tweets consumer.
     */
    public void subscribe(BiConsumer<String, TweetRecord> consumer) {
        this.listener.addListener(consumer);
    }

    /**
     * Starts listening.
     * @param track Track to filter tweets by.
     */
    public void start(String... track) {
        final FilterQuery filter = new FilterQuery().track(track);
        this.stream.filter(filter);
    }

    /**
     * Terminates tweets streaming.
     */
    @Override
    public void close() throws IOException {
        log.info("Closing tweets streamer...");
        stream.clearListeners();
        stream.shutdown();
    }

    private static class TweetsListener extends StatusAdapter {

        private final Queue<BiConsumer<String, TweetRecord>> listeners;

        private TweetsListener() {
            this.listeners = new ArrayDeque<>();
        }

        public void addListener(BiConsumer<String, TweetRecord> listener) {
            this.listeners.add(listener);
        }

        @Override
        public void onStatus(Status status) {
            // skip records without hash tags
            if (status.getHashtagEntities().length == 0) {
                return;
            }

            final TweetRecord tweet = TweetRecord.newBuilder()
                    .setId(status.getId())
                    .setDt(status.getCreatedAt().getTime())
                    .setText(status.getText())
                    .setHashtags(Stream.of(status.getHashtagEntities()).map(HashtagEntity::getText).collect(Collectors.toList()))
                    .build()
                    ;

            final String author = TweetUtils.usernameOrDefault(status);

            this.listeners.forEach(listener -> {
                try {
                    listener.accept(author, tweet);
                } catch (Exception e) {
                    log.warn("Failed to consume tweet {} from {} due to the error below", tweet, author, e);
                }
            });
        }
    }
}

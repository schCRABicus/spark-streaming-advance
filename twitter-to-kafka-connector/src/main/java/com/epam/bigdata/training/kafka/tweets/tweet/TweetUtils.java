package com.epam.bigdata.training.kafka.tweets.tweet;

import twitter4j.Status;
import twitter4j.User;

import java.util.Optional;

/**
 * TweetUtils utilities.
 */
public class TweetUtils {

    /**
     * Default unresolved username.
     */
    private static final String DEFAULT_USERNAME = "unknown";

    /**
     * Resolves user name or returns default value if not defined.
     * @return user name or default value if null.
     */
    public static String usernameOrDefault(Status status) {
        return Optional.ofNullable(status.getUser()).map(User::getName).orElse(DEFAULT_USERNAME);
    }
}

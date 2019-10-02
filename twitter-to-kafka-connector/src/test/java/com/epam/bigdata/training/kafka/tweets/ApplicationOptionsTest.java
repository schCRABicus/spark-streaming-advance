package com.epam.bigdata.training.kafka.tweets;

import org.hamcrest.core.Is;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;

public class ApplicationOptionsTest {

    @Test
    public void testOkResolve() {
        // given
        String[] args = new String[] {
                "--bootstrap_servers", "kafka-1:9091,kafka-2:9092,kafka-3:9093",
                "--schema_registry_url", "http://localhost:8081",
                "--topic", "big-data-tweets",
                "--consumer_key", "jmNrxfuTzspBmrBf9qSE...",
                "--consumer_secret", "qnI7MTufqtKcwIMDlGKRxbySWEO8Q3GjuopnfMn2t0SSe...",
                "--access_token", "1091327668262453248-ACpYxysAvMk9URdquysYDS72...",
                "--access_token_secret", "jJcUksP5QSP0kCNprbjczoqVRGjmtlW8pGFT9Mkl5r..."
        };

        // when
        ApplicationOptions options = ApplicationOptions.resolve(args);

        // then ok
        Assert.assertThat(options.accessToken(), Is.is("1091327668262453248-ACpYxysAvMk9URdquysYDS72..."));
    }

    @Test
    @Ignore("Exits programm")
    public void testPrintsHelpIfNotOk() {
        // given
        String[] args = new String[] {
                "--bootstrap_servers", "kafka-1:9091,kafka-2:9092,kafka-3:9093",
                "--schema_registry_url", "http://localhost:8081",
                "--topic", "big-data-tweets",
                // missing config --> "--consumer_key", "jmNrxfuTzspBmrBf9qSE...",
                "--consumer_secret", "qnI7MTufqtKcwIMDlGKRxbySWEO8Q3GjuopnfMn2t0SSe...",
                "--access_token", "1091327668262453248-ACpYxysAvMk9URdquysYDS72...",
                "--access_token_secret", "jJcUksP5QSP0kCNprbjczoqVRGjmtlW8pGFT9Mkl5r..."
        };

        // when
        ApplicationOptions options = ApplicationOptions.resolve(args);

        // then fail
    }
}
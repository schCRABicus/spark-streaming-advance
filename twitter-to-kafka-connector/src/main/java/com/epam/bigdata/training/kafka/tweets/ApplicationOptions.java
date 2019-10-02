package com.epam.bigdata.training.kafka.tweets;

import lombok.Data;
import lombok.experimental.Accessors;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Applciation startup options.
 */
@Data
@Accessors(chain = true, fluent = true)
public class ApplicationOptions {

    private static final Logger log = LoggerFactory.getLogger(ApplicationOptions.class);

    private String kafkaServers;
    private String kafkaSchemaRegistry;
    private String kafkaTopic;
    private String consumerKey;
    private String consumerSecret;
    private String accessToken;
    private String accessTokenSecret;

    /**
     * Utility method to resolve appliaction options from the provided command-line arguments.
     * @param args  COmmand line arguments.
     * @return  Resolved Application options instance.
     */
    public static ApplicationOptions resolve(String[] args) {
        Options options = new Options();

        options.addOption(Option.builder("b").longOpt("bootstrap_servers").hasArg().desc("Kafka bootstrap servers, comma-separated").required().build());
        options.addOption(Option.builder("u").longOpt("schema_registry_url").hasArg().desc("Avro schema registry url").required().build());
        options.addOption(Option.builder("t").longOpt("topic").hasArg().desc("Target topic").required().build());
        options.addOption(Option.builder("ck").longOpt("consumer_key").hasArg().desc("Consumer key").required().build());
        options.addOption(Option.builder("cs").longOpt("consumer_secret").hasArg().desc("Consumer secret").required().build());
        options.addOption(Option.builder("at").longOpt("access_token").hasArg().desc("Access token").required().build());
        options.addOption(Option.builder("as").longOpt("access_token_secret").hasArg().desc("Access token secret").required().build());

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = null;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            log.error("Failed to parse command line options", e);

            new HelpFormatter().printHelp("twitter-tweets-to-kafka", options);

            System.exit(1);
        }

        return new ApplicationOptions()
                .kafkaServers(cmd.getOptionValue("bootstrap_servers"))
                .kafkaSchemaRegistry(cmd.getOptionValue("schema_registry_url"))
                .kafkaTopic(cmd.getOptionValue("topic"))
                .consumerKey(cmd.getOptionValue("consumer_key"))
                .consumerSecret(cmd.getOptionValue("consumer_secret"))
                .accessToken(cmd.getOptionValue("access_token"))
                .accessTokenSecret(cmd.getOptionValue("access_token_secret"))
                ;
    }
}

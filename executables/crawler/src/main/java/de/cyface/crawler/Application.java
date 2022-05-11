/*
 * Copyright 2021 Cyface GmbH
 *
 * This file is part of the Cyface Crawler.
 *
 *  The Cyface Crawler is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  The Cyface Crawler is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with the Cyface Crawler.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.cyface.crawler;

import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.Validate;
import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This application starts a scheduler which crawls an API regularly and persists the data returned.
 * <p>
 * To view the accepted command line parameters just call the application without any.
 *
 * @author Armin Schnabel
 */
public class Application {

    /**
     * The logger used by objects of this class. Configure it using <tt>src/main/resources/logback.xml</tt>.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(Application.class);
    /**
     * The short command line input option used to accept the API key for the TIER API.
     * Usually this option is provided after a dash (-). If you want to use the long option view
     * {@link #TIER_API_KEY_LONG_OPTION}.
     */
    static final String TIER_API_KEY_SHORT_OPTION = "tk";
    /**
     * The long command line input option used to accept the API key for the TIER API.
     * Usually this option is provided after a double dash (--). If you want to use the short option view
     * {@link #TIER_API_KEY_SHORT_OPTION}.
     */
    static final String TIER_API_KEY_LONG_OPTION = "tier-api-key";
    /**
     * The short command line input option used to accept the auth token for the Lime API.
     * Usually this option is provided after a dash (-). If you want to use the long option view
     * {@link #LIME_AUTH_TOKEN_LONG_OPTION}.
     */
    static final String LIME_AUTH_TOKEN_SHORT_OPTION = "lt";
    /**
     * The long command line input option used to accept the auth token for the Lime API.
     * Usually this option is provided after a double dash (--). If you want to use the short option view
     * {@link #LIME_AUTH_TOKEN_SHORT_OPTION}.
     */
    static final String LIME_AUTH_TOKEN_LONG_OPTION = "lime-auth-token";
    static final String MONGO_HOST_SHORT_OPTION = "mh";
    static final String MONGO_HOST_LONG_OPTION = "mongo-host";
    static final String MONGO_PORT_SHORT_OPTION = "mp";
    static final String MONGO_PORT_LONG_OPTION = "mongo-port";
    static final String MONGO_DATABASE_SHORT_OPTION = "md";
    static final String MONGO_DATABASE_LONG_OPTION = "mongo-database";
    static final String MONGO_COLLECTION_SHORT_OPTION = "mc";
    static final String MONGO_COLLECTION_LONG_OPTION = "mongo-collection";
    static final String MONGO_USER_SHORT_OPTION = "mu";
    static final String MONGO_USER_LONG_OPTION = "mongo-user";
    static final String MONGO_PASSWORD_SHORT_OPTION = "mpw";
    static final String MONGO_PASSWORD_LONG_OPTION = "mongo-password";
    static final String DEBUG_MODE_SHORT_OPTION = "dm";
    static final String DEBUG_MODE_LONG_OPTION = "debug-mode";
    static final String MILLISECONDS_BETWEEN_REQUESTS_SHORT_OPTION = "mbr";
    static final String MILLISECONDS_BETWEEN_REQUESTS_LONG_OPTION = "milliseconds-between-requests";
    static final String MAX_REQUESTS_PER_CRAWL_SHORT_OPTION = "mrc";
    static final String MAX_REQUESTS_PER_CRAWL_LONG_OPTION = "max-request-per-crawl";
    static final String MAX_REQUESTS_PER_HOUR_SHORT_OPTION = "mrh";
    static final String MAX_REQUESTS_PER_HOUR_LONG_OPTION = "max-requests-per-hour";
    static final String CRAWLER_NUMBER_SHORT_OPTION = "cn";
    static final String CRAWLER_NUMBER_LONG_OPTION = "crawler-number";
    static final String NUMBER_OF_CRAWLERS_SHORT_OPTION = "noc";
    static final String NUMBER_OF_CRAWLERS_LONG_OPTION = "number-of-crawlers";
    /**
     * {@code Null} to continue until no no vehicles are found or the number of requests.
     */
    private final static int DEFAULT_MAX_REQUESTS_PER_CRAWL = 340;
    /**
     * Milliseconds after which a new request can be sent.
     */
    private final static int DEFAULT_MILLISECONDS_BETWEEN_REQUESTS = 3_500;
    /**
     * The maximum crawls per minute.
     */
    private static final long DEFAULT_MAX_REQUESTS_PER_HOUR = 170;
    /**
     * The number of this crawler, e.g. 1, 2, ..., 8 for 8 {@code DEFAULT_NUMBER_OF_CRAWLERS}.
     * <p>
     * Or set this to `0` for `0` {@code DEFAULT_NUMBER_OF_CRAWLERS} to start crawling without delay.
     */
    private static final long DEFAULT_CRAWLER_NUMBER = 0;
    /**
     * The number of crawlers which should be scheduled equally during the day.
     * <p>
     * Or set this to `0` for `0` {@code DEFAULT_CRAWLER_NUMBER} to start crawling without delay.
     */
    private static final long DEFAULT_NUMBER_OF_CRAWLERS = 0;
    /**
     * The crawler used to crawl the TIER API.
     */
    private Crawler tierCrawler;
    /**
     * The crawler used to crawl the Lime API.
     */
    private Crawler limeCrawler;
    /**
     * Database to persist crawled data into.
     */
    private final MongoConnection mongoConnection;

    /**
     *
     * @param mongoHost
     * @param mongoPort
     * @param mongoDatabase
     * @param mongoCollection
     * @param mongoUser
     * @param mongoPassword
     */
    public Application(String mongoHost, String mongoPort, String mongoDatabase, String mongoCollection,
            String mongoUser, String mongoPassword) {
        this.mongoConnection = new MongoConnection(mongoHost, Integer.parseInt(mongoPort), mongoDatabase, mongoUser,
                mongoPassword);
        mongoConnection.check(mongoCollection);
    }

    /**
     * Runs the application from the command line.
     *
     * @param args The arguments provided via the current application execution. To get a list of available arguments,
     *            run the program without any.
     */
    public static void main(final String[] args) {
        CommandLine commandLine;
        try {
            // Parse Command Line
            commandLine = commandLineParser().parse(options(), args);
            final var tierApiKey = commandLine.getOptionValue(TIER_API_KEY_SHORT_OPTION, null);
            final var limeAuthToken = commandLine.getOptionValue(LIME_AUTH_TOKEN_SHORT_OPTION);
            final var mongoHost = commandLine.getOptionValue(MONGO_HOST_SHORT_OPTION, "mongo-data");
            final var mongoPort = commandLine.getOptionValue(MONGO_PORT_SHORT_OPTION, "27017");
            final var mongoDatabase = commandLine.getOptionValue(MONGO_DATABASE_SHORT_OPTION, "scone");
            final var mongoCollection = commandLine.getOptionValue(MONGO_COLLECTION_SHORT_OPTION, "lime_records");
            final var mongoUser = commandLine.getOptionValue(MONGO_USER_SHORT_OPTION, "root");
            final var mongoPassword = commandLine.getOptionValue(MONGO_PASSWORD_SHORT_OPTION, "example");
            final var debugMode = commandLine.hasOption(DEBUG_MODE_SHORT_OPTION);
            final var millisecondsBetweenRequests = commandLine
                    .getOptionValue(MILLISECONDS_BETWEEN_REQUESTS_SHORT_OPTION,
                            String.valueOf(DEFAULT_MILLISECONDS_BETWEEN_REQUESTS));
            final var maxRequestsPerHour = commandLine
                    .getOptionValue(MAX_REQUESTS_PER_HOUR_SHORT_OPTION,
                            String.valueOf(DEFAULT_MAX_REQUESTS_PER_HOUR));
            final var maxRequestsPerCrawl = commandLine
                    .getOptionValue(MAX_REQUESTS_PER_CRAWL_SHORT_OPTION,
                            String.valueOf(DEFAULT_MAX_REQUESTS_PER_CRAWL));
            final var crawlerNumber = commandLine.getOptionValue(CRAWLER_NUMBER_SHORT_OPTION,
                    String.valueOf(DEFAULT_CRAWLER_NUMBER));
            final var numberOfCrawlers = commandLine.getOptionValue(NUMBER_OF_CRAWLERS_SHORT_OPTION,
                    String.valueOf(DEFAULT_NUMBER_OF_CRAWLERS));

            // Execution
            final var application = new Application(mongoHost, mongoPort, mongoDatabase, mongoCollection, mongoUser,
                    mongoPassword);
            application.run(tierApiKey, limeAuthToken, debugMode, millisecondsBetweenRequests, maxRequestsPerHour,
                    maxRequestsPerCrawl, crawlerNumber, numberOfCrawlers);

        } catch (ParseException e) {
            final var header = String.format("API Crawler Input Preparation%n%n\tError: %s%n%n",
                    e.getLocalizedMessage());
            final var footer = "\nPlease provide appropriate arguments!";
            final var formatter = new HelpFormatter();
            formatter.printHelp("crawler", header, options(), footer, true);
            LOGGER.error("Error: \n", e);
            throw new IllegalStateException(e);
        }
    }

    /**
     * Runs the application.
     * 
     * @param tierApiKey {@code null} to disable tier crawling or the access token otherwise.
     * @param limeAuthToken required to crawl the Lime API
     * @param debugMode {@code true} to log processing results into CSV files
     * @param millisecondsBetweenRequests Milliseconds after which a new request can be sent.
     * @param maxRequestsPerHour The maximum crawls per minute which did not instantly lead to a REQUEST_LIMIT_EXCEEDED
     *            result.
     * @param maxRequestsPerCrawl {@code Null} to continue until no no vehicles are found or the number of requests.
     * @param crawlerNumber the number of this crawler, e.g. 1, 2, ..., 8 for 8 {@code numberOfCrawlers}. Or `0` for `0`
     *            {@code numberOfCrawlers} to start crawling without delay.
     * @param numberOfCrawlers The number of crawlers which should be scheduled equally during the day. Or `0` for `0`
     *            {@code crawlerNumber} to start crawling without delay.
     */
    public void run(final String tierApiKey, final String limeAuthToken, final boolean debugMode,
            final String millisecondsBetweenRequests, final String maxRequestsPerHour,
            final String maxRequestsPerCrawl, final String crawlerNumber, final String numberOfCrawlers) {

        // Start scheduler
        final var scheduler = Executors.newScheduledThreadPool(1);
        // Number of crawls to schedule per hour.
        final double crawlsPerHour = Integer.parseInt(maxRequestsPerHour)
                / (double)Integer.parseInt(maxRequestsPerCrawl);
        final long secondsPerMinute = 60;
        final long minutesPerHour = 60;
        // Time to wait between two "crawl" schedules.
        final long secondsBetweenCrawls = (long)Math.ceil(minutesPerHour / crawlsPerHour * secondsPerMinute);
        final long initialDelay = initialDelay(Integer.parseInt(crawlerNumber), Integer.parseInt(numberOfCrawlers),
                secondsBetweenCrawls);
        // Non-concurrent scheduling (subsequent starting late if previous still ongoing)
        scheduler.scheduleAtFixedRate(
                () -> {
                    // Initializing the crawler here to create new dump files for testing each crawl
                    this.limeCrawler = new LimeCrawler(limeAuthToken, Integer.parseInt(maxRequestsPerCrawl),
                            Integer.parseInt(millisecondsBetweenRequests), debugMode);
                    this.tierCrawler = tierApiKey != null ? new TierCrawler(tierApiKey) : null;

                    try {
                        limeCrawler.crawl(mongoConnection);
                    } catch (JSONException e) {
                        LOGGER.error(e.getMessage());
                        throw new IllegalStateException(e);
                    }

                    if (tierCrawler != null) {
                        try {
                            tierCrawler.crawl(mongoConnection);
                        } catch (JSONException e) {
                            LOGGER.error(e.getMessage());
                            throw new IllegalStateException(e);
                        }
                    }
                }, initialDelay, secondsBetweenCrawls, TimeUnit.SECONDS);
    }

    /**
     * To support multiple Crawler instances this method allows each instance to start at a different time frame.
     * <p>
     * For this we use the numberOfCrawlers and this crawler's crawlerNumber, e.g.:
     * - secondsBetweenCrawls = 2 hours
     * - crawlerNumber 1 of 8: starts at next 0:00, 2:00, 4:00, ..., 22:00 time spot
     * - crawlerNumber 2 of 8: starts at next 0:15, 2:15, ..., 22:15 time spot, etc.
     *
     * @param crawlerNumber the number of this crawler, e.g. 1, 2, ..., 8 for 8 {@code numberOfCrawlers}. Or `0` for `0`
     *            {@code numberOfCrawlers} to start crawling without delay.
     * @param numberOfCrawlers The number of crawlers which should be scheduled equally during the day. Or `0` for `0`
     *            {@code crawlerNumber} to start crawling without delay.
     * @param secondsBetweenCrawls The number of seconds between two crawls of one crawler.
     * @return The number of seconds until this crawler should start it's first crawl
     */
    private long initialDelay(long crawlerNumber, long numberOfCrawlers, long secondsBetweenCrawls) {
        if (crawlerNumber == 0 && numberOfCrawlers == 0) {
            return 0;
        }
        Validate.isTrue(crawlerNumber >= 1 && crawlerNumber <= numberOfCrawlers,
                String.format("crawlerNumber out of Range: %s != [1...%s]", crawlerNumber, numberOfCrawlers));
        final ZonedDateTime nowZoned = ZonedDateTime.now();
        final Instant midnight = nowZoned.toLocalDate().atStartOfDay(nowZoned.getZone()).toInstant();
        final Duration sinceMidnight = Duration.between(midnight, Instant.now());
        final long secondsSinceMidnight = sinceMidnight.getSeconds();
        final long nextCrawlWindow = (long)Math.ceil(secondsSinceMidnight / (double)secondsBetweenCrawls);
        final long nextCrawlWindowSeconds = nextCrawlWindow * secondsBetweenCrawls;
        final long secondsBetweenCrawlers = secondsBetweenCrawls / numberOfCrawlers;
        final long crawlerDelay = secondsBetweenCrawlers * (crawlerNumber - 1);
        final long crawlerDelayUntilNextWindow = (nextCrawlWindowSeconds - secondsSinceMidnight) + crawlerDelay;
        final long initialDelay = crawlerDelayUntilNextWindow > secondsBetweenCrawls
                ? crawlerDelayUntilNextWindow - secondsBetweenCrawls
                : crawlerDelayUntilNextWindow;
        LOGGER.info("Scheduling initial crawl at " + nowZoned.plusSeconds(initialDelay));
        Validate.isTrue(initialDelay <= secondsBetweenCrawls,
                String.format("initialDelay %s > secondsBetweenCrawls %s", initialDelay, secondsBetweenCrawls));
        return initialDelay;
    }

    /**
     * @return An Apache CLI <code>Options</code> object configured with the options available for this application.
     */
    private static Options options() {
        final var ret = new Options();
        ret.addOption(TIER_API_KEY_SHORT_OPTION, TIER_API_KEY_LONG_OPTION, true,
                "If a TIER API key is provided, the TIER API is crawled, too.");
        ret.addRequiredOption(LIME_AUTH_TOKEN_SHORT_OPTION, LIME_AUTH_TOKEN_LONG_OPTION, true,
                "Please provide a Lime API auth token.");
        ret.addOption(MONGO_HOST_SHORT_OPTION, MONGO_HOST_LONG_OPTION, true,
                "Please provide a Mongo Database hostname.");
        ret.addOption(MONGO_PORT_SHORT_OPTION, MONGO_PORT_LONG_OPTION, true,
                "Please provide a Mongo Database port.");
        ret.addOption(MONGO_DATABASE_SHORT_OPTION, MONGO_DATABASE_LONG_OPTION, true,
                "Please provide a Mongo Database name.");
        ret.addOption(MONGO_COLLECTION_SHORT_OPTION, MONGO_COLLECTION_LONG_OPTION, true,
                "Please provide a Mongo Database collection name.");
        ret.addOption(MONGO_USER_SHORT_OPTION, MONGO_USER_LONG_OPTION, true,
                "Please provide a Mongo Database username.");
        ret.addOption(MONGO_PASSWORD_SHORT_OPTION, MONGO_PASSWORD_LONG_OPTION, true,
                "Please provide a Mongo Database password.");
        ret.addOption(DEBUG_MODE_SHORT_OPTION, DEBUG_MODE_LONG_OPTION, false,
                "Set this flag to log crawling results into CSV files.");
        ret.addOption(MILLISECONDS_BETWEEN_REQUESTS_SHORT_OPTION, MILLISECONDS_BETWEEN_REQUESTS_LONG_OPTION,
                true, "Please provide the number of milliseconds between two request.");
        ret.addOption(MAX_REQUESTS_PER_HOUR_SHORT_OPTION, MAX_REQUESTS_PER_HOUR_LONG_OPTION,
                true, "Please provide the maximum number of requests allowed per hour.");
        ret.addOption(MAX_REQUESTS_PER_CRAWL_SHORT_OPTION, MAX_REQUESTS_PER_CRAWL_LONG_OPTION,
                true, "Please provide the maximum number of requests allowed per crawl.");
        ret.addOption(CRAWLER_NUMBER_SHORT_OPTION, CRAWLER_NUMBER_LONG_OPTION,
                true, "Please provide the number of this crawler.");
        ret.addOption(NUMBER_OF_CRAWLERS_SHORT_OPTION, NUMBER_OF_CRAWLERS_LONG_OPTION,
                true, "Please provide the number of crawlers.");
        return ret;
    }

    /**
     * @return An Apache CLI parser used to parse the command line arguments.
     */
    private static CommandLineParser commandLineParser() {
        return new DefaultParser();
    }
}

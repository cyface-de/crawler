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
package de.cyface.crawler.processor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This application processes the data collected by the crawler and extracts source-destination-relations.
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
    static final String POSTGRES_URL_SHORT_OPTION = "purl";
    static final String POSTGRES_URL_LONG_OPTION = "postgres-url";
    static final String POSTGRES_TABLE_SHORT_OPTION = "pt";
    static final String POSTGRES_TABLE_LONG_OPTION = "postgres-table";
    static final String POSTGRES_USER_SHORT_OPTION = "pu";
    static final String POSTGRES_USER_LONG_OPTION = "postgres-user";
    static final String POSTGRES_PASSWORD_SHORT_OPTION = "pp";
    static final String POSTGRES_PASSWORD_LONG_OPTION = "postgres-password";
    static final String DEBUG_MODE_SHORT_OPTION = "dm";
    static final String DEBUG_MODE_LONG_OPTION = "debug-mode";

    private final MongoConnection dataSource;
    private final PostgresConnection dataLake;

    public Application(String mongoHost, String mongoPort, String mongoDatabase, String mongoUser, String mongoPassword,
            String postgresUrl, String postgresUser, String postgresPassword) {
        this.dataSource = new MongoConnection(mongoHost, Integer.parseInt(mongoPort), mongoDatabase, mongoUser,
                mongoPassword);
        this.dataLake = new PostgresConnection(postgresUrl, postgresUser, postgresPassword);
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
            final var mongoHost = commandLine.getOptionValue(MONGO_HOST_SHORT_OPTION, "mongo-data");
            final var mongoPort = commandLine.getOptionValue(MONGO_PORT_SHORT_OPTION, "27017");
            final var mongoDatabase = commandLine.getOptionValue(MONGO_DATABASE_SHORT_OPTION, "scone");
            final var mongoCollection = commandLine.getOptionValue(MONGO_COLLECTION_SHORT_OPTION, "lime_records");
            final var mongoUser = commandLine.getOptionValue(MONGO_USER_SHORT_OPTION, "root");
            final var mongoPassword = commandLine.getOptionValue(MONGO_PASSWORD_SHORT_OPTION, "example");
            final var postgresUrl = commandLine.getOptionValue(POSTGRES_URL_SHORT_OPTION,
                    "jdbc:postgresql://postgres:5432/postgres");
            final var postgresTable = commandLine.getOptionValue(POSTGRES_TABLE_SHORT_OPTION, "source_destination");
            final var postgresUser = commandLine.getOptionValue(POSTGRES_USER_SHORT_OPTION, "postgres");
            final var postgresPassword = commandLine.getOptionValue(POSTGRES_PASSWORD_SHORT_OPTION, "postgres");
            final var debugMode = commandLine.hasOption(DEBUG_MODE_SHORT_OPTION);

            // Execution
            final var application = new Application(mongoHost, mongoPort, mongoDatabase, mongoUser,
                    mongoPassword, postgresUrl, postgresUser, postgresPassword);
            application.run(mongoCollection, postgresTable, debugMode);

        } catch (ParseException e) {
            final var header = String.format("Processing Input Preparation%n%n\tError: %s%n%n",
                    e.getLocalizedMessage());
            final var footer = "\nPlease provide appropriate arguments!";
            final var formatter = new HelpFormatter();
            formatter.printHelp("processing", header, options(), footer, true);
            LOGGER.error("Error: \n", e);
            throw new IllegalStateException(e);
        }
    }

    /**
     * Runs the application.
     * 
     * @param mongoCollection name of the collection to load the raw data from
     * @param postgresTable name of the table to write the processing result to
     * @param debugMode {@code true} to log processing results into CSV files
     */
    public void run(final String mongoCollection, String postgresTable, boolean debugMode) {
        final var res = new Processor(dataSource).run(mongoCollection);
        final var relations = new ArrayList<SourceDestinationRelation>();
        res.values().forEach(relations::addAll);

        try {
            if (debugMode) {
                dumpToFile(relations);
            }

            // Write into result DB
            dataLake.write(relations, postgresTable);
        } catch (SQLException | IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private void dumpToFile(ArrayList<SourceDestinationRelation> relations) throws IOException {
        // Dump into file for fast debugging
        final var requestsFile = Paths.get(new Date().getTime() + "_results.csv");
        Files.writeString(requestsFile,
                "sourceLat, sourceLon, destinationLat, destinationLon, lastActivity, sourceRequest, destinationRequest\n",
                StandardOpenOption.CREATE);
        relations.forEach(r -> {
            final var builder = r.source.getLatitude() + "," + r.source.getLongitude() + ","
                    + r.destination.getLatitude() + "," + r.destination.getLongitude() + ","
                    + r.destination.getLastActivityAt() + "," + r.source.getRequestTime()
                    + "," + r.destination.getRequestTime() + "\n";
            try {
                Files.writeString(requestsFile, builder, StandardOpenOption.APPEND);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        });
    }

    /**
     * @return An Apache CLI <code>Options</code> object configured with the options available for this application.
     */
    private static Options options() {
        final var ret = new Options();
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
        ret.addOption(POSTGRES_URL_SHORT_OPTION, POSTGRES_URL_LONG_OPTION, true,
                "Please provide a Postgres Database URL.");
        ret.addOption(POSTGRES_TABLE_SHORT_OPTION, POSTGRES_TABLE_LONG_OPTION, true,
                "Please provide a Postgres Database table name.");
        ret.addOption(POSTGRES_USER_SHORT_OPTION, POSTGRES_USER_LONG_OPTION, true,
                "Please provide a Postgres Database username.");
        ret.addOption(POSTGRES_PASSWORD_SHORT_OPTION, POSTGRES_PASSWORD_LONG_OPTION, true,
                "Please provide a Postgres Database password.");
        ret.addOption(DEBUG_MODE_SHORT_OPTION, DEBUG_MODE_LONG_OPTION, false,
                "Set this flag to log processing results into CSV files.");
        return ret;
    }

    /**
     * @return An Apache CLI parser used to parse the command line arguments.
     */
    private static CommandLineParser commandLineParser() {
        return new DefaultParser();
    }
}

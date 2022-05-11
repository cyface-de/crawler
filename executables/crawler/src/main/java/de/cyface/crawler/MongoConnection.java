/*
 * Copyright 2021 Cyface GmbH
 *
 * This file is part of the Cyface Crawler.
 *
 * The Cyface Crawler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The Cyface Crawler is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the Cyface Crawler. If not, see <http://www.gnu.org/licenses/>.
 */
package de.cyface.crawler;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.Validate;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

import de.cyface.crawler.model.LimeVehicle;

/**
 * A data lake to write records to and read from.
 *
 * @author Armin Schnabel
 */
public final class MongoConnection {

    /**
     * The logger used by objects of this class. Configure it using <tt>src/main/resources/logback.xml</tt>.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(MongoConnection.class);

    /**
     * The MongoDB database to use.
     */
    private final String databaseName;
    /**
     * A MongoDB client to access the Mongo database.
     */
    private MongoClient client;
    /**
     * The host name used to access the Mongo database to write to.
     */
    private final String host;
    /**
     * The port at which the Mongo database is reachable at.
     */
    private final int port;
    /**
     * The name of the user to authenticate at the database.
     */
    private final String username;
    /**
     * The password of the user to authenticate at the database.
     */
    private final String password;

    /**
     * Creates a new completely initialized database source for one Mongo database instance.
     * 
     * @param host The host name used to access the Mongo database
     * @param port The port at which the Mongo database is reachable at
     * @param databaseName The mongoDB database to write to
     * @param username The name of the user to authenticate at the database
     * @param password The password of the user to authenticate at the database
     */
    public MongoConnection(final String host, final int port, final String databaseName, final String username,
            final String password) {
        Validate.notNull(host);
        Validate.notNull(databaseName);
        Validate.notNull(username);
        Validate.notNull(password);

        this.host = host;
        this.port = port;
        this.databaseName = databaseName;
        this.username = username;
        this.password = password;
    }

    /**
     * Persist a list of documents.
     * 
     * @param documents the data to persist
     * @param collectionName to write the data to
     */
    private void write(final List<Document> documents, final String collectionName) {

        final var connectionString = new ConnectionString(
                String.format("mongodb://%s:%s@%s:%s", username, password, host, port));
        final var settings = MongoClientSettings.builder()
                .applyConnectionString(connectionString)
                .retryWrites(true)
                .build();
        try {
            client = MongoClients.create(settings);
            final var database = client.getDatabase(databaseName);
            final var collection = database.getCollection(collectionName);
            collection.insertMany(documents);
        } finally {
            if (client != null) {
                client.close();
            }
        }
    }

    /**
     * Persist a list of vehicle records.
     *
     * @param records the data to persist
     * @param collectionName to write the data to
     */
    public void write(final Set<LimeVehicle> records, final String collectionName) {

        final var documents = records.stream().map(LimeVehicle::toBson).collect(Collectors.toList());
        write(documents, collectionName);
    }

    /**
     * Checks if a mongo collection exists.
     *
     * @param collectionName The name of the collection to check.
     */
    public void check(final String collectionName) {

        final String connectionString = String.format("mongodb://%s:%s@%s:%s", username, password, host, port);
        try {
            client = MongoClients.create(new ConnectionString(connectionString));
            final var db = client.getDatabase(databaseName);
            final var collection = db.getCollection(collectionName);
            final var count = collection.countDocuments();
            LOGGER.info("------------- Connected to mongoDB, " + count + " " + collectionName + " found. ---------");
        } finally {
            if (client != null) {
                client.close();
            }
        }
    }
}

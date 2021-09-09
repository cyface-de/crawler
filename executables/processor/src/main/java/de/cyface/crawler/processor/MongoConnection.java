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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.Validate;

import com.mongodb.BasicDBObject;
import com.mongodb.ConnectionString;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

import de.cyface.crawler.model.Record;

/**
 * A data lake to write records to and read from.
 *
 * @author Armin Schnabel
 */
public final class MongoConnection {

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

    public Map<String, List<Record>> records(final String collectionName) {

        final String connectionString = String.format("mongodb://%s:%s@%s:%s", username, password, host, port);
        try {
            client = MongoClients.create(new ConnectionString(connectionString));
            final var db = client.getDatabase(databaseName);
            final var collection = db.getCollection(collectionName);

            final var sort = new BasicDBObject("last_three", 1).append("request_time", 1);
            // Allowing disk use or else sorting a large data set (e.g. 500k records) throws the error:
            // 'Sort exceeded memory limit of 104857600 bytes, but did not opt in to external sorting.'
            final var res = collection.find().sort(sort).allowDiskUse(true);

            // TODO: Inefficient code
            final var ret = new HashMap<String, List<Record>>();
            res.forEach(d -> {
                final var lastThree = d.getString("last_three");
                final var list = ret.containsKey(lastThree) ? ret.get(lastThree) : new ArrayList<Record>();
                list.add(new Record(d.getObjectId("_id"), d.getString("last_three"), d.getDouble("latitude"),
                        d.getDouble("longitude"), d.getDate("last_activity_at"), d.getDate("request_time"),
                        d.getInteger("meter_range"), d.getDate("crawling_started"),
                        d.getInteger("battery_percentage"), d.getString("plate_number")));
                ret.put(lastThree, list);
            });
            return ret;
        } finally {
            if (client != null) {
                client.close();
            }
        }
    }
}

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

import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A data lake to write to.
 *
 * @author Armin Schnabel
 */
public final class PostgresConnection {

    /**
     * The logger used by objects of this class. Configure it using <tt>src/main/resources/logback.xml</tt>.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(PostgresConnection.class);

    /**
     * The JDBC database URL used to access the Mongo database to write to.
     */
    private final String url;
    /**
     * The name of the user to authenticate at the database.
     */
    private final String username;
    /**
     * The password of the user to authenticate at the database.
     */
    private final String password;

    /**
     * Creates a new completely initialized database source for one Postgres database instance.
     *
     * @param url The JDBC database URL used to access the Postgres database to write to.
     * @param username The name of the user to authenticate at the database
     * @param password The password of the user to authenticate at the database
     */
    public PostgresConnection(final String url, final String username, final String password) {
        Validate.notNull(url);
        Validate.notNull(username);
        Validate.notNull(password);

        this.url = url;
        this.username = username;
        this.password = password;
    }

    public void write(ArrayList<SourceDestinationRelation> relations, String tableName) throws SQLException {

        final var props = new Properties();
        props.setProperty("user", username);
        props.setProperty("password", password);

        try (var conn = DriverManager.getConnection(url, props)) {

            final var createTableQuery = "CREATE TABLE IF NOT EXISTS " + tableName + " (\n" +
                    "  id bigserial primary key,\n" +
                    "  sourceLat float NOT NULL,\n" +
                    "  sourceLon float NOT NULL,\n" +
                    "  destinationLat float NOT NULL,\n" +
                    "  destinationLon float NOT NULL,\n" +
                    "  lastActivity timestamptz NOT NULL,\n" +
                    "  sourceRequest timestamptz NOT NULL,\n" +
                    "  destinationRequest timestamptz NOT NULL,\n" +
                    "  plateNumber VARCHAR(7) NOT NULL,\n" +
                    "  sourceBattery SMALLINT NOT NULL,\n" +
                    "  destinationBattery SMALLINT NOT NULL,\n" +
                    "  sourceRange INT NOT NULL,\n" +
                    "  destinationRange INT NOT NULL,\n" +
                    "  sourceId VARCHAR(24) NOT NULL,\n" +
                    "  destinationId VARCHAR(24) NOT NULL\n" +
                    ")";
            final var createStatement = conn.prepareStatement(createTableQuery);
            createStatement.execute();

            final var query = "INSERT INTO " + tableName + "\n"
                    + "(sourceLat, sourceLon, destinationLat, destinationLon, lastActivity, sourceRequest, destinationRequest, plateNumber, sourceBattery, destinationBattery, sourceRange, destinationRange, sourceId, destinationId)\n"
                    + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
            final var statement = conn.prepareStatement(query);

            AtomicInteger count = new AtomicInteger();
            AtomicInteger inserted = new AtomicInteger();
            relations.forEach(relation -> {
                try {
                    statement.setDouble(1, relation.source.getLatitude());
                    statement.setDouble(2, relation.source.getLongitude());
                    statement.setDouble(3, relation.destination.getLatitude());
                    statement.setDouble(4, relation.destination.getLongitude());
                    statement.setTimestamp(5, new Timestamp(relation.destination.getLastActivityAt().getTime()));
                    statement.setTimestamp(6, new Timestamp(relation.source.getRequestTime().getTime()));
                    statement.setTimestamp(7, new Timestamp(relation.destination.getRequestTime().getTime()));
                    statement.setString(8, relation.source.getPlateNumber());
                    statement.setShort(9, (short) relation.source.getBatteryPercentage());
                    statement.setShort(10, (short) relation.destination.getBatteryPercentage());
                    statement.setInt(11, relation.source.getMeterRange());
                    statement.setInt(12, relation.destination.getMeterRange());
                    statement.setString(13, relation.source.getId().toString());
                    statement.setString(14, relation.destination.getId().toString());

                    statement.addBatch();
                    count.getAndIncrement();

                    // execute every 100 rows or less
                    if (count.get() % 100 == 0 || count.get() == relations.size()) {
                        final var affectedRows = statement.executeBatch();
                        final var expected = (count.get() - inserted.get());
                        if (affectedRows.length != expected) {
                            LOGGER.warn("Unexpected number of rows affected after insert: " + affectedRows.length
                                    + " instead of " + expected);
                        }
                        inserted.addAndGet(affectedRows.length);
                    }
                } catch (SQLException e) {
                    throw new IllegalStateException(e);
                }
            });
        }
    }
}

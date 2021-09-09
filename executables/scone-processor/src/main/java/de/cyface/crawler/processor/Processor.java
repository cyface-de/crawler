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
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.cyface.crawler.model.Record;

/**
 * Processes the vehicle records acquired by the API Crawler and extracts the source-destination relations.
 *
 * @author Armin Schnabel
 */
public class Processor {

    /**
     * The logger used by objects of this class. Configure it using <tt>src/main/resources/logback.xml</tt>.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(Processor.class);
    /**
     * The maximum number of minutes allowed between source and destination.
     * <p>
     * This values is suggested by VÖ and often used in literature as the longest drives are usually 2-3 hours.
     */
    private static final long MAX_CRAWLING_GAP_MINUTES = 120;
    /**
     * The minimum distance between source and destination.
     * <p>
     * This value is suggested by VÖ and often used in literature. Relations lower than this are considered:
     * - GPS noise, round-trips with similar source/destination or when passengers "push" vehicles around
     */
    private static final double MIN_DISTANCE_KM = 0.15;
    /**
     * The data source of the raw vehicle records to process.
     */
    private final MongoConnection mongoConnection;

    /**
     * @param mongoConnection The data source of the raw vehicle records to process.
     */
    public Processor(MongoConnection mongoConnection) {
        this.mongoConnection = mongoConnection;
    }

    /**
     * Executable to process the raw data.
     *
     * @param mongoCollection The data source of the raw vehicle records to process.
     * @return a list of source-destination relations for all vehicles (key = lastThree). Relations considered "invalid"
     *         where filtered from the results.
     */
    public Map<String, List<SourceDestinationRelation>> run(final String mongoCollection) {
        final var records = mongoConnection.records(mongoCollection);

        // Building pairs for all locations of one plate number (TS1,TS2), (TS2,TS3), etc.
        final var locationPairs = pair(records);

        // Check and filter invalid pairs
        final var result = new HashMap<String, List<SourceDestinationRelation>>();
        locationPairs.forEach((plate, pairs) -> {

            // Filter entries without location change
            final var withoutIdenticalLocations = pairs.stream()
                    .filter(r -> r.source.getLatitude() != r.destination.getLatitude()
                            && r.source.getLongitude() != r.destination.getLongitude())
                    .collect(Collectors.toList());

            // Filter vehicles which share the same `lastThree` plate number:
            // - The destination "last active" is earlier then the source "last active" time
            // - The same algorithm is used by VÖ (post-processing)
            final var duplicatePlates = withoutIdenticalLocations.stream()
                    .filter(p -> p.destination.getLastActivityAt().getTime() < p.source.getLastActivityAt().getTime())
                    .collect(Collectors.toList());

            // Ignore plate number completely (instead of just filtering pairs!)
            if (duplicatePlates.size() == 0) {

                // Filter when the travel time is unrealistically large
                final var withoutCrawlingGaps = withoutIdenticalLocations.stream()
                        .filter(p -> (p.destination.getRequestTime().getTime()
                                - p.source.getRequestTime().getTime()) <= MAX_CRAWLING_GAP_MINUTES * 1000 * 60)
                        .collect(Collectors.toList());

                // Filter relations which are too close to each other (GPS noise, round-trips, etc.)
                final var withoutCloseRelations = withoutCrawlingGaps.stream()
                        .filter(p -> distanceKm(p.source.getLatitude(), p.source.getLongitude(),
                                p.destination.getLatitude(), p.destination.getLongitude()) >= MIN_DISTANCE_KM)
                        .collect(Collectors.toList());

                /*
                 * We don't remove "maintenance drives"
                 * - this is done in post-processing (VÖ) as the "drives" are routed and with that things like
                 * disposition, battery range vs. traveled range discrepancies etc. can be identified better
                 *
                 * final var recharged = withoutCrawlingGaps.stream().filter(p -> p.destination.getBatteryPercentage()
                 * == 100).collect(Collectors.toList());
                 * final var batteryIncreased = withoutCrawlingGaps.stream().filter(p ->
                 * p.destination.getBatteryPercentage() > p.source.getBatteryPercentage()).collect(Collectors.toList());
                 * final var rangeDistanceNotEqual = withoutCrawlingGaps.stream().filter(p -> {
                 * final var meterRangeDecrease = p.source.getMeterRange() - p.destination.getMeterRange();
                 * final var metersTraveled = distanceKm(p.source.getLatitude(), p.source.getLongitude(),
                 * p.destination.getLatitude(), p.destination.getLongitude()) * 1000;
                 * final var diffBatteryDistance = metersTraveled - meterRangeDecrease;
                 * return diffBatteryDistance > 1000;
                 * }).collect(Collectors.toList());
                 *
                 * Identify vehicles which moved without lastActiveAt changes (potential service moves)
                 * - When the lastActiveAt did not change at all [never happens]
                 * final var serviceMoves = withoutCloseRelations.stream()
                 * .filter(p -> p.source.getLastActivityAt() == p.destination.getLastActivityAt())
                 * .collect(Collectors.toList());
                 * final var withoutServiceMoves = withoutCloseRelations.stream()
                 * .filter(p -> p.source.getLastActivityAt() != p.destination.getLastActivityAt())
                 * .collect(Collectors.toList());
                 */

                if (withoutCloseRelations.size() > 0) {
                    result.put(plate, withoutCloseRelations);
                }
            }
        });

        final var numberOfRelations = result.values().stream().mapToInt(List::size).sum();

        LOGGER.info(String.format("%d Source-Destination relations found from %d different plate numbers.",
                numberOfRelations, result.size()));
        return result;
    }

    /**
     * Creates pairs from an ordered list of records.
     *
     * @param plateRecords the ordered list of records of each vehicle ("lastThree" of the plate)
     * @return the pairs of each vehicle
     */
    private Map<String, List<SourceDestinationRelation>> pair(Map<String, List<Record>> plateRecords) {
        final var ret = new HashMap<String, List<SourceDestinationRelation>>();
        plateRecords.keySet().forEach(plate -> {
            final var records = plateRecords.get(plate);
            final var res = new ArrayList<SourceDestinationRelation>();
            // Start with seconds, collect pairs
            for (int i = 1; i < records.size(); i++) {
                res.add(new SourceDestinationRelation(records.get(i - 1), records.get(i)));
            }
            ret.put(plate, res);
        });
        return ret;
    }

    /**
     * Calculates the distance from this geo-locations to another one based on their latitude and longitude. This simple
     * formula assumes the earth is a perfect sphere. As the earth is a spheroid instead, the result can be inaccurate,
     * especially for longer distances.
     * <p>
     * Source: https://stackoverflow.com/a/27943/5815054
     *
     * @return the estimated distance between both locations in kilometers
     */
    private double distanceKm(double lat1, double lon1, double lat2, double lon2) {
        final int earthRadiusKm = 6371;
        final double latitudeDifferenceRad = degreeToRad(lat2 - lat1);
        final double longitudeDifferenceRad = degreeToRad(lon2 - lon1);
        final double a = Math.sin(latitudeDifferenceRad / 2) * Math.sin(latitudeDifferenceRad / 2) +
                Math.cos(degreeToRad(lat1)) * Math.cos(degreeToRad(lat2)) *
                        Math.sin(longitudeDifferenceRad / 2) * Math.sin(longitudeDifferenceRad / 2);
        final double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadiusKm * c;
    }

    /**
     * Converts a degree value to the "rad" unit.
     * <p>
     * Source: https://stackoverflow.com/a/27943/5815054
     *
     * @param degree the value to be converted in the degree unit
     * @return the value in the rad unit
     */
    private double degreeToRad(final double degree) {
        return degree * (Math.PI / 180);
    }
}

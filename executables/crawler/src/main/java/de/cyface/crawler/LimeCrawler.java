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

import java.io.IOException;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.apache.commons.lang3.Validate;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.cyface.crawler.model.BoundingBox;
import de.cyface.crawler.model.LimeVehicle;

/**
 * A crawler implementation for the e-scooter provider Lime.
 *
 * @author Armin Schnabel
 * @version 1.0.0
 * @since 1.0.0
 */
public class LimeCrawler implements Crawler {

    /**
     * The logger used by objects of this class. Configure it using <tt>src/main/resources/logback.xml</tt>.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(LimeCrawler.class);

    /**
     * {@code Null} to continue until no no vehicles are found or the number of requests.
     */
    private final int maxRequestsPerCrawl;

    /**
     * Milliseconds after which a new request can be sent.
     */
    private final int millisecondsBetweenRequests;

    /**
     * {@code true} to log processing results into CSV files.
     */
    private final boolean debugMode;

    /**
     * The API to be crawled.
     */
    private final LimeApi api;

    /**
     * Creates a fully initialized instance of this class.
     *
     * @param limeAuthToken required to crawl the Lime API
     * @param maxRequestsPerCrawl {@code Null} to continue until no no vehicles are found or the number of requests.
     * @param millisecondsBetweenRequests Milliseconds after which a new request can be sent.
     * @param debugMode {@code true} to log processing results into CSV files
     */
    public LimeCrawler(final String limeAuthToken, final int maxRequestsPerCrawl, final int millisecondsBetweenRequests,
            final boolean debugMode) {
        final var httpClient = HttpClient.newBuilder().build();
        this.api = new LimeApi(httpClient, limeAuthToken);
        this.maxRequestsPerCrawl = maxRequestsPerCrawl;
        this.debugMode = debugMode;
        this.millisecondsBetweenRequests = millisecondsBetweenRequests;
    }

    @Override
    public void crawl(MongoConnection mongoWriter) throws JSONException {
        LOGGER.info("Crawling LimeApi @ " + new Date());

        // Bounding box representing "green zone" of a sample city (+ 50 m)
        final var northEastLat = 51.090157213909116;
        final var northEastLon = 13.809081655279853;
        final var southWestLat = 51.02319889010608;
        final var southWestLon = 13.686292542430092;
        // Zoom 15+ returns `bikes`, zoom < 15 returns 401 with attributes.title "No nearby vehicles"
        final var zoom = (short)15;
        final var bb = new BoundingBox(0, northEastLat, northEastLon, southWestLat, southWestLon, zoom);

        try {
            scheduleCrawling(bb, mongoWriter);
        } catch (IOException e) {
            LOGGER.warn(e.getMessage()); // Continue on next scheduling event
            e.printStackTrace();
        }
    }

    /**
     * Starts crawling all vehicles in the defined region. Multiple requests will be sent until no more scooters are
     * found or until the request limits defined in the constructor are reached.
     *
     * @param initialRegion The region to scan for vehicles
     * @param mongoWriter The sink to write the vehicle locations into
     * @throws IOException When the program failed to write to the log file
     */
    private void scheduleCrawling(final BoundingBox initialRegion, final MongoConnection mongoWriter)
            throws IOException {

        final Date crawlStarted = new Date();
        final var plates = new HashSet<String>();
        final var vehicles = new HashSet<LimeVehicle>();
        final var requestCounter = new Integer[] {0};
        final var errorReceived = new Boolean[] {false};
        final var regions = new LinkedList<BoundingBox>();
        regions.add(initialRegion);
        final Path requestsFile = Paths.get(crawlStarted.getTime() + "_requests.csv");
        if (debugMode) {
            Files.writeString(requestsFile, "request,timestamp,lat,lon,found,parentFound,zoom,queue\n",
                    StandardOpenOption.CREATE);
        }

        // Start scheduler
        final var scheduler = Executors.newScheduledThreadPool(1);
        // Non-concurrent scheduling (subsequent starting late if previous still ongoing)
        // noinspection rawtypes
        final AtomicReference<ScheduledFuture> futureReference = new AtomicReference<>();
        final var exec = scheduler.scheduleAtFixedRate(
                () -> {
                    try {

                        // Stop requests when limit is reached or queue is empty
                        if (errorReceived[0] || regions.isEmpty() || requestCounter[0] >= maxRequestsPerCrawl) {
                            futureReference.get().cancel(false); // Or else the persisting could be canceled?

                            LOGGER.info("\n\n-------------- Done crawling, persisting data ... -------------");
                            if (debugMode) {
                                dumpToFile(plates, vehicles, crawlStarted);
                            }
                            mongoWriter.write(vehicles, "lime_records");
                            LOGGER.info("-------------- Data persisted. -------------\n\n");
                            return;
                        }

                        // Request
                        final var requestTime = new Date();
                        final var bb = regions.removeFirst();
                        final JSONArray result;
                        result = api.vehicles(bb, regions.size(), vehicles.size(), requestCounter, requestTime);
                        Validate.isTrue(result.length() == 50);

                        // Collect new vehicles
                        final var platesFound = plates(result);
                        final var newVehicles = plates.addAll(platesFound);
                        final var sizeBefore = vehicles.size();
                        if (newVehicles) {
                            vehicles.addAll(vehicles(result, requestTime, crawlStarted));
                        }
                        final var newFound = vehicles.size() - sizeBefore;
                        log(requestCounter, requestTime, bb, newFound, regions, requestsFile);

                        // Calculate sub-regions
                        if (newVehicles) {
                            // Tried multiple slicing strategies, the "slowest" seemed to be the best (small queue)
                            final var slices = bb.getZoom() % 2 == 1 ? new int[] {2, 1} : new int[] {1, 2};
                            LOGGER.info(newFound + " new found");
                            final var subRegions = subRegions(bb, newFound, slices[0], slices[1]);
                            regions.addAll(subRegions);
                        }

                    } catch (ApiUnavailable | IOException e) {
                        LOGGER.warn(e.getMessage()); // Continue on next scheduling event
                        e.printStackTrace();
                        errorReceived[0] = true;
                    }

                }, 0, millisecondsBetweenRequests, TimeUnit.MILLISECONDS);
        futureReference.set(exec);
    }

    /**
     * Write statistics into a log file for monitoring or debugging purposes.
     *
     * @param requestCounter The number of the request sent.
     * @param requestTime The time when the request was sent.
     * @param bb The bounding box which was requested at the API.
     * @param newFound The number of new vehicles found in the request.
     * @param regions The regions still in the queue for subsequent requests.
     * @param requestsFile The file to write the log to.
     * @throws IOException When the program failed to write to the log file.
     */
    private void log(final Integer[] requestCounter, final Date requestTime, final BoundingBox bb, final int newFound,
            final LinkedList<BoundingBox> regions, final Path requestsFile) throws IOException {

        if (debugMode) {
            final var builder = requestCounter[0] + "," + requestTime.getTime() +
                    "," + bb.getCenterLat() + "," + bb.getCenterLon() + "," + newFound +
                    "," + bb.getFoundByParent() + "," + bb.getZoom() + "," + regions.size() + "\n";
            Files.writeString(requestsFile, builder, StandardOpenOption.APPEND);
        }
    }

    /**
     * Extracts the plates of the vehicles found from the `bikes` part of the API response.
     *
     * @param bikes The `bikes` part of the API response.
     * @return The extracted plates {@code String}, together with the coordinates, to catch vehicles with identical
     *         "last three" plate numbers but different locations
     */
    private Set<String> plates(final JSONArray bikes) {

        return bikes.toList().stream().map(b -> {
            final var bike = ((HashMap<?, ?>)b);
            final var bikeAttributes = (HashMap<?, ?>)bike.get("attributes");
            // There are scooters with identical same plate_number (last three) so we also check the location
            return bikeAttributes.get("plate_number") + "," + bikeAttributes.get("latitude") + ","
                    + bikeAttributes.get("latitude");
        }).collect(Collectors.toSet());
    }

    /**
     * Reads the data returned from the API as {@link LimeVehicle} objects.
     *
     * @param bikes the `bikes` data returned from the API
     * @param requestTime the time at which the request was sent
     * @param crawlStarted the time at which the crawler was stated
     * @return the parsed data as POJOs
     */
    private Set<LimeVehicle> vehicles(final JSONArray bikes, final Date requestTime, final Date crawlStarted) {

        final var ret = new HashSet<LimeVehicle>();
        bikes.forEach(v -> ret.add(new LimeVehicle((JSONObject)v, requestTime, crawlStarted)));
        return ret;
    }

    /**
     * Slices a bounding box into a specific number of equal sized parts.
     *
     * @param bb The bounding box of the previous request to be sliced.
     * @param newFound The number of new vehicles found in the previous request (which is the reason why the previous
     *            region is sliced).
     * @param rows The number of rows to slice the region into
     * @param cols The number of columns to slice the region into
     * @return The subregions
     */
    List<BoundingBox> subRegions(final BoundingBox bb, final int newFound,
            @SuppressWarnings("SameParameterValue") final int rows,
            @SuppressWarnings("SameParameterValue") final int cols) {

        final var latDiff = bb.getNorthEastLat() - bb.getSouthWestLat();
        final var lonDiff = bb.getNorthEastLon() - bb.getSouthWestLon();
        final var latSlice = latDiff / rows;
        final var lonSlice = lonDiff / cols;
        final var nextZoom = (short)(bb.getZoom() + 1);

        final var res = new ArrayList<BoundingBox>();
        for (int row = 0; row < rows; ++row) {
            for (int col = 0; col < cols; ++col) {
                final var southWestLat = bb.getSouthWestLat() + row * latSlice;
                final var northEastLat = bb.getSouthWestLat() + (row + 1) * latSlice;
                final var southWestLon = bb.getSouthWestLon() + col * lonSlice;
                final var northEastLon = bb.getSouthWestLon() + (col + 1) * lonSlice;
                res.add(new BoundingBox(newFound, northEastLat, northEastLon, southWestLat, southWestLon, nextZoom));
            }
        }
        return res;
    }

    /**
     * Writes the current state of the crawl into a file for debugging.
     *
     * @param knownPlates The vehicle plates found so far.
     * @param knownVehicles The vehicles found so far.
     * @param crawlStarted The time when the crawl started.
     * @throws IOException If the file could not be written to.
     */
    private void dumpToFile(final Set<String> knownPlates, final Set<LimeVehicle> knownVehicles,
            final Date crawlStarted) throws IOException {

        final var platesFile = Paths.get(crawlStarted.getTime() + "_plates.csv");
        Files.createFile(platesFile);
        final var vehiclesFile = Paths.get(crawlStarted.getTime() + "_vehicles.csv");
        Files.createFile(vehiclesFile);

        // Dump plate_numbers state
        final var platesBuilder = new StringBuilder();
        platesBuilder.append("plate_number").append(",").append("lat").append(",").append("lon").append("\n");
        knownPlates.forEach(plate_number -> platesBuilder.append(plate_number).append("\n"));
        Files.writeString(platesFile, platesBuilder.toString(), StandardOpenOption.APPEND);

        // Dump vehicles state
        final var vehiclesBuilder = new StringBuilder();
        vehiclesBuilder.append("plate_number").append(",").append("lat").append(",").append("lon").append(",")
                .append("meterRange").append(",").append("status").append(",").append("lastActivityAt").append(",")
                .append("batteryLevel").append(",").append("typeName").append(",").append("requestTime")
                .append("\n");
        knownVehicles.forEach(v -> vehiclesBuilder.append(v.getPlateNumber()).append(",").append(v.getLatitude())
                .append(",").append(v.getLongitude()).append(",").append(v.getMeterRange()).append(",")
                .append(v.getStatus()).append(",").append(v.getLastActivityAt()).append(",").append(v.getBatteryLevel())
                .append(",").append(v.getTypeName()).append(",").append(v.getRequestTime().toString()).append("\n"));
        Files.writeString(vehiclesFile, vehiclesBuilder.toString(), StandardOpenOption.APPEND);
    }
}

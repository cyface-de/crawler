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
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;

import org.apache.commons.lang3.Validate;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.cyface.crawler.model.BoundingBox;

/**
 * Represents the Lime API which can be crawled for vehicle records.
 *
 * @author Armin Schnabel
 * @version 1.0.0
 */
public class LimeApi {

    /**
     * The logger used by objects of this class. Configure it using <tt>src/main/resources/logback.xml</tt>.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(LimeApi.class);

    /**
     * The uniform resource identifier of the endpoint providing the Lime-API to query.
     */
    private static final String URI_STRING = "https://web-production.lime.bike/api/rider/v1/views/map";

    /**
     * This token has to be manually requested via REST and SMS. It's user-dependent.
     */
    private final String authToken;

    /**
     * Client used to send HTTP requests to a server running a Lime API.
     */
    private final HttpClient httpClient;

    /**
     * Constructs a fully initialized instance of this class.
     *
     * @param httpClient Client used to send HTTP requests to a server running a Lime API.
     * @param limeAuthToken This token has to be manually requested via REST and SMS. It's user-dependent.
     */
    public LimeApi(final HttpClient httpClient, final String limeAuthToken) {
        this.httpClient = httpClient;
        this.authToken = limeAuthToken;
    }

    /**
     * Queries the API for vehicle locations in a specific bounding box.
     *
     * @param bb The bounding box to search vehicles for.
     * @param regionCount The number of regions in the queue for subsequent API requests.
     * @param vehicleCount The number of vehicles found so far.
     * @param requestCounter The number of requests sent so far.
     * @param requestTime The time when this request was initiated.
     * @return The `bikes` part of the response from the API request.
     * @throws ApiUnavailable If the API is not available.
     * @throws JSONException If the program failed to write to the log file.
     */
    public JSONArray vehicles(final BoundingBox bb, final int regionCount, final int vehicleCount,
            Integer[] requestCounter, final Date requestTime) throws ApiUnavailable, JSONException {

        // API Request
        requestCounter[0]++;
        final var query = query(requestTime, requestCounter, vehicleCount, bb, regionCount);
        final var attributes = sendRequest(query);

        // API Result
        return attributes.getJSONArray("bikes");
    }

    /**
     * Creates the query for the API request.
     *
     * @param requestTime The time when this request was initiated.
     * @param requestCounter The number of requests sent so far.
     * @param vehicleCount The number of vehicles found so far.
     * @param bb The bounding box to search vehicles for.
     * @param regionCount The number of regions in the queue for subsequent API requests.
     * @return The query parameters starting with `?`
     */
    private String query(Date requestTime, Integer[] requestCounter, final int vehicleCount, BoundingBox bb,
            final int regionCount) {
        LOGGER.info(requestTime.toString() + " request " + requestCounter[0] + ": "
                + vehicleCount + " knownBefore [parent: " + bb.getFoundByParent() + "], zoom " + bb.getZoom()
                + ", queue: " + regionCount + ", centerLat: " + bb.getCenterLat() + ", centerLon: "
                + bb.getCenterLon());

        return String.format(
                "?ne_lat=%f&ne_lng=%f&sw_lat=%f&sw_lng=%f&user_latitude=%f&user_longitude=%f&zoom=%d",
                bb.getNorthEastLat(), bb.getNorthEastLon(), bb.getSouthWestLat(), bb.getSouthWestLon(),
                bb.getCenterLat(), bb.getCenterLon(), bb.getZoom());
    }

    /**
     * Sends the API requests.
     *
     * @param query the query to send.
     * @return The `data.attributes` part of the API response.
     * @throws ApiUnavailable If the API is not available.
     */
    private JSONObject sendRequest(final String query) throws ApiUnavailable {
        try {
            final var request = HttpRequest.newBuilder(URI.create(URI_STRING + query))
                    .timeout(Duration.ofSeconds(30))
                    .header("authorization", String.format("Bearer %s", authToken))
                    .GET().build();
            final var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            Validate.notNull(response);
            if (response.statusCode() == 200) {
                final var responseBody = new JSONObject(response.body());

                // Validate response
                final var attributes = responseBody.getJSONObject("data").getJSONObject("attributes");
                Validate.isTrue(attributes.getString("current_level").equals("block"), String.format(
                        "Zoom levels below 15 were not supported anymore, not expecting city levels.\nRequest: %s.\nResponse.attributes: %s",
                        query, attributes));
                Validate.isTrue(attributes.get("bikes") != null, "Error: " + attributes.getString("title"));

                return attributes;
            } else {
                throw new ApiUnavailable(
                        String.format("LimeApi request returned wrong HTTP status code. Expected 200! received %d",
                                response.statusCode()));
            }
        } catch (IOException | InterruptedException e) {
            throw new ApiUnavailable(e);
        }
    }
}

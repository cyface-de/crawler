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

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import de.cyface.crawler.model.TierVehicle;
import org.apache.commons.lang3.Validate;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class TierApi {
    /**
     * The uniform resource identifier of the endpoint providing the TIER-API to query.
     */
    private static final String URI_STRING = "https://platform.tier-services.io/v2/vehicle";

    /**
     * This token is hard-coded into the TIER apk and is user-independent.
     */
    private final String apiKey;

    /**
     * Client used to send HTTP requests to a server running a TIER API.
     */
    private final HttpClient httpClient;

    public TierApi(HttpClient httpClient, String tierApiKey) {
        this.httpClient = httpClient;
        this.apiKey = tierApiKey;
    }

    public Set<TierVehicle> vehicles(final String zoneId) throws ApiUnavailable, JSONException {
        final var query = String.format("?zoneId=%s", zoneId);
        final var responseBody = sendRequest(query);
        return vehicles(responseBody.getJSONArray("data"));
    }

    private Set<TierVehicle> vehicles(JSONArray vehicles) {
        final var ret = new HashSet<TierVehicle>();
        vehicles.forEach(v -> ret.add(new TierVehicle((JSONObject)v)));
        return ret;
    }

    private JSONObject sendRequest(final String query) throws ApiUnavailable {
        try {
            final var request = HttpRequest.newBuilder(URI.create(URI_STRING + query))
                    .header("X-Api-Key", apiKey)
                    .GET().build();
            final var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            Validate.notNull(response);
            if (response.statusCode() == 200) {
                final var responseBody = response.body();
                return new JSONObject(responseBody);
            } else {
                throw new ApiUnavailable(
                        String.format("TierApi request returned wrong HTTP status code. Expected 200! received %d",
                                response.statusCode()));
            }
        } catch (IOException | InterruptedException e) {
            throw new ApiUnavailable(e);
        }
    }
}

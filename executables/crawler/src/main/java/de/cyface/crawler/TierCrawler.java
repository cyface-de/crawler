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

import java.net.http.HttpClient;

import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A crawler implementation for the e-scooter provider Tier.
 * <p>
 * <b>Attention: As the provider Tier was not available anymore in the region of concern at the time of this project,
 * this implementation was not completed and abandoned.</b>
 *
 * @author Armin Schnabel
 * @version 1.0.0
 * @since 1.0.0
 */
@Deprecated
public class TierCrawler implements Crawler {

    /**
     * The logger used by objects of this class. Configure it using <tt>src/main/resources/logback.xml</tt>.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(TierCrawler.class);

    /**
     * The API to be crawled.
     */
    private final TierApi api;

    /**
     * Creates a fully initialized instance of this class.
     *
     * @param tierApiKey required to crawl the Tier API
     */
    public TierCrawler(final String tierApiKey) {
        final var httpClient = HttpClient.newBuilder().build();
        this.api = new TierApi(httpClient, tierApiKey);
    }

    @Override
    public void crawl(MongoConnection mongoWriter) throws JSONException {
        LOGGER.info("Crawling TierApi ...");
        try {
            final var vehicles = api.vehicles("DRESDEN");
        } catch (ApiUnavailable apiUnavailable) {
            LOGGER.warn(apiUnavailable.getMessage());
        }
        // TODO: Persist data - postponed until TIER is back in our sample region
        LOGGER.warn("THIS CRAWLER IS NOT YET FULLY IMPLEMENTED");
    }
}

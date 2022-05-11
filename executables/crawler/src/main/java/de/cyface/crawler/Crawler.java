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

import org.json.JSONException;

/**
 * The interface for crawler implementations for different e-scooter providers.
 *
 * @author Armin Schnabel
 * @since 1.0.0
 * @version 1.0.0
 */
public interface Crawler {

    /**
     * Starts a crawling session which sends multiple requests to the API to search for all vehicle locations.
     *
     * @param mongoWriter The sink to write the found locations to.
     * @throws JSONException If the API response cannot be parsed.
     */
    void crawl(final MongoConnection mongoWriter) throws JSONException;
}

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

/**
 * An <code>Exception</code> thrown if a request to an API was not successful.
 */
public class ApiUnavailable extends Exception {
    /**
     * Creates a new completely initialized object of this class, providing a detailed error message.
     *
     * @param message The detailed description of the error causing the API to not answer at the moment
     */
    public ApiUnavailable(final String message) {
        super(message);
    }

    /**
     * Creates a new completely initialized object of this class, as a wrapper for the actual cause of the API not being
     * available.
     *
     * @param cause The <code>Exception</code> which has caused
     */
    public ApiUnavailable(final Exception cause) {
        super(cause);
    }
}

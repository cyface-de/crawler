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
package de.cyface.crawler.processor;

import de.cyface.crawler.model.Record;

/**
 * A class which represents a source-destination relation, i.e. two subsequent {@link Record}s recorded for a vehicle
 * which might represent a ride.
 *
 * @author Armin Schnabel
 * @since 1.0.0
 * @version 1.0.0
 */
public class SourceDestinationRelation {

    /**
     * The earlier record.
     */
    public final Record source;
    /**
     * The later record.
     */
    public final Record destination;

    /**
     * Constructs a fully initialized instance of this class.
     *
     * @param source The earlier record
     * @param destination The later record
     */
    public SourceDestinationRelation(final Record source, final Record destination) {
        this.source = source;
        this.destination = destination;
    }
}

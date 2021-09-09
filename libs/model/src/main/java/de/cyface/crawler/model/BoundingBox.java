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
package de.cyface.crawler.model;

import java.util.Objects;

import org.apache.commons.lang3.Validate;

/**
 * Defines a rectangular region.
 *
 * @author Armin Schnabel
 */
public final class BoundingBox {

    /**
     * number of new vehicles found by "parent" bounding box.
     * <p>
     * Helps to prioritize the sub-requests.
     */
    private final int foundByParent;
    /**
     * of the bounding box - which is ignored in favor of `zoom` (returns more/less bikes)
     */
    private final double northEastLat;
    /**
     * of the bounding box - which is ignored in favor of `zoom` (returns more/less bikes)
     */
    private final double northEastLon;
    /**
     * of the bounding box - which is ignored in favor of `zoom` (returns more/less bikes)
     */
    private final double southWestLat;
    /**
     * of the bounding box - which is ignored in favor of `zoom` (returns more/less bikes)
     */
    private final double southWestLon;
    /**
     * defines the middle of the bounding box
     */
    private final double centerLat;
    /**
     * defines the middle of the bounding box
     */
    private final double centerLon;
    /**
     * Zoom level to be used when querying for this bounding box.
     * <p>
     * Below 10 `clusters` and `bikes` are `null`.
     * From 10 to 14 `bikes` are clustered.
     * Above 14 `bikes` are returned.
     * <p>
     * 2021-03-18: zoom levels below 15 return 401 with attributes.title "No nearby vehicles"
     */
    private final short zoom;

    /**
     * Creates a fully initialized instance of this class.
     *
     * @param foundByParent number of new vehicles found by "parent" bounding box.
     * @param northEastLat of the bounding box - which is ignored in favor of `zoom` (returns more/less bikes)
     * @param northEastLon of the bounding box - which is ignored in favor of `zoom` (returns more/less bikes)
     * @param southWestLat of the bounding box - which is ignored in favor of `zoom` (returns more/less bikes)
     * @param southWestLon of the bounding box - which is ignored in favor of `zoom` (returns more/less bikes)
     * @param zoom Zoom level to be used when querying for this bounding box.
     */
    public BoundingBox(final int foundByParent, final double northEastLat, final double northEastLon,
            final double southWestLat, final double southWestLon, final short zoom) {
        this.foundByParent = foundByParent;
        this.northEastLat = northEastLat;
        this.northEastLon = northEastLon;
        this.southWestLat = southWestLat;
        this.southWestLon = southWestLon;
        this.centerLat = middleBetween(southWestLat, northEastLat);
        this.centerLon = middleBetween(southWestLon, northEastLon);
        this.zoom = zoom;
    }

    /**
     * @param number1 one number
     * @param number2 another number
     * @return the middle between both numbers
     */
    public static double middleBetween(double number1, double number2) {
        final var smaller = Math.min(number1, number2);
        final var larger = Math.max(number1, number2);
        Validate.isTrue(smaller != larger);
        return smaller + (larger - smaller) / 2;
    }

    /**
     * @return number of new vehicles found by "parent" bounding box.
     */
    public int getFoundByParent() {
        return foundByParent;
    }

    /**
     * @return of the bounding box - which is ignored in favor of `zoom` (returns more/less bikes)
     */
    public double getNorthEastLat() {
        return northEastLat;
    }

    /**
     * @return of the bounding box - which is ignored in favor of `zoom` (returns more/less bikes)
     */
    public double getNorthEastLon() {
        return northEastLon;
    }

    /**
     * @return of the bounding box - which is ignored in favor of `zoom` (returns more/less bikes)
     */
    public double getSouthWestLat() {
        return southWestLat;
    }

    /**
     * @return of the bounding box - which is ignored in favor of `zoom` (returns more/less bikes)
     */
    public double getSouthWestLon() {
        return southWestLon;
    }

    /**
     * @return defines the middle of the bounding box
     */
    public double getCenterLat() {
        return centerLat;
    }

    /**
     * @return defines the middle of the bounding box
     */
    public double getCenterLon() {
        return centerLon;
    }

    /**
     * @return Zoom level to be used when querying for this bounding box.
     */
    public short getZoom() {
        return zoom;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        BoundingBox that = (BoundingBox)o;
        return foundByParent == that.foundByParent && Double.compare(that.northEastLat, northEastLat) == 0
                && Double.compare(that.northEastLon, northEastLon) == 0
                && Double.compare(that.southWestLat, southWestLat) == 0
                && Double.compare(that.southWestLon, southWestLon) == 0
                && Double.compare(that.centerLat, centerLat) == 0 && Double.compare(that.centerLon, centerLon) == 0
                && zoom == that.zoom;
    }

    @Override
    public int hashCode() {
        return Objects.hash(foundByParent, northEastLat, northEastLon, southWestLat, southWestLon, centerLat,
                centerLon, zoom);
    }

    @Override
    public String toString() {
        return "BoundingBox{"
                + "foundByParent=" + foundByParent
                + ", northEastLat=" + northEastLat
                + ", northEastLon=" + northEastLon
                + ", southWestLat=" + southWestLat
                + ", southWestLon=" + southWestLon
                + ", centerLat=" + centerLat
                + ", centerLon=" + centerLon
                + ", zoom=" + zoom
                + '}';
    }
}

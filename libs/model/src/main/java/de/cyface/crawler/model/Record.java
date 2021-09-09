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

import java.util.Date;

import org.bson.types.ObjectId;

/**
 * The for us relevant part of the information returned by the Lime API for one vehicle at a given time.
 *
 * @author Armin Schnabel
 */
public final class Record {

    /**
     * the {@code ObjectId} of the {@code Record}'s entry in the mongoDB containing the raw data for debugging
     */
    private final ObjectId id;
    /**
     * the last three letters of the plate number
     */
    private final String lastThree;
    /**
     * of the vehicle
     */
    private final double latitude;
    /**
     * of the vehicle
     */
    private final double longitude;
    /**
     * last time the vehicle was active - whatever that means
     */
    private final Date lastActivityAt;
    /**
     * when the API request was sent which returned this vehicle record
     */
    private final Date requestTime;
    /**
     * how far the vehicle can travel with the current battery level
     */
    private final int meterRange;
    /**
     * percentage of the battery charge left
     */
    private final int batteryPercentage;
    /**
     * when the scheduler started the crawling which includes the request which returned this vehicle record
     */
    private final Date crawlingStarted;
    /**
     * the plate number of the vehicle
     */
    private final String plateNumber;

    /**
     * @param id the {@code ObjectId} of the {@code Record}'s entry in the mongoDB containing the raw data for debugging
     * @param lastThree the last three letters of the plate number
     * @param latitude of the vehicle
     * @param longitude of the vehicle
     * @param lastActivityAt last time the vehicle was active - whatever that means
     * @param requestTime when the API request was sent which returned this vehicle record
     * @param meterRange how far the vehicle can travel with the current battery level
     * @param crawlingStarted when the scheduler started the crawling which includes the request which returned this
     *            vehicle record
     * @param batteryPercentage percentage of the battery charge left
     * @param plateNumber the plate number of the vehicle
     */
    public Record(ObjectId id, String lastThree, double latitude, double longitude, Date lastActivityAt,
            Date requestTime,
            int meterRange, Date crawlingStarted, int batteryPercentage, String plateNumber) {
        this.id = id;
        this.lastThree = lastThree;
        this.latitude = latitude;
        this.longitude = longitude;
        this.lastActivityAt = lastActivityAt;
        this.requestTime = requestTime;
        this.meterRange = meterRange;
        this.crawlingStarted = crawlingStarted;
        this.batteryPercentage = batteryPercentage;
        this.plateNumber = plateNumber;
    }

    /**
     * @return the last three letters of the plate number
     */
    @SuppressWarnings("unused")
    public String getLastThree() {
        return lastThree;
    }

    /**
     * @return of the vehicle
     */
    @SuppressWarnings("unused")
    public double getLatitude() {
        return latitude;
    }

    /**
     * @return of the vehicle
     */
    @SuppressWarnings("unused")
    public double getLongitude() {
        return longitude;
    }

    /**
     * @return last time the vehicle was active - whatever that means
     */
    @SuppressWarnings("unused")
    public Date getLastActivityAt() {
        return lastActivityAt;
    }

    /**
     * @return when the API request was sent which returned this vehicle record
     */
    @SuppressWarnings("unused")
    public Date getRequestTime() {
        return requestTime;
    }

    /**
     * @return how far the vehicle can travel with the current battery level
     */
    @SuppressWarnings("unused")
    public int getMeterRange() {
        return meterRange;
    }

    /**
     * @return percentage of the battery charge left
     */
    @SuppressWarnings("unused")
    public int getBatteryPercentage() {
        return batteryPercentage;
    }

    /**
     * @return when the scheduler started the crawling which includes the request which returned this vehicle record
     */
    @SuppressWarnings("unused")
    public Date getCrawlingStarted() {
        return crawlingStarted;
    }

    /**
     * @return the {@code ObjectId} of the {@code Record}'s entry in the mongoDB containing the raw data for debugging
     */
    @SuppressWarnings("unused")
    public ObjectId getId() {
        return id;
    }

    /**
     * @return the plate number of the vehicle
     */
    @SuppressWarnings("unused")
    public String getPlateNumber() {
        return plateNumber;
    }

    @Override
    public String toString() {
        return "Record{" +
                "id=" + id +
                ", lastThree='" + lastThree + '\'' +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", lastActivityAt=" + lastActivityAt +
                ", requestTime=" + requestTime +
                ", meterRange=" + meterRange +
                ", batteryPercentage=" + batteryPercentage +
                ", crawlingStarted=" + crawlingStarted +
                ", plateNumber='" + plateNumber + '\'' +
                '}';
    }
}

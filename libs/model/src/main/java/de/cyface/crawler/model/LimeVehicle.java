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

import java.time.OffsetDateTime;
import java.util.Date;
import java.util.Objects;

import org.apache.commons.lang3.Validate;
import org.bson.Document;
import org.json.JSONObject;

/**
 * Vehicle which are returned by the Lime API.
 *
 * @author Armin Schnabel
 */
public final class LimeVehicle {

    /**
     * <b>Attention</b>: Is encoded. It cannot be used to identify the same vehicle as it changes between requests.
     */
    private final String id;
    /**
     * of the vehicle
     */
    private final String type;
    /**
     * of the vehicle
     */
    private final String generation;
    /**
     * {@code true} if the battery can be changed easily
     */
    private final boolean swappableBattery;
    /**
     * the vehicle type
     */
    private final String typeName;
    /**
     * something like "high", "low", etc.
     */
    private final String batteryLevel;
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
     * how far the vehicle can travel with the current battery level
     */
    private final int meterRange;
    /**
     * last time the vehicle was active - whatever that means
     */
    private final Date lastActivityAt;
    /**
     * of the vehicle. Currently, only the {@code lastThree} letters are shown, everything else is like XXX-
     */
    private final String plateNumber;
    /**
     * percentage of the battery charge left
     */
    private final int batteryPercentage;
    /**
     * of the vehicle
     */
    private final String brand;
    /**
     * of the vehicle
     */
    private final String status;
    /**
     * when the API request was sent which returned this vehicle record
     */
    private final Date requestTime;
    /**
     * when the scheduler started the crawling which includes the request which returned this vehicle record
     */
    private final Date crawlingStarted;

    /**
     * @param vehicle the record returned by the API
     * @param requestTime when the API request was sent which returned this vehicle record
     * @param crawlStarted when the scheduler started the crawling which includes the request which returned this
     *            vehicle record
     */
    public LimeVehicle(JSONObject vehicle, Date requestTime, Date crawlStarted) {
        Validate.isTrue(vehicle.has("id"));
        Validate.isTrue(vehicle.has("type"));
        Validate.isTrue(vehicle.has("attributes"));
        this.id = vehicle.getString("id");
        this.type = vehicle.getString("type");
        final var attributes = vehicle.getJSONObject("attributes");
        this.generation = attributes.getString("generation");
        this.swappableBattery = attributes.getBoolean("swappable_battery");
        this.typeName = attributes.getString("type_name");
        this.batteryLevel = attributes.getString("battery_level");
        this.lastThree = attributes.getString("last_three");
        this.latitude = attributes.getDouble("latitude");
        this.longitude = attributes.getDouble("longitude");
        this.meterRange = attributes.getInt("meter_range");
        final var lastActivityAtString = attributes.getString("last_activity_at");
        final var offsetDateTime = OffsetDateTime.parse(lastActivityAtString);
        final var instant = offsetDateTime.toInstant();
        this.lastActivityAt = Date.from(instant);
        this.plateNumber = attributes.getString("plate_number");
        this.batteryPercentage = attributes.getInt("battery_percentage");
        this.brand = attributes.getString("brand");
        this.status = attributes.getString("status");
        this.requestTime = requestTime;
        this.crawlingStarted = crawlStarted;
    }

    /**
     * @return this vehicle as a {@code Document} which can be inserted into a mongo db
     */
    public Document toBson() {
        return new Document("id", id)
                .append("type", type)
                .append("generation", generation)
                .append("swappable_battery", swappableBattery)
                .append("type_name", typeName)
                .append("battery_level", batteryLevel)
                .append("last_three", lastThree)
                .append("latitude", latitude)
                .append("longitude", longitude)
                .append("meter_range", meterRange)
                .append("last_activity_at", lastActivityAt)
                .append("plate_number", plateNumber)
                .append("battery_percentage", batteryPercentage)
                .append("brand", brand)
                .append("status", status)
                .append("request_time", requestTime)
                .append("crawling_started", crawlingStarted);
    }

    /**
     * @return <b>Attention</b>: Is encoded. It cannot be used to identify the same vehicle as it changes between
     *         requests.
     */
    @SuppressWarnings("unused")
    public String getId() {
        return id;
    }

    /**
     * @return of the vehicle
     */
    @SuppressWarnings("unused")
    public String getType() {
        return type;
    }

    /**
     * @return of the vehicle
     */
    @SuppressWarnings("unused")
    public String getGeneration() {
        return generation;
    }

    /**
     * @return {@code true} if the battery can be changed easily
     */
    @SuppressWarnings("unused")
    public boolean isSwappableBattery() {
        return swappableBattery;
    }

    /**
     * @return the vehicle type
     */
    public String getTypeName() {
        return typeName;
    }

    /**
     * @return something like "high", "low", etc.
     */
    public String getBatteryLevel() {
        return batteryLevel;
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
    public double getLatitude() {
        return latitude;
    }

    /**
     * @return of the vehicle
     */
    public double getLongitude() {
        return longitude;
    }

    /**
     * @return how far the vehicle can travel with the current battery level
     */
    public int getMeterRange() {
        return meterRange;
    }

    /**
     * @return last time the vehicle was active - whatever that means
     */
    public Date getLastActivityAt() {
        return lastActivityAt;
    }

    /**
     * @return of the vehicle. Currently only the {@code lastThree} letters are shown, everything else is like XXX-
     */
    public String getPlateNumber() {
        return plateNumber;
    }

    /**
     * @return percentage of the battery charge left
     */
    @SuppressWarnings("unused")
    public int getBatteryPercentage() {
        return batteryPercentage;
    }

    /**
     * @return of the vehicle
     */
    @SuppressWarnings("unused")
    public String getBrand() {
        return brand;
    }

    /**
     * @return of the vehicle
     */
    public String getStatus() {
        return status;
    }

    /**
     * @return when the API request was sent which returned this vehicle record
     */
    public Date getRequestTime() {
        return requestTime;
    }

    /**
     * @return when the scheduler started the crawling which includes the request which returned this vehicle record
     */
    @SuppressWarnings("unused")
    public Date getCrawlingStarted() {
        return crawlingStarted;
    }

    /**
     * @return an unique hash if the {@code lastThree} and coordinates combo is unique. The reason for this is that we
     *         want to collect vehicles with the same lastThree but different coordinates as there are vehicles with the
     *         same lastThree.
     */
    @Override
    public int hashCode() {
        return Objects.hash(lastThree, latitude, longitude);
    }

    /**
     * We explicitly don't care about different request times or else we collect duplicate
     * vehicle records without any actually important parameters changed (e.g. power, location, etc.)
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        LimeVehicle that = (LimeVehicle)o;
        return swappableBattery == that.swappableBattery && Double.compare(that.latitude, latitude) == 0
                && Double.compare(that.longitude, longitude) == 0 && meterRange == that.meterRange
                && batteryPercentage == that.batteryPercentage && Objects.equals(type, that.type)
                && Objects.equals(generation, that.generation) && Objects.equals(typeName, that.typeName)
                && Objects.equals(batteryLevel, that.batteryLevel) && Objects.equals(lastThree, that.lastThree)
                && Objects.equals(lastActivityAt, that.lastActivityAt) && Objects.equals(plateNumber, that.plateNumber)
                && Objects.equals(brand, that.brand) && Objects.equals(status, that.status);
    }
}
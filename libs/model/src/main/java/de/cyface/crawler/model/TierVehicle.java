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

import org.apache.commons.lang3.Validate;
import org.json.JSONObject;

/**
 * Vehicle which are returned by the Tier API.
 *
 * @author Armin Schnabel
 */
public class TierVehicle {

    /**
     * of the vehicle
     */
    private final String id;
    /**
     * of the vehicle
     */
    private final String type;
    /**
     * of the vehicle
     */
    private final String lastStateChange;
    /**
     * of the vehicle
     */
    private final int code;
    /**
     * of the vehicle
     */
    private final double lat;
    /**
     * of the vehicle
     */
    private final double lng;
    /**
     * of the vehicle
     */
    private final boolean isRentable;
    /**
     * of the vehicle
     */
    private final String licencePlate;
    /**
     * of the vehicle
     */
    private final boolean hasHelmetBox;
    /**
     * of the vehicle
     */
    private final int maxSpeed;
    /**
     * of the vehicle
     */
    private final boolean hasHelmet;
    /**
     * of the vehicle
     */
    private final String zoneId;
    /**
     * of the vehicle
     */
    private final String state;
    /**
     * of the vehicle
     */
    private final String iotVendor;
    /**
     * of the vehicle
     */
    private final String vehicleType;
    /**
     * of the vehicle
     */
    private final String lastLocationUpdate;
    /**
     * of the vehicle
     */
    private final int batteryLevel;

    /**
     * @param vehicle the record returned from the API
     */
    public TierVehicle(JSONObject vehicle) {
        Validate.isTrue(vehicle.has("id"));
        Validate.isTrue(vehicle.has("type"));
        Validate.isTrue(vehicle.has("attributes"));
        this.id = vehicle.getString("id");
        this.type = vehicle.getString("type");
        final var attributes = vehicle.getJSONObject("attributes");
        this.lastStateChange = attributes.getString("lastStateChange");
        this.code = attributes.getInt("code");
        this.lat = attributes.getDouble("lat");
        this.lng = attributes.getDouble("lng");
        this.isRentable = attributes.getBoolean("isRentable");
        this.licencePlate = attributes.getString("licencePlate");
        this.hasHelmetBox = attributes.getBoolean("hasHelmetBox");
        this.maxSpeed = attributes.getInt("maxSpeed");
        this.hasHelmet = attributes.getBoolean("hasHelmet");
        this.zoneId = attributes.getString("zoneId");
        this.state = attributes.getString("state");
        this.iotVendor = attributes.getString("iotVendor");
        this.vehicleType = attributes.getString("vehicleType");
        this.lastLocationUpdate = attributes.getString("lastLocationUpdate");
        this.batteryLevel = attributes.getInt("batteryLevel");
    }

    /**
     * @return of the vehicle
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
    public String getLastStateChange() {
        return lastStateChange;
    }

    /**
     * @return of the vehicle
     */
    @SuppressWarnings("unused")
    public int getCode() {
        return code;
    }

    /**
     * @return of the vehicle
     */
    @SuppressWarnings("unused")
    public double getLat() {
        return lat;
    }

    /**
     * @return of the vehicle
     */
    @SuppressWarnings("unused")
    public double getLng() {
        return lng;
    }

    /**
     * @return of the vehicle
     */
    @SuppressWarnings("unused")
    public boolean isRentable() {
        return isRentable;
    }

    /**
     * @return of the vehicle
     */
    @SuppressWarnings("unused")
    public String getLicencePlate() {
        return licencePlate;
    }

    /**
     * @return of the vehicle
     */
    @SuppressWarnings("unused")
    public boolean isHasHelmetBox() {
        return hasHelmetBox;
    }

    /**
     * @return of the vehicle
     */
    @SuppressWarnings("unused")
    public int getMaxSpeed() {
        return maxSpeed;
    }

    /**
     * @return of the vehicle
     */
    @SuppressWarnings("unused")
    public boolean isHasHelmet() {
        return hasHelmet;
    }

    /**
     * @return of the vehicle
     */
    @SuppressWarnings("unused")
    public String getZoneId() {
        return zoneId;
    }

    /**
     * @return of the vehicle
     */
    @SuppressWarnings("unused")
    public String getState() {
        return state;
    }

    /**
     * @return of the vehicle
     */
    @SuppressWarnings("unused")
    public String getIotVendor() {
        return iotVendor;
    }

    /**
     * @return of the vehicle
     */
    @SuppressWarnings("unused")
    public String getVehicleType() {
        return vehicleType;
    }

    /**
     * @return of the vehicle
     */
    @SuppressWarnings("unused")
    public String getLastLocationUpdate() {
        return lastLocationUpdate;
    }

    /**
     * @return of the vehicle
     */
    @SuppressWarnings("unused")
    public int getBatteryLevel() {
        return batteryLevel;
    }
}
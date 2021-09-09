#!/bin/bash
# Copyright 2021 Cyface GmbH
#
# This file is part of the Cyface Crawler.
#
#  The Cyface Crawler is free software: you can redistribute it and/or modify
#  it under the terms of the GNU General Public License as published by
#  the Free Software Foundation, either version 3 of the License, or
#  (at your option) any later version.
#
#  The Cyface Crawler is distributed in the hope that it will be useful,
#  but WITHOUT ANY WARRANTY; without even the implied warranty of
#  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
#  GNU General Public License for more details.
#
#  You should have received a copy of the GNU General Public License
#  along with the Cyface Crawler.  If not, see <http://www.gnu.org/licenses/>.
#
# Version 1.0.0

echo "Running Scone Processor"

echo "Waiting for Databases to start!"

# POSTGRES DB
POSTGRES_STATUS="not running"
POSTGRES_COUNTER=0
while [ $POSTGRES_COUNTER -lt 10 ] && [ "$POSTGRES_STATUS" = "not running" ]; do
    ((POSTGRES_COUNTER++))
    echo "Try $POSTGRES_COUNTER"

    if (nc -z postgres 5432); then
	      echo "Postgres Database is up!"
        POSTGRES_STATUS="running"
    else
        sleep 5s
    fi
done

if [ "$POSTGRES_COUNTER" -ge 10 ]; then
    echo "Unable to find Postgres Database! Processor will not start!"
    exit 1
fi

# MONGO DB
MONGO_STATUS="not running"
COUNTER=0
while [ $COUNTER -lt 10 ] && [ "$MONGO_STATUS" = "not running" ]; do
    ((COUNTER++))
    echo "Try $COUNTER"

    if (nc -z mongo-data 27017); then
	      echo "Mongo Database is up!"
        MONGO_STATUS="running"
    else
        sleep 5s
    fi
done

if [ "$COUNTER" -ge 10 ]; then
    echo "Unable to find Mongo Database! Processor will not start!"
    exit 1
fi

# PROCESSOR
echo "Starting Scone Processor"
java -jar scone-processor-all.jar &> /logs/scone-processor-out.log

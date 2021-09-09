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

if [[ -z $LIME_API_TOKEN ]]; then
	echo "No Lime API Token provided! Starting aborted."
	exit 1
fi

MBR_PARAMETER=""
if [[ -n $MILLISECONDS_BETWEEN_REQUESTS ]]; then
	echo "'Milliseconds between requests' parameter provided: $MILLISECONDS_BETWEEN_REQUESTS"
	MBR_PARAMETER=" -mbr $MILLISECONDS_BETWEEN_REQUESTS "
fi

MRH_PARAMETER=""
if [[ -n $MAX_REQUESTS_PER_HOUR ]]; then
	echo "'Max requests per hour' parameter provided: $MAX_REQUESTS_PER_HOUR"
	MRH_PARAMETER=" -mrh $MAX_REQUESTS_PER_HOUR "
fi

MRC_PARAMETER=""
if [[ -n $MAX_REQUESTS_PER_CRAWL ]]; then
	echo "'Max requests per crawl' parameter provided: $MAX_REQUESTS_PER_CRAWL"
	MRC_PARAMETER=" -mrc $MAX_REQUESTS_PER_CRAWL "
fi

CN_PARAMETER=""
if [[ -n $CRAWLER_NUMBER ]]; then
	echo "'Crawler number' parameter provided: $CRAWLER_NUMBER"
	CN_PARAMETER=" -cn $CRAWLER_NUMBER "
fi

NOC_PARAMETER=""
if [[ -n $NUMBER_OF_CRAWLERS ]]; then
	echo "'Number of Crawlers' parameter provided: $NUMBER_OF_CRAWLERS"
	NOC_PARAMETER=" -noc $NUMBER_OF_CRAWLERS "
fi

echo "Running Crawler"

echo "Waiting for Database to start!"

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
    echo "Unable to find Mongo Database! Crawler will not start!"
    exit 1
fi

echo "Starting Crawler"
java -jar crawler-all.jar -lt "$LIME_API_TOKEN" "$MBR_PARAMETER" "$MRH_PARAMETER" "$MRC_PARAMETER" "$CN_PARAMETER" "$NOC_PARAMETER" &> /logs/crawler-out.log

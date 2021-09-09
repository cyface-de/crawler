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

import static de.cyface.crawler.model.BoundingBox.middleBetween;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import de.cyface.crawler.model.BoundingBox;

@ExtendWith(MockitoExtension.class)
public class LimeCrawlerTest {

    @Test
    public void testSubRegions() {
        // Arrange
        final var oocut = new LimeCrawler("MOCK_TOKEN", 10, 100, false);
        final var northEastLat = 51.090157213909116;
        final var northEastLon = 13.809081655279853;
        final var southWestLat = 51.02319889010608;
        final var southWestLon = 13.686292542430092;
        final var zoom = (short)15;
        final var bb = new BoundingBox(0, northEastLat, northEastLon, southWestLat, southWestLon, zoom);

        // Act
        final var res = oocut.subRegions(bb, 0, 2, 2);

        // Assert
        final var centerLon = middleBetween(bb.getNorthEastLon(), bb.getSouthWestLon());
        final var centerLat = middleBetween(bb.getSouthWestLat(), bb.getNorthEastLat());
        final var nextZoom = (short)(bb.getZoom() + 1);
        final var northWest = new BoundingBox(bb.getFoundByParent(), bb.getNorthEastLat(), centerLon, centerLat,
                bb.getSouthWestLon(), nextZoom);
        final var northEast = new BoundingBox(bb.getFoundByParent(), bb.getNorthEastLat(), bb.getNorthEastLon(),
                centerLat, centerLon, nextZoom);
        final var southWest = new BoundingBox(bb.getFoundByParent(), centerLat, centerLon, bb.getSouthWestLat(),
                bb.getSouthWestLon(), nextZoom);
        final var southEast = new BoundingBox(bb.getFoundByParent(), centerLat, bb.getNorthEastLon(),
                bb.getSouthWestLat(), centerLon, nextZoom);
        final var expected = new BoundingBox[] {southWest, southEast, northWest, northEast};
        assertThat(res, is(equalTo(Arrays.asList(expected.clone()))));
    }
}

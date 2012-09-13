/**
 * Copyright (C) 2010-2012 Patrick Decat
 *
 * TrafficInfoGrabber is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * TrafficInfoGrabber is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with TrafficInfoGrabber.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.decat.tig.web;

public class WebviewSettings {

    public final String title;
    public final String url;
    public final int xmin;
    public final int ymin;
    public final int xmax;
    public final int ymax;

    public WebviewSettings(String title, String url, int xmin, int ymin, int xmax, int ymax) {
        this.title = title;
        this.url = url;
        this.xmin = xmin;
        this.ymin = ymin;
        this.xmax = xmax;
        this.ymax = ymax;
    }
}
package org.decat.tig.web;

/*
 * #%L
 * TrafficInfoGrabber
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2010 - 2012 Patrick Decat
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

public class WebviewSettings {

	public final String title;
	public final String url;
	public final int xmin;
	public final int ymin;
	public final int xmax;
	public final int ymax;
	public final boolean zoomControls;
	public final boolean scrollbar;

	public WebviewSettings(String title, String url, int xmin, int ymin, int xmax, int ymax, boolean zoomControls, boolean scrollbar) {
		this.title = title;
		this.url = url;
		this.xmin = xmin;
		this.ymin = ymin;
		this.xmax = xmax;
		this.ymax = ymax;
		this.zoomControls = zoomControls;
		this.scrollbar = scrollbar;
	}

	public WebviewSettings(String title, String url, int xmin, int ymin, int xmax, int ymax) {
		this(title, url, xmin, ymin, xmax, ymax, true, true);
	}
}
package org.decat.qs;

/*
 **
 **       Copyright (C) 2010-2011 Patrick Decat
 ** 
 **       This file is part of QuickSleep.
 **
 **   QuickSleep is free software: you can redistribute it and/or modify
 **   it under the terms of the GNU General Public License as published by
 **   the Free Software Foundation, either version 3 of the License, or
 **   (at your option) any later version.
 **
 **   QuickSleep is distributed in the hope that it will be useful,
 **   but WITHOUT ANY WARRANTY; without even the implied warranty of
 **   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 **   GNU General Public License for more details.
 **
 **   You should have received a copy of the GNU General Public License
 **   along with QuickSleep.  If not, see <http://www.gnu.org/licenses/>.
 **
 */

import android.app.admin.DeviceAdminReceiver;
import android.content.Context;
import android.content.Intent;

public class QuickSleepDeviceAdminReceiver extends DeviceAdminReceiver {
	@Override
	public void onEnabled(Context context, Intent intent) {
		QuickSleep.showToast(context, "Admin priviledges granted.");
	}
}

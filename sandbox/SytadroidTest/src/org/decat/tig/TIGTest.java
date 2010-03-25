package org.decat.tig;

import java.text.SimpleDateFormat;

//@TestTargetClass(value = null)
public class TIGTest {

	public static void testLastModifiedFormat() {
		SimpleDateFormat sdt = new SimpleDateFormat();
		String lastModified = "Tue, 16 Mar 2010 11:06:55 GMT";
		lastModified = sdt.format(lastModified);
	}
}

/**
 * Aptana Studio
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the GNU Public License (GPL) v3 (with exceptions).
 * Please see the license.html included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */

package com.aptana.preview;

import java.net.URL;

/**
 * @author Max Stepanov
 * 
 */
public final class PreviewConfig {

	private URL url;

	/**
	 * 
	 */
	public PreviewConfig(URL url) {
		this.url = url;
	}

	/**
	 * @return the url
	 */
	public URL getURL() {
		return url;
	}

}

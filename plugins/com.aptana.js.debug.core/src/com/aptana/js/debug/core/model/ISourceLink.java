/**
 * Aptana Studio
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the GNU Public License (GPL) v3 (with exceptions).
 * Please see the license.html included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.aptana.js.debug.core.model;

import java.net.URI;

import org.eclipse.debug.core.model.IDebugElement;

/**
 * @author Max Stepanov
 */
public interface ISourceLink extends IDebugElement {

	/**
	 * Source location (path or URL)
	 * 
	 * @return String
	 */
	URI getLocation();

}

/**
 * Aptana Studio
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the GNU Public License (GPL) v3 (with exceptions).
 * Please see the license.html included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.aptana.debug.core;

import java.net.URL;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;

/**
 * @author Max Stepanov
 */
public interface IActiveResourcePathGetterAdapter {
	/**
	 * getActiveResource
	 * 
	 * @return IResource
	 */
	IResource getActiveResource();

	/**
	 * getActiveResourcePath
	 * 
	 * @return IPath
	 */
	IPath getActiveResourcePath();

	/**
	 * getActiveResourceURL
	 * 
	 * @return URL
	 */
	URL getActiveResourceURL();
}

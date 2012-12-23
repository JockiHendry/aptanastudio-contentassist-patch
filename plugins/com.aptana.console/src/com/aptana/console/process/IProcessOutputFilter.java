/**
 * Aptana Studio
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the GNU Public License (GPL) v3 (with exceptions).
 * Please see the license.html included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */

package com.aptana.console.process;

/**
 * @author Max Stepanov
 *
 */
public interface IProcessOutputFilter {

	/**
	 * Returns the filtered line or null is the whole line should be filtered out
	 * @param output
	 * @return
	 */
	public String filter(String line);
}

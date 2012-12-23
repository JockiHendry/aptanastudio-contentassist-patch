/**
 * Aptana Studio
 * Copyright (c) 2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the GNU Public License (GPL) v3 (with exceptions).
 * Please see the license.html included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.aptana.projects.primary.natures;

import org.eclipse.core.runtime.IPath;

/**
 * 
 * @author pinnamuri
 *
 */
public interface IPrimaryNatureContributor
{
	/**
	 * Indicates not an eligible one for being Primary
	 */
	public int NOT_PRIMARY = 0;
	
	/**
	 * Indicates eligible for being primary
	 */
	public int CAN_BE_PRIMARY = 1;
	
	/**
	 * Indicates it has to be primary nature for the given project
	 */
	public int IS_PRIMARY = 2;
	
	/**
	 * Gets the primary nature rank based on the project type,
	 * current perspective and may be, other conditions.
	 * 
	 * @param projectPath
	 * @return
	 */
	public int getPrimaryNatureRank (IPath projectPath);
}

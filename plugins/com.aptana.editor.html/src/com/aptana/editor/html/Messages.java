/**
 * Aptana Studio
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the GNU Public License (GPL) v3 (with exceptions).
 * Please see the license.html included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.aptana.editor.html;

import org.eclipse.osgi.util.NLS;

/**
 * @author klindsey
 *
 */
public class Messages extends NLS
{
	private static final String BUNDLE_NAME = "com.aptana.editor.html.messages"; //$NON-NLS-1$
	public static String HTMLMetadataLoader_Loading_Metadata;
	static
	{
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages()
	{
	}
}

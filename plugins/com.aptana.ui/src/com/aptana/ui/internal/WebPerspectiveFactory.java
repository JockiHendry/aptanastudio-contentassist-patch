/**
 * Aptana Studio
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the GNU Public License (GPL) v3 (with exceptions).
 * Please see the license.html included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.aptana.ui.internal;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;
import org.eclipse.ui.console.IConsoleConstants;

import com.jocki.test.JavaScriptEvalView;

public class WebPerspectiveFactory implements IPerspectiveFactory
{

	public static final String ID = "com.aptana.ui.WebPerspective"; //$NON-NLS-1$

	/**
	 * NOTE: Update this when the perspective layout changes
	 */
	public static final int VERSION = 103;

	private static final String APP_EXPLORER_ID = "com.aptana.explorer.view"; //$NON-NLS-1$

	public void createInitialLayout(IPageLayout layout)
	{
		// Get the editor area
		String editorArea = layout.getEditorArea();

		// Left
		IFolderLayout left = layout.createFolder("left", IPageLayout.LEFT, 0.20f, editorArea); //$NON-NLS-1$
		left.addView(APP_EXPLORER_ID);

		// Bottom right: Console. Had to leave this programmatic to get the Console appear in bottom right
		IFolderLayout bottomArea = layout.createFolder("terminalArea", IPageLayout.BOTTOM, 0.75f, //$NON-NLS-1$
				editorArea);
		bottomArea.addView(IConsoleConstants.ID_CONSOLE_VIEW);
		
		// Oleh Jocki
		bottomArea.addView(JavaScriptEvalView.VIEW_ID);
	}
}

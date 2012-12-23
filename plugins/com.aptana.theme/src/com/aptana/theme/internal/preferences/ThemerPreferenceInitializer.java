/**
 * Aptana Studio
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the GNU Public License (GPL) v3 (with exceptions).
 * Please see the license.html included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.aptana.theme.internal.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;

import com.aptana.core.util.EclipseUtil;
import com.aptana.theme.ThemePlugin;
import com.aptana.theme.preferences.IPreferenceConstants;

public class ThemerPreferenceInitializer extends AbstractPreferenceInitializer
{

	public static final String DEFAULT_THEME = "Aptana Studio"; //$NON-NLS-1$

	@Override
	public void initializeDefaultPreferences()
	{
		IEclipsePreferences node = EclipseUtil.defaultScope().getNode(ThemePlugin.PLUGIN_ID);
		node.put(IPreferenceConstants.ACTIVE_THEME, DEFAULT_THEME);
		// Make "invasive theming" the default for our standalones.
		boolean isStandalone = (EclipseUtil.isStandalone() || EclipseUtil
				.getPluginVersion("com.appcelerator.titanium.rcp") != null); //$NON-NLS-1$

		node.putBoolean(IPreferenceConstants.APPLY_TO_ALL_VIEWS, isStandalone);
		node.putBoolean(IPreferenceConstants.APPLY_TO_ALL_EDITORS, isStandalone);
	}
}

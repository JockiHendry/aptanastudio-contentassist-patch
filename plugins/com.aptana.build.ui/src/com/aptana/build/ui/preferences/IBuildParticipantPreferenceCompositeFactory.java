/**
 * Aptana Studio
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the GNU Public License (GPL) v3 (with exceptions).
 * Please see the license.html included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.aptana.build.ui.preferences;

import org.eclipse.swt.widgets.Composite;

import com.aptana.core.build.IBuildParticipantWorkingCopy;

public interface IBuildParticipantPreferenceCompositeFactory
{

	public Composite createPreferenceComposite(Composite parent, IBuildParticipantWorkingCopy participant);

}
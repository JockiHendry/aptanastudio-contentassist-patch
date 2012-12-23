/**
 * Aptana Studio
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the GNU Public License (GPL) v3 (with exceptions).
 * Please see the license.html included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.aptana.ide.syncing.ui.handlers;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;

import com.aptana.ide.core.io.preferences.CloakingUtils;
import com.aptana.ide.syncing.ui.actions.UncloakAction;

public class UncloakHandler extends BaseCloakHandler
{

	public Object execute(ExecutionEvent event) throws ExecutionException
	{
		UncloakAction action = new UncloakAction();
		action.setActivePart(null, HandlerUtil.getActivePart(event));
		action.setSelection(new StructuredSelection(getSelectedFiles()));
		action.run(null);

		return null;
	}

	@Override
	public boolean isEnabled()
	{
		IFileStore[] selectedFiles = getSelectedFiles();
		for (IFileStore fileStore : selectedFiles)
		{
			if (CloakingUtils.isFileCloaked(fileStore))
			{
				return true;
			}
		}
		return false;
	}
}

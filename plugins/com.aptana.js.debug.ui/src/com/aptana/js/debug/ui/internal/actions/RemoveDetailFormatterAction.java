/**
 * Aptana Studio
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the GNU Public License (GPL) v3 (with exceptions).
 * Please see the license.html included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.aptana.js.debug.ui.internal.actions;

import org.eclipse.debug.core.DebugException;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.IStructuredSelection;

import com.aptana.core.logging.IdeLog;
import com.aptana.debug.core.DebugOptionsManager;
import com.aptana.debug.core.DetailFormatter;
import com.aptana.js.debug.core.JSDebugPlugin;
import com.aptana.js.debug.core.model.IJSVariable;
import com.aptana.js.debug.ui.JSDebugUIPlugin;

/**
 * @author Max Stepanov
 */
public class RemoveDetailFormatterAction extends ObjectActionDelegate {
	/**
	 * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
	 */
	public void run(IAction action) {
		IStructuredSelection selection = getCurrentSelection();
		if (selection == null || selection.size() != 1) {
			return;
		}
		Object element = selection.getFirstElement();
		String typeName;
		try {
			if (element instanceof IJSVariable) {
				typeName = ((IJSVariable) element).getReferenceTypeName();
			} else {
				return;
			}
		} catch (DebugException e) {
			IdeLog.logError(JSDebugUIPlugin.getDefault(), e);
			return;
		}
		DebugOptionsManager detailFormattersManager = JSDebugPlugin.getDefault().getDebugOptionsManager();
		DetailFormatter detailFormatter = detailFormattersManager.getAssociatedDetailFormatter(typeName);
		detailFormattersManager.removeAssociatedDetailFormatter(detailFormatter);
		refreshCurrentSelection();
	}
}

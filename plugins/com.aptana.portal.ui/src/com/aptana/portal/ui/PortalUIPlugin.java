/**
 * Aptana Studio
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the GNU Public License (GPL) v3 (with exceptions).
 * Please see the license.html included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.aptana.portal.ui;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import com.aptana.core.resources.IProjectContext;
import com.aptana.explorer.ExplorerPlugin;
import com.aptana.explorer.IExplorerUIConstants;
import com.aptana.explorer.IPreferenceConstants;

/**
 * The activator class controls the plug-in life cycle
 */
public class PortalUIPlugin extends AbstractUIPlugin
{

	// The plug-in ID
	public static final String PLUGIN_ID = "com.aptana.portal.ui"; //$NON-NLS-1$

	// The browser Portal ID
	public static final String PORTAL_ID = "com.aptana.portal.main"; //$NON-NLS-1$

	/**
	 * Ruby image key
	 */
	public static final String RUBY_IMAGE = "/icons/wizban/ruby.png"; //$NON-NLS-1$
	public static final String XAMPP_IMAGE = "/icons/wizban/xampp.png"; //$NON-NLS-1$
	public static final String JS_IMAGE = "/icons/wizban/js.png"; //$NON-NLS-1$
	public static final String PYTHON_IMAGE = "/icons/wizban/python.png"; //$NON-NLS-1$

	// The shared instance
	private static PortalUIPlugin plugin;

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception
	{
		super.start(context);
		plugin = this;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception
	{
		plugin = null;
		super.stop(context);
	}

	public static BundleContext getContext()
	{
		return getDefault().getBundle().getBundleContext();
	}

	/**
	 * Returns the shared instance
	 * 
	 * @return the shared instance
	 */
	public static PortalUIPlugin getDefault()
	{
		return plugin;
	}

	/**
	 * Try to resolve and return the last active project in the App Explorer.
	 * 
	 * @return The active IProject. Can be null if not resolved.
	 */
	public static IProject getActiveProject()
	{
		// FIXME: Shalom - This is a modified of a code taken from the com.aptana.explorer plugin. Change this code to
		// use a more generic solution for the active project problem once it's implemented.

		// First try and get the active project for the instance of the App Explorer open in the active window
		final IProject[] projects = new IProject[1];
		PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable()
		{
			public void run()
			{
				IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
				if (window == null)
				{
					return;
				}
				IWorkbenchPage page = window.getActivePage();
				if (page == null)
				{
					return;
				}
				// First, check the view references.
				IViewReference[] refs = page.getViewReferences();
				if (refs == null)
				{
					return;
				}
				for (IViewReference ref : refs)
				{
					if (ref == null || !ref.getId().equals(IExplorerUIConstants.VIEW_ID))
					{
						continue;
					}
					IProjectContext view = (IProjectContext) ref.getPart(false);
					if (view == null)
					{
						continue;
					}
					IProject activeProject = view.getActiveProject();
					if (activeProject != null)
					{
						projects[0] = activeProject;
						return;
					}
				}
				// If we got to this point, we could not find the SingleProjectView and its active project.
				// Try to find the a project by the active editor.
				IEditorPart activeEditor = page.getActiveEditor();
				if (activeEditor != null)
				{
					IResource resource = null;
					if (activeEditor.getEditorInput().getPersistable() != null)
					{
						// it's probably a non-browser editor.
						resource = (IResource) activeEditor.getEditorInput().getAdapter(IResource.class);
					}
					else
					{
						// look for the first persistable editor input we can find.
						IEditorReference[] editorReferences = page.getEditorReferences();
						for (IEditorReference reference : editorReferences)
						{
							IEditorPart editor = reference.getEditor(false);
							if (editor != null)
							{
								resource = (IResource) editor.getEditorInput().getAdapter(IResource.class);
								if (resource != null)
								{
									break;
								}
							}
						}
					}
					if (resource != null)
					{
						projects[0] = ((IResource) resource).getProject();
						return;
					}
				}
			}
		});
		if (projects[0] != null)
		{
			return projects[0];
		}

		// Fall back to using project stored in prefs.
		IPreferencesService preferencesService = Platform.getPreferencesService();
		String activeProjectName = preferencesService.getString(ExplorerPlugin.PLUGIN_ID,
				IPreferenceConstants.ACTIVE_PROJECT, null, null);
		IProject result = null;

		if (activeProjectName != null)
		{
			result = ResourcesPlugin.getWorkspace().getRoot().getProject(activeProjectName);
		}

		return result;
	}

	@Override
	protected void initializeImageRegistry(ImageRegistry reg)
	{
		reg.put(RUBY_IMAGE, imageDescriptorFromPlugin(PLUGIN_ID, RUBY_IMAGE));
		reg.put(XAMPP_IMAGE, imageDescriptorFromPlugin(PLUGIN_ID, XAMPP_IMAGE));
		reg.put(JS_IMAGE, imageDescriptorFromPlugin(PLUGIN_ID, JS_IMAGE));
		reg.put(PYTHON_IMAGE, imageDescriptorFromPlugin(PLUGIN_ID, PYTHON_IMAGE));
	}
}

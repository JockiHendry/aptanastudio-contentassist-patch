/**
 * Aptana Studio
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the GNU Public License (GPL) v3 (with exceptions).
 * Please see the license.html included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.aptana.core;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent;
import org.osgi.framework.BundleContext;

import com.aptana.core.internal.UserAgentManager;
import com.aptana.core.logging.IdeLog;
import com.aptana.core.util.EclipseUtil;
import com.aptana.core.util.IOUtil;
import com.eaio.uuid.MACAddress;

/**
 * The activator class controls the plug-in life cycle
 */
public class CorePlugin extends Plugin implements IPreferenceChangeListener
{

	// The plug-in ID
	public static final String PLUGIN_ID = "com.aptana.core"; //$NON-NLS-1$

	// The machine id
	private static final String MID_SEPARATOR = "-"; //$NON-NLS-1$
	private static String mid;

	// The shared instance
	private static CorePlugin plugin;

	private BundleContext context;

	private UserAgentManager fUserAgentManager;

	/**
	 * The constructor
	 */
	public CorePlugin()
	{
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.core.runtime.Plugins#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception
	{
		this.context = context;
		super.start(context);

		plugin = this;

		Job job = new Job("Enable debugging and flush log cache") //$NON-NLS-1$
		{
			@Override
			protected IStatus run(IProgressMonitor monitor)
			{
				// Perhaps don't enable this if platform is already in -debug mode?
				//
				// Place after context & plugin assignments, as this relies on both existing already
				enableDebugging();
				IdeLog.flushCache();
				return Status.OK_STATUS;
			}
		};
		// DO NOT CALL EclipseUtil.setSystemForJob!!! It breaks startup by causing plugin loading issues in
		// resources.core plugin
		job.setSystem(true);
		job.schedule();
	}

	/**
	 * Enable the debugging options
	 */
	private void enableDebugging()
	{
		EclipseUtil.instanceScope().getNode(CorePlugin.PLUGIN_ID).addPreferenceChangeListener(this);

		// Returns the current severity preference
		IdeLog.StatusLevel currentSeverity = IdeLog.getSeverityPreference();
		IdeLog.setCurrentSeverity(currentSeverity);

		// If we are currently in debug mode, don't change the default settings
		if (!Platform.inDebugMode())
		{
			Boolean checked = Platform.getPreferencesService().getBoolean(CorePlugin.PLUGIN_ID,
					ICorePreferenceConstants.PREF_ENABLE_COMPONENT_DEBUGGING, false, null);
			EclipseUtil.setPlatformDebugging(checked);
			if (checked)
			{
				String[] components = EclipseUtil.getCurrentDebuggableComponents();
				EclipseUtil.setBundleDebugOptions(components, true);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.core.runtime.Plugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception
	{
		try
		{
			// Don't listen to debug changes anymore
			EclipseUtil.instanceScope().getNode(CorePlugin.PLUGIN_ID).removePreferenceChangeListener(this);

			if (fUserAgentManager != null)
			{
				fUserAgentManager = null;
			}
		}
		finally
		{
			plugin = null;
			super.stop(context);
		}
	}

	/**
	 * Returns the shared instance
	 * 
	 * @return the shared instance
	 */
	public static CorePlugin getDefault()
	{
		return plugin;
	}

	/**
	 * @return
	 * @deprecated uses {@link EclipseUtil#getStudioVersion()} instead
	 */
	public static String getAptanaStudioVersion()
	{
		return EclipseUtil.getStudioVersion();
	}

	/**
	 * Returns the current bundle context
	 * 
	 * @return
	 */
	public BundleContext getContext()
	{
		// FIXME Can't we just call getBundle().getBundleContext()?
		return context;
	}

	/**
	 * Respond to a preference change event
	 */
	public void preferenceChange(PreferenceChangeEvent event)
	{
		if (ICorePreferenceConstants.PREF_DEBUG_LEVEL.equals(event.getKey()))
		{
			IdeLog.setCurrentSeverity(IdeLog.getSeverityPreference());
		}
	}

	public static String getMID()
	{
		// we don't synchronize because it's no big deal if we generate mid multiple times on initial access (by
		// multiple threads, if it happens). Should always be same.
		if (mid == null)
		{
			mid = generateMID();
		}
		return mid;
	}

	private static String generateMID()
	{
		try
		{
			MessageDigest md = MessageDigest.getInstance("MD5"); //$NON-NLS-1$
			byte[] result = md.digest(MACAddress.getMACAddress().getBytes(IOUtil.UTF_8));
			Formatter formatter = new Formatter();
			for (byte b : result)
			{
				formatter.format("%02x", b); //$NON-NLS-1$
			}
			// puts mid in 8-4-4-4-12 format
			String value = formatter.toString();
			StringBuilder buildMe = new StringBuilder();
			buildMe.append(value.substring(0, 8));
			buildMe.append(MID_SEPARATOR);
			buildMe.append(value.substring(8, 12));
			buildMe.append(MID_SEPARATOR);
			buildMe.append(value.substring(12, 16));
			buildMe.append(MID_SEPARATOR);
			buildMe.append(value.substring(16, 20));
			buildMe.append(MID_SEPARATOR);
			buildMe.append(value.substring(20, 32));
			return buildMe.toString();
		}
		catch (NoSuchAlgorithmException e)
		{
			IdeLog.logError(getDefault(), Messages.CorePlugin_MD5_generation_error, e);
		}
		catch (UnsupportedEncodingException e)
		{
			IdeLog.logError(getDefault(), e);
		}
		return null;
	}

	public synchronized IUserAgentManager getUserAgentManager()
	{
		if (fUserAgentManager == null)
		{
			fUserAgentManager = new UserAgentManager();
		}
		return fUserAgentManager;
	}
}

/**
 * Aptana Studio
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the GNU Public License (GPL) v3 (with exceptions).
 * Please see the license.html included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */

package com.aptana.core;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.osgi.service.prefs.BackingStoreException;

import com.aptana.core.logging.IdeLog;
import com.aptana.core.util.EclipseUtil;
import com.aptana.core.util.ExecutableUtil;
import com.aptana.core.util.PlatformUtil;
import com.aptana.core.util.ProcessUtil;
import com.aptana.core.util.ResourceUtil;
import com.aptana.core.util.StringUtil;

/**
 * @author Max Stepanov
 */
public final class ShellExecutable
{

	private static final String APTANA_VERSION = "APTANA_VERSION"; //$NON-NLS-1$
	private static final String BASH_ENV = "BASH_ENV"; //$NON-NLS-1$

	private static final String[] POSSIBLE_SHELL_LOCATIONS_WIN32 = new String[] { "%PROGRAMW6432%\\Git\\bin", //$NON-NLS-1$
			"%PROGRAMFILES%\\Git\\bin", //$NON-NLS-1$
			"%PROGRAMFILES(X86)%\\Git\\bin", //$NON-NLS-1$ 
			"C:\\RailsInstaller\\Git\\bin" //$NON-NLS-1$ // Default install location of RailsInstaller's Git
	};

	private static final String[] ENV_FILTER = new String[] { "_", //$NON-NLS-1$
			BASH_ENV, "TMP", //$NON-NLS-1$
			"APP_ICON*", //$NON-NLS-1$
			"JAVA_MAIN_CLASS*", //$NON-NLS-1$
			"JAVA_STARTED_ON_FIRST_THREAD*" //$NON-NLS-1$
	};

	public static final String PATH_SEPARATOR = ":"; //$NON-NLS-1$

	private static final String SH_EXE = "sh.exe"; //$NON-NLS-1$
	private static final String BASH = "bash"; //$NON-NLS-1$
	private static final String RCFILE = "$os$/.aptanarc"; //$NON-NLS-1$

	private static boolean initializing = false;
	private static IPath shellPath = null;
	private static IPath shellRCPath = null;
	private static Map<String, String> shellEnvironment;
	private static Map<IPath, Map<String, String>> workingDirToEnvCache = new HashMap<IPath, Map<String, String>>();

	/**
	 * 
	 */
	private ShellExecutable()
	{
	}

	public static synchronized IPath getPath() throws CoreException
	{
		if (shellPath == null)
		{
			// Avoid infinite loops here. If we're trying to find the shell path
			// recursively, return null.
			// Chicken-and-egg problem with ExecutableUtil.find() asking for
			// environment from shell.
			if (initializing)
			{
				return null;
			}
			boolean isWin32 = Platform.OS_WIN32.equals(Platform.getOS());
			try
			{
				initializing = true;
				shellPath = getPreferenceShellPath();
				if (shellPath == null)
				{
					shellPath = ExecutableUtil.find(isWin32 ? SH_EXE : BASH, false, getPossibleShellLocations());
				}
			}
			finally
			{
				initializing = false;
			}
			if (shellPath == null)
			{
				throw new CoreException(new Status(Status.ERROR, CorePlugin.PLUGIN_ID,
						"Shell executable could not be found.")); //$NON-NLS-1$
			}
		}
		return shellPath;
	}

	private static List<IPath> getPossibleShellLocations()
	{
		if (Platform.OS_WIN32.equals(Platform.getOS()))
		{
			List<IPath> list = new ArrayList<IPath>();
			for (String location : POSSIBLE_SHELL_LOCATIONS_WIN32)
			{
				IPath path = Path.fromOSString(PlatformUtil.expandEnvironmentStrings(location));
				if (path.toFile().isDirectory())
				{
					list.add(path);
				}
			}
			return list;
		}
		return null;
	}

	public static synchronized IPath getShellRCPath()
	{
		if (shellRCPath == null)
		{
			URL url = FileLocator.find(CorePlugin.getDefault().getBundle(), Path.fromPortableString(RCFILE), null);
			if (url != null)
			{
				File file = ResourceUtil.resourcePathToFile(url);
				if (file != null && file.exists())
				{
					shellRCPath = Path.fromOSString(file.getAbsolutePath());
				}
			}
		}
		return shellRCPath;
	}

	private static IPath getPreferenceShellPath()
	{
		String pref = EclipseUtil.instanceScope().getNode(CorePlugin.PLUGIN_ID)
				.get(ICorePreferenceConstants.PREF_SHELL_EXECUTABLE_PATH, null);
		if (pref != null && !StringUtil.isEmpty(pref))
		{
			IPath path = Path.fromOSString(pref);
			if (path.toFile().isDirectory())
			{
				boolean isWin32 = Platform.OS_WIN32.equals(Platform.getOS());
				path = path.append(isWin32 ? SH_EXE : BASH);
			}
			if (ExecutableUtil.isExecutable(path))
			{
				return path;
			}
			IdeLog.logWarning(CorePlugin.getDefault(), "Shell executable path preference point to an invalid location"); //$NON-NLS-1$
		}
		return null;
	}

	public static void setPreferenceShellPath(IPath path)
	{
		IEclipsePreferences prefs = EclipseUtil.instanceScope().getNode(CorePlugin.PLUGIN_ID);
		if (path != null)
		{
			prefs.put(ICorePreferenceConstants.PREF_SHELL_EXECUTABLE_PATH, path.toOSString());
		}
		else
		{
			prefs.remove(ICorePreferenceConstants.PREF_SHELL_EXECUTABLE_PATH);
		}
		try
		{
			prefs.flush();
		}
		catch (BackingStoreException e)
		{
			IdeLog.logError(CorePlugin.getDefault(), "Saving preferences failed.", e); //$NON-NLS-1$
		}
		shellPath = null;
		shellEnvironment = null;
	}

	public synchronized static Map<String, String> getEnvironment()
	{
		if (shellEnvironment == null)
		{
			// If we haven't set up a shell yet, return Java's env for now.
			try
			{
				// Force detection of shell. Must have one before we try "env"
				IPath shellPath = getPath();
				if (shellPath == null)
				{
					return System.getenv();
				}
				shellEnvironment = getEnvironment(null);
			}
			catch (CoreException e)
			{
				return System.getenv();
			}
		}
		return shellEnvironment;
	}

	public synchronized static Map<String, String> getEnvironment(IPath workingDirectory)
	{
		if (workingDirectory == null && shellEnvironment != null)
		{
			return shellEnvironment;
		}

		if (!workingDirToEnvCache.containsKey(workingDirectory))
		{
			// Do we have a shell to run "env" in?
			IPath shellPath = null;
			try
			{
				// Force detection of shell. Must have one before we try "env"
				shellPath = getPath();
			}
			catch (CoreException e)
			{
				// handled by null check below
			}

			Map<String, String> result = null;
			if (shellPath != null)
			{
				// OK, we do have a shell.
				String envCommand = "env"; //$NON-NLS-1$
				if (Platform.OS_WIN32.equals(Platform.getOS()))
				{
					IPath envPath = shellPath.removeLastSegments(1).append("env.exe"); //$NON-NLS-1$
					if (envPath.toFile().isFile())
					{
						envCommand = envPath.toPortableString();
					}
				}
				try
				{
					IStatus status = ProcessUtil.processResult(run(envCommand, workingDirectory, null));
					if (status.isOK())
					{
						result = buildEnvironment(status.getMessage());
					}
					else
					{
						IdeLog.logError(CorePlugin.getDefault(), "Get shell environment failed: " + status.getMessage()); //$NON-NLS-1$
					}
				}
				catch (Exception e)
				{
					IdeLog.logError(CorePlugin.getDefault(), "Get shell environment failed.", e); //$NON-NLS-1$
					// failed to generate an env, we'll use JVM env and not
					// cache, see below...
				}
			}

			if (result == null)
			{
				// Grabbing the environment from shell failed, just use env we
				// have in JVM, but don't cache it!
				return System.getenv();
			}
			workingDirToEnvCache.put(workingDirectory, result);
		}
		return workingDirToEnvCache.get(workingDirectory);
	}

	private static Map<String, String> buildEnvironment(String envp)
	{
		Map<String, String> env = new HashMap<String, String>();
		env.put("HOME", PlatformUtil.expandEnvironmentStrings("~")); //$NON-NLS-1$ //$NON-NLS-2$
		StringTokenizer tok = new StringTokenizer(envp, "\r\n"); //$NON-NLS-1$
		while (tok.hasMoreTokens())
		{
			String envstring = tok.nextToken();
			int eqlsign = envstring.indexOf('=');
			if (eqlsign != -1)
			{
				env.put(envstring.substring(0, eqlsign), envstring.substring(eqlsign + 1));
			}
		}
		for (String var : ENV_FILTER)
		{
			if (var.charAt(var.length() - 1) == '*')
			{
				String prefix = var.substring(0, var.length() - 1);
				for (Iterator<Entry<String, String>> i = env.entrySet().iterator(); i.hasNext();)
				{
					if (i.next().getKey().startsWith(prefix))
					{
						i.remove();
					}
				}
			}
			else
			{
				env.remove(var);
			}
		}
		return env;
	}

	private synchronized static List<String> toShellCommand(List<String> command) throws CoreException
	{
		if (initializing)
		{
			return command;
		}
		List<String> shellCommand = new ArrayList<String>();
		shellCommand.add(getPath().toOSString());
		shellCommand.add("--login"); //$NON-NLS-1$
		shellCommand.add("--noprofile"); //$NON-NLS-1$
		shellCommand.add("-c"); //$NON-NLS-1$
		StringBuffer sb = new StringBuffer();
		for (String arg : command)
		{
			sb.append(arg.replaceAll("\"|\'|\\(|\\)| ", "\\\\$0")).append(' '); //$NON-NLS-1$ //$NON-NLS-2$
		}
		shellCommand.add(sb.toString().trim());
		return shellCommand;
	}

	private synchronized static Map<String, String> toShellEnvironment(Map<String, String> environment)
	{
		environment.put(APTANA_VERSION, CorePlugin.getAptanaStudioVersion());
		if (initializing)
		{
			return environment;
		}
		IPath rcPath = getShellRCPath();
		if (rcPath != null)
		{
			environment.put(BASH_ENV, rcPath.toOSString());
		}
		return environment;
	}

	public static List<String> toShellCommand(String command, String... arguments) throws CoreException
	{
		List<String> commands = new ArrayList<String>(Arrays.asList(arguments));
		commands.add(0, command);
		return toShellCommand(commands);
	}

	public static Process run(List<String> command, IPath workingDirectory, Map<String, String> environment)
			throws IOException, CoreException
	{
		ProcessBuilder processBuilder = new ProcessBuilder(toShellCommand(command));
		if (workingDirectory != null && workingDirectory.toFile().isDirectory())
		{
			processBuilder.directory(workingDirectory.toFile());
		}
		if (environment != null && !environment.isEmpty())
		{
			processBuilder.environment().putAll(environment);
		}
		processBuilder.environment().putAll(toShellEnvironment(processBuilder.environment()));
		return processBuilder.start();
	}

	public static Process run(String command, IPath workingDirectory, Map<String, String> environment,
			String... arguments) throws IOException, CoreException
	{
		List<String> commands = new ArrayList<String>(Arrays.asList(arguments));
		commands.add(0, command);
		return run(commands, workingDirectory, environment);
	}

	public static Process run(IPath executablePath, IPath workingDirectory, Map<String, String> environment,
			String... arguments) throws IOException, CoreException
	{
		return run(executablePath.toOSString(), workingDirectory, environment, arguments);
	}

	/**
	 * To be called when we know the ENV has changed (typically after an installer runs and alters PATH).
	 */
	public static void wipeCache()
	{
		shellEnvironment = null;
		workingDirToEnvCache.clear();
	}

}
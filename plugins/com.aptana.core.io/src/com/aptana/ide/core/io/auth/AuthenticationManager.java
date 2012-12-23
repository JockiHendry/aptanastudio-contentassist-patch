/**
 * Aptana Studio
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the GNU Public License (GPL) v3 (with exceptions).
 * Please see the license.html included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
// $codepro.audit.disable staticFieldNamingConvention

package com.aptana.ide.core.io.auth;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.Platform;
import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.SecurePreferencesFactory;
import org.eclipse.equinox.security.storage.StorageException;

import com.aptana.core.logging.IdeLog;
import com.aptana.ide.core.io.CoreIOPlugin;

/**
 * @author Max Stepanov
 *
 */
public final class AuthenticationManager implements IAuthenticationManager {

	private static final String SECURITY_NODE = "com.aptana.core.io.auth"; //$NON-NLS-1$
	private static final String PROP_PASSWORD = "password"; //$NON-NLS-1$
	
	private static AuthenticationManager instance;
	
	private transient Map<String, char[]> sessionPasswords = new HashMap<String, char[]>();
		
	/**
	 * 
	 */
	private AuthenticationManager() {
	}

	public static AuthenticationManager getInstance() {
		if (instance == null) {
			instance = new AuthenticationManager();
		}
		return instance;
	}

	/* (non-Javadoc)
	 * @see com.aptana.ide.core.io.auth.IAuthenticationManager#hasPersistent(java.lang.String)
	 */
	public boolean hasPersistent(String authId) {
		ISecurePreferences preferences = getSecurePreferences();
		if (preferences.nodeExists(authId)) {
			ISecurePreferences node = preferences.node(authId);
			return Arrays.asList(node.keys()).contains(PROP_PASSWORD);
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see com.aptana.ide.core.io.auth.IAuthenticationManager#getPassword(java.lang.String)
	 */
	public char[] getPassword(String authId) {
		ISecurePreferences preferences = getSecurePreferences();
		if (preferences.nodeExists(authId)) {
			try {
				ISecurePreferences node = preferences.node(authId);
				String password = node.get(PROP_PASSWORD, null);
				if (password != null) {
					return password.toCharArray();
				}
			} catch (StorageException e) {
				IdeLog.logWarning(CoreIOPlugin.getDefault(), Messages.AuthenticationManager_FailedGetSecurePreference, e);
			}
		}
		if (sessionPasswords.containsKey(authId)) {
			return sessionPasswords.get(authId);
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see com.aptana.ide.core.io.auth.IAuthenticationManager#promptPassword(java.lang.String, java.lang.String, java.lang.String, java.lang.String)
	 */
	public char[] promptPassword(String authId, String login, String title, String message) {
		IAuthenticationPrompt authPrompt = (IAuthenticationPrompt) Platform.getAdapterManager()
					.getAdapter(this, IAuthenticationPrompt.class);
		if (authPrompt == null && Platform.getAdapterManager().hasAdapter(this, IAuthenticationPrompt.class.getName())) {
			authPrompt = (IAuthenticationPrompt) Platform.getAdapterManager().loadAdapter(this, IAuthenticationPrompt.class.getName());
		}
		if (authPrompt != null) {
			if (authPrompt.promptPassword(this, authId, login, title, message)) {
				return sessionPasswords.get(authId);
			}
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see com.aptana.ide.core.io.auth.IAuthenticationManager#resetPassword(java.lang.String)
	 */
	public void resetPassword(String authId) {
		ISecurePreferences preferences = getSecurePreferences();
		if (preferences.nodeExists(authId)) {
			ISecurePreferences node = preferences.node(authId);
			node.removeNode();
		}
		sessionPasswords.remove(authId);
	}

	/* (non-Javadoc)
	 * @see com.aptana.ide.core.io.auth.IAuthenticationManager#setAuth(java.lang.String, char[], boolean)
	 */
	public void setPassword(String authId, char[] password, boolean persistent) {
		ISecurePreferences node = getSecurePreferences().node(authId);
		try {
			sessionPasswords.remove(authId);
			if (password != null) {
				sessionPasswords.put(authId, password);
				if (persistent) {
					node.put(PROP_PASSWORD, String.copyValueOf(password), true);
				} else {
					node.removeNode();
				}
			}
			getSecurePreferences().flush();
		} catch (Exception e) {
			IdeLog.logWarning(CoreIOPlugin.getDefault(), Messages.AuthenticationManager_FailedSaveSecurePreference, e);
		}
	}

	private ISecurePreferences getSecurePreferences() {
		return SecurePreferencesFactory.getDefault().node(SECURITY_NODE);
	}
}

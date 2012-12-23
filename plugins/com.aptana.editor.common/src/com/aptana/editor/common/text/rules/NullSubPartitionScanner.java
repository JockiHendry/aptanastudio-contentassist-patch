/**
 * Aptana Studio
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the GNU Public License (GPL) v3 (with exceptions).
 * Please see the license.html included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */

package com.aptana.editor.common.text.rules;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.rules.ICharacterScanner;
import org.eclipse.jface.text.rules.IPredicateRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.Token;

import com.aptana.editor.common.IPartitionScannerSwitchStrategy;

/**
 * @author Max Stepanov
 *
 */
public class NullSubPartitionScanner implements ISubPartitionScanner {

	private static final IToken DEFAULT_TOKEN = new Token(IDocument.DEFAULT_CONTENT_TYPE);
	private static final IPredicateRule[] EMPTY = new IPredicateRule[0];

	private ICharacterScanner characterScanner;
	
	/* (non-Javadoc)
	 * @see com.aptana.editor.common.ISubPartitionScanner#doResetRules()
	 */
	public boolean doResetRules() {
		return false;
	}

	/* (non-Javadoc)
	 * @see com.aptana.editor.common.ISubPartitionScanner#foundSequence()
	 */
	public boolean foundSequence() {
		return false;
	}

	/* (non-Javadoc)
	 * @see com.aptana.editor.common.ISubPartitionScanner#getCharacterScanner()
	 */
	public ICharacterScanner getCharacterScanner() {
		return characterScanner;
	}

	/* (non-Javadoc)
	 * @see com.aptana.editor.common.ISubPartitionScanner#getDefaultToken()
	 */
	public IToken getDefaultToken() {
		return DEFAULT_TOKEN;
	}

	/* (non-Javadoc)
	 * @see com.aptana.editor.common.ISubPartitionScanner#getRules()
	 */
	public IPredicateRule[] getRules() {
		return EMPTY;
	}

	/* (non-Javadoc)
	 * @see com.aptana.editor.common.ISubPartitionScanner#hasContentType(java.lang.String)
	 */
	public boolean hasContentType(String contentType) {
		return false;
	}

	/* (non-Javadoc)
	 * @see com.aptana.editor.common.ISubPartitionScanner#initCharacterScanner(org.eclipse.jface.text.rules.ICharacterScanner, com.aptana.editor.common.IPartitionScannerSwitchStrategy)
	 */
	public void initCharacterScanner(ICharacterScanner baseCharacterScanner, IPartitionScannerSwitchStrategy switchStrategy) {
		characterScanner = baseCharacterScanner;
	}

	/* (non-Javadoc)
	 * @see com.aptana.editor.common.ISubPartitionScanner#setLastToken(org.eclipse.jface.text.rules.IToken)
	 */
	public void setLastToken(IToken token) {
	}

	/* (non-Javadoc)
	 * @see com.aptana.editor.common.text.rules.ISubPartitionScanner#getLastToken()
	 */
	public IToken getLastToken() {
		return null;
	}

	/* (non-Javadoc)
	 * @see com.aptana.editor.common.text.rules.ISubPartitionScanner#getResumeToken()
	 */
	public IToken getResumeToken() {
		return null;
	}

}

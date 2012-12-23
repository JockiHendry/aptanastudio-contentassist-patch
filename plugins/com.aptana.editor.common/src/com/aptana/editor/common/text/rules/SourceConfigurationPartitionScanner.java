/**
 * Aptana Studio
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the GNU Public License (GPL) v3 (with exceptions).
 * Please see the license.html included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */

package com.aptana.editor.common.text.rules;

import org.eclipse.jface.text.rules.RuleBasedPartitionScanner;

import com.aptana.editor.common.IPartitioningConfiguration;

/**
 * @author Max Stepanov
 *
 */
public class SourceConfigurationPartitionScanner extends RuleBasedPartitionScanner {

	/**
	 * 
	 */
	public SourceConfigurationPartitionScanner(IPartitioningConfiguration partitioningConfiguration) {
		setPredicateRules(partitioningConfiguration.getPartitioningRules());
	}

}

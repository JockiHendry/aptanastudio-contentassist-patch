/**
 * Aptana Studio
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the GNU Public License (GPL) v3 (with exceptions).
 * Please see the license.html included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.aptana.editor.markdown.internal;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.swt.widgets.Composite;

import com.aptana.editor.common.ExtendedFastPartitioner;
import com.aptana.editor.common.viewer.CommonMergeViewer;
import com.aptana.editor.markdown.MarkdownEditor;
import com.aptana.editor.markdown.MarkdownSourceConfiguration;
import com.aptana.editor.markdown.MarkdownSourceViewerConfiguration;
import com.aptana.editor.markdown.text.rules.MarkdownPartitionScanner;

/**
 * @author cwilliams
 */
public class MarkdownMergeViewer extends CommonMergeViewer
{
	public MarkdownMergeViewer(Composite parent, CompareConfiguration configuration)
	{
		super(parent, configuration);
	}

	@Override
	protected IDocumentPartitioner getDocumentPartitioner()
	{
		IDocumentPartitioner partitioner = new ExtendedFastPartitioner(new MarkdownPartitionScanner(),
				MarkdownSourceConfiguration.getDefault().getContentTypes());
		return partitioner;
	}

	@Override
	protected void configureTextViewer(TextViewer textViewer)
	{
		super.configureTextViewer(textViewer);

		if (textViewer instanceof SourceViewer)
		{
			SourceViewer sourceViewer = (SourceViewer) textViewer;
			sourceViewer.unconfigure();
			IPreferenceStore preferences = MarkdownEditor.getChainedPreferenceStore();
			MarkdownSourceViewerConfiguration config = new MarkdownSourceViewerConfiguration(preferences, null);
			sourceViewer.configure(config);
		}
	}
}

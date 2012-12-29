/**
 * Aptana Studio
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the GNU Public License (GPL) v3 (with exceptions).
 * Please see the license.html included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.aptana.editor.js.contentassist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.swt.graphics.Image;

import com.aptana.core.util.StringUtil;
import com.aptana.editor.common.AbstractThemeableEditor;
import com.aptana.editor.common.CommonContentAssistProcessor;
import com.aptana.editor.common.contentassist.CommonCompletionProposal;
import com.aptana.editor.common.contentassist.UserAgentManager;
import com.aptana.editor.css.CSSPlugin;
import com.aptana.editor.css.contentassist.CSSIndexQueryHelper;
import com.aptana.editor.css.contentassist.index.ICSSIndexConstants;
import com.aptana.editor.css.internal.text.CSSModelFormatter;

/**
 * This class will provide content assist for jQuery selector.  For example, it will
 * provide CSS ids and classes as content assist proposals.
 * 
 * @author JockiHendry
 *
 */
public class JQuerySelectorContentAssistProcessor extends CommonContentAssistProcessor
{
	private static final Image ELEMENT_ICON = CSSPlugin.getImage("/icons/element.png");
	private CSSIndexQueryHelper indexHelper;

	/**
	 * JSIndexContentAssistProcessor
	 * 
	 * @param editor
	 */
	public JQuerySelectorContentAssistProcessor(AbstractThemeableEditor editor)
	{
		super(editor);

		indexHelper = new CSSIndexQueryHelper();
	}

	/**
	 * Add proposal where name is also the display name. The proposal will be marked as coming from the CSS core.
	 * 
	 * @param proposals
	 * @param name
	 * @param image
	 * @param description
	 * @param userAgents
	 * @param offset
	 */
	protected void addProposal(List<ICompletionProposal> proposals, String name, Image image, String description,
			String[] userAgentIds, int offset)
	{
		addProposal(proposals, name, image, description, userAgentIds, ICSSIndexConstants.CORE, offset);
	}

	/**
	 * Add proposal where name is also the display name.
	 * 
	 * @param proposals
	 * @param name
	 * @param image
	 * @param description
	 * @param userAgents
	 * @param fileLocation
	 * @param offset
	 */
	protected void addProposal(List<ICompletionProposal> proposals, String name, Image image, String description,
			String[] userAgentIds, String fileLocation, int offset)
	{
		addProposal(proposals, name, name, image, description, userAgentIds, fileLocation, offset);
	}

	/**
	 * Add proposal where the location will be marked as coming from the CSS core.
	 * 
	 * @param proposals
	 * @param displayName
	 * @param name
	 * @param image
	 * @param description
	 * @param userAgents
	 * @param offset
	 */
	protected void addProposal(List<ICompletionProposal> proposals, String displayName, String name, Image image,
			String description, String[] userAgentIds, int offset)
	{
		addProposal(proposals, displayName, name, image, description, userAgentIds, ICSSIndexConstants.CORE, offset);
	}

	/**
	 * Add proposal
	 * 
	 * @param proposals
	 * @param displayName
	 * @param name
	 * @param image
	 * @param description
	 * @param userAgents
	 * @param fileLocation
	 * @param offset
	 */
	protected void addProposal(List<ICompletionProposal> proposals, String displayName, String name, Image image,
			String description, String[] userAgentIds, String fileLocation, int offset)
	{
		if (isActiveByUserAgent(userAgentIds))
		{
			ICompletionProposal proposal = createProposal(displayName, name, image, description, userAgentIds,
					fileLocation, offset);

			proposals.add(proposal);
		}
	}

	/**
	 * createProposal
	 * 
	 * @param displayName
	 * @param name
	 * @param image
	 * @param description
	 * @param userAgents
	 * @param fileLocation
	 * @param offset
	 * @return
	 */
	protected CommonCompletionProposal createProposal(String displayName, String name, Image image, String description,
			String[] userAgentIds, String fileLocation, int offset)
	{
		int length = name.length();
		IContextInformation contextInfo = null;
		int replaceLength = 0;

		// build proposal
		Image[] userAgents = UserAgentManager.getInstance().getUserAgentImages(getProject(), userAgentIds);

		CommonCompletionProposal proposal = new CommonCompletionProposal(name, offset, replaceLength, length, image,
				displayName, contextInfo, description);
		proposal.setFileLocation(fileLocation);
		proposal.setUserAgentImages(userAgents);
		proposal.setTriggerCharacters(getProposalTriggerCharacters());
		return proposal;
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * com.aptana.editor.common.CommonContentAssistProcessor#doComputeCompletionProposals(org.eclipse.jface.text.ITextViewer
	 * , int, char, boolean)
	 */
	@Override
	protected ICompletionProposal[] doComputeCompletionProposals(ITextViewer viewer, int offset, char activationChar,
			boolean autoActivated)
	{
		List<ICompletionProposal> result = new ArrayList<ICompletionProposal>();

		// Find token start
		IDocument document = viewer.getDocument();
		String selector = null;
		ITypedRegion selectorRegion = null;
		try
		{
			selectorRegion = document.getPartition(offset);
			selector = document.get(selectorRegion.getOffset(), selectorRegion.getLength());			
		}
		catch (BadLocationException e)
		{
			e.printStackTrace();
		}
		int tokenStart = StringUtil.findPreviousWhitespaceOffset(selector, offset-selectorRegion.getOffset());
		if (tokenStart < 0) {
			tokenStart = selectorRegion.getOffset() + 1;
		} else {
			tokenStart += selectorRegion.getOffset() + 1;
		}

		// Add CSS IDs
		Map<String, String> ids = this.indexHelper.getIDs(this.getIndex());
		if (ids != null)
		{
			UserAgentManager manager = UserAgentManager.getInstance();
			String[] userAgentIds = manager.getActiveUserAgentIDs(getProject()); // classes can be used by all user agents

			for (Entry<String, String> entry : ids.entrySet())
			{
				String name = "#" + entry.getKey(); //$NON-NLS-1$
				String location = CSSModelFormatter.getDocumentDisplayName(entry.getValue());

				addProposal(result, name, ELEMENT_ICON, null, userAgentIds, location, tokenStart);
			}			
		}
		
		// Add CSS classes
		Map<String, String> classes = this.indexHelper.getClasses(this.getIndex());

		if (classes != null)
		{
			UserAgentManager manager = UserAgentManager.getInstance();
			String[] userAgentIds = manager.getActiveUserAgentIDs(getProject()); // classes can be used by all user agents

			for (Entry<String, String> entry : classes.entrySet())
			{
				String name = "." + entry.getKey(); //$NON-NLS-1$
				String location = CSSModelFormatter.getDocumentDisplayName(entry.getValue());

				addProposal(result, name, ELEMENT_ICON, null, userAgentIds, location, tokenStart);
			}
		}
				
		// sort by display name
		Collections.sort(result, new Comparator<ICompletionProposal>()
		{
			public int compare(ICompletionProposal o1, ICompletionProposal o2)
			{
				return o1.getDisplayString().compareToIgnoreCase(o2.getDisplayString());
			}
		});
		
		ICompletionProposal[] proposals = result.toArray(new ICompletionProposal[result.size()]);

		// select the current proposal based on the current lexeme
		try
		{
			setSelectedProposal(document.get(tokenStart, offset-tokenStart), proposals);
		}
		catch (BadLocationException e)
		{
			e.printStackTrace();
		}

		// return results
		return proposals;
	}

		
}

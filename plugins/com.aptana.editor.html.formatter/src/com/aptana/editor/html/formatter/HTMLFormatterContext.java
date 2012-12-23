/**
 * Aptana Studio
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the GNU Public License (GPL) v3 (with exceptions).
 * Please see the license.html included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.aptana.editor.html.formatter;

import com.aptana.core.util.StringUtil;
import com.aptana.formatter.FormatterContext;
import com.aptana.formatter.nodes.IFormatterContainerNode;
import com.aptana.formatter.nodes.IFormatterNode;

/**
 * An HTML formatter context.
 * 
 * @author Shalom Gibly <sgibly@aptana.com>
 */
public class HTMLFormatterContext extends FormatterContext
{

	/**
	 * @param indent
	 */
	public HTMLFormatterContext(int indent)
	{
		super(indent);
	}

	/**
	 * Returns true only if the given node is a container node (of type {@link IFormatterContainerNode}).
	 * 
	 * @param node
	 *            An {@link IFormatterNode}
	 * @return True only if the given node is a container node; False, otherwise.
	 * @see com.aptana.formatter.FormatterContext#isCountable(com.aptana.formatter.nodes.IFormatterNode)
	 */
	protected boolean isCountable(IFormatterNode node)
	{
		return node instanceof IFormatterContainerNode;
	}

	/**
	 * Check if the char sequence starts with a '&lt!' sequence or a '&lt!--' sequence. If so, return the length of the
	 * sequence; Otherwise, return 0.
	 * 
	 * @see com.aptana.formatter.IFormatterContext#getCommentStartLength(CharSequence, int)
	 */
	public int getCommentStartLength(CharSequence chars, int offset)
	{
		int count = 0;
		if (chars.length() > offset + 1)
		{
			if (chars.charAt(offset) == '<' && chars.charAt(offset + 1) == '!')
			{
				count = 2;
			}
			if (chars.length() > offset + 3)
			{
				if (chars.charAt(offset + 2) == '-' && chars.charAt(offset + 3) == '-')
				{
					count += 2;
				}
			}
		}
		return count;
	}

	/*
	 * (non-Javadoc)
	 * @see com.aptana.formatter.IFormatterContext#getWrappingCommentPrefix(java.lang.String)
	 */
	public String getWrappingCommentPrefix(String text)
	{
		return StringUtil.EMPTY;
	}
}

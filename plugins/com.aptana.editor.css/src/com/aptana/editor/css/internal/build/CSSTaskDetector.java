/**
 * Aptana Studio
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the GNU Public License (GPL) v3 (with exceptions).
 * Please see the license.html included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.aptana.editor.css.internal.build;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;

import com.aptana.core.build.IProblem;
import com.aptana.core.build.RequiredBuildParticipant;
import com.aptana.core.resources.IMarkerConstants;
import com.aptana.core.util.ArrayUtil;
import com.aptana.editor.css.parsing.ast.CSSCommentNode;
import com.aptana.index.core.build.BuildContext;
import com.aptana.parsing.ast.IParseNode;
import com.aptana.parsing.ast.IParseRootNode;

/**
 * Detects task tags in CSS comments.
 * 
 * @author cwilliams
 */
public class CSSTaskDetector extends RequiredBuildParticipant
{
	private static final String COMMENT_ENDING = "*/"; //$NON-NLS-1$

	public void buildFile(BuildContext context, IProgressMonitor monitor)
	{
		if (context == null)
		{
			return;
		}

		Collection<IProblem> tasks = detectTasks(context, monitor);
		context.putProblems(IMarkerConstants.TASK_MARKER, tasks);
	}

	public void deleteFile(BuildContext context, IProgressMonitor monitor)
	{
		if (context == null)
		{
			return;
		}

		context.removeProblems(IMarkerConstants.TASK_MARKER);
	}

	private Collection<IProblem> detectTasks(BuildContext context, IProgressMonitor monitor)
	{
		if (context == null)
		{
			return Collections.emptyList();
		}

		try
		{
			return detectTasks(context.getAST(), context, monitor);
		}
		catch (CoreException e)
		{
			// ignores the parser exception
		}
		return Collections.emptyList();
	}

	/**
	 * Method for detecting tasks in the root of a CSS AST. This is public for the HTML task detector to be able to
	 * delegate to this for CSS as a sub-language.
	 */
	public Collection<IProblem> detectTasks(IParseRootNode rootNode, BuildContext context, IProgressMonitor monitor)
	{
		if (context == null || rootNode == null)
		{
			return Collections.emptyList();
		}

		IParseNode[] comments = rootNode.getCommentNodes();
		if (ArrayUtil.isEmpty(comments))
		{
			return Collections.emptyList();
		}

		SubMonitor sub = SubMonitor.convert(monitor, comments.length);
		Collection<IProblem> tasks = new ArrayList<IProblem>(comments.length);
		try
		{
			String source = context.getContents();
			String filePath = context.getURI().toString();

			for (IParseNode commentNode : comments)
			{
				if (commentNode instanceof CSSCommentNode)
				{
					tasks.addAll(processCommentNode(filePath, source, 0, commentNode, COMMENT_ENDING));
				}
				sub.worked(1);
			}
		}
		finally
		{
			sub.done();
		}
		return tasks;
	}
}

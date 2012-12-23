/**
 * Aptana Studio
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the GNU Public License (GPL) v3 (with exceptions).
 * Please see the license.html included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.aptana.editor.dtd.parsing.ast;

import com.aptana.parsing.ast.IParseNode;

public class DTDElementDeclNode extends DTDNode
{
	private String _name;

	/**
	 * DTDElementDeclarationNode
	 */
	public DTDElementDeclNode(String name)
	{
		super(DTDNodeType.ELEMENT_DECLARATION);

		this._name = name;
	}

	/*
	 * (non-Javadoc)
	 * @see com.aptana.editor.dtd.parsing.ast.DTDNode#accept(com.aptana.editor.dtd.parsing.ast.DTDTreeWalker)
	 */
	public void accept(DTDTreeWalker walker)
	{
		walker.visit(this);
	}

	/**
	 * getChildExpression
	 * 
	 * @return
	 */
	public IParseNode getChildExpression()
	{
		return this.getFirstChild();
	}

	/**
	 * getName
	 */
	public String getName()
	{
		return this._name;
	}
}

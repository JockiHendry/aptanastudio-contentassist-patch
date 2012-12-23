/**
 * Aptana Studio
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the GNU Public License (GPL) v3 (with exceptions).
 * Please see the license.html included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.aptana.editor.css.contentassist.model;

import java.util.EnumSet;
import java.util.Set;

import com.aptana.editor.css.contentassist.index.ICSSIndexConstants;
import com.aptana.index.core.Index;
import com.aptana.index.core.ui.views.IPropertyInformation;

/**
 * CSSElement
 */
public class CSSElement extends BaseElement<CSSElement.Property>
{
	enum Property implements IPropertyInformation<CSSElement>
	{
		NAME(Messages.CSSElement_NameLabel)
		{
			public Object getPropertyValue(CSSElement node)
			{
				return node.getName();
			}
		},
		INDEX(Messages.CSSElement_IndexLabel)
		{
			public Object getPropertyValue(CSSElement node)
			{
				return node.getIndex().toString();
			}
		},
		INDEX_FILE(Messages.CSSElement_IndexFileLabel)
		{
			public Object getPropertyValue(CSSElement node)
			{
				return node.getIndex().getIndexFile().getAbsolutePath();
			}
		},
		INDEX_FILE_SIZE(Messages.CSSElement_IndexFileSizeLabel)
		{
			public Object getPropertyValue(CSSElement node)
			{
				return node.getIndex().getIndexFile().length();
			}
		},
		CHILD_COUNT(Messages.CSSElement_ChildCountLabel)
		{
			public Object getPropertyValue(CSSElement node)
			{
				// TODO: don't emit children with no content?
				return 3;
			}
		},
		VERSION(Messages.CSSElement_VersionLabel)
		{
			public Object getPropertyValue(CSSElement node)
			{
				return ICSSIndexConstants.INDEX_VERSION;
			}
		};

		private String header;
		private String category;

		private Property(String header) // $codepro.audit.disable unusedMethod
		{
			this.header = header;
		}

		private Property(String header, String category)
		{
			this.category = category;
		}

		public String getCategory()
		{
			return category;
		}

		public String getHeader()
		{
			return header;
		}
	}

	private Index index;

	public CSSElement(Index index)
	{
		this.index = index;
		setName(Messages.CSSElement_ElementName);
	}

	public Index getIndex()
	{
		return index;
	}

	@Override
	protected Set<Property> getPropertyInfoSet()
	{
		return EnumSet.allOf(Property.class);
	}
}

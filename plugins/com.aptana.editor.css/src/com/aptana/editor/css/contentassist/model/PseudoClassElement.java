/**
 * Aptana Studio
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the GNU Public License (GPL) v3 (with exceptions).
 * Please see the license.html included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.aptana.editor.css.contentassist.model;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.aptana.jetty.util.epl.ajax.JSON.Output;

import com.aptana.core.util.CollectionsUtil;
import com.aptana.core.util.ObjectUtil;
import com.aptana.index.core.IndexUtil;
import com.aptana.index.core.ui.views.IPropertyInformation;

public class PseudoClassElement extends BaseElement<PseudoClassElement.Property>
{
	enum Property implements IPropertyInformation<PseudoClassElement>
	{
		NAME(Messages.PseudoClassElement_NameLabel)
		{
			public Object getPropertyValue(PseudoClassElement node)
			{
				return node.getName();
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

	private static final String VALUES_PROPERTY = "values"; //$NON-NLS-1$
	private static final String SPECIFICATIONS_PROPERTY = "specifications"; //$NON-NLS-1$

	private List<SpecificationElement> _specifications;
	private List<ValueElement> _values;

	/**
	 * PseudoClassElement
	 */
	public PseudoClassElement()
	{
		super();
	}

	/**
	 * addSpecification
	 * 
	 * @param specification
	 */
	public void addSpecification(SpecificationElement specification)
	{
		if (specification != null)
		{
			if (this._specifications == null)
			{
				this._specifications = new ArrayList<SpecificationElement>();
			}

			this._specifications.add(specification);
		}
	}

	/**
	 * addValue
	 * 
	 * @param value
	 */
	public void addValue(ValueElement value)
	{
		if (value != null)
		{
			if (this._values == null)
			{
				this._values = new ArrayList<ValueElement>();
			}

			this._values.add(value);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see com.aptana.editor.css.contentassist.model.AbstractCSSMetadataElement#fromJSON(java.util.Map)
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public void fromJSON(Map object)
	{
		super.fromJSON(object);

		this._values = IndexUtil.createList(object.get(VALUES_PROPERTY), ValueElement.class);
		this._specifications = IndexUtil.createList(object.get(SPECIFICATIONS_PROPERTY), SpecificationElement.class);
	}

	@Override
	protected Set<Property> getPropertyInfoSet()
	{
		return EnumSet.allOf(Property.class);
	}

	/**
	 * getSpecifications
	 * 
	 * @return
	 */
	public List<SpecificationElement> getSpecifications()
	{
		return CollectionsUtil.getListValue(this._specifications);
	}

	/**
	 * getValues
	 * 
	 * @return
	 */
	public List<ValueElement> getValues()
	{
		return CollectionsUtil.getListValue(this._values);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * com.aptana.editor.css.contentassist.model.AbstractCSSMetadataElement#toJSON(com.aptana.jetty.util.epl.ajax.JSON.Output)
	 */
	@Override
	public void toJSON(Output out)
	{
		super.toJSON(out);

		out.add(SPECIFICATIONS_PROPERTY, this.getSpecifications());
		out.add(VALUES_PROPERTY, this.getValues());
	}

	@Override
	public boolean equals(Object obj)
	{
		if (!(obj instanceof PseudoClassElement))
		{
			return false;
		}
		PseudoClassElement other = (PseudoClassElement) obj;
		return ObjectUtil.areEqual(getName(), other.getName());
	}

	@Override
	public int hashCode()
	{
		return getName().hashCode();
	}
}

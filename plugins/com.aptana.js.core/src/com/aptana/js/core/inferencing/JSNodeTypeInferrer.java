/**
 * Aptana Studio
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the GNU Public License (GPL) v3 (with exceptions).
 * Please see the license.html included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.aptana.js.core.inferencing;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;

import com.aptana.core.util.CollectionsUtil;
import com.aptana.core.util.URIUtil;
import com.aptana.index.core.Index;
import com.aptana.js.core.JSTypeConstants;
import com.aptana.js.core.index.JSIndexQueryHelper;
import com.aptana.js.core.model.FunctionElement;
import com.aptana.js.core.model.ParameterElement;
import com.aptana.js.core.model.PropertyElement;
import com.aptana.js.core.parsing.JSTokenType;
import com.aptana.js.core.parsing.ast.IJSNodeTypes;
import com.aptana.js.core.parsing.ast.JSArgumentsNode;
import com.aptana.js.core.parsing.ast.JSArrayNode;
import com.aptana.js.core.parsing.ast.JSAssignmentNode;
import com.aptana.js.core.parsing.ast.JSBinaryArithmeticOperatorNode;
import com.aptana.js.core.parsing.ast.JSBinaryBooleanOperatorNode;
import com.aptana.js.core.parsing.ast.JSConditionalNode;
import com.aptana.js.core.parsing.ast.JSConstructNode;
import com.aptana.js.core.parsing.ast.JSFalseNode;
import com.aptana.js.core.parsing.ast.JSFunctionNode;
import com.aptana.js.core.parsing.ast.JSGetElementNode;
import com.aptana.js.core.parsing.ast.JSGetPropertyNode;
import com.aptana.js.core.parsing.ast.JSGroupNode;
import com.aptana.js.core.parsing.ast.JSIdentifierNode;
import com.aptana.js.core.parsing.ast.JSInvokeNode;
import com.aptana.js.core.parsing.ast.JSNode;
import com.aptana.js.core.parsing.ast.JSNumberNode;
import com.aptana.js.core.parsing.ast.JSObjectNode;
import com.aptana.js.core.parsing.ast.JSPostUnaryOperatorNode;
import com.aptana.js.core.parsing.ast.JSPreUnaryOperatorNode;
import com.aptana.js.core.parsing.ast.JSRegexNode;
import com.aptana.js.core.parsing.ast.JSReturnNode;
import com.aptana.js.core.parsing.ast.JSStringNode;
import com.aptana.js.core.parsing.ast.JSTreeWalker;
import com.aptana.js.core.parsing.ast.JSTrueNode;
import com.aptana.js.internal.core.inferencing.JSPropertyCollector;
import com.aptana.js.internal.core.inferencing.JSSymbolTypeInferrer;
import com.aptana.js.internal.core.parsing.sdoc.model.DocumentationBlock;
import com.aptana.js.internal.core.parsing.sdoc.model.ParamTag;
import com.aptana.js.internal.core.parsing.sdoc.model.Tag;
import com.aptana.js.internal.core.parsing.sdoc.model.TagType;
import com.aptana.js.internal.core.parsing.sdoc.model.Type;
import com.aptana.parsing.ast.IParseNode;

public class JSNodeTypeInferrer extends JSTreeWalker
{
	private JSScope _scope;
	private Index _index;
	private URI _location;
	private List<String> _types;
	private JSIndexQueryHelper _queryHelper;

	/**
	 * JSNodeTypeInferrer
	 * 
	 * @param scope
	 * @param projectIndex
	 * @param location
	 */
	public JSNodeTypeInferrer(JSScope scope, Index projectIndex, URI location)
	{
		this(scope, projectIndex, location, new JSIndexQueryHelper());
	}

	/**
	 * @param scope
	 * @param projectIndex
	 * @param location
	 * @param queryHelper
	 */
	public JSNodeTypeInferrer(JSScope scope, Index projectIndex, URI location, JSIndexQueryHelper queryHelper)
	{
		this._scope = scope;
		this._index = projectIndex;
		this._location = location;
		this._queryHelper = queryHelper;
	}

	/**
	 * addParameterTypes
	 * 
	 * @param identifierNode
	 */
	private void addParameterTypes(JSIdentifierNode identifierNode)
	{
		IParseNode parent = identifierNode.getParent();
		IParseNode grandparent = (parent != null) ? parent.getParent() : null;
		boolean foundType = false;

		if (grandparent != null && grandparent.getNodeType() == IJSNodeTypes.FUNCTION)
		{
			DocumentationBlock docs = ((JSNode) grandparent).getDocumentation();

			if (docs != null)
			{
				String name = identifierNode.getText();
				int index = identifierNode.getIndex();
				List<Tag> params = docs.getTags(TagType.PARAM);

				if (params != null && index < params.size())
				{
					ParamTag param = (ParamTag) params.get(index);

					if (name.equals(param.getName()))
					{
						for (Type parameterType : param.getTypes())
						{
							String type = parameterType.getName();

							// Fix up type names as might be necessary
							type = JSTypeMapper.getInstance().getMappedType(type);

							this.addType(type);
							foundType = true;
						}
					}
				}
			}
		}

		// Use "Object" as parameter type if we didn't find types by other
		// means
		if (!foundType)
		{
			this.addType(JSTypeConstants.DEFAULT_PARAMETER_TYPE);
		}
	}

	/**
	 * addType
	 * 
	 * @param type
	 */
	public void addType(String type)
	{
		if (type != null && type.length() > 0)
		{
			if (this._types == null)
			{
				this._types = new ArrayList<String>();
			}
			type = JSTypeUtil.validateTypeName(type);
			if (!this._types.contains(type))
			{
				this._types.add(type);
			}
		}
	}

	/**
	 * addTypes
	 * 
	 * @param node
	 */
	protected void addTypes(IParseNode node)
	{
		if (node instanceof JSNode)
		{
			((JSNode) node).accept(this);
		}
	}

	/**
	 * addTypes
	 * 
	 * @param types
	 */
	protected void addTypes(List<String> types)
	{
		if (types != null)
		{
			for (String type : types)
			{
				this.addType(type);
			}
		}
	}

	/**
	 * getActiveScope
	 * 
	 * @param offset
	 * @return
	 */
	protected JSScope getActiveScope(int offset)
	{
		JSScope result = null;

		if (this._scope != null)
		{
			// find the global scope
			JSScope root = this._scope;

			while (true)
			{
				JSScope candidate = root.getParentScope();

				if (candidate == null)
				{
					break;
				}
				else
				{
					root = candidate;
				}
			}

			// find scope containing the specified offset
			result = root.getScopeAtOffset(offset);
		}

		return result;
	}

	/**
	 * getTypes
	 * 
	 * @return
	 */
	public List<String> getTypes()
	{
		return CollectionsUtil.getListValue(_types);
	}

	/**
	 * getTypes
	 * 
	 * @param node
	 * @return
	 */
	public List<String> getTypes(IParseNode node)
	{
		return this.getTypes(node, this._scope);
	}

	/**
	 * getTypes
	 * 
	 * @param node
	 * @return
	 */
	public List<String> getTypes(IParseNode node, JSScope scope)
	{
		List<String> result;

		if (node instanceof JSNode)
		{
			// create new nested walker
			JSNodeTypeInferrer walker = new JSNodeTypeInferrer(scope, this._index, this._location, this._queryHelper);

			// collect types
			walker.visit((JSNode) node);

			// return collected types
			result = walker.getTypes();
		}
		else
		{
			result = Collections.emptyList();
		}

		return result;
	}

	/*
	 * (non-Javadoc)
	 * @see com.aptana.editor.js.parsing.ast.JSTreeWalker#visit(com.aptana.editor.js.parsing.ast.JSArrayNode)
	 */
	@Override
	public void visit(JSArrayNode node)
	{
		if (!node.hasChildren())
		{
			this.addType(JSTypeConstants.ARRAY_TYPE);
		}
		else
		{
			// TODO: Add all element types?
			// TODO: Create equivalent of "structure" type if element types vary?
			for (String type : this.getTypes(node.getFirstChild()))
			{
				this.addType(JSTypeUtil.createGenericArrayType(type));
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see com.aptana.editor.js.parsing.ast.JSTreeWalker#visit(com.aptana.editor.js.parsing.ast.JSAssignmentNode)
	 */
	@Override
	public void visit(JSAssignmentNode node)
	{
		switch (node.getNodeType())
		{
			case IJSNodeTypes.ASSIGN:
				this.addTypes(node.getRightHandSide());
				break;

			case IJSNodeTypes.ADD_AND_ASSIGN:
				String type = JSTypeConstants.NUMBER_TYPE;
				List<String> lhsTypes = this.getTypes(node.getLeftHandSide());
				List<String> rhsTypes = this.getTypes(node.getRightHandSide());

				if (lhsTypes.contains(JSTypeConstants.STRING_TYPE) || rhsTypes.contains(JSTypeConstants.STRING_TYPE))
				{
					type = JSTypeConstants.STRING_TYPE;
				}

				this.addType(type);
				break;

			default:
				this.addType(JSTypeConstants.DEFAULT_ASSIGNMENT_TYPE);
				break;
		}
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * com.aptana.editor.js.parsing.ast.JSTreeWalker#visit(com.aptana.editor.js.parsing.ast.JSArithmeticOperatorNode)
	 */
	@Override
	public void visit(JSBinaryArithmeticOperatorNode node)
	{
		String type = JSTypeConstants.NUMBER_TYPE;

		if (node.getNodeType() == IJSNodeTypes.ADD)
		{
			IParseNode lhs = node.getLeftHandSide();
			IParseNode rhs = node.getRightHandSide();

			// NOTE: Iterate down the tree until we find the first non-addition node or the first string
			while (lhs.getNodeType() == IJSNodeTypes.ADD)
			{
				rhs = lhs.getLastChild();
				lhs = lhs.getFirstChild();

				if (rhs instanceof JSStringNode)
				{
					break;
				}
			}

			if (lhs instanceof JSStringNode || rhs instanceof JSStringNode)
			{
				type = JSTypeConstants.STRING_TYPE;
			}
			else
			{
				List<String> lhsTypes = this.getTypes(lhs);
				List<String> rhsTypes = this.getTypes(rhs);

				if (lhsTypes.contains(JSTypeConstants.STRING_TYPE) || rhsTypes.contains(JSTypeConstants.STRING_TYPE))
				{
					type = JSTypeConstants.STRING_TYPE;
				}
			}
		}

		this.addType(type);
	}

	/*
	 * (non-Javadoc)
	 * @see com.aptana.editor.js.parsing.ast.JSTreeWalker#visit(com.aptana.editor.js.parsing.ast.JSBooleanOperatorNode)
	 */
	@Override
	public void visit(JSBinaryBooleanOperatorNode node)
	{
		JSTokenType token = JSTokenType.get((String) node.getOperator().value);

		switch (token)
		{
			case AMPERSAND_AMPERSAND:
			case PIPE_PIPE:
				this.addTypes(node.getLeftHandSide());
				this.addTypes(node.getRightHandSide());
				break;

			default:
				this.addType(JSTypeConstants.BOOLEAN_TYPE);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see com.aptana.editor.js.parsing.ast.JSTreeWalker#visit(com.aptana.editor.js.parsing.ast.JSConditionalNode)
	 */
	@Override
	public void visit(JSConditionalNode node)
	{
		this.addTypes(node.getTrueExpression());
		this.addTypes(node.getFalseExpression());
	}

	/*
	 * (non-Javadoc)
	 * @see com.aptana.editor.js.parsing.ast.JSTreeWalker#visit(com.aptana.editor.js.parsing.ast.JSConstructNode)
	 */
	@Override
	public void visit(JSConstructNode node)
	{
		// TODO: Need to handle any property assignments off of "this"
		IParseNode child = node.getExpression();

		if (child instanceof JSNode)
		{
			List<String> types = this.getTypes(child);
			List<String> returnTypes = new ArrayList<String>();

			for (String typeName : types)
			{
				if (typeName.startsWith(JSTypeConstants.GENERIC_CLASS_OPEN))
				{
					returnTypes.add(JSTypeUtil.getClassType(typeName));
				}
				else
				{
					// FIXME If this is a function that returns a type, assume that function is a constructor and we've
					// defined the type as Function<Type>.
					// This is where the properties for that type are going to be hung.
					// That may not be "right". We may want to unwrap the function's return type and use that as the
					// type we're dealing with.
					// in that case, we need to change JSSymbolTypeInferrer#generateType to also unwrap Function<Type>
					// properly.
					returnTypes.add(typeName);
				}
			}

			for (String typeName : returnTypes)
			{
				Collection<PropertyElement> properties = this._queryHelper.getTypeMembers(this._index, typeName,
						JSTypeConstants.PROTOTYPE_PROPERTY);

				if (properties != null)
				{
					for (PropertyElement property : properties)
					{
						for (String propertyType : property.getTypeNames())
						{
							this.addType(propertyType);
						}
					}
				}
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see com.aptana.editor.js.parsing.ast.JSTreeWalker#visit(com.aptana.editor.js.parsing.ast.JSFalseNode)
	 */
	@Override
	public void visit(JSFalseNode node)
	{
		this.addType(JSTypeConstants.BOOLEAN_TYPE);
	}

	/*
	 * (non-Javadoc)
	 * @see com.aptana.editor.js.parsing.ast.JSTreeWalker#visit(com.aptana.editor.js.parsing.ast.JSFunctionNode)
	 */
	@Override
	public void visit(JSFunctionNode node)
	{
		List<String> types = new ArrayList<String>();
		JSScope scope = this.getActiveScope(node.getBody().getStartingOffset());
		boolean foundReturnExpression = false;

		// infer return types
		for (JSReturnNode returnValue : node.getReturnNodes())
		{
			IParseNode expression = returnValue.getExpression();

			if (!expression.isEmpty())
			{
				foundReturnExpression = true;

				types.addAll(this.getTypes(expression, scope));
			}
		}

		// If we couldn't infer a return type and we had a return
		// expression, then have it return Object
		if (foundReturnExpression && types.isEmpty())
		{
			types.add(JSTypeConstants.OBJECT_TYPE);
		}

		// build function type, including return values
		String type = JSTypeUtil.toFunctionType(types);
		this.addType(type);
	}

	/*
	 * (non-Javadoc)
	 * @see com.aptana.editor.js.parsing.ast.JSTreeWalker#visit(com.aptana.editor.js.parsing.ast.JSGetElementNode)
	 */
	@Override
	public void visit(JSGetElementNode node)
	{
		// TODO: Should check subscript to determine if the type is a Number or
		// a String. If it is a String, then this should behave like get-property
		// assuming we can retrieve a literal string.
		IParseNode lhs = node.getLeftHandSide();

		if (lhs instanceof JSNode)
		{
			for (String typeName : this.getTypes(lhs))
			{
				String typeString = JSTypeUtil.getArrayElementType(typeName);

				if (typeString != null)
				{
					this.addType(typeString);
				}
				else
				{
					this.addType(JSTypeConstants.OBJECT_TYPE);
				}
			}
		}
	}
		
	/**
	 * Memeriksa apakah argumen untuk sebuah parameter memiliki tipe yang benar atau tidak.
	 * 
	 * @param node adalah argumen yang diberikan.
	 * @param expectedParameter adalah parameter yang dihasilkan.
	 * @return nilai <code>true</code> bila argumen sesuai dengan parameter, atau <code>false</code> bila sebaliknya.
	 */
	private boolean periksaArgumen(JSNode node, ParameterElement expectedParameter) {
		
		if (node instanceof JSNumberNode) {
			if (!expectedParameter.getTypes().contains("Number")) return false;
		} else if (node instanceof JSStringNode) {
			if (!expectedParameter.getTypes().contains("String")) return false;
		} else if (node instanceof JSObjectNode) {
			if (expectedParameter.getTypes().contains("Object") ||
				expectedParameter.getTypes().contains("Map")) {
				return true;
			}  else {
				return false;
			}
		} else if (node instanceof JSArrayNode) {
			if (!expectedParameter.getTypes().contains("Array")) return false;
		} else if (node instanceof JSFunctionNode) {
			if (!expectedParameter.getTypes().contains("Function")) return false;
		}
		
		return true;
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.aptana.editor.js.parsing.ast.JSTreeWalker#visit(com.aptana.editor.js.parsing.ast.JSGetPropertyNode)
	 */
	@Override
	public void visit(JSGetPropertyNode node)
	{
		IParseNode lhs = node.getLeftHandSide();

		if (lhs instanceof JSNode)
		{
			IParseNode rhs = node.getRightHandSide();
			String memberName = rhs.getText();
						
			for (String typeName : this.getTypes(lhs))
			{
				// Fix up type names as might be necessary
				typeName = JSTypeMapper.getInstance().getMappedType(typeName);

				// lookup up rhs name in type and add that value's type here
				Collection<PropertyElement> properties = this._queryHelper.getTypeMembers(this._index, typeName,
						memberName);			

				if (properties != null)
				{				
//					// Argumen untuk node yang dicari saat ini
//					JSNode argument = null;
//					if (node.getRightHandSide()!=null && node.getRightHandSide().getNextNode() !=null && node.getRightHandSide().getNextNode() instanceof JSArgumentsNode) {
//						argument = (JSNode) node.getRightHandSide().getNextNode().getNextNode();
//						if (argument!=null && argument instanceof JSIdentifierNode &&  argument.getText().isEmpty()) {
//							argument = null;
//						}
//					}
					
					for (PropertyElement property : properties)
					{
						if (property instanceof FunctionElement)
						{
							FunctionElement function = (FunctionElement) property;
							
//							// Periksa apakah parameter sesuai dengan fungsi yang dicari saat ini, jika
//							// terdapat lebih dari 1 properties
//							if (properties.size() > 1 && argument!=null) {
//								
//								boolean valid = true;
//								
//								List<ParameterElement> listParameters = function.getParameters();
//								int curParameterIndex = 0;
//								
//								// bila terdapat argumen, tetapi tidak ada parameter, maka ini tidak valid
//								if (listParameters.isEmpty()) {
//									continue;
//								}
//								
//								// Periksa setiap argument hingga selesai
//								while (argument!=null) {
//									
//									// Menjaga-jaga
//									if (curParameterIndex >= listParameters.size()) break;									
//									
//									if (periksaArgumen(argument, listParameters.get(curParameterIndex))==true) {
//										
//										// proses parameter berikutnya 
//										curParameterIndex++;
//										if (curParameterIndex >= listParameters.size()) break;
//										
//									} else {
//										// bila argumen tidak sesuai dengan parameter dan parameter tidak optional,
//										// maka argumen ini tidak valid.
//										if (listParameters.get(curParameterIndex).getUsage().equals("required")) {
//											valid = false; 
//											break;
//										} 
//										
//										// bila argumen ini optional, tetapi tidak ada parameter berikutnya, maka
//										// argumen ini tidak valid.
//										if (curParameterIndex==listParameters.size()-1) {
//											valid = false;
//											break;
//										}
//									}
//																		
//									// proses argumen berikutnya									
//									argument = (JSNode) argument.getNextSibling();
//									
//									// bila sudah tidak ada argumen lagi, tetapi masih ada parameter yang perlu diproses,
//									// pastikan bahwa semua parameter yang perlu diproses adalah optional.
//									if (argument==null && curParameterIndex < listParameters.size()-1) {										
//										for (int i = curParameterIndex+1; i < listParameters.size(); i++) {
//											if (!listParameters.get(i).getUsage().equals("optional")) {
//												valid = false;
//												break;
//											}
//										}
//									}
//																		
//								}
//								
//								if (!valid) continue;
//							}
//							
//							// Bila tidak ada argumen yang diberikan, pastikan bahwa tidak ada parameter, atau
//							// seluruh parameter bersifat optional.
//							if (properties.size() > 1 && argument==null) {
//								List<ParameterElement> listParameters = function.getParameters();
//								boolean valid = true;
//								for (ParameterElement parameter: listParameters) {
//									if (!parameter.getUsage().equals("optional")) {
//										valid = false;
//										break;
//									}
//								}
//								if (!valid) continue;
//							}
//							
														
							// Bila terdapat lebih dari satu dengan nama yang sama, hanya memakai versi yang mengandung dokumentasi
							boolean adaVersiTerdokumentasi = false;
							if (function.getDescription()==null || function.getDescription().length()==0) {
								for (PropertyElement item: properties) {
									if (item instanceof FunctionElement && item.getName().equals(function.getName()) && 
										item.getDescription()!=null && !item.getDescription().isEmpty()) {
										adaVersiTerdokumentasi = true;
										break;
									}
								}
							}
							if (adaVersiTerdokumentasi) continue;
							
							for (String type : function.getSignatureTypes())
							{
								this.addType(type);
							}
						}
						else
						{
							for (String type : property.getTypeNames())
							{
								this.addType(type);
							}
						}
					}
				}
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see com.aptana.editor.js.parsing.ast.JSTreeWalker#visit(com.aptana.editor.js.parsing.ast.JSGroupNode)
	 */
	@Override
	public void visit(JSGroupNode node)
	{
		IParseNode expression = node.getExpression();

		if (expression instanceof JSNode)
		{
			((JSNode) expression).accept(this);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see com.aptana.editor.js.parsing.ast.JSTreeWalker#visit(com.aptana.editor.js.parsing.ast.JSIdentifierNode)
	 */
	@Override
	public void visit(JSIdentifierNode node)
	{
		String name = node.getText();
		Collection<PropertyElement> properties = null;

		if (this._scope != null && this._scope.hasSymbol(name))
		{
			IParseNode parent = node.getParent();

			if (parent != null && parent.getNodeType() == IJSNodeTypes.PARAMETERS)
			{
				// special handling of parameters to potentially get the type
				// from documentation and to prevent an infinite loop since
				// parameters point to themselves in the symbol table
				this.addParameterTypes(node);
			}
			else
			{
				// Check the local scope for type first
				JSSymbolTypeInferrer symbolInferrer = new JSSymbolTypeInferrer(this._scope, this._index, this._location);
				PropertyElement property = symbolInferrer.getSymbolPropertyElement(name);
				if (property != null)
				{
					// We found a match in the local scope
					properties = CollectionsUtil.newList(property);
				}
				else
				{
					// No match in the local scope, query the globals in index
					properties = this._queryHelper.getGlobals(this._index, getProject(), getFileName(), name);
				}
			}
		}
		else
		{
			// Scope says it doesn't has a symbol with that name, so query the globals in index
			properties = this._queryHelper.getGlobals(this._index, getProject(), getFileName(), name);
		}

		// Hopefully we found at least one match...
		if (properties != null)
		{
			for (PropertyElement property : properties)
			{
				if (property instanceof FunctionElement)
				{
					FunctionElement function = (FunctionElement) property;
					for (String type : function.getSignatureTypes())
					{
						this.addType(type);
					}
				}
				else
				{
					for (String type : property.getTypeNames())
					{
						this.addType(type);
					}
				}
			}
		}
	}

	protected String getFileName()
	{
		return URIUtil.getFileName(_location);
	}

	protected IProject getProject()
	{
		URI root = _index.getRoot();
		IPath containerPath = org.eclipse.core.filesystem.URIUtil.toPath(root);
		if (containerPath == null)
		{
			return null;
		}
		IContainer container = ResourcesPlugin.getWorkspace().getRoot().getContainerForLocation(containerPath);
		if (container == null)
		{
			return null;
		}
		return container.getProject();
	}

	/*
	 * (non-Javadoc)
	 * @see com.aptana.editor.js.parsing.ast.JSTreeWalker#visit(com.aptana.editor.js.parsing.ast.JSInvokeNode)
	 */
	@Override
	public void visit(JSInvokeNode node)
	{
		IParseNode child = node.getExpression();

		if (child instanceof JSNode)
		{
			// TODO hang the "require" string as a constant somewhere!
			if (child instanceof JSIdentifierNode && "require".equals(child.getNameNode().getName())) //$NON-NLS-1$
			{
				// it's a requires!
				JSArgumentsNode args = (JSArgumentsNode) node.getArguments();
				IParseNode[] children = args.getChildren();
				for (IParseNode arg : children)
				{
					if (arg instanceof JSStringNode)
					{
						JSStringNode string = (JSStringNode) arg;
						String text = string.getText();
						// strip quotes TODO Use util method to strip quotes!
						if (text.startsWith("'") || text.startsWith("\"")) //$NON-NLS-1$ //$NON-NLS-2$
						{
							text = text.substring(1, text.length() - 1);
						}

						// Handle resolving absolute versus relative module ids!
						URI resolved = _location.resolve(text);
						URI relative = _index.getRelativeDocumentPath(resolved);
						this.addType(relative.getPath() + ".exports"); //$NON-NLS-1$
					}
				}
			}

			List<String> types = this.getTypes(child);

			// NOTE: This is a special case for functions used as a RHS of assignments or as part of a property chain.
			// If the invocation returns undefined, we change that to Object.
			// TODO: As a refinement, we want to check that the function being called is not defined in the current
			// scope
			if (types.isEmpty())
			{
				IParseNode parent = node.getParent();

				if (parent != null)
				{
					switch (parent.getNodeType())
					{
						case IJSNodeTypes.ASSIGN:
							if (node.getIndex() == 1)
							{
								this.addType(JSTypeConstants.OBJECT_TYPE);
							}
							break;

						case IJSNodeTypes.GET_PROPERTY:
							this.addType(JSTypeConstants.OBJECT_TYPE);
							break;

						default:
							break;
					}
				}
			}

			for (String typeName : types)
			{
				if (JSTypeUtil.isFunctionPrefix(typeName))
				{
					List<String> returnTypes = JSTypeUtil.getFunctionSignatureReturnTypeNames(typeName);
					for (String returnTypeName : returnTypes)
					{
						this.addType(returnTypeName);
					}
				}
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see com.aptana.editor.js.parsing.ast.JSTreeWalker#visit(com.aptana.editor.js.parsing.ast.JSNumberNode)
	 */
	@Override
	public void visit(JSNumberNode node)
	{
		this.addType(JSTypeConstants.NUMBER_TYPE);
	}

	/*
	 * (non-Javadoc)
	 * @see com.aptana.editor.js.parsing.ast.JSTreeWalker#visit(com.aptana.editor.js.parsing.ast.JSObjectNode)
	 */
	@Override
	public void visit(JSObjectNode node)
	{
		if (node.hasChildren())
		{
			// collect all descendants into a property collection
			JSPropertyCollection symbol = new JSPropertyCollection();
			JSPropertyCollector collector = new JSPropertyCollector(symbol);

			collector.visit(node);
			symbol.addValue(node);

			// infer type
			JSSymbolTypeInferrer inferrer = new JSSymbolTypeInferrer(this._scope, this._index, this._location);
			Set<String> types = new LinkedHashSet<String>();

			inferrer.processProperties(symbol, types);

			this.addTypes(new ArrayList<String>(types));
		}
		else
		{
			this.addType(JSTypeConstants.OBJECT_TYPE);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * com.aptana.editor.js.parsing.ast.JSTreeWalker#visit(com.aptana.editor.js.parsing.ast.JSPostUnaryOperatorNode)
	 */
	@Override
	public void visit(JSPostUnaryOperatorNode node)
	{
		this.addType(JSTypeConstants.NUMBER_TYPE);
	}

	/*
	 * (non-Javadoc)
	 * @see com.aptana.editor.js.parsing.ast.JSTreeWalker#visit(com.aptana.editor.js.parsing.ast.JSPreUnaryOperatorNode)
	 */
	@Override
	public void visit(JSPreUnaryOperatorNode node)
	{
		switch (node.getNodeType())
		{
			case IJSNodeTypes.DELETE:
			case IJSNodeTypes.LOGICAL_NOT:
				this.addType(JSTypeConstants.BOOLEAN_TYPE);
				break;

			case IJSNodeTypes.TYPEOF:
				this.addType(JSTypeConstants.STRING_TYPE);
				break;

			case IJSNodeTypes.VOID:
				// technically this returns 'undefined', but we return nothing
				// for both types
				break;

			default:
				this.addType(JSTypeConstants.NUMBER_TYPE);
				break;
		}
	}

	/*
	 * (non-Javadoc)
	 * @see com.aptana.editor.js.parsing.ast.JSTreeWalker#visit(com.aptana.editor.js.parsing.ast.JSRegexNode)
	 */
	@Override
	public void visit(JSRegexNode node)
	{
		this.addType(JSTypeConstants.REG_EXP_TYPE);
	}

	/*
	 * (non-Javadoc)
	 * @see com.aptana.editor.js.parsing.ast.JSTreeWalker#visit(com.aptana.editor.js.parsing.ast.JSStringNode)
	 */
	@Override
	public void visit(JSStringNode node)
	{
		this.addType(JSTypeConstants.STRING_TYPE);
	}

	/*
	 * (non-Javadoc)
	 * @see com.aptana.editor.js.parsing.ast.JSTreeWalker#visit(com.aptana.editor.js.parsing.ast.JSTrueNode)
	 */
	@Override
	public void visit(JSTrueNode node)
	{
		this.addType(JSTypeConstants.BOOLEAN_TYPE);
	}
}

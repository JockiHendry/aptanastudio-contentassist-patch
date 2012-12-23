/**
 * Aptana Studio
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the GNU Public License (GPL) v3 (with exceptions).
 * Please see the license.html included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.aptana.editor.js.contentassist;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.contentassist.ContextInformation;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.eclipse.swt.graphics.Image;

import beaver.Scanner;

import com.aptana.core.IFilter;
import com.aptana.core.util.ChainedFilter;
import com.aptana.core.util.CollectionsUtil;
import com.aptana.core.util.StringUtil;
import com.aptana.editor.common.AbstractThemeableEditor;
import com.aptana.editor.common.CommonContentAssistProcessor;
import com.aptana.editor.common.contentassist.CommonCompletionProposal;
import com.aptana.editor.common.contentassist.ILexemeProvider;
import com.aptana.editor.common.contentassist.UserAgentManager;
import com.aptana.editor.common.util.EditorUtil;
import com.aptana.editor.js.JSPlugin;
import com.aptana.editor.js.JSSourceConfiguration;
import com.aptana.editor.js.text.JSFlexLexemeProvider;
import com.aptana.index.core.Index;
import com.aptana.js.core.IJSConstants;
import com.aptana.js.core.JSLanguageConstants;
import com.aptana.js.core.index.IJSIndexConstants;
import com.aptana.js.core.index.JSIndexQueryHelper;
import com.aptana.js.core.inferencing.JSNodeTypeInferrer;
import com.aptana.js.core.inferencing.JSPropertyCollection;
import com.aptana.js.core.inferencing.JSScope;
import com.aptana.js.core.inferencing.JSTypeUtil;
import com.aptana.js.core.model.FunctionElement;
import com.aptana.js.core.model.ParameterElement;
import com.aptana.js.core.model.PropertyElement;
import com.aptana.js.core.parsing.JSFlexScanner;
import com.aptana.js.core.parsing.JSParseState;
import com.aptana.js.core.parsing.JSTokenType;
import com.aptana.js.core.parsing.ast.IJSNodeTypes;
import com.aptana.js.core.parsing.ast.JSArgumentsNode;
import com.aptana.js.core.parsing.ast.JSAssignmentNode;
import com.aptana.js.core.parsing.ast.JSFunctionNode;
import com.aptana.js.core.parsing.ast.JSGetPropertyNode;
import com.aptana.js.core.parsing.ast.JSNode;
import com.aptana.js.core.parsing.ast.JSObjectNode;
import com.aptana.js.core.parsing.ast.JSParseRootNode;
import com.aptana.parsing.ParserPoolFactory;
import com.aptana.parsing.ast.INameNode;
import com.aptana.parsing.ast.IParseNode;
import com.aptana.parsing.lexer.IRange;
import com.aptana.parsing.lexer.Lexeme;

public class JSContentAssistProcessor extends CommonContentAssistProcessor
{
	/**
	 * This class is used via {@link CollectionsUtil#filter(Collection, IFilter)} to remove duplicate proposals based on
	 * display names. Duplicate proposals are merged into a single entry
	 */
	public class ProposalMerger implements IFilter<ICompletionProposal>
	{
		private ICompletionProposal lastProposal = null;

		public boolean include(ICompletionProposal item)
		{
			boolean result;

			if (lastProposal == null || !lastProposal.getDisplayString().equals(item.getDisplayString()))
			{
				result = true;
				lastProposal = item;
			}
			else
			{
				// It is now okay to have more than one proposal item				
				if (lastProposal instanceof PropertyElementProposal && item instanceof PropertyElementProposal) {					
					result = true;
					lastProposal = item;
				} else {
					result = false;
				}
			}

			return result;
		}
	}

	private static final Image JS_FUNCTION = JSPlugin.getImage("/icons/js_function.png"); //$NON-NLS-1$
	private static final Image JS_PROPERTY = JSPlugin.getImage("/icons/js_property.png"); //$NON-NLS-1$
	private static final Image JS_KEYWORD = JSPlugin.getImage("/icons/keyword.png"); //$NON-NLS-1$

	/**
	 * Filters out internal properties.
	 */
	private static final IFilter<PropertyElement> isVisibleFilter = new IFilter<PropertyElement>()
	{
		public boolean include(PropertyElement item)
		{
			return !item.isInternal();
		}
	};

	/**
	 * Filters out functions that are constructors.
	 */
	private static final IFilter<PropertyElement> isNotConstructorFilter = new IFilter<PropertyElement>()
	{
		public boolean include(PropertyElement item)
		{
			if (!(item instanceof FunctionElement))
			{
				return true;
			}
			return !((FunctionElement) item).isConstructor();
		}
	};

	private static Set<String> AUTO_ACTIVATION_PARTITION_TYPES;
	{
		AUTO_ACTIVATION_PARTITION_TYPES = CollectionsUtil.newSet(JSSourceConfiguration.DEFAULT,
				IDocument.DEFAULT_CONTENT_TYPE);
	}

	private JSIndexQueryHelper indexHelper;
	private IParseNode targetNode;
	private IParseNode statementNode;
	private IRange replaceRange;
	private IRange activeRange;

	/**
	 * JSIndexContentAssistProcessor
	 * 
	 * @param editor
	 */
	public JSContentAssistProcessor(AbstractThemeableEditor editor)
	{
		super(editor);

		indexHelper = new JSIndexQueryHelper();
	}

	/**
	 * JSContentAssistProcessor
	 * 
	 * @param editor
	 * @param activeRange
	 */
	public JSContentAssistProcessor(AbstractThemeableEditor editor, IRange activeRange)
	{
		this(editor);

		this.activeRange = activeRange;
	}

	/**
	 * addCoreGlobals
	 * 
	 * @param proposals
	 * @param offset
	 */
	private void addCoreGlobals(Set<ICompletionProposal> proposals, int offset)
	{
		Collection<PropertyElement> globals = indexHelper.getCoreGlobals(getProject(), getFilename());

		if (!CollectionsUtil.isEmpty(globals))
		{
			for (PropertyElement property : CollectionsUtil.filter(globals, isVisibleFilter))
			{
				addProposal(proposals, property, offset, null, Messages.JSContentAssistProcessor_KeywordLocation);
			}
		}
	}

	/**
	 * @param prefix
	 * @param completionProposals
	 */
	private void addKeywords(Set<ICompletionProposal> proposals, int offset)
	{
		for (String name : JSLanguageConstants.KEYWORDS)
		{
			// TODO Create a KeywordProposal class that lazily generates description, etc?
			String description = StringUtil.format(Messages.JSContentAssistProcessor_KeywordDescription, name);
			addProposal(proposals, name, JS_KEYWORD, description, getActiveUserAgentIds(),
					Messages.JSContentAssistProcessor_KeywordLocation, offset);
		}
	}

	/**
	 * addObjectLiteralProperties
	 * 
	 * @param proposals
	 * @param offset
	 */
	protected void addObjectLiteralProperties(Set<ICompletionProposal> proposals, ITextViewer viewer, int offset)
	{
		// Find one or more FunctionElement that matches.
		List<FunctionElement> listFunction = getFunctionElementList(viewer, offset);

		for (FunctionElement function: listFunction)
		{
			List<ParameterElement> params = function.getParameters();
			int index = getArgumentIndex(offset);

			if (0 <= index && index < params.size())
			{
				ParameterElement param = params.get(index);
				URI projectURI = getProjectURI();

				for (String type : param.getTypes())
				{
					Collection<PropertyElement> properties = indexHelper.getTypeProperties(getIndex(), type);

					for (PropertyElement property : CollectionsUtil.filter(properties, isVisibleFilter))
					{
						addProposal(proposals, property, offset, projectURI, null);
					}
				}
			}
		}
	}

	/**
	 * addProjectGlobalFunctions
	 * 
	 * @param proposals
	 * @param offset
	 */
	private void addProjectGlobals(Set<ICompletionProposal> proposals, int offset)
	{
		Collection<PropertyElement> projectGlobals = indexHelper.getGlobals(getIndex(), getProject(), getFilename());

		if (!CollectionsUtil.isEmpty(projectGlobals))
		{
			String[] userAgentNames = getActiveUserAgentIds();
			URI projectURI = getProjectURI();
			
			// Tampung dulu seluruh FunctionElement yang akan diproses			
			List<FunctionElement> listFunctionElement = new ArrayList<FunctionElement>();
			for (PropertyElement property: CollectionsUtil.filter(projectGlobals, isVisibleFilter)) {
				if (property instanceof FunctionElement) {
					listFunctionElement.add((FunctionElement)property);
				}				
			}
			
			for (PropertyElement property : CollectionsUtil.filter(projectGlobals, isVisibleFilter))
			{
				// Will include proposal only if there are no documented version.
				if (property instanceof FunctionElement) {					
					FunctionElement thisFunctionElement = (FunctionElement) property;					
					if (thisFunctionElement.getDescription()==null || thisFunctionElement.getDescription().length()==0) {
						boolean ketemuYangTerdokumentasi = false;
						for (FunctionElement item: listFunctionElement) {
							// Find documented version. 
							if (thisFunctionElement.getName().equals(item.getName()) && item.getDescription()!=null && item.getDescription().length()>0) {
								ketemuYangTerdokumentasi = true;
							}
						}
						if (ketemuYangTerdokumentasi) continue;
					}
				}
				String location = null;				
				List<String> documents = property.getDocuments();
				if (!CollectionsUtil.isEmpty(documents))
				{
					String docString = documents.get(0);
					int index = docString.lastIndexOf('/');
					if (index != -1)
					{
						location = docString.substring(index + 1);
					}
					else
					{
						location = docString;
					}
				}
				addProposal(proposals, property, offset, projectURI, location, userAgentNames);
			}
		}
	}

	/**
	 * addProperties
	 * 
	 * @param proposals
	 * @param offset
	 */
	protected void addProperties(Set<ICompletionProposal> proposals, int offset) {
		JSGetPropertyNode node = ParseUtil.getGetPropertyNode(targetNode, statementNode);
		List<String> types = getParentObjectTypes(node, offset);

		// add all properties of each type to our proposal list
		for (String type : types)
		{
			addTypeProperties(proposals, type, offset);
		}
	}

	/**
	 * addProposal
	 * 
	 * @param proposals
	 * @param property
	 * @param offset
	 * @param projectURI
	 * @param overriddenLocation
	 */
	private void addProposal(Set<ICompletionProposal> proposals, PropertyElement property, int offset, URI projectURI,
			String overriddenLocation)
	{
		List<String> userAgentNameList = property.getUserAgentNames();
		String[] userAgentNames = userAgentNameList.toArray(new String[userAgentNameList.size()]);

		addProposal(proposals, property, offset, projectURI, overriddenLocation, userAgentNames);	
	}

	/**
	 * addProposal
	 * 
	 * @param proposals
	 * @param property
	 * @param offset
	 * @param projectURI
	 * @param overriddenLocation
	 * @param userAgentNames
	 */
	private void addProposal(Set<ICompletionProposal> proposals, PropertyElement property, int offset, URI projectURI,
			String overriddenLocation, String[] userAgentNames)
	{
		if (isActiveByUserAgent(userAgentNames))
		{
			// calculate what text will be replaced
			int replaceLength = 0;

			if (replaceRange != null)
			{
				offset = replaceRange.getStartingOffset(); // $codepro.audit.disable questionableAssignment
				replaceLength = replaceRange.getLength();				
			}

			
			PropertyElementProposal proposal = null;
			
			
			if (property instanceof FunctionElement) {
				// If this is a FunctionElement, then create context info
				// for the proposal, based on docmentation.
				FunctionElement function = (FunctionElement) property;
				String documentation = JSModelFormatter.CONTEXT_INFO.getDocumentation(function);
				ContextInformation contextInformation = new ContextInformation(function.getName(), documentation);
				
				String proposalValue = StringUtil.join(null, function.getName(), "(", 
					StringUtil.join(", ", function.getParameterNames()), ")");
				
				proposal = new PropertyElementProposal(proposalValue, function.getName()+"()", function.getName().length()+1, 
					function, offset, replaceLength, projectURI, contextInformation);
			} else {
				proposal = new PropertyElementProposal(property, offset, replaceLength, projectURI);
			}			
			
			proposal.setTriggerCharacters(getProposalTriggerCharacters());			
			if (!StringUtil.isEmpty(overriddenLocation))
			{
				proposal.setFileLocation(overriddenLocation);
			}

			Image[] userAgents = UserAgentManager.getInstance().getUserAgentImages(getProject(), userAgentNames);
			proposal.setUserAgentImages(userAgents);

			// add the proposal to the list
			proposals.add(proposal);
		}

	}

	/**
	 * addProposal - The display name is used as the insertion text
	 * 
	 * @param proposals
	 * @param displayName
	 * @param image
	 * @param description
	 * @param userAgents
	 * @param fileLocation
	 * @param offset
	 */
	private void addProposal(Set<ICompletionProposal> proposals, String displayName, Image image, String description,
			String[] userAgentIds, String fileLocation, int offset)
	{
		if (isActiveByUserAgent(userAgentIds))
		{
			int length = displayName.length();

			// calculate what text will be replaced
			int replaceLength = 0;

			if (replaceRange != null)
			{
				offset = replaceRange.getStartingOffset(); // $codepro.audit.disable questionableAssignment
				replaceLength = replaceRange.getLength();
			}

			// build proposal
			IContextInformation contextInfo = null;
			Image[] userAgents = UserAgentManager.getInstance().getUserAgentImages(getProject(), userAgentIds);

			CommonCompletionProposal proposal = new CommonCompletionProposal(displayName, offset, replaceLength,
					length, image, displayName, contextInfo, description);
			proposal.setFileLocation(fileLocation);
			proposal.setUserAgentImages(userAgents);
			proposal.setTriggerCharacters(getProposalTriggerCharacters());

			// add the proposal to the list
			proposals.add(proposal);
		}
	}

	/**
	 * addSymbolsInScope
	 * 
	 * @param proposals
	 */
	protected void addSymbolsInScope(Set<ICompletionProposal> proposals, int offset)
	{
		if (targetNode != null)
		{
			JSScope globalScope = ParseUtil.getGlobalScope(targetNode);

			if (globalScope != null)
			{
				JSScope localScope = globalScope.getScopeAtOffset(offset);
				String fileLocation = getFilename();
				String[] userAgentNames = getActiveUserAgentIds();

				while (localScope != null && localScope != globalScope)
				{
					List<String> symbols = localScope.getLocalSymbolNames();

					for (String symbol : symbols)
					{
						boolean isFunction = false;
						JSPropertyCollection object = localScope.getLocalSymbol(symbol);
						List<JSNode> nodes = object.getValues();

						if (nodes != null)
						{
							for (JSNode node : nodes)
							{
								if (node instanceof JSFunctionNode)
								{
									isFunction = true;
									break;
								}
							}
						}

						String name = symbol;
						String description = null;
						Image image = (isFunction) ? JS_FUNCTION : JS_PROPERTY;

						// TODO Add a JSPropertyCollectionProposal that takes the object and generates the rest?
						addProposal(proposals, name, image, description, userAgentNames, fileLocation, offset);
					}

					localScope = localScope.getParentScope();
				}
			}
		}
	}

	/**
	 * addThisProposals
	 * 
	 * @param proposals
	 * @param offset
	 */
	protected void addThisProperties(Set<ICompletionProposal> proposals, int offset)
	{
		// find containing function or JSParseRootNode
		IParseNode activeNode = getActiveASTNode(offset);

		while (!(activeNode instanceof JSFunctionNode))
		{
			activeNode = activeNode.getParent();
			if (activeNode instanceof JSParseRootNode)
			{
				// If we've gotten to the root, just bail out.
				return;
			}
		}
		JSFunctionNode currentFunctionNode = (JSFunctionNode) activeNode;

		String functionName = getFunctionName(currentFunctionNode);

		if (functionName != null)
		{
			functionName = StringUtil.dotFirst(functionName).trim();
			if (functionName.length() == 0)
			{
				// Empty name
				functionName = null;
			}
		}

		List<JSFunctionNode> functionsToAnalyze;
		if (functionName == null)
		{
			// Unable to get a name for the current function: don't try to find any other
			// JS prototypes.
			functionsToAnalyze = Arrays.asList(currentFunctionNode);
		}
		else
		{
			// We want to match the following:
			// myFunc function(){...}
			// myFunc = function(){...}
			// myFunc.prototype.foo = function(){...}
			IParseNode parent = currentFunctionNode.getParent();
			if (parent.getNodeType() == IJSNodeTypes.ASSIGN)
			{
				parent = parent.getParent();
			}
			IParseNode[] children = parent.getChildren();
			functionsToAnalyze = new LinkedList<JSFunctionNode>();
			for (int i = 0; i < children.length; i++)
			{
				String childName = null;
				IParseNode childNode = children[i];
				JSFunctionNode jsFunctionNode = null;
				if (childNode instanceof JSFunctionNode)
				{
					jsFunctionNode = (JSFunctionNode) childNode;
					childName = jsFunctionNode.getNameNode().getName();

				}
				else if (childNode.getNodeType() == IJSNodeTypes.ASSIGN)
				{
					JSAssignmentNode assignmentNode = (JSAssignmentNode) childNode;
					IParseNode rightHandSide = assignmentNode.getRightHandSide();
					if (rightHandSide instanceof JSFunctionNode)
					{
						jsFunctionNode = (JSFunctionNode) rightHandSide;
						childName = getAssignmentLeftNodeName(assignmentNode);
					}
				}

				if (childName != null && jsFunctionNode != null)
				{
					if (StringUtil.dotFirst(childName).equals(functionName))
					{
						functionsToAnalyze.add(jsFunctionNode);
					}
				}
			}
		}

		for (JSFunctionNode function : functionsToAnalyze)
		{
			// collect all this.property assignments
			ThisAssignmentCollector collector = new ThisAssignmentCollector();
			((JSNode) function.getBody()).accept(collector);
			List<JSAssignmentNode> assignments = collector.getAssignments();

			if (!CollectionsUtil.isEmpty(assignments))
			{
				JSScope globalScope = ParseUtil.getGlobalScope(targetNode);

				if (globalScope != null)
				{
					JSScope localScope = globalScope.getScopeAtOffset(offset);
					Index index = getIndex();
					URI location = EditorUtil.getURI(editor);
					String typeName = StringUtil.concat(getNestedFunctionTypeName(function)
							+ IJSIndexConstants.NESTED_TYPE_SEPARATOR + "this"); //$NON-NLS-1$

					// infer each property and add proposal
					for (JSAssignmentNode assignment : assignments)
					{
						IParseNode lhs = assignment.getLeftHandSide();
						IParseNode rhs = assignment.getRightHandSide();
						String name = lhs.getLastChild().getText();

						JSNodeTypeInferrer nodeInferrer = new JSNodeTypeInferrer(localScope, index, location);
						((JSNode) rhs).accept(nodeInferrer);
						List<String> types = nodeInferrer.getTypes();

						PropertyElement property = new PropertyElement();
						property.setName(name);
						property.setHasAllUserAgents();

						if (!CollectionsUtil.isEmpty(types))
						{
							for (String type : types)
							{
								property.addType(type);
							}
						}

						addProposal(proposals, property, offset, getProjectURI(), typeName);
					}
				}
			}
		}
	}

	/**
	 * Given a function node, discover its name either declared directly or through a parent assign to the function
	 * (i.e.: myFunc function(){} or myFunc = function(){...}). May return null if unable to get the name.
	 */
	private String getFunctionName(JSFunctionNode currentFunctionNode)
	{
		String functionName = null;
		// Discover the name context name of where we are (function or assign to function).
		INameNode nameNode = currentFunctionNode.getNameNode();
		if (nameNode.getName().length() == 0)
		{
			IParseNode functionParent = currentFunctionNode.getParent();
			if (functionParent.getNodeType() == IJSNodeTypes.ASSIGN)
			{
				functionName = getAssignmentLeftNodeName((JSAssignmentNode) functionParent);
			}
		}
		else
		{
			// Found as: myFunc function(){...}
			functionName = nameNode.getName();
		}
		return functionName;
	}

	//@formatter:off 
	/**
	 * @return the left-hand side name we can discover in an assign
	 * I.e.: something as: 
	 * 
	 * myFunc = function(){...}
	 * myFunc.prototype.foo = function(){...} 
	 * 
	 * Will return myFunc / myFunc.prototype.foo
	 */
	//@formatter:on
	private String getAssignmentLeftNodeName(JSAssignmentNode assignmentNode)
	{
		IParseNode leftHandSide = assignmentNode.getLeftHandSide();
		if (leftHandSide.getNodeType() == IJSNodeTypes.GET_PROPERTY)
		{
			return leftHandSide.toString();
		}
		return null;
	}

	/**
	 * addTypeProperties
	 * 
	 * @param proposals
	 * @param typeName
	 * @param offset
	 */
	@SuppressWarnings("unchecked")
	protected void addTypeProperties(Set<ICompletionProposal> proposals, String typeName, int offset)
	{
		Index index = getIndex();

		// grab all ancestors of the specified type
		List<String> allTypes = indexHelper.getTypeAncestorNames(index, typeName);		

		// include the type in the list as well
		allTypes.add(0, typeName);

		// add properties and methods
		Collection<PropertyElement> properties = indexHelper.getTypeMembers(index, allTypes);
		URI projectURI = getProjectURI();
		
		// Will select documented version over undocument one.
		// Will only select undocumented version if no documented version available.	
		List<PropertyElement> listProperties = CollectionsUtil.filter(properties, new ChainedFilter<PropertyElement>(
				isNotConstructorFilter, isVisibleFilter));
		listProperties = ParseUtil.filterDokumentasi(listProperties);

		for (PropertyElement property: listProperties) {			
			addProposal(proposals, property, offset, projectURI, null);
		}

	}

	/*
	 * (non-Javadoc)
	 * @see
	 * com.aptana.editor.common.CommonContentAssistProcessor#computeContextInformation(org.eclipse.jface.text.ITextViewer
	 * , int)
	 */
	@Override
	public IContextInformation[] computeContextInformation(ITextViewer viewer, int offset)
	{
		List<IContextInformation> result = new ArrayList<IContextInformation>();
		// Return one or more FunctionElement
		List<FunctionElement> listFunction = getFunctionElementList(viewer, offset);
		
		for (FunctionElement function: listFunction)
		{
			JSArgumentsNode node = getArgumentsNode(offset);

			if (node != null)
			{
				boolean inObjectLiteral = false;

				// find argument we're in
				for (IParseNode arg : node)
				{
					if (arg.contains(offset))
					{
						// Not foolproof, but this should cover 99% of the cases we're likely to encounter
						inObjectLiteral = (arg instanceof JSObjectNode);
						break;
					}
				}

				// prevent context info popup from appearing and immediately disappearing
				if (!inObjectLiteral)
				{
					IContextInformation ci = new JSContextInformation(function, getProjectURI(),
							node.getStartingOffset());

					result.add(ci);
				}
			}
		}

		return result.toArray(new IContextInformation[result.size()]);
	}

	/**
	 * createLexemeProvider
	 * 
	 * @param document
	 * @param offset
	 * @return
	 */
	ILexemeProvider<JSTokenType> createLexemeProvider(IDocument document, int offset)
	{
		Scanner scanner = new JSFlexScanner();
		ILexemeProvider<JSTokenType> result;

		// NOTE: use active range temporarily until we get proper partitions for JS inside of HTML
		if (activeRange != null)
		{
			result = new JSFlexLexemeProvider(document, activeRange, scanner);
		}
		else if (statementNode != null)
		{
			result = new JSFlexLexemeProvider(document, statementNode, scanner);
		}
		else
		{
			result = new JSFlexLexemeProvider(document, offset, scanner);
		}

		return result;
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
		// NOTE: Using a linked hash set to preserve add-order. We need this in case we end up filtering proposals. This
		// will give precedence to the first of a collection of proposals with like names
		Set<ICompletionProposal> result = new LinkedHashSet<ICompletionProposal>();

		// grab document
		IDocument document = viewer.getDocument();

		// determine the content assist location type
		LocationType location = getLocationType(document, offset);	

		// process the resulting location
		switch (location)
		{
			case IN_PROPERTY_NAME:				
				addProperties(result, offset);
				break;

			case IN_VARIABLE_NAME:
			case IN_GLOBAL:
			case IN_CONSTRUCTOR:
				addKeywords(result, offset);
				addCoreGlobals(result, offset);
				addProjectGlobals(result, offset);
				addSymbolsInScope(result, offset);				
				break;

			case IN_OBJECT_LITERAL_PROPERTY:
				addObjectLiteralProperties(result, viewer, offset);
				break;

			case IN_THIS:
				addThisProperties(result, offset);
				break;

			default:
				break;
		}

		// merge and remove duplicates from the proposal list
		List<ICompletionProposal> filteredProposalList = getMergedProposals(new ArrayList<ICompletionProposal>(result));
		ICompletionProposal[] resultList = filteredProposalList.toArray(new ICompletionProposal[filteredProposalList
				.size()]);

		// select the current proposal based on the prefix
		if (replaceRange != null)
		{
			try
			{
				String prefix = document.get(replaceRange.getStartingOffset(), replaceRange.getLength());

				setSelectedProposal(prefix, resultList);
			}
			catch (BadLocationException e) // $codepro.audit.disable emptyCatchClause
			{
				// ignore
			}
		}

		return resultList;
	}

		
	JSLocationIdentifier getJSLocationIdentifier(int offset) {
		IParseNode result = null;

		try
		{
			// grab document
			IDocument doc = editor.getDocumentProvider().getDocument(editor.getEditorInput());

			// grab source which is either the whole document for JS files or a subset for nested JS
			// @formatter:off
			String source =
				(activeRange != null)
					? doc.get(activeRange.getStartingOffset(), activeRange.getLength())
					: doc.get();
			// @formatter:on
			int startingOffset = (activeRange != null) ? activeRange.getStartingOffset() : 0;
			

			// If JavaScript is inside HTML, then we need to parse only JavaScript.
			// I will assume all JavaScript in HTML are inside <script type="application/javascript"> tag.  This is naive!
			
			final String SCAN_OPEN_JSTAG = "<script type=\"application/javascript\">";
			final String SCAN_CLOSE_JSTAG = "</script>";

			int posStart = 0;
			int posClose = 0;
			StringBuffer newSource = new StringBuffer();
			while ((posStart=source.indexOf(SCAN_OPEN_JSTAG,posClose))>=0) {				
				posClose = source.indexOf(SCAN_CLOSE_JSTAG, posStart);
				if (posClose==-1) break;
				newSource.append(source.substring(posStart+SCAN_OPEN_JSTAG.length(), posClose));
				if (offset >= posStart && offset <= posClose) {				
					offset-= posStart+SCAN_OPEN_JSTAG.length();
				}
			}
			if (newSource.length()>0) {
				source = newSource.toString();
			}
						
			// create parse state and turn off all processing of comments
			JSParseState parseState = new JSParseState(source, startingOffset, true, true);

			// parse and grab resulting AST
			IParseNode ast = ParserPoolFactory.parse(IJSConstants.CONTENT_TYPE_JS, parseState).getRootNode();

			if (ast != null)
			{
				result = ast.getNodeAtOffset(offset);

				// We won't get a current node if the cursor is outside of the positions
				// recorded by the AST
				if (result == null)
				{
					if (offset < ast.getStartingOffset())
					{
						result = ast.getNodeAtOffset(ast.getStartingOffset());
					}
					else if (ast.getEndingOffset() < offset)
					{
						result = ast.getNodeAtOffset(ast.getEndingOffset());
					}
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			// ignore parse error exception since the user will get markers and/or entries in the Problems View
		}

		return new JSLocationIdentifier(offset, result);
	}
	
	/**
	 * getActiveASTNode
	 * 
	 * @param offset
	 * @return
	 */
	IParseNode getActiveASTNode(int offset)
	{
		IParseNode result = null;

		try
		{
			// grab document
			IDocument doc = editor.getDocumentProvider().getDocument(editor.getEditorInput());

			// grab source which is either the whole document for JS files or a subset for nested JS
			// @formatter:off
			String source =
				(activeRange != null)
					? doc.get(activeRange.getStartingOffset(), activeRange.getLength())
					: doc.get();
			// @formatter:on
			int startingOffset = (activeRange != null) ? activeRange.getStartingOffset() : 0;
			
			// create parse state and turn off all processing of comments
			JSParseState parseState = new JSParseState(source, startingOffset, true, true);

			// parse and grab resulting AST
			IParseNode ast = ParserPoolFactory.parse(IJSConstants.CONTENT_TYPE_JS, parseState).getRootNode();

			if (ast != null)
			{
				result = ast.getNodeAtOffset(offset);

				// We won't get a current node if the cursor is outside of the positions
				// recorded by the AST
				if (result == null)
				{
					if (offset < ast.getStartingOffset())
					{
						result = ast.getNodeAtOffset(ast.getStartingOffset());
					}
					else if (ast.getEndingOffset() < offset)
					{
						result = ast.getNodeAtOffset(ast.getEndingOffset());
					}
				}
			}
		}
		catch (Exception e)
		{
			// ignore parse error exception since the user will get markers and/or entries in the Problems View
		}

		return result;
	}

	/**
	 * getArgumentIndex
	 * 
	 * @param offset
	 * @return
	 */
	private int getArgumentIndex(int offset)
	{
		JSArgumentsNode arguments = getArgumentsNode(offset);
		int result = -1;

		if (arguments != null)
		{
			for (IParseNode child : arguments)
			{
				if (child.contains(offset))
				{
					result = child.getIndex();
					break;
				}
			}
		}

		return result;
	}

	/**
	 * getArgumentsNode
	 * 
	 * @param offset
	 * @return
	 */
	private JSArgumentsNode getArgumentsNode(int offset)
	{
		IParseNode node = getActiveASTNode(offset);
		JSArgumentsNode result = null;

		// work a way up the AST to determine if we're in an arguments node
		while ((node instanceof JSNode) && node.getNodeType() != IJSNodeTypes.ARGUMENTS)
		{
			node = node.getParent();
		}		

		// process arguments node as long as we're not to the left of the opening parenthesis
		if (node instanceof JSNode && node.getNodeType() == IJSNodeTypes.ARGUMENTS
				&& node.getStartingOffset() != offset)
		{
			result = (JSArgumentsNode) node;
		}

		return result;
	}

	/*
	 * (non-Javadoc)
	 * @see com.aptana.editor.common.CommonContentAssistProcessor#getContextInformationValidator()
	 */
	@Override
	public IContextInformationValidator getContextInformationValidator()
	{
		return new JSContextInformationValidator();
	}
	
	/**
	 * Retrive one or more <code>FunctionElement</code>.  This method replace <code>getFunctionElement()</code> to
	 * provide 'fake polymorphism'.
	 * 
	 * @param viewer is a <code>ITextViewer</code>.
	 * @param offset is the current offset in viewer.
	 * @return <code>List</code> of matched <code>FunctionElement</code>, or emtpy <code>List</code> if there is no match.
	 */
	private List<FunctionElement> getFunctionElementList(ITextViewer viewer, int offset) {
		JSArgumentsNode node = getArgumentsNode(offset);
		List<FunctionElement> listReturn = new ArrayList<FunctionElement>();

		// process arguments node as long as we're not to the left of the opening parenthesis
		if (node != null)
		{
			// save current replace range. A bit hacky but better than adding a flag into getLocation's signature
			IRange range = replaceRange;

			// grab the content assist location type for the symbol before the arguments list
			int functionOffset = node.getStartingOffset();
			//LocationType locationAndOffset = getLocationType(viewer.getDocument(), functionOffset);
			LocationType location = getLocationType(viewer.getDocument(), functionOffset);

			// restore replace range
			replaceRange = range;

			// init type and method names
			String typeName = null;
			String methodName = null;

			switch (location)
			{
				case IN_VARIABLE_NAME:
				{
					typeName = JSTypeUtil.getGlobalType(getProject(), getFilename());
					methodName = node.getParent().getFirstChild().getText();
					break;
				}

				case IN_PROPERTY_NAME:
				{
					JSGetPropertyNode propertyNode = ParseUtil.getGetPropertyNode(node,
							((JSNode) node).getContainingStatementNode());
					List<String> types = getParentObjectTypes(propertyNode, offset);

					if (types.size() > 0)
					{
						typeName = types.get(0);
						methodName = propertyNode.getLastChild().getText();
					}
					break;
				}

				default:
					break;
			}

			if (typeName != null && methodName != null)
			{
				Collection<PropertyElement> properties = indexHelper.getTypeMembers(getIndex(), typeName, methodName);

				if (properties != null)
				{
					for (PropertyElement property : properties)
					{
						if (property instanceof FunctionElement)
						{
							FunctionElement currentFunction = (FunctionElement) property;
							
							// Function name and parameter name matched?
							boolean sudahAda = false;
							boolean ketemu = false;
							for (int i=0; i<listReturn.size(); i++) {
								if (currentFunction.getParameterNames().containsAll(listReturn.get(i).getParameterNames()) &&
									currentFunction.getParameterNames().size()==listReturn.get(i).getParameterNames().size()) {
									ketemu = true;
									// if function already in the list
									if (currentFunction.getDescription().trim().length()>0) {
										listReturn.set(i, currentFunction);
										sudahAda = true;										
										break;
									}
								}
							}
							
							// if function is not in the list, then add it to the list.
							if (!sudahAda && !ketemu) {
								listReturn.add(currentFunction);
							}
						}
					}
				}
			}
		}
		
		return listReturn;
	}

	/**
	 * getLocationByLexeme
	 * 
	 * @param lexemeProvider
	 * @param offset
	 * @return
	 */
	LocationType getLocationByLexeme(IDocument document, int offset)
	{
		// grab relevant lexemes around the current offset
		ILexemeProvider<JSTokenType> lexemeProvider = createLexemeProvider(document, offset);

		// assume we can't determine the location type
		LocationType result = LocationType.UNKNOWN;

		// find lexeme nearest to our offset
		int index = lexemeProvider.getLexemeIndex(offset);

		if (index < 0)
		{
			int candidateIndex = lexemeProvider.getLexemeFloorIndex(offset);
			Lexeme<JSTokenType> lexeme = lexemeProvider.getLexeme(candidateIndex);

			if (lexeme != null)
			{
				if (lexeme.getEndingOffset() == offset)
				{
					index = candidateIndex;
				}
				else if (lexeme.getType() == JSTokenType.NEW)
				{
					index = candidateIndex;
				}
			}
		}

		if (index >= 0)
		{
			Lexeme<JSTokenType> lexeme = lexemeProvider.getLexeme(index);

			switch (lexeme.getType())
			{
				case DOT:
					result = LocationType.IN_PROPERTY_NAME;
					break;

				case SEMICOLON:
					if (index > 0)
					{
						Lexeme<JSTokenType> previousLexeme = lexemeProvider.getLexeme(index - 1);

						switch (previousLexeme.getType())
						{
							case IDENTIFIER:
								result = LocationType.IN_GLOBAL;
								break;

							default:
								break;
						}
					}
					break;

				case LPAREN:
					if (offset == lexeme.getEndingOffset())
					{
						Lexeme<JSTokenType> previousLexeme = lexemeProvider.getLexeme(index - 1);

						if (previousLexeme.getType() != JSTokenType.IDENTIFIER)
						{
							result = LocationType.IN_GLOBAL;
						}
					}
					break;

				case RPAREN:
					if (offset == lexeme.getStartingOffset())
					{
						result = LocationType.IN_GLOBAL;
					}
					break;

				case IDENTIFIER:
					if (index > 0)
					{
						Lexeme<JSTokenType> previousLexeme = lexemeProvider.getLexeme(index - 1);

						switch (previousLexeme.getType())
						{
							case DOT:
								result = LocationType.IN_PROPERTY_NAME;
								break;

							case NEW:
								result = LocationType.IN_CONSTRUCTOR;
								break;

							case VAR:
								result = LocationType.IN_VARIABLE_DECLARATION;
								break;

							default:
								result = LocationType.IN_VARIABLE_NAME;
								break;
						}
					}
					else
					{
						result = LocationType.IN_VARIABLE_NAME;
					}
					break;

				default:
					break;
			}
		}
		else if (lexemeProvider.size() == 0)
		{
			result = LocationType.IN_GLOBAL;
		}

		return result;
	}

	/**
	 * getLocation
	 * 
	 * @param lexemeProvider
	 * @param offset
	 * @return
	 */
	LocationType getLocationType(IDocument document, int offset)
	{
		JSLocationIdentifier identifier = new JSLocationIdentifier(offset, getActiveASTNode(offset - 1));
		LocationType result = identifier.getType();

		targetNode = identifier.getTargetNode();
		statementNode = identifier.getStatementNode();
		replaceRange = identifier.getReplaceRange();

		// if we couldn't determine the location type with the AST, then
		// fallback to using lexemes
		if (result == LocationType.UNKNOWN)
		{
			// NOTE: this method call sets replaceRange as a side-effect
			result = getLocationByLexeme(document, offset);
		}

		return result;
	}

	/**
	 * @param result
	 * @return
	 */
	protected List<ICompletionProposal> getMergedProposals(List<ICompletionProposal> proposals)
	{
		// order proposals by display name
		Collections.sort(proposals, new Comparator<ICompletionProposal>()
		{
			public int compare(ICompletionProposal o1, ICompletionProposal o2)
			{
				int result = getImageIndex(o1) - getImageIndex(o2);

				if (result == 0)
				{
					result = o1.getDisplayString().compareTo(o2.getDisplayString());
				}

				return result;
			}

			protected int getImageIndex(ICompletionProposal proposal)
			{
				Image image = proposal.getImage();
				int result = 0;

				if (image == JS_KEYWORD)
				{
					result = 1;
				}
				else if (image == JS_PROPERTY)
				{
					result = 2;
				}
				else if (image == JS_PROPERTY)
				{
					result = 3;
				}

				return result;
			}
		});

		// remove duplicates, merging duplicates into a single proposal
		return CollectionsUtil.filter(proposals, new ProposalMerger());
	}

	private String getNestedFunctionTypeName(JSFunctionNode function)
	{
		List<String> names = new ArrayList<String>();
		IParseNode current = function;

		while (current != null && !(current instanceof JSParseRootNode))
		{
			if (current instanceof JSFunctionNode)
			{
				JSFunctionNode currentFunction = (JSFunctionNode) current;

				names.add(currentFunction.getName().getText());
			}

			current = current.getParent();
		}

		Collections.reverse(names);

		return StringUtil.join(IJSIndexConstants.NESTED_TYPE_SEPARATOR, names);
	}

	/**
	 * getParentObjectTypes
	 * 
	 * @param node
	 * @param offset
	 * @return
	 */
	protected List<String> getParentObjectTypes(JSGetPropertyNode node, int offset)
	{
		return ParseUtil.getParentObjectTypes(getIndex(), getURI(), targetNode, node, offset);
	}

	/*
	 * (non-Javadoc)
	 * @see com.aptana.editor.common.CommonContentAssistProcessor#getPreferenceNodeQualifier()
	 */
	protected String getPreferenceNodeQualifier()
	{
		return JSPlugin.PLUGIN_ID;
	}

	/**
	 * Expose replace range field for unit tests
	 * 
	 * @return
	 */
	IRange getReplaceRange()
	{
		return replaceRange;
	}

	/*
	 * (non-Javadoc)
	 * @see com.aptana.editor.common.CommonContentAssistProcessor#isValidActivationCharacter(char, int)
	 */
	public boolean isValidActivationCharacter(char c, int keyCode)
	{
		return Character.isWhitespace(c);
	}

	/*
	 * (non-Javadoc)
	 * @see com.aptana.editor.common.CommonContentAssistProcessor#triggerAdditionalAutoActivation(char, int,
	 * org.eclipse.jface.text.IDocument, int)
	 */
	public boolean isValidAutoActivationLocation(char c, int keyCode, IDocument document, int offset)
	{
		// NOTE: If auto-activation logic changes it may be necessary to change this logic
		// to continue walking backwards through partitions until a) a valid activation character
		// or b) a non-whitespace non-valid activation character is encountered. That implementation
		// would need to skip partitions that are effectively whitespace, for example, comment
		// partitions
		boolean result = false;

		try
		{
			ITypedRegion partition = document.getPartition(offset);

			if (partition != null && AUTO_ACTIVATION_PARTITION_TYPES.contains(partition.getType()))
			{
				int start = partition.getOffset();
				int index = offset - 1;

				while (index >= start)
				{
					char candidate = document.getChar(index);

					if (candidate == ',' || candidate == '(' || candidate == '{')
					{
						result = true;
						break;
					}
					else if (!Character.isWhitespace(candidate))
					{
						break;
					}

					index--;
				}
			}
		}
		catch (BadLocationException e)
		{
			// ignore
		}

		return result;
	}

	/*
	 * (non-Javadoc)
	 * @see com.aptana.editor.common.CommonContentAssistProcessor#isValidIdentifier(char, int)
	 */
	public boolean isValidIdentifier(char c, int keyCode)
	{
		return Character.isJavaIdentifierStart(c) || Character.isJavaIdentifierPart(c) || c == '$';
	}

	/**
	 * The currently active range
	 * 
	 * @param activeRange
	 */
	public void setActiveRange(IRange activeRange)
	{
		this.activeRange = activeRange;
	}
}

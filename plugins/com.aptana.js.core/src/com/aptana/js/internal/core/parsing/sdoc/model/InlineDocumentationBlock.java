package com.aptana.js.internal.core.parsing.sdoc.model;

/**
 * Model to represent inline doc comments. See 
 * <a href="http://code.google.com/p/jsdoc-toolkit/wiki/InlineDocs">
 * http://code.google.com/p/jsdoc-toolkit/wiki/InlineDocs</a> for example.
 * 
 * @author SolidSnake
 *
 */
public class InlineDocumentationBlock extends DocumentationBlock {
	
	public InlineDocumentationBlock(String content) {
		super(content);
	}

}

package com.aptana.editor.js.contentassist;

import java.net.URI;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ContextInformation;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension5;
import org.eclipse.swt.graphics.Image;

import com.aptana.editor.common.contentassist.CommonCompletionProposal;
import com.aptana.js.core.model.PropertyElement;

public class PropertyElementProposal extends CommonCompletionProposal implements ICompletionProposalExtension5
{

	private PropertyElement property;
	private URI uri;

	public PropertyElementProposal(PropertyElement property, int offset, int replaceLength, URI uri)
	{
		super(property.getName(), offset, replaceLength, property.getName().length(), null, property.getName(), null,
				null);
		this.property = property;
		this.uri = uri;
	}
	
	/*
	 * By Jocki Hendry - Support for ContextInformation
	 */
	public PropertyElementProposal(String displayString, String replacementString, int cursorPosition, 
		PropertyElement property, int offset, int replaceLength, URI uri, ContextInformation contextInformation)
	{		
		super(replacementString, offset, replaceLength, cursorPosition, null, displayString, contextInformation,
				null);
		this.property = property;
		this.uri = uri;
	}	

	@Override
	public String getFileLocation()
	{
		if (_fileLocation == null)
		{
			_fileLocation = JSModelFormatter.getTypeDisplayName(property.getOwningType());
		}
		return super.getFileLocation();
	}

	@Override
	public Image getImage()
	{
		if (_image == null)
		{
			_image = JSModelFormatter.ADDITIONAL_INFO.getImage(property);
		}
		return super.getImage();
	}

	public Object getAdditionalProposalInfo(IProgressMonitor monitor)
	{
		return JSModelFormatter.ADDITIONAL_INFO.getDescription(property, uri);
	}
	
	@Override
	public boolean validate(IDocument document, int offset, DocumentEvent event)
	{
		if (offset < this._replacementOffset)
			return false;
		
		int posisiKurung = getDisplayString().indexOf('(');
		String propertyName = null;
		if (posisiKurung == -1) {
			propertyName = getDisplayString();
		} else {
			propertyName = getDisplayString().substring(0, getDisplayString().indexOf('('));
		}
		int overlapIndex = propertyName.length() - _replacementString.length();
		overlapIndex = Math.max(0, overlapIndex);
		String endPortion = getDisplayString().substring(overlapIndex);
		boolean validated = isValidPrefix(getPrefix(document, offset), endPortion);

		if (validated && event != null)
		{
			// make sure that we change the replacement length as the document content changes
			int delta = ((event.fText == null) ? 0 : event.fText.length()) - event.fLength;
			final int newLength = Math.max(_replacementLength + delta, 0);
			_replacementLength = newLength;
		}

		return validated;
	}
	
	@Override
	public int getContextInformationPosition()
	{
		return super._replacementOffset;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((property == null) ? 0 : property.hashCode());
		result = prime * result + ((uri == null) ? 0 : uri.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		PropertyElementProposal other = (PropertyElementProposal) obj;
		if (property == null)
		{
			if (other.property != null)
				return false;
		}
		else if (!property.equals(other.property))
			return false;
		if (uri == null)
		{
			if (other.uri != null)
				return false;
		}
		else if (!uri.equals(other.uri))
			return false;
		return true;
	}
	
	

}

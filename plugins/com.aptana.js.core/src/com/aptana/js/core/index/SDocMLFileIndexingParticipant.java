/**
 * Aptana Studio
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the GNU Public License (GPL) v3 (with exceptions).
 * Please see the license.html included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.aptana.js.core.index;

import java.io.InputStream;
import java.net.URI;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;

import com.aptana.core.logging.IdeLog;
import com.aptana.core.util.CollectionsUtil;
import com.aptana.index.core.AbstractFileIndexingParticipant;
import com.aptana.index.core.Index;
import com.aptana.index.core.build.BuildContext;
import com.aptana.js.core.JSCorePlugin;
import com.aptana.js.core.JSTypeConstants;
import com.aptana.js.core.inferencing.JSTypeUtil;
import com.aptana.js.core.model.AliasElement;
import com.aptana.js.core.model.FunctionElement;
import com.aptana.js.core.model.PropertyElement;
import com.aptana.js.core.model.TypeElement;
import com.aptana.js.internal.core.index.JSIndexReader;
import com.aptana.js.internal.core.index.JSIndexWriter;
import com.aptana.js.internal.core.index.JSMetadataReader;

public class SDocMLFileIndexingParticipant extends AbstractFileIndexingParticipant
{
	public void index(BuildContext context, Index index, IProgressMonitor monitor) throws CoreException
	{
		if (context == null || index == null)
		{
			return;
		}

		SubMonitor sub = SubMonitor.convert(monitor, 100);
		try
		{
			sub.subTask(getIndexingMessage(index, context.getURI()));

			try
			{
				JSMetadataReader reader = new JSMetadataReader();

				InputStream stream = context.openInputStream(sub.newChild(5));

				// parse
				reader.loadXML(stream, context.getURI().toString());
				sub.worked(45);

				// create new Window type for this file
				JSIndexReader jsir = new JSIndexReader();
				String globalTypeName = JSTypeUtil.getGlobalType(context.getProject(), context.getName());
				List<TypeElement> windows = jsir.getType(index, globalTypeName, true);
				TypeElement window;

				if (!CollectionsUtil.isEmpty(windows))
				{
					window = windows.get(windows.size() - 1);
				}
				else
				{
					window = JSTypeUtil.createGlobalType(globalTypeName);
				}

				// process results
				JSIndexWriter indexer = new JSIndexWriter();
				TypeElement[] types = reader.getTypes();
				AliasElement[] aliases = reader.getAliases();
				URI location = context.getURI();

				// write types and add properties to Window
				for (TypeElement type : types)
				{
					// apply user agents to type
					type.setHasAllUserAgents();

					// apply user agents to all properties
					for (PropertyElement property : type.getProperties())
					{
						property.setHasAllUserAgents();
					}

					String typeName = type.getName();

					if (!typeName.contains(".") && !typeName.startsWith(JSTypeConstants.GENERIC_CLASS_OPEN)) //$NON-NLS-1$
					{
						List<FunctionElement> constructors = type.getConstructors();

						if (!constructors.isEmpty())
						{
							for (FunctionElement constructor : constructors)
							{
								// remove the constructor and make it a global function
								type.removeProperty(constructor);
								window.addProperty(constructor);
							}

							// wrap the type name in Function<> and update the property owningType references to
							// that name
							String newName = JSTypeUtil.toFunctionType(typeName);

							type.setName(newName);

							for (PropertyElement property : type.getProperties())
							{
								property.setOwningType(newName);
							}
						}
						else
						{
							PropertyElement property = window.getProperty(typeName);

							if (property == null)
							{
								property = new PropertyElement();

								property.setName(typeName);
								property.addType(typeName);
								property.setHasAllUserAgents();

								window.addProperty(property);
							}
						}
					}

					// NOTE: we write the type after processing top-level types in case the type name changes
					indexer.writeType(index, type, location);
				}

				for (AliasElement alias : aliases)
				{
					String typeName = alias.getType();

					// NOTE: we currently assume we can only alias types that were encountered in this sdocml file
					List<PropertyElement> listProperties = window.getProperties(typeName);

					if (listProperties.size() > 0) {
						
						for (PropertyElement property: listProperties) {
							// we found a property, now clone it
							if (property instanceof FunctionElement)
							{
								property = new FunctionElement((FunctionElement) property);
							}
							else
							{
								property = new PropertyElement(property);
							}
	
							// and change the name to match our alias
							property.setName(alias.getName());
							
							window.addProperty(property);
	
						}
						
					} else {
						
						// didn't find anything, so create a new property
						PropertyElement property = new PropertyElement();
						property.setName(alias.getName());
						property.addType(typeName);
						window.addProperty(property);
					}
						

				}

				// write global type info
				indexer.writeType(index, window, location);
			}
			catch (Throwable e)
			{
				IdeLog.logError(JSCorePlugin.getDefault(), e);
			}
		}
		finally
		{
			sub.done();
		}
	}
}

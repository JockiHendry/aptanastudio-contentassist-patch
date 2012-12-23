package com.aptana.editor.json;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class JSONPlugin extends AbstractUIPlugin
{
	public static final String PLUGIN_ID = "com.aptana.editor.json"; //$NON-NLS-1$
	private static JSONPlugin plugin;
	
	private IDocumentProvider jsonDocumentProvider;


	/**
	 * Returns the shared instance
	 * 
	 * @return the shared instance
	 */
	public static JSONPlugin getDefault()
	{
		return plugin;
	}

	/**
	 * getImage
	 * 
	 * @param path
	 * @return
	 */
	public static Image getImage(String path)
	{
		ImageRegistry registry = plugin.getImageRegistry();
		Image image = registry.get(path);

		if (image == null)
		{
			ImageDescriptor id = getImageDescriptor(path);

			if (id == null)
			{
				return null;
			}

			registry.put(path, id);
			image = registry.get(path);
		}

		return image;
	}

	/**
	 * getImageDescriptor
	 * 
	 * @param path
	 * @return
	 */
	public static ImageDescriptor getImageDescriptor(String path)
	{
		return AbstractUIPlugin.imageDescriptorFromPlugin(PLUGIN_ID, path);
	}

	/**
	 * The constructor
	 */
	public JSONPlugin()
	{
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception
	{
		super.start(context);
		plugin = this;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception
	{
		plugin = null;
		super.stop(context);
	}
	
	/**
	 * Returns JSON document provider
	 * @return
	 */
	public synchronized IDocumentProvider getJSONDocumentProvider()
	{
		if (jsonDocumentProvider == null)
		{
			jsonDocumentProvider = new JSONDocumentProvider();
		}
		return jsonDocumentProvider;
	}

}

/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.aptana.formatter.ui.dialogs;

import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.SelectionStatusDialog;
import org.eclipse.ui.model.WorkbenchLabelProvider;

import com.aptana.core.logging.IdeLog;
import com.aptana.formatter.IDebugScopes;
import com.aptana.formatter.ui.epl.FormatterUIEplPlugin;
import com.aptana.formatter.ui.util.StatusInfo;

/**
 * ProjectSelectionDialog
 */
public class ProjectSelectionDialog extends SelectionStatusDialog
{

	// the visual selection widget group
	private TableViewer fTableViewer;
	private Set<IProject> fProjectsWithSpecifics;

	// sizing constants
	private final static int SIZING_SELECTION_WIDGET_HEIGHT = 250;
	private final static int SIZING_SELECTION_WIDGET_WIDTH = 300;

	private final static String DIALOG_SETTINGS_SHOW_ALL = "ProjectSelectionDialog.show_all"; //$NON-NLS-1$

	private ViewerFilter fFilter;
	private String natureId;

	/**
	 * Constructs a new ProjectSelectionDialog.
	 * 
	 * @param parentShell
	 * @param projectsWithSpecifics
	 * @param natureId
	 */
	public ProjectSelectionDialog(Shell parentShell, Set<IProject> projectsWithSpecifics, String natureId)
	{
		super(parentShell);
		setTitle(Messages.ProjectSelectionDialog_projectSelectionTitle);
		setMessage(Messages.ProjectSelectionDialog_projectSelectionDescription);
		fProjectsWithSpecifics = projectsWithSpecifics;
		this.natureId = natureId;
		int shellStyle = getShellStyle();
		setShellStyle(shellStyle | SWT.MAX | SWT.RESIZE);

		fFilter = new ViewerFilter()
		{
			public boolean select(Viewer viewer, Object parentElement, Object element)
			{
				return fProjectsWithSpecifics.contains(element);
			}
		};

	}

	/**
	 * @see org.eclipse.jface.dialogs.Dialog#createDialogArea(org.eclipse.swt.widgets.Composite)
	 */
	protected Control createDialogArea(Composite parent)
	{
		// page group
		Composite composite = (Composite) super.createDialogArea(parent);

		Font font = parent.getFont();
		composite.setFont(font);

		createMessageArea(composite);

		fTableViewer = new TableViewer(composite, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
		fTableViewer.addSelectionChangedListener(new ISelectionChangedListener()
		{
			public void selectionChanged(SelectionChangedEvent event)
			{
				doSelectionChanged(((IStructuredSelection) event.getSelection()).toArray());
			}
		});
		fTableViewer.addDoubleClickListener(new IDoubleClickListener()
		{
			public void doubleClick(DoubleClickEvent event)
			{
				okPressed();
			}
		});
		GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
		data.heightHint = SIZING_SELECTION_WIDGET_HEIGHT;
		data.widthHint = SIZING_SELECTION_WIDGET_WIDTH;
		fTableViewer.getTable().setLayoutData(data);

		fTableViewer.setLabelProvider(new WorkbenchLabelProvider());
		fTableViewer.setContentProvider(ArrayContentProvider.getInstance());

		fTableViewer.getControl().setFont(font);

		if (natureId != null)
		{
			fTableViewer.addFilter(new ViewerFilter()
			{
				public boolean select(Viewer viewer, Object parentElement, Object element)
				{
					if (element instanceof IProject)
					{
						IProject project = (IProject) element;
						try
						{
							return project.hasNature(natureId);
						}
						catch (CoreException e)
						{
							IdeLog.logError(FormatterUIEplPlugin.getDefault(), e, IDebugScopes.DEBUG);
							return false;
						}
					}
					return true;
				}
			});
		}

		Button checkbox = new Button(composite, SWT.CHECK);
		checkbox.setText(Messages.ProjectSelectionDialog_filter);
		checkbox.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, true, false));
		checkbox.addSelectionListener(new SelectionListener()
		{
			public void widgetSelected(SelectionEvent e)
			{
				updateFilter(((Button) e.widget).getSelection());
			}

			public void widgetDefaultSelected(SelectionEvent e)
			{
				updateFilter(((Button) e.widget).getSelection());
			}
		});
		IDialogSettings dialogSettings = FormatterUIEplPlugin.getDefault().getDialogSettings();
		boolean doFilter = !dialogSettings.getBoolean(DIALOG_SETTINGS_SHOW_ALL) && !fProjectsWithSpecifics.isEmpty();
		checkbox.setSelection(doFilter);
		updateFilter(doFilter);

		IProject[] input = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		fTableViewer.setInput(input);

		doSelectionChanged(new Object[0]);
		Dialog.applyDialogFont(composite);
		return composite;
	}

	/**
	 * @param selected
	 */
	protected void updateFilter(boolean selected)
	{
		if (selected)
		{
			fTableViewer.addFilter(fFilter);
		}
		else
		{
			fTableViewer.removeFilter(fFilter);
		}
		FormatterUIEplPlugin.getDefault().getDialogSettings().put(DIALOG_SETTINGS_SHOW_ALL, !selected);
	}

	private void doSelectionChanged(Object[] objects)
	{
		if (objects.length != 1)
		{
			updateStatus(new StatusInfo(IStatus.ERROR, "")); //$NON-NLS-1$
			setSelectionResult(null);
		}
		else
		{
			updateStatus(new StatusInfo());
			setSelectionResult(objects);
		}
	}

	/**
	 * @see org.eclipse.ui.dialogs.SelectionStatusDialog#computeResult()
	 */
	protected void computeResult()
	{
	}
}

package com.jocki.test;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.part.ViewPart;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.EcmaError;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.Scriptable;

public class JavaScriptEvalView extends ViewPart implements SelectionListener {

	public static final String VIEW_ID = "com.jocki.JavaScriptEvalView"; //$NON-NLS-1$

	private Text txtInput, txtOutput;
	private Button btnProses;
	private Context rhinoContext;
	private Scriptable scope;
	
	public JavaScriptEvalView() {
		rhinoContext = Context.enter();
		scope = rhinoContext.initStandardObjects();
	}

	@Override
	public void createPartControl(Composite parent) {
		
		parent.setLayout(new GridLayout(3, false));
		
		Label lblNama = new Label(parent, SWT.NONE);
		lblNama.setText("Expression: ");
		
		txtInput = new Text(parent, SWT.BORDER);
		GridData gridData = new GridData();
		gridData.horizontalAlignment = SWT.FILL;
		gridData.grabExcessHorizontalSpace = true;
		txtInput.setLayoutData(gridData);
		txtInput.addSelectionListener(this);
		
		btnProses = new Button(parent, SWT.PUSH);
		btnProses.setText("E&xecute");
		btnProses.addSelectionListener(this);
		
		txtOutput = new Text(parent, SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL | SWT.H_SCROLL);		
		gridData = new GridData();
		gridData.horizontalAlignment = SWT.FILL;
		gridData.verticalAlignment = SWT.FILL;
		gridData.horizontalSpan = 3;
		gridData.grabExcessHorizontalSpace = true;
		gridData.grabExcessVerticalSpace = true;
		txtOutput.setLayoutData(gridData);
		txtOutput.setEditable(false);		
	}

	@Override
	public void setFocus() {
		txtInput.selectAll();
		txtInput.setFocus();
	}

	public void widgetSelected(SelectionEvent e) {
		prosesJavaScript();
	}

	public void widgetDefaultSelected(SelectionEvent e) {
		prosesJavaScript();
	}
		
	private void prosesJavaScript() {
		Object result = null;
		try {
			result = rhinoContext.evaluateString(scope, txtInput.getText(), "<eval>", 1, null);
		} catch (EcmaError error) {
			result = error.getErrorMessage();			
		} catch (EvaluatorException error) {
			result = error.getMessage();
		} catch (Exception ex) {
			result = ex.getMessage();
		}
		txtOutput.setText(Context.toString(result));
	}
	
	@Override
	public void dispose() {
		Context.exit();
	}
	
}

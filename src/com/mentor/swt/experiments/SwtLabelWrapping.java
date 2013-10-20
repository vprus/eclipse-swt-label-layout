/*******************************************************************************
 * Copyright (C) 2013 Mentor Graphics
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Vladimir Prus - initial API and implementation
 *******************************************************************************/
package com.mentor.swt.experiments;

import org.eclipse.swt.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.TextLayout;
import org.eclipse.swt.layout.*;

/* In SWT, a label with SWT.WRAP flag will still report full unwrapped width as size hint.
 * Therefore, putting wrapping label with long text inside a dialog can make it unreasonably
 * wide. It is possible to set a hardcoded size for the dialog, but it's better solved at
 * the level of label, which is what this test code tries to implement.
 * 
 * Another SWT deficiency is that shell.pack just sets the size to initial 
 * 
 * */
public class SwtLabelWrapping {
	
	// Different ways to compute label size hint
	enum LabelHint { 
		DEFAULT_HINT,  // Use default SWT implementation 
		REDUCED_WIDTH, // Return width of 1 
		PROPORTIONAL   // Try to make rectangle of reasonable proportions 
	};
	static LabelHint labelHint = LabelHint.REDUCED_WIDTH;
	
	// For PROPORTIONAL mode, use heuristic from Qt library.
	static boolean USE_QT_HEURISTIC = true;
	
	// If true, do a second iteration of packing, with width set.
	static boolean DOUBLE_PACK = true;
	
	
	public static final class WrappingLabel extends Label {
		private WrappingLabel(Composite parent, int style) {
			super(parent, style);
		}

		@Override
		public Point computeSize(int wHint, int hHint, boolean changed) {
			
			Point p = super.computeSize(wHint, hHint, changed);
			
			if (labelHint == LabelHint.DEFAULT_HINT) {
				// Do nothing.				
			}
			
			if (labelHint == LabelHint.REDUCED_WIDTH) {
				// Unless width is already set, say we just want one pixel of width, so
				// as not to constrain the parent. If we are given the width, and so
				// computed height for that width already, return it.
				// See use of DOUBLE_PACK below for more comments.
				if (wHint == SWT.DEFAULT && hHint == SWT.DEFAULT) {	
					p.x = 1;
				}				
			}
			
			if (labelHint == LabelHint.PROPORTIONAL) {
				
				if (wHint == SWT.DEFAULT) {
					
					// No specific width request yet.
										
					GC gc = new GC(this);
					FontMetrics fm = gc.getFontMetrics();
					int averageCharWidth = fm.getAverageCharWidth();
					gc.dispose();
					
					TextLayout layout = new TextLayout(this.getDisplay());
					
					
					Rectangle bounds;
					layout.setText(getText());
					
					if (!USE_QT_HEURISTIC)
					{
						// The simple way is to force the label into readable number of characters,
						// horizontally. What's readable is subjective, we'll use 65 characters. 
						int w = 65 * averageCharWidth;
						layout.setWidth(w);							
						bounds = layout.getBounds();
					}
					else
					{
						// This approach is what Qt does:
						//    https://qt.gitorious.org/qt/qtbase/source/a94e9329450be060256e5040c095c6175d5ec19e:src/widgets/widgets/qlabel.cpp#L627
						// There's no documentation, so one can assume that they consider two things ugly:
						// - Block of 1 to 3 lines wider that 40 'average' characters, and
						// - Block of 1 line wider that 20 'average' characters.
						bounds = layout.getBounds();
						int w = 80 * averageCharWidth;
					
						if (layout.getLineCount() < 4 && bounds.width > w/2)
						{
							layout.setWidth(w/2);
							bounds = layout.getBounds();
						}
						
						if (layout.getLineCount() < 2 && bounds.width > w/4)
						{
							layout.setWidth(w/4);
							bounds = layout.getBounds();
						}						
					}
					
					p.x = bounds.width;
					p.y = bounds.height;
				}
				else
				{
					// The width is already set. Just fill all of it.
				}				
			}
			return p;
		}

		@Override
		protected void checkSubclass() {				
		}
	}

	
	public static void main(String[] args) throws IllegalArgumentException, IllegalAccessException {
		
		Display display = new Display();
		Shell shell = new Shell(display) {
			
			@Override
			public void pack(boolean changed) {
				Point p = computeSize (SWT.DEFAULT, SWT.DEFAULT, changed);
				// The final width might differ from preferred width of children.
				// One obvious case is when a child is narrower than the computed
				// hint for the shell (which is determined by some other child).
				// Or, when a label is configured to always return preferred width
				// of 1, the computed size of shell might be smaller than normal
				// width of widget. This case is somewhat artificial.
				//
				// Either case, because the width is not equal to preferred width,
				// the height may differ from preferred height. If we don't call
				// computeSize again with the final width, then we might have 
				// either too much vertical space, or too little vertical space.
				if (DOUBLE_PACK) {
					p = computeSize (p.x, SWT.DEFAULT, false);
				}
				setSize(p);
			}
			
			@Override
			protected void checkSubclass() {
			}
		};
		shell.setText("SWT Label Wrapping");
		GridLayout layout = new GridLayout(1, false);
		shell.setLayout(layout);
		
		Label l = new WrappingLabel(shell, SWT.WRAP);
		l.setText("What is the best way to put a label with long text in UI, like some help text?");
		
		GridData gd1 = new GridData();
		gd1.grabExcessHorizontalSpace = true;
		gd1.horizontalAlignment = SWT.FILL;
		l.setLayoutData(gd1);
		
		Button b = new Button(shell, SWT.PUSH);
		b.setText("Got it");
		
		GridData gd2 = new GridData();
		gd2.grabExcessHorizontalSpace = true;	
		gd2.horizontalAlignment = SWT.FILL;
		b.setLayoutData(gd2);	
				
		shell.pack();
		shell.open();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) display.sleep();
		}
		display.dispose();
	}
}
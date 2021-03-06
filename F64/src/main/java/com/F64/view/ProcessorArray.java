package com.F64.view;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;

import com.F64.Compiler;
import com.F64.Dictionary;
import com.F64.Interpreter;

@SuppressWarnings("serial")
public class ProcessorArray extends JFrame implements ActionListener, ItemListener, Runnable {
	private Processor			view;
	private System				system_view;
	private Disassemble			disassemble_view;
	private com.F64.ProcessorArray		processor_array;
	private Interpreter			interpreter;
	private JToggleButton[][]	toggle_array;
	private JTextField[][]		connector_array1;
	private JTextField[][]		connector_array2;
	private JTextField[][]		connector_array3;
	private JTextField[][]		connector_array4;
	private JSplitPane			main_split_pane;
	private JToolBar			toolbar;
	private JButton				run;
	private JButton				go;
	private JButton				trace;
	private JButton				step;
	private JButton				stop;
	private JButton				reset;
	private JButton				power_on;
	private JScrollPane 		scroll;
	private JPanel				main_panel;
	private int					selected_x;
	private int					selected_y;
	private volatile boolean	updating;
	private volatile boolean	tracing;
	private volatile boolean	running;
	private volatile boolean	free_running;

	private void connect(int columns, int rows, int x, int y)
	{
		com.F64.Processor from = processor_array.getProcessor(x, y);
		for (com.F64.Port p : com.F64.Port.values()) {
			com.F64.Processor to = processor_array.getNeighbor(x, y, p);
			if (to != null) {
				from.setPortPartner(p, to);
				to.setPortPartner(p, from);
			}
		}
	}
	
	private JTextField getPort(int x, int y, com.F64.Port p)
	{
		switch (p) {
		case DOWN:
			if ((y & 1) == 0) {return connector_array4[y][x];}
			return connector_array3[y][x];
		case LEFT:
			if ((x & 1) == 0) {return connector_array1[y][x];}
			return connector_array2[y][x];
		case RIGHT:
			if ((x & 1) == 0) {return connector_array2[y][x];}
			return connector_array1[y][x];
		case UP:
			if ((y & 1) == 0) {return connector_array3[y][x];}
			return connector_array4[y][x];
		default:
			break;
		
		}
		return null;
	}
	
	private void addArray(JPanel panel, int x0, int y0)
	{
		JLabel label;
		Font font = new Font(Font.MONOSPACED, Font.BOLD , 12);
		int rows = this.processor_array.getRows();
		int columns = this.processor_array.getColumns();
		Insets insets = new Insets( 0, 0, 0, 0);
		this.toggle_array = new JToggleButton[rows][columns];
		if ((rows > 1) && (columns > 1)) {
			this.connector_array1 = new JTextField[rows][columns];
			this.connector_array2 = new JTextField[rows][columns];
			this.connector_array3 = new JTextField[rows][columns];
			this.connector_array4 = new JTextField[rows][columns];
		}
		final int factor = 5;
		for (int y=0; y<rows; ++y) {
			for (int x=0; x<columns; ++x) {
				this.processor_array.getProcessor(x, y).powerOn();
				this.toggle_array[y][x] = new JToggleButton(x+"."+y);
				this.toggle_array[y][x].addActionListener(this);
				int xpos = x0+factor*x;
				int ypos = y0+factor*y;
				panel.add(
					this.toggle_array[y][x],
					new GridBagConstraints(
						xpos+1, ypos+1,
						factor-2, factor-2,
						0.0, 1.0,
						GridBagConstraints.WEST,
						GridBagConstraints.BOTH,
						insets,
						0, 0
					)
				);
				// left port
				this.connector_array1[y][x] = new JTextField("", 1);
				this.connector_array1[y][x].setFont(font);
				this.connector_array1[y][x].setHorizontalAlignment(JTextField.CENTER);
				panel.add(
					this.connector_array1[y][x],
					new GridBagConstraints(
						xpos, ypos+2,
						1, 1,
						0.5, 1.0,
						GridBagConstraints.WEST,
						GridBagConstraints.BOTH,
						insets,
						0, 0
					)
				);
				// right port
				this.connector_array2[y][x] = new JTextField("", 1);
				this.connector_array2[y][x].setFont(font);
				this.connector_array2[y][x].setHorizontalAlignment(JTextField.CENTER);
				panel.add(
					this.connector_array2[y][x],
					new GridBagConstraints(
						xpos+factor-1, ypos+2,
						1, 1,
						0.5, 1.0,
						GridBagConstraints.WEST,
						GridBagConstraints.BOTH,
						insets,
						0, 0
					)
				);
				// up port
				this.connector_array3[y][x] = new JTextField("", 1);
				this.connector_array3[y][x].setFont(font);
				this.connector_array3[y][x].setHorizontalAlignment(JTextField.CENTER);
				panel.add(
					this.connector_array3[y][x],
					new GridBagConstraints(
						xpos+2, ypos,
						1, 1,
						1.0, 0.5,
						GridBagConstraints.WEST,
						GridBagConstraints.BOTH,
						insets,
						0, 0
					)
				);
				// down port
				this.connector_array4[y][x] = new JTextField("", 1);
				this.connector_array4[y][x].setFont(font);
				this.connector_array4[y][x].setHorizontalAlignment(JTextField.CENTER);
				panel.add(
					this.connector_array4[y][x],
					new GridBagConstraints(
						xpos+2, ypos+factor-1,
						1, 1,
						1.0, 0.5,
						GridBagConstraints.WEST,
						GridBagConstraints.BOTH,
						insets,
						0, 0
					)
				);
				// horizontal label
				if (x == 0) {
					label = new JLabel(((x & 1) != 0) ? "R" : "L");
					label.setHorizontalAlignment(JTextField.CENTER);
					panel.add(
						label,
						new GridBagConstraints(
							xpos, ypos+1,
							1, 1,
							0.0, 0.0,
							GridBagConstraints.WEST,
							GridBagConstraints.BOTH,
							insets,
							0, 0
						)
					);
					//
					label = new JLabel(((x & 1) != 0) ? "R" : "L");
					label.setHorizontalAlignment(JTextField.CENTER);
					panel.add(
						label,
						new GridBagConstraints(
							xpos, ypos+3,
							1, 1,
							0.0, 0.0,
							GridBagConstraints.WEST,
							GridBagConstraints.BOTH,
							insets,
							0, 0
						)
					);
				}
				else {
					label = new JLabel(((x & 1) != 0) ? "R" : "L");
					label.setHorizontalAlignment(JTextField.CENTER);
					panel.add(
						label,
						new GridBagConstraints(
							xpos-1, ypos+1,
							2, 1,
							0.0, 0.0,
							GridBagConstraints.WEST,
							GridBagConstraints.BOTH,
							insets,
							0, 0
						)
					);
					//
					label = new JLabel(((x & 1) != 0) ? "R" : "L");
					label.setHorizontalAlignment(JTextField.CENTER);
					panel.add(
						label,
						new GridBagConstraints(
							xpos-1, ypos+3,
							2, 1,
							0.0, 0.0,
							GridBagConstraints.WEST,
							GridBagConstraints.BOTH,
							insets,
							0, 0
						)
					);
				}
				if (x == (columns-1)) {
					label = new JLabel(((x & 1) == 0) ? "R" : "L");
					label.setHorizontalAlignment(JTextField.CENTER);
					panel.add(
						label,
						new GridBagConstraints(
							xpos+factor-1, ypos+1,
							1, 1,
							0.0, 0.0,
							GridBagConstraints.WEST,
							GridBagConstraints.BOTH,
							insets,
							0, 0
						)
					);
					//
					label = new JLabel(((x & 1) == 0) ? "R" : "L");
					label.setHorizontalAlignment(JTextField.CENTER);
					panel.add(
						label,
						new GridBagConstraints(
							xpos+factor-1, ypos+3,
							1, 1,
							0.0, 0.0,
							GridBagConstraints.WEST,
							GridBagConstraints.BOTH,
							insets,
							0, 0
						)
					);
				}
				// vertical label
				if (y == 0) {
					label = new JLabel(((y & 1) == 0) ? "U" : "D");
					label.setHorizontalAlignment(JTextField.CENTER);
					panel.add(
						label,
						new GridBagConstraints(
							xpos+1, ypos,
							1, 1,
							0.0, 0.0,
							GridBagConstraints.WEST,
							GridBagConstraints.BOTH,
							insets,
							0, 0
						)
					);
					//
					label = new JLabel(((y & 1) == 0) ? "U" : "D");
					label.setHorizontalAlignment(JTextField.CENTER);
					panel.add(
						label,
						new GridBagConstraints(
							xpos+3, ypos,
							1, 1,
							0.0, 0.0,
							GridBagConstraints.WEST,
							GridBagConstraints.BOTH,
							insets,
							0, 0
						)
					);
				}
				else {
					label = new JLabel(((y & 1) == 0) ? "U" : "D");
					label.setHorizontalAlignment(JTextField.CENTER);
					panel.add(
						label,
						new GridBagConstraints(
							xpos+1, ypos-1,
							1, 2,
							0.0, 0.0,
							GridBagConstraints.WEST,
							GridBagConstraints.BOTH,
							insets,
							0, 0
						)
					);
					//
					label = new JLabel(((y & 1) == 0) ? "U" : "D");
					label.setHorizontalAlignment(JTextField.CENTER);
					panel.add(
						label,
						new GridBagConstraints(
							xpos+3, ypos-1,
							1, 2,
							0.0, 0.0,
							GridBagConstraints.WEST,
							GridBagConstraints.BOTH,
							insets,
							0, 0
						)
					);
				}
				if (y == (rows-1)) {
					label = new JLabel(((y & 1) != 0) ? "U" : "D");
					label.setHorizontalAlignment(JTextField.CENTER);
					panel.add(
						label,
						new GridBagConstraints(
							xpos+1, ypos+factor-1,
							1, 1,
							0.0, 0.0,
							GridBagConstraints.WEST,
							GridBagConstraints.BOTH,
							insets,
							0, 0
						)
					);
					//
					label = new JLabel(((y & 1) != 0) ? "U" : "D");
					label.setHorizontalAlignment(JTextField.CENTER);
					panel.add(
						label,
						new GridBagConstraints(
							xpos+3, ypos+factor-1,
							1, 1,
							0.0, 0.0,
							GridBagConstraints.WEST,
							GridBagConstraints.BOTH,
							insets,
							0, 0
						)
					);
				}

				//
//				label = new JLabel(((x & 1) == 0) ? "R" : "L");
//				label.setHorizontalAlignment(JTextField.CENTER);
//				panel.add(
//					label,
//					new GridBagConstraints(
//						x0+factor*x+factor-2, y0+factor*y,
//						2, 1,
//						0.0, 0.0,
//						GridBagConstraints.WEST,
//						GridBagConstraints.BOTH,
//						insets,
//						0, 0
//					)
//				);
//				label = new JLabel(((x & 1) == 0) ? "R" : "L");
//				label.setHorizontalAlignment(JTextField.CENTER);
//				panel.add(
//					label,
//					new GridBagConstraints(
//						x0+factor*x+factor-2, y0+factor*y+2,
//						2, 1,
//						0.0, 0.0,
//						GridBagConstraints.WEST,
//						GridBagConstraints.BOTH,
//						insets,
//						0, 0
//					)
//				);
//				//
//				this.connector_array1[y][x] = new JTextField(" ");
//				this.connector_array1[y][x].setHorizontalAlignment(JTextField.CENTER);
//				panel.add(
//					this.connector_array1[y][x],
//					new GridBagConstraints(
//						x0+factor*x+1, y0+factor*y+factor-2,
//						1, 1,
//						0.5, 0.0,
//						GridBagConstraints.WEST,
//						GridBagConstraints.BOTH,
//						insets,
//						0, 0
//					)
//				);
//				this.connector_array2[y][x] = new JTextField(" ");
//				this.connector_array2[y][x].setHorizontalAlignment(JTextField.CENTER);
//				panel.add(
//					this.connector_array2[y][x],
//					new GridBagConstraints(
//						x0+factor*x+1, y0+factor*y+factor-1,
//						1, 1,
//						0.5, 0.0,
//						GridBagConstraints.WEST,
//						GridBagConstraints.BOTH,
//						insets,
//						0, 0
//					)
//				);
//				//
//				label = new JLabel(((y & 1) == 0) ? "U" : "D");
//				panel.add(
//					label,
//					new GridBagConstraints(
//						x0+factor*x, y0+factor*y+factor-2,
//						1, 2,
//						0.0, 0.0,
//						GridBagConstraints.WEST,
//						GridBagConstraints.BOTH,
//						insets,
//						0, 0
//					)
//				);
//				label = new JLabel(((y & 1) == 0) ? "U" : "D");
//				panel.add(
//					label,
//					new GridBagConstraints(
//						x0+factor*x+2, y0+factor*y+factor-2,
//						1, 2,
//						0.0, 0.0,
//						GridBagConstraints.WEST,
//						GridBagConstraints.BOTH,
//						insets,
//						0, 0
//					)
//				);
//				//
//				this.connector_array3[y][x] = new JTextField(" ");
//				panel.add(
//					this.connector_array3[y][x],
//					new GridBagConstraints(
//						x0+factor*x+factor-2, y0+factor*y+1,
//						1, 1,
//						0.0, 0.5,
//						GridBagConstraints.WEST,
//						GridBagConstraints.BOTH,
//						insets,
//						10, 0
//					)
//				);
//				this.connector_array4[y][x] = new JTextField(" ");
//				panel.add(
//					this.connector_array4[y][x],
//					new GridBagConstraints(
//						x0+factor*x+factor-1, y0+factor*y+1,
//						1, 1,
//						0.0, 0.5,
//						GridBagConstraints.WEST,
//						GridBagConstraints.BOTH,
//						insets,
//						10, 0
//					)
//				);
				connect(columns, rows, x, y);
//				if ((x > 0) && (y > 0)) {
//					
//				}
			}			
		}
		this.toggle_array[0][0].setSelected(true);
	}
	
	public ProcessorArray(com.F64.ProcessorArray pa, Interpreter i, Compiler c, com.F64.System s, Dictionary d)
	{
		processor_array = pa;
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setSize(720,480);
		this.setTitle("Processor Array View");
		
		this.run = new JButton("Run");
		this.go = new JButton("Go");
		this.trace = new JButton("Trace");
		this.step = new JButton("Step");
		this.stop = new JButton("Stop");
		this.reset = new JButton("Reset");
		this.power_on = new JButton("Power on");

		this.toolbar = new JToolBar();
		this.toolbar.setFloatable(false);
		this.toolbar.add(this.run);
		this.toolbar.add(this.trace);
		this.toolbar.add(this.step);
		this.toolbar.add(this.stop);
		this.toolbar.add(this.go);
		this.toolbar.add(this.reset);
		this.toolbar.add(this.power_on);
		this.main_panel = new JPanel( new GridBagLayout() );
		this.main_split_pane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		this.scroll = new JScrollPane(this.main_panel);
		this.main_split_pane.setTopComponent(this.toolbar);
		this.main_split_pane.setBottomComponent(this.scroll);

		addArray(this.main_panel, 0, 0);

		this.stop.setEnabled(false);
		this.run.addActionListener(this);
		this.go.addActionListener(this);
		this.trace.addActionListener(this);
		this.step.addActionListener(this);
		this.stop.addActionListener(this);
		this.reset.addActionListener(this);
		this.power_on.addActionListener(this);
		this.add(this.main_split_pane);

		this.interpreter = i;
		this.view = new Processor(pa.getProcessor(0, 0), i, c, s, d);
		this.system_view = new System(s);
		this.disassemble_view = new Disassemble(s);
		
		setVisible(true);
	}
	
	@Override
	public void itemStateChanged(ItemEvent ev)
	{
		if (this.updating) {return;}
		
	}

	@Override
	public void actionPerformed(ActionEvent ev)
	{
		if (this.updating) {return;}
		Object source = ev.getSource();
		if (source == this.step) {
			step();
			this.update();
		}
		else if (source == this.run) {
			this.start();
		}
		else if (source == this.trace) {
			this.trace();
		}
		else if (source == this.stop) {
			this.stop();
		}
		else if (source == this.reset) {
			this.reset();
		}
		else if (source == this.go) {
			this.go();
		}
		else if (source == this.power_on) {
			this.powerOn();
		}
		else {
			int rows = this.processor_array.getRows();
			int columns = this.processor_array.getColumns();
			for (int y=0; y<rows; ++y) {
				for (int x=0; x<columns; ++x) {
					if (this.toggle_array[y][x] == source) {
						this.updating = true;
						if ((this.selected_x == x) && (this.selected_y == y)) {
							// deselect current
							this.toggle_array[y][x].setSelected(true);
						}
						else {
							this.toggle_array[this.selected_y][this.selected_x].setSelected(false);
							com.F64.Processor p = this.processor_array.getProcessor(x, y);
							this.interpreter.setProcessor(p);
							this.view.setProcessor(p);
							this.selected_x = x;
							this.selected_y = y;
						}
						this.updating = false;
						return;
					}
				}
			}
		}
	}

	public boolean step()
	{
		this.processor_array.boot();
		boolean res = true;
		int x = 0, y = 0;
		int rows = this.processor_array.getRows();
		int columns = this.processor_array.getColumns();
		for (y=0; y<rows; ++y) {
			for (x=0; x<columns; ++x) {
				try {
					this.processor_array.getProcessor(x, y).step();
				}
				catch (java.lang.Exception ex) {
					res = false;
				}
			}
		}
		return res;
	}

	public void start()
	{
		if (!this.running) {
			this.running = true;
			this.free_running = false;
			this.tracing = false;
			this.step.setEnabled(false);
			this.run.setEnabled(false);
			this.go.setEnabled(false);
			this.trace.setEnabled(false);
			this.reset.setEnabled(false);
			this.stop.setEnabled(true);
			Thread thread = new Thread(this);
			thread.start();
		}
	}

	public void go()
	{
		if (!this.running) {
			this.running = true;
			this.free_running = true;
			this.processor_array.start();
			this.tracing = false;
			this.step.setEnabled(false);
			this.run.setEnabled(false);
			this.go.setEnabled(false);
			this.trace.setEnabled(false);
			this.reset.setEnabled(false);
			this.stop.setEnabled(true);
			Thread thread = new Thread(this);
			thread.start();
		}
	}

	public void trace()
	{
		if (!this.running) {
			this.running = true;
			this.tracing = true;
			this.step.setEnabled(false);
			this.run.setEnabled(false);
			this.go.setEnabled(false);
			this.trace.setEnabled(false);
			this.reset.setEnabled(false);
			this.stop.setEnabled(true);
			Thread thread = new Thread(this);
			thread.start();
		}
	}

	public void stop()
	{
		this.running = false;
		if (this.free_running) {
			this.processor_array.stop();
			this.free_running = false;
		}
		this.step.setEnabled(true);
		this.run.setEnabled(true);
		this.go.setEnabled(true);
		this.trace.setEnabled(true);
		this.stop.setEnabled(false);
		this.reset.setEnabled(true);
	}


	public void reset()
	{
		this.stop();
		this.processor_array.reset();
		this.update();
	}

	public void powerOn()
	{
		this.stop();
		this.processor_array.powerOn();
		this.update();
	}

	public void update()
	{
		view.update();
		system_view.update();
		disassemble_view.update();
		int x = 0, y = 0;
		int rows = this.processor_array.getRows();
		int columns = this.processor_array.getColumns();
		JTextField field;
		for (y=0; y<rows; ++y) {
			for (x=0; x<columns; ++x) {
				com.F64.Processor proc = processor_array.getProcessor(x, y);
				if (proc.isWaiting()) {
					int mask = proc.getPortReadMask();
					if (mask != 0) {
						for (com.F64.Port p : com.F64.Port.values()) {
							if ((mask & (1 << p.ordinal())) != 0) {
								field = this.getPort(x, y, p);
								if (field != null) {field.setText("@");}
							}
						}
					}
					mask = proc.getPortWriteMask();
					if (mask != 0) {
						for (com.F64.Port p : com.F64.Port.values()) {
							if ((mask & (1 << p.ordinal())) != 0) {
								field = this.getPort(x, y, p);
								if (field != null) {field.setText("!");}
							}
						}
					}
				}
				else {
					this.getPort(x, y, com.F64.Port.RIGHT).setText("");
					this.getPort(x, y, com.F64.Port.LEFT).setText("");
					this.getPort(x, y, com.F64.Port.UP).setText("");
					this.getPort(x, y, com.F64.Port.DOWN).setText("");
				}
				if (proc.hasFailed()) {
					this.toggle_array[y][x].setBackground(Color.RED);
				}
				else {
					this.toggle_array[y][x].setBackground(null);
				}
			}
		}
	}

	@Override
	public void run()
	{
		int cnt = 0;
		while (this.running) {
			if (this.free_running) {
				this.update();
				try {
					Thread.sleep(100);
				}
				catch (InterruptedException e) {
				}
			}
			else {
				if (!this.step()) {
					this.update();
					this.stop();
				}
				if (this.tracing) {
					this.update();
					try {
						Thread.sleep(100);
					}
					catch (InterruptedException e) {
					}
				}
				else {
					if ((++cnt & 0xff) == 0) {
						this.update();
					}
				}
			}
		}
	}
	


}

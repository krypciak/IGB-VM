package me.krypek.igb.vm;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Graphics2D;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.Stack;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import me.krypek.freeargparser.ArgType;
import me.krypek.freeargparser.ParsedData;
import me.krypek.freeargparser.ParserBuilder;
import me.krypek.igb.cl1.IGB_CL1;
import me.krypek.igb.cl1.IGB_L1;
import me.krypek.igb.cl1.IGB_MA;
import me.krypek.igb.cl2.IGB_CL2;
import me.krypek.utils.Utils;

public class IGB_VM {

	public static void error(String a, String b) { JOptionPane.showMessageDialog(null, b, a, 0); }

	public static void info(String a, String b) { JOptionPane.showMessageDialog(null, b, a, 1); }

	public static void main(String[] args) {
		//@f:off
		ParsedData data = new ParserBuilder() 
				.add("p", 	"pes", 		false, false, ArgType.String, 		"Path to .igb_pes file")
				.add("cp", 	"codePaths",false, false, ArgType.StringArray, 	"Array of code paths to compile.")
				.add("sl", 	"startline",true,  false, ArgType.Int, 			"Start line.")
				.add("pu", 	"popup", 	false, false, ArgType.None, 		"If selected, at program end will popup info about execution.")
				.add("l", 	"log", 		false, false, ArgType.None, 		"If selected, will log to terminal.")
				.add("fl", 	"fileLog", 	false, false, ArgType.String, 		"If selected, will log to file path provided.")
				.add("sr", 	"saveRAM", 	false, false, ArgType.String, 		"If selected, at program end will save RAM to provided path.")
				.add("lr",  "loadRAM", 	false, false, ArgType.String, 		"If selected, will load RAM from file.") 
				.add("rs", 	"RAM_Size",	true,  false, ArgType.Int, 			"RAM size. Select if loadRAM isn't selected. Default: 1000")
				.add("drsf","dontReScaleFrame", false, false, ArgType.None, "If selected, won't auto scale the frame.")
				.add("wac", "waitAfterInstruction", false, false, ArgType.Int,  "If selected, will wait X nano seconds between each instruction.")
				.add("fps", "fps", 		false, false, ArgType.Int, 			"Default: 60")
				.add("ps", "PES_Size",false,   false, ArgType.Int, 			"PES size. Select if pes isn't selected. Default: 10000")
				.add("ws", "workspaceFolder", false, false, ArgType.String, "Workspace folder") 
				.parse(args);
		//@f:on

		final String pesPath = data.getStringOrDef("p", null);
		final int pesSize = data.getIntOrDef("ps", 10000);
		final String[] codePaths = data.getStringArrayOrDef("cp", null);
		final int startline = data.getInt("sl");
		final boolean popup = data.has("pu");
		final boolean logTerminal = data.has("l");
		final String fileLogPath = data.getStringOrDef("fl", null);
		final String saveRAM = data.getStringOrDef("sr", null);
		final int ramSize = data.getIntOrDef("rs", 1000);
		final String ramPath = data.getStringOrDef("lr", null);
		final boolean rescaleFrame = !data.has("drsf");
		final int wac = data.getIntOrDef("wac", 0);
		final int fps = data.getIntOrDef("fps", 60);
		final String ws = data.getStringOrDef("ws", null);

		IGB_VM vm = new IGB_VM(500, 500, popup, rescaleFrame, fileLogPath, logTerminal, wac, saveRAM);

		if(ramPath == null) {
			vm.initRAM(ramSize);
		} else {
			int[] ram = Utils.deserialize(ramPath);
			if(ram == null)
				throw new IGB_VM_Exception("Error occured while deserializing RAM.");
			vm.setRAM(ram);
		}

		// parsing
		if(pesPath != null) {
			int[][] pes = Utils.deserialize(pesPath);
			if(pes == null)
				throw new IGB_VM_Exception("Error occured while deserializing PES.");
			vm.setPES(pes);
		} else if(codePaths != null) {
			List<String> fileNames = new ArrayList<>();
			List<IGB_L1> l1_toCompile = new ArrayList<>();
			String l2Path = null;

			for (String path : codePaths) {
				String ext = Utils.getFileExtension(Utils.getFileName(path));
				String fileName = Utils.getFileNameWithoutExtension(Utils.getFileName(path));
				switch (ext) {
					case "igb_bin" -> {
						int[][] binary = Utils.deserialize(path);
						vm.parse(binary);
						System.out.println("Parsed \"" + path + "\" into PES.");
					}
					case "igb_l1" -> {
						IGB_L1 igbl1 = Utils.deserialize(path);
						l1_toCompile.add(igbl1);
						fileNames.add(fileName);
					}
					case "igb_l2" -> {
						if(l2Path != null)
							throw new IGB_VM_Exception("You can specify an L2 file once.");
						l2Path = path;
					}
					default -> throw new IGB_VM_Exception("Unsupported file extension: \"." + ext + "\"  File: \"" + path + "\".");
				}
			}

			if(l2Path != null) {
				IGB_CL2 cl2 = new IGB_CL2();
				IGB_L1[] igbl1 = cl2.compile(l2Path, false);
				System.out.println("L2 file compiled.");

				for (int i = 0; i < igbl1.length; i++) {
					l1_toCompile.add(igbl1[i]);
					String fileName = Utils.getFileName(igbl1[i].path);
					fileNames.add(fileName);

				}
				if(ws != null) {
					File outDir = new File(ws + File.separator + "L1");
					outDir.mkdirs();

					for (int i = 0; i < igbl1.length; i++) {
						IGB_L1 l1 = igbl1[i];

						String fileName1 = Utils.getFileNameWithoutExtension(new File(l1.path).getName());
						Utils.serialize(l1, outDir.getAbsolutePath() + File.separator + fileName1 + ".igb_l1");
						Utils.writeIntoFile(outDir.getAbsolutePath() + File.separator + fileName1 + ".igb_l1_readable", l1.toString());
					}
				}
			}

			assert l1_toCompile.size() == fileNames.size();

			if(!l1_toCompile.isEmpty()) {
				IGB_CL1 cl1 = new IGB_CL1();
				String[] fileNamesA = fileNames.toArray(String[]::new);
				int[][][] compiled = cl1.compile(l1_toCompile.toArray(IGB_L1[]::new), fileNamesA, pesSize);
				System.out.println("L1 compiled.");

				vm.setPES(compiled[0]);
				if(ws != null) {
					File outDir = new File(ws + File.separator + "BIN");
					outDir.mkdirs();
					String fileName = fileNames.get(0);
					Utils.serialize(compiled[0], outDir.getAbsolutePath() + File.separator + fileName + ".igb_pes");
				}
				System.out.println("Custom PES parsed.");
				/*
				 * } else { vm.initPES(pesSize);
				 * 
				 * for (int i = 0; i < compiled.length; i++) { vm.parse(compiled[i]);
				 * System.out.println("Parsed \"" + fileNamesA[i] + "\" into PES.");
				 * 
				 * }
				 * 
				 * if(ws != null) { File outDir = new File(ws + File.separator + "BIN");
				 * outDir.mkdirs(); final String t1 = outDir.getAbsolutePath() + File.separator;
				 * for (int i = 0; i < compiled.length; i++) { String fileName =
				 * Utils.getFileNameWithoutExtension(fileNamesA[i]);
				 * Utils.serialize(compiled[i], t1 + fileName + ".igb_bin");
				 * Utils.writeIntoFile(t1 + fileName + ".igb_bin_readable",
				 * Utils.arrayToString(compiled[i], " ", "\n")); } } }
				 */
			}

		} else
			throw new IGB_VM_Exception("You must either provie pes file or code paths.");

		vm.runRender(fps);
		vm.run(startline);
	}

	private int[][] p;

	public void setPES(int[][] pes) { this.p = pes; }

	public void initPES(int size) { p = new int[size][]; }

	private int[] r;

	public void setRAM(int[] r) { this.r = r; }

	public void initRAM(int size) { r = new int[size]; }

	private int width, height, screenMulti = 1, scaledWidth, scaledHeight, wac;
	private boolean rescaleFrame, popup, logToTerm;

	long startTime;

	private final int upperInsert;
	private JFrame frame;
	private JPanel panel;
	private BufferedImage img;
	private JLabel label;
	private ImageIcon icon;

	private String fileLogPath, saveRAM;
	private PrintWriter logWriter;

	private int l, instructionCount;
	private Stack<Integer> stack;
	private boolean screenType, exited = false;

	private int pixelCache;

	public IGB_VM(int width, int height, boolean popup, boolean rescaleFrame, String fileLogPath, boolean logToTerm, int wac, String saveRAM) {
		JFrame tmpFrame = new JFrame();
		tmpFrame.setSize(0, 0);
		tmpFrame.setVisible(true);
		try {
			Thread.sleep(10);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		upperInsert = (tmpFrame.getInsets()).top;
		tmpFrame.setVisible(false);
		tmpFrame.dispose();

		this.width = width;
		this.height = height;
		this.rescaleFrame = rescaleFrame;
		this.popup = popup;
		this.fileLogPath = fileLogPath;
		this.logToTerm = logToTerm;
		this.wac = wac;
		this.saveRAM = saveRAM;

		frame = new JFrame();
		frame.setResizable(false);
		frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				if(!exited)
					exit();
				System.exit(0);
			}
		});
		frame.addKeyListener(new KeyListener() {
			public void keyTyped(KeyEvent e) {}

			public void keyPressed(KeyEvent e) {
				if(e.getKeyCode() == KeyEvent.VK_ESCAPE) {
					if(!exited)
						exit();
					System.exit(0);
				}
				char c = e.getKeyChar();
				r[IGB_MA.KEYBOARD_INPUT] = c * 1000;
			}

			public void keyReleased(KeyEvent e) {}
		});

		panel = new JPanel();
		icon = new ImageIcon();

		updateScreenSize(true);
		panel.setLayout(new FlowLayout(0, 0, 0));

		label = new JLabel();
		label.setIcon(icon);
		panel.add(label);
		frame.setContentPane(panel);

		frame.setLocationRelativeTo(null);
	}

	public void updateScreenSize(boolean ignoreResize) {
		if(rescaleFrame) {
			int Wssm = (int) ((Toolkit.getDefaultToolkit().getScreenSize()).width * 0.7D / width);
			int Hssm = (int) ((Toolkit.getDefaultToolkit().getScreenSize()).height * 0.7D / height);
			screenMulti = (Wssm >= Hssm) ? Hssm : Wssm;
			if(screenMulti == 0)
				screenMulti = 1;
		}
		scaledWidth = width * screenMulti;
		scaledHeight = height * screenMulti + upperInsert;
		if(scaledWidth <= 0)
			throw new IGB_VM_Exception("Invalid scaled width: " + scaledWidth);

		if(scaledHeight <= 0)
			throw new IGB_VM_Exception("Invalid scaled height: " + scaledWidth);

		if(!ignoreResize) {
			frame.setSize(scaledWidth, scaledHeight);
			frame.setLocationRelativeTo(null);
			panel.setSize(scaledWidth, scaledHeight);
		}
		img = new BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_INT_RGB);
		{
			Graphics2D g = img.createGraphics();
			g.setColor(Color.white);
			g.fillRect(0, 0, scaledWidth, scaledHeight);
		}
		icon.setImage(img);
	}

	public void updateScreen() {
		width = r[IGB_MA.SCREEN_WIDTH] / 1000;
		height = r[IGB_MA.SCREEN_HEIGHT] / 1000;
		if(r[IGB_MA.SCREEN_TYPE] / 1000 == 0)
			screenType = false;
		else if(r[IGB_MA.SCREEN_TYPE] / 1000 == 1)
			screenType = true;
		else
			throw new IGB_VM_Exception("Cell " + IGB_MA.SCREEN_TYPE + " is screen type, it can only be 0 or 1. It is: " + r[IGB_MA.SCREEN_TYPE]);

		updateScreenSize(false);
		frame.setVisible(true);
	}

	public void parse(int[][] code) {
		int startline = code[0][0];
		System.out.println("startline: " + startline);
		for (int i = 0; i < code.length - 1; i++)
			p[i + startline] = code[i + 1];
	}

	private String getLogString() {
		if(l < 0 || p[l] == null)
			return "null*";
		StringBuilder sb = new StringBuilder("line " + l + ": ");
		for (int i = 0; i < p[l].length; i++) {
			if(p[l][i] == IGB_CL1.IGNORE_INT)
				break;
			sb.append(p[l][i]);

			sb.append(" ");
		}
		return sb.toString();
	}

	public void run(int startline) {
		new Thread(() -> {
			try {
				run1(startline);
			} catch (Exception e) {
				e.printStackTrace();
				exit();
				return;
			}
		}).start();
	}

	public void runRender(int FPS) {
		final int sleepTime = 1000 / FPS;
		new Thread(() -> {
			while (true) {
				icon.setImage(img);
				frame.repaint();
				try {
					Thread.sleep(sleepTime);
				} catch (InterruptedException e) {
					return;
				}
			}
		}).start();

	}

	private void run1(int startline) throws Exception {
		l = startline;
		stack = new Stack<>();

		if(fileLogPath != null) {
			try {
				logWriter = new PrintWriter(fileLogPath);
			} catch (FileNotFoundException e) {
				throw e;
			}
		}

		startTime = System.nanoTime();

		while (true) {
			if(logToTerm)
				System.out.println(getLogString());

			if(fileLogPath != null)
				logWriter.println(getLogString());

			try {
				inst();
			} catch (Exception e) {
				e.printStackTrace();
				System.out.println("DEBUG: \n\tline: " + l + "\n\tinstruction: " + Arrays.toString(p[l]));
				System.in.read();
				System.exit(1);
			}
			if(l++ < -1) {
				if(!exited)
					exit();
				return;
			}
			instructionCount++;

			if(wac != 0) {
				try {
					Thread.sleep(wac / 1000000, wac % 1000000);
				} catch (InterruptedException e) {
					throw e;
				}
			}
		}

	}

	private void exit() {
		l = -2;

		if(fileLogPath != null) {
			logWriter.close();
		}
		if(saveRAM != null) {
			Utils.serialize(r, saveRAM);
		}
		exited = true;
		if(popup) {
			double time2 = System.nanoTime();
			double totalTimeInNano = time2 - startTime;
			double totalTimeInMs = totalTimeInNano / 1000000.0D;
			double totalTimeInSeconds = totalTimeInMs / 1000.0D;

			String msg = "Execution finished, took " + String.format("%,.2f", Double.valueOf(totalTimeInSeconds)) + " sec.\nTotal instructions: "
					+ instructionCount + "\nCalculated IPS: " + String.format("%,.2f", (instructionCount / totalTimeInSeconds));

			info("IGB VM", msg);
		}
	}

	private void wait(int ticks) {
		long nano1 = System.nanoTime();
		try {
			Thread.sleep(ticks * 50);
		} catch (InterruptedException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		long nano2 = System.nanoTime();

		startTime -= nano1 - nano2;
	}

	private void log(int val) {
		// if(logToTerm)
		System.out.println("LOG: " + val / 1000d);

		if(fileLogPath != null)
			logWriter.println("LOG: " + val / 1000d);

	}

	//@f:off
	private void inst() {
		if(-1 > l) return;
		switch (p[l][0]) {
		case 0 -> {
			if(switch (p[l][1]) {
			case 0 -> r[p[l][2]] != (p[l][3] == 1 ? r[p[l][4]] : p[l][4]);
			case 1 -> r[p[l][2]] >= (p[l][3] == 1 ? r[p[l][4]] : p[l][4]);
			case 2 -> r[p[l][2]] <= (p[l][3] == 1 ? r[p[l][4]] : p[l][4]);
			case 3 -> r[p[l][2]] >  (p[l][3] == 1 ? r[p[l][4]] : p[l][4]);
			case 4 -> r[p[l][2]] <  (p[l][3] == 1 ? r[p[l][4]] : p[l][4]);
			case 5 -> r[p[l][2]] == (p[l][3] == 1 ? r[p[l][4]] : p[l][4]);
			default -> throw new IllegalArgumentException();
			}) l = p[l][5];
		}
		case 1 -> r[p[l][2]] = p[l][1];
		case 2 -> r[p[l][2]] = r[p[l][1]];
		case 3 -> r[p[l][4]] = r[p[l][1]] + (p[l][2] == 1 ? r[p[l][3]] : p[l][3]);
		case 4 -> {
			switch(p[l][1]) {
			case 0 -> l = p[l][2];
			case 1 -> {
					stack.push(l);
					l = p[l][2];
				}
			case 2 -> l = stack.pop();
			}
		}
		case 5 -> {
			if(screenType) {
				if(p[l][1] == -1) {
					if(p[l][2] == -1) 
						pixelCache = getRGBValueFromMCRGBValue(p[l][3]);
					 else 
						pixelCache = getRGBValue((p[l][2] == 1 ? r[p[l][3]]/1000 : p[l][3]),
												 (p[l][4] == 1 ? r[p[l][5]]/1000 : p[l][5]),
												 (p[l][6] == 1 ? r[p[l][7]]/1000 : p[l][7]));
				} else {
					if(p[l][5] != 0) {
						int cell = p[l][5];
						int[] obj = getpixelRGB((p[l][1] == 1 ? r[p[l][2]]/1000 : p[l][2]),
								 (p[l][3] == 1 ? r[p[l][4]]/1000 : p[l][4]));
						
						r[cell]   = obj[0]*1000;
						r[cell+1] = obj[1]*1000;
						r[cell+2] = obj[2]*1000;
					} else setpixel((p[l][1] == 1 ? r[p[l][2]]/1000 : p[l][2]),
									(p[l][3] == 1 ? r[p[l][4]]/1000 : p[l][4]));
				}
			} else {
				if(p[l][1] == -1) {
					if(p[l][2] == -1) 
						pixelCache = _16Color.values()[p[l][3]].mcrgb;
					else 
						pixelCache = _16Color.values()[r[p[l][2]]/1000].mcrgb;
				} else {
					if(p[l][5] != -1) {
						r[p[l][5]] = getpixel16c((p[l][1] == 1 ? r[p[l][2]]/1000 : p[l][2]),
												 (p[l][3] == 1 ? r[p[l][4]]/1000 : p[l][4]))
												 *1000;
						
					} else setpixel((p[l][1] == 1 ? r[p[l][2]]/1000 : p[l][2]),
									(p[l][3] == 1 ? r[p[l][4]]/1000 : p[l][4]));
					
				}
			}
		}
		case 6 -> {
			switch(p[l][1]) {
			case 0 -> wait(p[l][2]);
			case 1 -> updateScreen();
			case 2 -> log(p[l][2] == 1 ? r[p[l][3]] : p[l][3]);
			}
		}
		case 7 -> {
			switch(p[l][1]) {
			case 0 -> r[p[l][5]] = r[p[l][2]] - (p[l][3] == 1 ? r[p[l][4]] : p[l][4]);
			case 1 -> r[p[l][5]] = r[p[l][2]] * (p[l][3] == 1 ? r[p[l][4]] : p[l][4])/1000;
			case 2 -> r[p[l][5]] = (int) ((r[p[l][2]]*1000d) / (p[l][3] == 1 ? r[p[l][4]] : p[l][4]));
			case 3 -> r[p[l][5]] = r[p[l][2]] % (p[l][3] == 1 ? r[p[l][4]] : p[l][4]);
			case 4 -> r[p[l][4]] = new Random().ints(p[l][2], p[l][3]).findFirst().getAsInt()*1000;
			case 5 -> r[p[l][3]] = r[r[p[l][2]]/1000];
			case 6 -> r[r[p[l][3]]/1000] = r[p[l][2]];
			case 7 -> {
				final int val = r[p[l][2]];
				final int valT = val * 1000;
				int t1 = val;
				int t2 = 1;
				while (t1 > t2) {
					t1 = (t2 + t1) / 2;
					t2 = valT / t1;
				}
				r[p[l][3]] = t1;
			}
			}
		}
		}
	}
	//@f:on

	private void setpixel(int x, int y) {
		if(0 > x || x >= width || 0 > y || y >= height) {
			throw new IGB_VM_Exception("Coordinate out of bounds, x: " + x + ", y: " + y);
		}

		int x_1 = x * screenMulti;
		int x_2 = (x + 1) * screenMulti;
		int y_1 = y * screenMulti;
		int y_2 = (y + 1) * screenMulti;
		for (int x1 = x_1; x1 < x_2; x1++)
			for (int y1 = y_1; y1 < y_2; y1++)
				img.setRGB(x1, y1, pixelCache);
	}

	private int[] getpixelRGB(int x, int y) {
		if(x < 0 || y < 0 || x >= this.width || y >= this.height)
			throw new IGB_VM_Exception("getpixel: x: " + x + ", y: " + y + " position out of bounds.");
		int color = img.getRGB(x * screenMulti, y * screenMulti);
		return new int[] { color >> 16 & 0xFF, color >> 8 & 0xFF, color & 0xFF };
	}

	private int getpixel16c(int x, int y) { return _16Color.getFromRGB(img.getRGB(x * screenMulti, y * screenMulti)).ordinal(); }

	private static int getRGBValueFromMCRGBValue(int mcCode) { return getRGBValue(mcCode >> 16 & 0xFF, mcCode >> 8 & 0xFF, mcCode & 0xFF); }

	public static int getRGBValue(int r, int g, int b) { return 0xFF000000 | (r & 0xFF) << 16 | (g & 0xFF) << 8 | b & 0xFF; }
}

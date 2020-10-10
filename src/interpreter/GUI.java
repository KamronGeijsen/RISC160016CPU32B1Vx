package interpreter;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;

import javax.swing.JFrame;

import interpreter.SystemAgent.RandomAccessMemoryUnit;

/**
 * @author Kamron Geijsen
 *
 */
public class GUI extends JFrame{

	private static final File defaultFile = new File("examples/o.exe");
	private static final long serialVersionUID = 0;
	
	
	final SystemAgent systemAgent;
	final Debugger debugger;
	
	final ArrayList<GUIPane> panes = new ArrayList<GUIPane>();
	final ArrayList<GUIPane> defaultPanes = new ArrayList<GUIPane>();

	final double targetFPS = 60.0;
	
	int width, height;
	
	
	GUI(File file) {
//		ka.keyToAddress.add(ka.onlyArrows);
		addComponentListener(new ComponentAdapter() {
			public void componentResized(ComponentEvent e) {
				width = getWidth();
				height = getHeight();
			}
		});
		setSize(1000, 1000);
		setTitle("160016CPU32B1Vx Interpreter");
		
		debugger = new Debugger(600, 700);
		addKeyListener(new KA());
		systemAgent = new SystemAgent(file, this, new TimerUnit());
		MemoryObserver memoryObserver = new MemoryObserver(systemAgent.RAM, 50, 50, 256, 256, 2);
		RegObserver regObserver = new RegObserver(systemAgent.interpreter.reg, systemAgent.interpreter.regSize, 600, 50, 16);
		
		defaultPanes.add(memoryObserver);
		defaultPanes.add(regObserver);
		defaultPanes.add(debugger);
		defaultPanes.trimToSize();
		
		panes.addAll(defaultPanes);
		panes.removeIf(p -> p==null);

		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setVisible(true);
		
	}
	@Override
	public void paint(Graphics screen) {
		long frameTime = System.nanoTime();
		BufferedImage bf = new BufferedImage(width,height,BufferedImage.TYPE_INT_RGB);
		Graphics g = bf.createGraphics();
		
		
		systemAgent.timedExecuteHandler(20);
		for(GUIPane pane : panes) 
			pane.draw(g);
		screen.drawImage(bf, 0, 0, width, height, this);
		
		final long currentTime = System.nanoTime();
		double wait = 1000_000_000d/targetFPS - (currentTime - frameTime);
		if(wait > 0) {
			try {
				Thread.sleep((long)wait/1000_000, (int)wait%1000_000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		repaint();
	}
	
	class KA extends KeyAdapter {
		@Override
		public void keyPressed(KeyEvent e) {
			System.out.println("h:" + Integer.toHexString(e.getKeyCode()) + "	d:" + e.getKeyCode() + "	c:" + e.getKeyChar() + "	cd:" + (int)e.getKeyChar());
			new ArrayList<>(systemAgent.keyHandlers).forEach((handler) -> handler.handle(e));
		}
		@Override
		public void keyReleased(KeyEvent e) {
			new ArrayList<>(systemAgent.keyHandlers).forEach((handler) -> handler.handle(e));
		}
	}
	class TimerUnit {
		long getMilis() {
			return System.currentTimeMillis();
		}
		long getNanos() {
			return System.nanoTime();
		}
	}
	
	class Debugger extends GUIPane {
		
		Debugger(int x, int y){
			super.drawX = x;
			super.drawY = y;
			super.drawW = 250;
			super.drawH = 100;
			super.title = "Debug";
		}
		
		long lastSecond;
		
		long instrCounter;
		long lastCountCyclesPerSecond;		
		long countCyclesPerSecond;
		long CPUuptimePerSecond;
		long lastCPUuptimePerSecond;
		long countFramesPerSecond;
		long lastcountFramesPerSecond;
		

		int lastAccess;
		int lastSize;
		boolean lastSet;
		boolean active;
		
		@Override
		void draw(Graphics g) {
			final int x = drawX + 5;
			int y = drawY+5;
			g.setColor(Color.white);
			g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 20));
			g.drawString("cycles/f : " + instrCounter, x, y+=20);
			g.drawString("cycles/s : " + lastCountCyclesPerSecond, x, y+=20);
			g.drawString("uptime   : " + (long)(lastCPUuptimePerSecond/100_000.0)/100.0 + "%", x, y+=20);
			g.drawString("frames/s : " + lastcountFramesPerSecond, x, y+=20);
			g.drawRect(drawX, drawY, drawW, drawH);
			final long currentTime = System.currentTimeMillis();
			if(lastSecond+1000 < currentTime) {
				lastSecond=currentTime;
				lastCountCyclesPerSecond = countCyclesPerSecond; 
				countCyclesPerSecond=0;
				lastCPUuptimePerSecond = CPUuptimePerSecond;
				CPUuptimePerSecond=0;
				lastcountFramesPerSecond = countFramesPerSecond;
				countFramesPerSecond=0;
			}
			
			countCyclesPerSecond += instrCounter;
			instrCounter = 0;
			countFramesPerSecond++;
			super.drawPaneTitle(g);
		}
	}
	
	abstract class GUIPane {
		GUIPane parent;
		String title = "";
		int drawX, drawY, drawW, drawH;
		
		abstract void draw(Graphics g);
		
		void drawPaneTitle(Graphics g) {
			int fSize = 16;
			final Font f = new Font(Font.MONOSPACED, Font.BOLD, fSize); 
			final int w = title.length()*(fSize/2+1) + fSize*2;
			final int h = fSize+4;

			g.setColor(Color.DARK_GRAY);
			g.fillRect(drawX, drawY-h, drawW, h);
			g.setColor(Color.GRAY);
			g.fillRect(drawX, drawY-h, w, h);
			g.setColor(Color.BLACK);
			g.setFont(f);
			g.drawString(title, drawX + 2*fSize/4, drawY - 4);
			g.setColor(Color.WHITE);
			g.drawRect(drawX, drawY-h, drawW, h);
		}
	}
	
	class MemoryObserver extends GUIPane{
		public MemoryObserver(RandomAccessMemoryUnit RAM, int drawX, int drawY, int w, int h, int tile) {
			super.drawX = drawX;
			super.drawY = drawY;
			super.drawW = tile*w;
			super.drawH = tile*h;
			super.title = "Memory";
			this.tile = tile;
			this.RAM = RAM;
		}
		final RandomAccessMemoryUnit RAM;
		final int tile;
		final int width = 256;
		final int height = 256;
		
		final int memUnitSize = 32;
		
		@Override
		void draw(Graphics g) {
			BufferedImage bf = new BufferedImage(width*tile, height*tile, BufferedImage.TYPE_BYTE_BINARY);
			for(int y = 0, drawY = 0; y < height; y++) {
				for(int x = 0, drawX = 0; x < width/memUnitSize; x++) {
					int data = RAM.data[y*width/memUnitSize+x];
					if(data==0) {
						drawX += tile*memUnitSize;
					}else
						for(int b = 0; b < memUnitSize; b++) {
							int rgb = (data&1)==0?0:0xffffff;
							bf.setRGB(drawX, drawY, rgb);
							bf.setRGB(drawX+1, drawY, rgb);
							bf.setRGB(drawX, drawY+1, rgb);
							bf.setRGB(drawX+1, drawY+1, rgb);
							drawX += tile;
							data>>=1;
						}
				}
				drawY += tile;
			}
			
			g.drawImage(bf, drawX, drawY, null);
			
			for(int mem = 0; mem < debugger.lastSize; mem++) {
				int addr = debugger.lastAccess+mem;
				g.setColor(RAM.GET(addr, (byte) 1) == 1?(debugger.lastSet?Color.RED:Color.GREEN):Color.DARK_GRAY);
				g.fillRect(this.drawX+(addr&0xff)*tile, this.drawY+((addr>>8)&0xff)*tile, tile, tile);
			}
			for(int mem = 0; mem < 32; mem++) {
				int addr = systemAgent.interpreter.rIP*32+mem;
				g.setColor(RAM.GET(addr, (byte) 1) == 1?Color.CYAN:Color.DARK_GRAY);
				g.fillRect(this.drawX+(addr&0xff)*tile, this.drawY+((addr>>8)&0xff)*tile, tile, tile);
			}
			g.setColor(Color.WHITE);
			g.drawRect(drawX, drawY, drawW, drawH);
			super.drawPaneTitle(g);
		}
	}
	class RegObserver extends GUIPane {
		public RegObserver(long[] reg, byte[] regSize, int drawX, int drawY, int size) {
			tileX = size/2+2;
			tileY = size;
			f = new Font(Font.MONOSPACED, Font.BOLD, size);
			super.drawX = drawX;
			super.drawY = drawY;
			super.drawW = tileX*37;
			super.drawH = tileY*32;
			super.title = "Registers";
			this.pointerTo_reg = reg;
			this.pointerTo_regSize = regSize;
		}
		
		final long[] pointerTo_reg; 
		final byte[] pointerTo_regSize;
		
		final Font f;
		final int tileX, tileY;
		
		@Override
		void draw(Graphics g) {
			g.setColor(Color.WHITE);
			g.setFont(f);
			for(int y = 0; y < 32; y++) {
				byte size = pointerTo_regSize[y];
				for(int x = 0; x < 32; x++) {
					g.setColor(x<(size)?Color.WHITE:Color.LIGHT_GRAY);
					String s = (pointerTo_reg[y]&(1<<x))!=0?"1":"0";
					g.drawString(s, drawX+(31-x)*tileX+1, drawY+y*tileY+tileY-2);
				}
				g.setColor(Color.WHITE);
				g.drawRect(drawX+(32-size)*tileX, drawY+y*tileY, size*tileX, tileY);
				g.drawString("r"+Integer.toHexString(y), drawX+33*tileX, drawY+y*tileY+tileY-2);
			}
			g.drawRect(this.drawX, this.drawY, tileX*37, tileY*32);
			
			
			for(int x = 0; x < 32; x++) {
				g.setColor(x>=5?Color.WHITE:Color.LIGHT_GRAY);
				String s = ((systemAgent.interpreter.rIP<<5)&(1<<x))!=0?"1":"0";
				g.drawString(s, this.drawX+(31-x)*tileX+1, this.drawY+tileY*33+tileY-2);
			}
			g.setColor(Color.WHITE);
			g.drawString("rIP", this.drawX+33*tileX, this.drawY+33*tileY+tileY-2);
			for(int x = 0; x < 32; x++) {
				String s = (systemAgent.RAM.data[systemAgent.interpreter.rIP]&(1<<x))!=0?"1":"0";
				g.drawString(s, this.drawX+(31-x)*tileX+1, this.drawY+tileY*34+tileY-2);
			}
			g.setColor(Color.WHITE);
			g.drawString("rIR", this.drawX+33*tileX, this.drawY+34*tileY+tileY-2);
			String s = systemAgent.disassembleCurrentInstruction().replaceAll("\t", "  ");
			g.drawString(s, this.drawX+2*tileX+1, this.drawY+35*tileY+tileY-2);
			
			g.drawRect(this.drawX, this.drawY+tileY*32, tileX*37, tileY*4);
			super.drawPaneTitle(g);
		}
	}
	class GridObserver extends GUIPane {
		public GridObserver(RandomAccessMemoryUnit RAM, int drawX, int drawY, int baseAddress, int w, int h, int tile, int bitDepth, int mode) {
			super.drawX = drawX;
			super.drawY = drawY;
			super.drawW = w * (mode == 3 ? tile/2 : tile);
			super.drawH = h * tile;
			super.title = "Screen";
			this.RAM = RAM;
			this.baseAddress = baseAddress;
			this.w = w;
			this.h = h;
			this.tile = tile;
			this.bitDepth = bitDepth;
			bitScale = Integer.numberOfTrailingZeros(bitDepth);
			this.mode = mode;
		}
		
		final RandomAccessMemoryUnit RAM;
		final int baseAddress;
		final int tile;
		final int w, h;
		final int bitDepth, bitScale, mode;
		
		/*
		 * 
		 Modes
		 0: grayscale
		 1: rgb
		 2: argb
		 3: characters (in white, transparent background)
		 4: characters in grayscale (transparent background) 
		 5: characters in rgb (transparent background)
		 6: characters in rgb and background in rgb
		 
		 HSV?
		 16-color HSV/Gray (12 hue increments of 30, 4 grayscale colors) 
		 
		 */
		
		@Override
		void draw(Graphics g) {
			g.setColor(Color.white);
			g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 15));
			for(int y = 0, drawY = this.drawY; y < h; y++) {
				for(int x = 0, drawX = this.drawX; x < w; x++) {
					if(mode == 0) {
						int grayscale = (int) (RAM.GET(baseAddress+(y*w+x<<bitScale), (byte) bitDepth)*256/((1<<bitDepth)-1));
						grayscale = Math.min(grayscale, 255);
						g.setColor(new Color(grayscale * 0x00010101));
						g.fillRect(drawX, drawY, tile, tile);
						g.setColor(Color.white);
						g.drawRect(drawX, drawY, tile, tile);
						drawX += tile;
					}
					
					else if(mode == 3) {
						long value = RAM.GET(baseAddress+(y*w+x<<bitScale), (byte) bitDepth);
						g.setColor(Color.WHITE);
						g.drawString(""+(char)value, drawX+1, drawY+tile-5);;
						g.drawRect(drawX, drawY, tile/2, tile);
						drawX += tile/2;
					}
				}
				drawY += tile;
			}
			super.drawPaneTitle(g);
		}
	}
	
	public static void main(String[] args) {
		File file = defaultFile;
		if(args != null && args.length == 1 && args[0] != null)
			file = new File(args[0]);
//		calc();
//		System.exit(0);
		new GUI(file);
	}
	
}

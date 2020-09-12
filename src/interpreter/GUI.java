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
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.util.Arrays;

import javax.swing.JFrame;

import compiler.MacroAssembler;
import compiler.MicroAssembler;

/**
 * @author Kamron Geijsen
 *
 */
public class GUI extends JFrame{

	private static final File defaultFile = new File("src/compiler/o.exe");
	
	private static final long serialVersionUID = -532126457262075773L;
	private static final boolean START_PAUSED = true;
	
	int width, height;
	

	
	class CA extends ComponentAdapter {
		public void componentResized(ComponentEvent e) {
			width = getWidth();
			height = getHeight();
		}
	}
	
	SystemAgent systemAgent;

	MemObserver memObserver;
	RegObserver regObserver;
	GridObserver gridObserver;
	
	Debugger debugger;
	
	GUI(File file) {
		addKeyListener(new KA());
		addComponentListener(new CA());
		setSize(1000, 1000);
		setTitle("160016CPU32B1Vx Interpreter");
		
		setDefaultCloseOperation(EXIT_ON_CLOSE);
	
		
		systemAgent = new SystemAgent(file);
		memObserver = new MemObserver(systemAgent.RAM, 50, 50, 256, 256, 2);
		regObserver = new RegObserver(systemAgent.interpreter.reg, systemAgent.interpreter.regSize, 600, 50, 16);
		debugger = new Debugger(600, 700);
		
		setVisible(true);
		
	}
	
	final double targetFPS = 60.0;
	
	
	BufferedImage bf;
	@Override
	public void paint(Graphics screen) {
		long frameTime = System.currentTimeMillis();
		bf = new BufferedImage(width,height,BufferedImage.TYPE_INT_RGB);
		Graphics g = bf.createGraphics();
		systemAgent.executeHandler();
		memObserver.draw(g);
		regObserver.draw(g);
		if(gridObserver != null) gridObserver.draw(g);
		debugger.draw(g);
		screen.drawImage(bf, 0, 0, width, height, this);
		
		final long currentTime = System.currentTimeMillis();
//		System.out.println(currentTime-frameTime);
		if(currentTime - frameTime < 1000/targetFPS) {
//			System.out.println("Schleep schleep!: " + (long) (1000/targetFPS - (currentTime - frameTime)));
			try {
				Thread.sleep((long) (1000/targetFPS - (currentTime - frameTime)));
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		repaint();
	}

	class Debugger {
		
		Debugger(int x, int y){
			drawX = x;
			drawY = y;
		}

		final int drawX, drawY;
		
		long lastSecond;
		long lastCountCyclesPerSecond;
		long countCyclesPerSecond;
		long lastCPUuptimePerSecond;
		long CPUuptimePerSecond;
		
		void draw(Graphics g) {
			g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 20));
			g.drawString("cycles/f : " + systemAgent.instrCounter, drawX, drawY);
			g.drawString("cycles/s : " + lastCountCyclesPerSecond, drawX, drawY+20);
			g.drawString("uptime   : " + (long)(lastCPUuptimePerSecond/100_000.0)/100.0 + "%", drawX, drawY+40);
			
			final long currentTime = System.currentTimeMillis();
			if(lastSecond+1000 < currentTime) {
				lastSecond=currentTime;
				lastCountCyclesPerSecond = countCyclesPerSecond; 
				countCyclesPerSecond=0;
				lastCPUuptimePerSecond = CPUuptimePerSecond;
				CPUuptimePerSecond=0;
			}
			
			countCyclesPerSecond += systemAgent.instrCounter;
			systemAgent.instrCounter = 0;
		}
	}
	class RandomAccessMemoryUnit {
		final static int DEFAULT_RAM_SIZE = 65536; // Bits
		int lastAccess;
		int lastSize;
		boolean lastSet;
		final int[] data;
		
		RandomAccessMemoryUnit() {
			data = new int[DEFAULT_RAM_SIZE/32];
		}
		RandomAccessMemoryUnit(int bits) {
			data = new int[bits/32];
		}
		RandomAccessMemoryUnit(int[] data) {
			this.data = data;
		}
		
		long GET(int address, byte size) {
			final int intAlligned = address >>> 5;
			final int unaligned = address & 0b11111;
			long result = unaligned + size > 32 ? ((long)data[intAlligned+1]<<32)|data[intAlligned] : data[intAlligned];
			result >>>= unaligned;
			result &= (1l<<size)-1;
			return result;
		}
		void SET(int address, byte size, long value) {
			final int intAlligned = address >>> 5;
			final int unaligned = address & 0b11111;
			long old = unaligned + size > 32 ? ((long)data[intAlligned+1]<<32)|data[intAlligned] : data[intAlligned];
			
			final long mask = (1l<<size)-1<<unaligned;
			old = (old&~mask) | ((value << unaligned)&mask);
			data[intAlligned] = (int) (old);
			if(unaligned + size > 32)
				data[intAlligned+1] = (int) (old >>> 32);
		}
		public long debugGET(int address, byte size) {
			final int intAlligned = address >>> 5;
			final int unaligned = address & 0b11111;
			lastAccess = address;
			lastSize = size;
			lastSet=false;
			System.out.println("address: " + address + "/0x" + Integer.toHexString(address));
			long result = unaligned + size > 32 ? data[intAlligned] + ((long)data[intAlligned+1]<<32) : data[intAlligned];
			
				System.out.println(result);
//			
			result >>>= unaligned;
				System.out.println(result);
			result &= (1l<<size)-1;
				System.out.println(result);
			return result;
		}
		public void debugSET(int address, byte size, long data) {
			final int intAlligned = address >>> 5;
			final int unaligned = address & 0b11111;
			lastAccess = address;
			lastSize = size;
			lastSet=true;
			System.out.println("address: " + address + "/0x" + Integer.toHexString(address));
			long old = unaligned + size > 32 ? this.data[intAlligned] + ((long)this.data[intAlligned+1]<<32) : this.data[intAlligned];
			System.out.println(Long.toHexString(intAlligned) + "|" + unaligned);
			System.out.println(unaligned + size > 32);
			System.out.println(old);
			long mask = (1l<<size)-1<<unaligned;
			old = (old&~mask) | ((data << unaligned)&mask);
			
			System.out.println(old);
			this.data[intAlligned] = (int) (old);
			if( unaligned + size > 32 )
				this.data[intAlligned+1] = (int) (old >>> 32);
		}
	}
	class SystemAgent {
		
		SystemAgent(File program){
			this.program = program;
			RAM = new RandomAccessMemoryUnit();
			interpreter = new Interpreter(RAM, this);
			disassembler = new Disassembler(interpreter);
			
			this.reset();
		}

		public void reset() {
			Arrays.fill(RAM.data, 0);
			try {
				byte[] bytes = Files.readAllBytes(program.toPath());
				IntBuffer intBuf = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN).asIntBuffer();
				int[] array = new int[intBuf.remaining()];
				intBuf.get(array);
				System.arraycopy(array, 0, RAM.data, 0, array.length);
			} catch (IOException e) {
				e.printStackTrace();
			}
			interpreter.initialize();
			
			interrupted = false;
			interruptWaitforTimer = false;
			interruptWaitForKey = false;
			gridObserver = null;
		}

		final File program;
		final static int GRAPHICS_BASE = 0x5000;
		final static int KEYSET_BASE = 0x6000;
		final RandomAccessMemoryUnit RAM;
		final Interpreter interpreter;
		final Disassembler disassembler;
		
		boolean interrupted = false;
		boolean paused = START_PAUSED;
		
		
		long interruptTimerWait = 0;
		boolean interruptWaitforTimer = false;
		boolean interruptWaitForKey = false;
		
		long instrCounter = 0;
		
		void executeHandler() {
			if(interruptWaitforTimer && interruptTimerWait < System.currentTimeMillis()) {
				interruptWaitforTimer = false;
				interrupted = false;
				interpreter.rIP++;
				interpreter.reg[0] = 0;
			}

			long starttime = System.nanoTime();
			long startTimer = System.currentTimeMillis()+20;
			if(!paused && !interrupted) {
				label: do {
					for(int i = 0; i < 64; i++) {
						instrCounter++;
						interpreter.executeCycle();
						if(RAM.data[interpreter.rIP] == 0) {
							interrupted = true;
							if(interruptHandler())
								break label;
						}
					}
				} while(startTimer > System.currentTimeMillis());
			}
			debugger.CPUuptimePerSecond += System.nanoTime()-starttime;
		}
		
		/*
		 * @return			true if the processor goes to sleep mode, otherwise false
		 */
		boolean interruptHandler() {
			long code = interpreter.reg[0];
			switch ((int)code) {
			case 0: {
				System.out.println("===Done executing successfully===");
				paused = true;
				return true;
			}
			case 1: {
				if(interruptTimerWait + (int) interpreter.reg[1] < System.currentTimeMillis())
					interruptTimerWait = System.currentTimeMillis();
				interruptTimerWait += (int) interpreter.reg[1];
				interruptWaitforTimer = true;
				return true;
			}
			case 2: {
				interruptWaitForKey = true;
				return true;
			}
			case 3: {
				if(gridObserver != null) {
					paused = true;
					throw new RuntimeException("gridObserver already initialized");
				}
					
				gridObserver = new GridObserver(RAM, 100, 600, 
						(int) interpreter.reg[1], 
						(int) interpreter.reg[2], 
						(int) interpreter.reg[3], 
						(int) interpreter.reg[4], 
						(int) interpreter.reg[5]);
	
				interrupted = false;
				interpreter.rIP++;
				interpreter.reg[0] = 0;
				return false;
			}
			case 4: {
				System.out.println("> " + interpreter.reg[1]);
				interrupted = false;
				interpreter.rIP++;
				interpreter.reg[0] = 0;
				return false;
			}
			case 5: {
				
				interrupted = false;
				interpreter.rIP++;
				interpreter.reg[0] = 0;
				return false;
			}
			case -1: {
				System.out.println("===Exited with error===");
				paused = true;
				return true;
			}
			default:
				throw new RuntimeException("Interrupt '" + code + "' not implemented");
			}
		}
		
		String disassembleCurrentInstructionFormated() {
			return Long.toHexString(interpreter.rIP | 0x10000).substring(1) + "\t"
					+ disassembleCurrentInstruction();
		}
		String disassembleCurrentInstruction() {
			return disassembler.disassemble(RAM.data[interpreter.rIP]);
		}
		
		long GET(int address, byte size) {
			return RAM.GET(address, size);
		}
		void SET(int address, byte size, long data) {
			RAM.SET(address, size, data);
		}
	}
	class KA extends KeyAdapter {
		final int left = KeyEvent.VK_LEFT;
		final int right = KeyEvent.VK_RIGHT;
		final int up = KeyEvent.VK_UP;
		final int down = KeyEvent.VK_DOWN;
		final int enter = KeyEvent.VK_ENTER;
		
		final int space = KeyEvent.VK_SPACE;
		final int f8 = KeyEvent.VK_F8;
		final int v = KeyEvent.VK_V;
		final int f5 = KeyEvent.VK_F5;
		@Override
		public void keyPressed(KeyEvent e) {
			int k = e.getKeyCode();
			switch(k) {
			case up:
				systemAgent.SET(SystemAgent.KEYSET_BASE+3, (byte) 1, 1);
			break;case down:
				systemAgent.SET(SystemAgent.KEYSET_BASE+2, (byte) 1, 1);
			break;case left:
				systemAgent.SET(SystemAgent.KEYSET_BASE+1, (byte) 1, 1);
			break;case right:
				systemAgent.SET(SystemAgent.KEYSET_BASE+0, (byte) 1, 1);
			break;case enter:
				systemAgent.SET(SystemAgent.KEYSET_BASE+4, (byte) 1, 1);
				if(systemAgent.interruptWaitForKey) {
					systemAgent.interrupted = false;
					systemAgent.interpreter.rIP++;
					systemAgent.interpreter.reg[0] = 0;
					systemAgent.interruptTimerWait = System.currentTimeMillis();
					systemAgent.interruptWaitForKey = false;
				}
			break;case space:
				System.out.println(systemAgent.disassembleCurrentInstructionFormated());
				systemAgent.interpreter.executeCycle();
				if(systemAgent.interrupted)
					systemAgent.interruptHandler();
				if(systemAgent.RAM.data[systemAgent.interpreter.rIP] == 0) systemAgent.interrupted = true;
			break;case v:
				for(int i = 0; i < 100; i++)systemAgent.interpreter.executeCycle();
			break;case f8:
				systemAgent.paused=!systemAgent.paused;
				systemAgent.interruptTimerWait = System.currentTimeMillis() + 0;
			break;case f5:
				MacroAssembler.main(null);
				MicroAssembler.main(null);
				systemAgent.reset();
			}
		}
		@Override
		public void keyReleased(KeyEvent e) {
			int k = e.getKeyCode();
			switch(k) {
			case up:
				systemAgent.SET(SystemAgent.KEYSET_BASE+3, (byte) 1, 0);
			break;case down:
				systemAgent.SET(SystemAgent.KEYSET_BASE+2, (byte) 1, 0);
			break;case left:
				systemAgent.SET(SystemAgent.KEYSET_BASE+1, (byte) 1, 0);
			break;case right:
				systemAgent.SET(SystemAgent.KEYSET_BASE+0, (byte) 1, 0);
			break;case enter:
				systemAgent.SET(SystemAgent.KEYSET_BASE+4, (byte) 1, 0);
			}	
		}
	}
	
	class MemObserver {
		public MemObserver(RandomAccessMemoryUnit RAM, int drawX, int drawY, int w, int h, int tile) {
			this.drawX = drawX;
			this.drawY = drawY;
			drawW = tile*w;
			drawH = tile*h;
			this.tile = tile;
			this.RAM = RAM;
		}
		final RandomAccessMemoryUnit RAM;
		final int tile;
		final int drawX, drawY, drawW, drawH;
		final int width = 256;
		final int height = 256;
		
		final int memUnitSize = 32;
		
		void draw(Graphics g) {
			BufferedImage bf = new BufferedImage(width*tile, height*tile, BufferedImage.TYPE_BYTE_BINARY);
			for(int y = 0, drawY = 0; y < height; y++) {
				for(int x = 0, drawX = 0; x < width/memUnitSize; x++) {
					int data = RAM.data[y*width/memUnitSize+x];
					if(data==0) {
						drawX += tile;
					}else
						for(int b = 0; b < memUnitSize; b++) {
							int rgb = (data&1)==0?0:0x00ffffff;
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
			g.drawImage(bf, this.drawX, this.drawY, null);
			g.setColor(Color.WHITE);
			g.drawRect(drawX, drawY, drawW, drawH);
		}
	}
	class RegObserver {
		public RegObserver(long[] reg, byte[] regSize, int drawX, int drawY, int size) {
			tileX = size/2+2;
			tileY = size;
			f = new Font(Font.MONOSPACED, Font.BOLD, size);
			this.drawX = drawX;
			this.drawY = drawY;
			this.reg = reg;
			this.regSize = regSize;
		}
		
		final long[] reg; 
		final byte[] regSize;
		
		final Font f;
		final int tileX, tileY;
		final int drawX, drawY;
		
		void draw(Graphics g) {
			g.setColor(Color.WHITE);
			g.setFont(f);
			for(int y = 0; y < 32; y++) {
				byte size = regSize[y];
				for(int x = 0; x < 32; x++) {
					g.setColor(x<(size)?Color.WHITE:Color.LIGHT_GRAY);
					g.drawString((reg[y]&(1<<x))!=0?"1":"0", this.drawX+(31-x)*tileX+1, this.drawY+y*tileY+tileY-2);
				}
				g.setColor(Color.WHITE);
				g.drawRect(this.drawX+(32-size)*tileX, this.drawY+y*tileY, size*tileX, tileY);
				g.drawString("r"+Integer.toHexString(y), this.drawX+33*tileX, this.drawY+y*tileY+tileY-2);
			}
			g.drawRect(this.drawX, this.drawY, tileX*37, tileY*32);
			
			
			for(int x = 0; x < 32; x++) {
				g.setColor(x>=5?Color.WHITE:Color.LIGHT_GRAY);
				g.drawString(((systemAgent.interpreter.rIP<<5)&(1<<x))!=0?"1":"0", this.drawX+(31-x)*tileX+1, this.drawY+tileY*33+tileY-2);
			}
			g.setColor(Color.WHITE);
			g.drawString("rIP", this.drawX+33*tileX, this.drawY+33*tileY+tileY-2);
			for(int x = 0; x < 32; x++) {
				g.drawString((systemAgent.RAM.data[systemAgent.interpreter.rIP]&(1<<x))!=0?"1":"0", this.drawX+(31-x)*tileX+1, this.drawY+tileY*34+tileY-2);
			}
			g.setColor(Color.WHITE);
			g.drawString("rIR", this.drawX+33*tileX, this.drawY+34*tileY+tileY-2);
			g.drawString(systemAgent.disassembleCurrentInstruction().replaceAll("\t", "  "), this.drawX+2*tileX, this.drawY+35*tileY+tileY-2);
			
			g.drawRect(this.drawX, this.drawY+tileY*33, tileX*37, tileY*3);
		}
	}
	class GridObserver {
		public GridObserver(RandomAccessMemoryUnit RAM, int drawX, int drawY, int w, int h, int tile, int bitDepth, int mode) {
			this.drawX = drawX;
			this.drawY = drawY;
			this.RAM = RAM;
			this.w = w;
			this.h = h;
			this.tile = tile;
			this.bitDepth = bitDepth;
			bitScale = Integer.numberOfTrailingZeros(bitDepth);
			this.mode = mode;
		}
		
		final RandomAccessMemoryUnit RAM;
		final int tile;
		final int drawX, drawY;
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
		
		
		
		void draw(Graphics g) {
			g.setColor(Color.white);
			g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 15));
			for(int y = 0, drawY = this.drawY; y < h; y++) {
				for(int x = 0, drawX = this.drawX; x < w; x++) {
					if(mode == 0) {
						int grayscale = (int) (systemAgent.GET(SystemAgent.GRAPHICS_BASE+(y*w+x<<bitScale), (byte) bitDepth)*256/((1<<bitDepth)-1));
						grayscale = Math.min(grayscale, 255);
						g.setColor(new Color(grayscale * 0x00010101));
						g.fillRect(drawX, drawY, tile, tile);
						g.setColor(Color.white);
						g.drawRect(drawX, drawY, tile, tile);
						drawX += tile;
					}
					
					else if(mode == 3) {
						long value = systemAgent.GET(SystemAgent.GRAPHICS_BASE+(y*w+x<<bitScale), (byte) bitDepth);
						g.setColor(Color.WHITE);
						g.drawString(""+(char)value, drawX+1, drawY+tile-2);;
						g.drawRect(drawX, drawY, tile/2, tile);
						drawX += tile/2;
					}
				}
				drawY += tile;
			}
		}
	}
	class DebugObserver {
		
	}
	
	public static void main(String[] args) {
		File file = defaultFile;
		if(args != null && args.length == 1 && args[0] != null)
			file = new File(args[0]);
		new GUI(file);
	}
}

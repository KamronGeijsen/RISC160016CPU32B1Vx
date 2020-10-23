package interpreter;

import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;

import assembler.Assembler;
import assembler.Disassembler;
import interpreter.GUI.GUIPane;
import interpreter.GUI.GridObserver;
import interpreter.GUI.TimerUnit;
import lib.ExecutableLinkableFormat;

/**
 * @author Kamron Geijsen
 *
 */
public class SystemAgent {
	
	public SystemAgent(File programFile, GUI gui, TimerUnit timerUnit){
		this.programFile = programFile;
		this.gui = gui;
		this.timerUnit = timerUnit;
		
		RAM = new RandomAccessMemoryUnit();
		interpreter = new Interpreter(new RandomMemoryAccessUnitDebug(RAM), this);
		disassembler = new Disassembler();
		
		reset();
	}

	public void reset() {
		RAM.clear();
		try {
			Integer rIP = 0;
			Integer memoffs = 0;
			byte[] programData = ExecutableLinkableFormat.loadProgramData(programFile, rIP, memoffs);

			IntBuffer intBuf = ByteBuffer.wrap(programData).order(ByteOrder.BIG_ENDIAN).asIntBuffer();
			int[] arr = new int[intBuf.remaining()];
			intBuf.get(arr);
			
			DirectMemoryAccessBufferWriter programLoadBuffer = new DirectMemoryAccessBufferWriter(RAM, 0, 0, arr.length*32, (byte) 32);
			programLoadBuffer.putAligned(arr);
			
		} catch (IOException e) {
			System.err.println("Error while loading in " + programFile);
			e.printStackTrace();
		}
		interpreter.initialize();
		
		gui.panes.removeIf(p -> !gui.defaultPanes.contains(p));

		timeHandlers.clear();
		pinHandlers.clear();
		keyHandlers.clear();
		pins.clear();
		keyHandlers.add(this::standardKeysHandler);
	}

	final static int DEFAULT_RAM_SIZE = 65536; // Bits
	
	final File programFile;
	final GUI gui;
	final TimerUnit timerUnit;
	final RandomAccessMemoryUnit RAM;
	final Interpreter interpreter;
	final Disassembler disassembler;
	
	final ArrayList<KeyHandler> keyHandlers = new ArrayList<>();
	final ArrayList<TimerHandler> timeHandlers = new ArrayList<>();
	final ArrayList<PinHandler> pinHandlers = new ArrayList<>();
	
	final ArrayList<Pin> pins = new ArrayList<>();
	public Pin pinAtOrNew(int address) {
		for(Pin p : pins)
			if(p.address == address)
				return p;
		Pin p = new Pin(address);
		pins.add(p);
		return p;
	}
	
	boolean paused = true;
	
	public String disassembleCurrentInstructionFormated() {
		return Long.toHexString(interpreter.rIP | 0x10000).substring(1) + "\t"
				+ disassembleCurrentInstruction();
	}
	public String disassembleCurrentInstruction() {
		return disassembler.disassemble(RAM.data[interpreter.rIP]);
	}
	public void timedExecuteHandler(int executeTimems) {
		new ArrayList<>(timeHandlers).forEach(t -> t.handle(t));
		new ArrayList<>(pinHandlers).forEach(p -> p.handle(p));

		long starttime = System.nanoTime();
		long stopExecute = System.currentTimeMillis()+executeTimems;
		if(!paused && !interpreter.interrupted) {
			label: do {
				for(int i = 0; i < 64; i++) {
					gui.debugger.instrCounter++;
					interpreter.executeCycle();
					if(RAM.data[interpreter.rIP] == 0) {
						interpreter.interrupted = true;
						if(interruptHandler())
							break label;
					}
				}
			} while(stopExecute > System.currentTimeMillis());
		}
		gui.debugger.CPUuptimePerSecond += System.nanoTime()-starttime;
	}
	
	/*
	 * @return			true if the processor goes to sleep mode, otherwise false
	 */
	private boolean interruptHandler() {
		long code = interpreter.reg[0];
		switch ((int)code) {
		case 0: { // Exit (successful)
			System.out.println("===Done executing successfully===");
			paused = true;
			return true;
		}
		case 1: { // Wait for milis
			int sleepMilis = (int) interpreter.reg[1];
			long fireTime = timerUnit.getMilis() + sleepMilis;
			TimerHandler timerHandler = (t) -> {
				if(fireTime < timerUnit.getMilis()) {
					interpreter.unblock();
					timeHandlers.remove(t);
				}
			};
			timeHandlers.add(timerHandler);
			interpreter.interrupted = true;
			return true;
		}
		case 2: {
			
			return true;
		}
		case 3: { // Create GridObserver
			for(GUIPane pane : gui.panes)
				if(pane instanceof GridObserver)
					throw new RuntimeException("gridObserver already initialized");
			
			long[] regs = interpreter.reg;
			GridObserver gridObserver = gui.new GridObserver(RAM, 100, 600, 
					(int) regs[1], (int) regs[2], (int) regs[3], (int) regs[4], (int) regs[5], (int) regs[6]);
			gui.panes.add(gridObserver);

			interpreter.unblock();
			return false;
		}
		case 4: { // Print number
			System.out.println("> " + interpreter.reg[1]);
			interpreter.unblock();
			return false;
		}
		case 5: { // Print string
			int address = (int) interpreter.reg[1];
			System.out.println(address + "\t" + RAM.GET(address, (byte)8));
			System.out.print("> ");
			for(char c; (c=(char) RAM.GET(address, (byte)8)) != 0; address+=8)
				System.out.print(c);
			System.out.println();
			
			interpreter.unblock();
			return false;
		}
		case 6: { //Wait for Pin event
			int address = (int)interpreter.reg[1];
			Pin pin = pinAtOrNew(address);
			pinHandlers.add(p -> {
				long memPin = RAM.GET(pin.address, (byte)1);
				if(memPin != 0) {
					//memPin != (pin.isOn?1:0)
					pin.isOn = memPin!=0;
					if(pin.isOn) {
						System.out.println("Unblock me");
						interpreter.unblock();
						pinHandlers.remove(p);
					}
				}
			});
			return true;
		}
		case 7: { //Create Keyboard pin
			int address = (int)interpreter.reg[1];
			int keyCode = (int)interpreter.reg[2];
			keyHandlers.add(e -> {
				if(e.getKeyCode() == keyCode)
					RAM.SET(address, (byte)1, e.getID()==KeyEvent.KEY_PRESSED?1:0);
			});
			pins.add(new Pin(address));
			
			interpreter.unblock();
			return false;
		}
		case 8: { // Keyboard key queue
			int baseAddress = (int)interpreter.reg[1];
			int indexAddress = (int)interpreter.reg[2];
			int length = (int)interpreter.reg[3];
			int mode = (int)interpreter.reg[4];
			int pinAddress = (int)interpreter.reg[5];
			System.out.println(pinAddress);
			
			DirectMemoryAccessRingBufferWriter keyQueue = new DirectMemoryAccessRingBufferWriter(RAM, baseAddress, indexAddress, length, (byte)8);
			KeyHandler[] modes = {
					e -> {
						if((e.getKeyChar() & ~0x7f) == 0) 
							keyQueue.put(e.getKeyChar() | (e.getID()==KeyEvent.KEY_PRESSED?0x80:0));
					},
					e -> {
						if((e.getKeyChar() & ~0x7f) == 0) 
							keyQueue.put(e.getKeyChar() | (e.getID()==KeyEvent.KEY_PRESSED?0x80:0));
					},
					e -> {
						if((e.getKeyChar() & ~0x7f) == 0 && e.getID()==KeyEvent.KEY_PRESSED && !(e.getKeyCode() == KeyEvent.VK_SPACE && paused)) {
							keyQueue.put(e.getKeyChar());
						}
					},
			};
			if(mode == 2) {
				keyQueue.addPinHandler((p) -> {
					RAM.SET(pinAddress, (byte)1, 1);
					System.out.println("new key!");
				});
			}
			
			
			keyHandlers.add(modes[mode]);
			
			interpreter.unblock();
			return false;
		}
		case 9: { // 
			
		}
		
		case -1: { // Force exit with default error code
			System.out.println("===Exited with error===");
			paused = true;
			return true;
		}
		default:
			throw new RuntimeException("Interrupt '" + code + "' not implemented");
		}
	}
	private void standardKeysHandler(KeyEvent e) {
		if(e.getID()==KeyEvent.KEY_PRESSED)
		switch (e.getKeyCode()) {
		case KeyEvent.VK_SPACE:
			if(paused) {
				new ArrayList<>(timeHandlers).forEach(t -> t.handle(t));
				new ArrayList<>(pinHandlers).forEach(p -> p.handle(p));
				
				System.out.println(disassembleCurrentInstructionFormated());
				if(!interpreter.interrupted) {
					interpreter.executeCycle();
					if(RAM.data[interpreter.rIP] == 0) {
						interpreter.interrupted = true;
						interruptHandler();
					}
				}
			}
		break;case KeyEvent.VK_V:
			for(int i = 0; i < 100; i++)
				if(paused) {
					new ArrayList<>(timeHandlers).forEach(t -> t.handle(t));
					new ArrayList<>(pinHandlers).forEach(p -> p.handle(p));

					System.out.println(disassembleCurrentInstructionFormated());
					if(!interpreter.interrupted) {
						interpreter.executeCycle();
						if(RAM.data[interpreter.rIP] == 0) {
							interpreter.interrupted = true;
							interruptHandler();
						}
					}
				}
		break;case KeyEvent.VK_F8:
			paused=!paused;
		break;case KeyEvent.VK_F5:
			Assembler.main(null);
			reset();
		}
	}
	
	
	class RandomAccessMemoryUnit {
		final int[] data;
		
		RandomAccessMemoryUnit() {
			this(SystemAgent.DEFAULT_RAM_SIZE);
		}
		RandomAccessMemoryUnit(int bits) {
			data = new int[bits/32];
		}
		RandomAccessMemoryUnit(int[] data) {
			this.data = data;
		}

		public void clear() {
			Arrays.fill(data, 0);
		}
		public int loadInstr(int instrAddress) {
			return data[instrAddress];
		}

		public long GET(int address, byte size) {
			final int intAlligned = address >>> 5;
			final int unaligned = address & 0b11111;
			long result = unaligned + size > 32 ? ((long)data[intAlligned+1]<<32)|data[intAlligned] : data[intAlligned];
			result >>>= unaligned;
			result &= (1l<<size)-1;
			return result;
		}
		public void SET(int address, byte size, long value) {
			final int intAlligned = address >>> 5;
			final int unaligned = address & 0b11111;
			long old = unaligned + size > 32 ? ((long)data[intAlligned+1]<<32)|data[intAlligned] : data[intAlligned];
			
			final long mask = (1l<<size)-1<<unaligned;
			old = (old&~mask) | ((value << unaligned)&mask);
			data[intAlligned] = (int) (old);
			if(unaligned + size > 32)
				data[intAlligned+1] = (int) (old >>> 32);
		}
	}
	class RandomMemoryAccessUnitDebug extends RandomAccessMemoryUnit{
		RandomMemoryAccessUnitDebug(RandomAccessMemoryUnit RAM){
			super(RAM.data);
		}
		
		public long GET(int address, byte size) {
			final int intAlligned = address >>> 5;
			final int unaligned = address & 0b11111;
			gui.debugger.lastAccess = address;
			gui.debugger.lastSize = size;
			gui.debugger.lastSet=false;
			long result = unaligned + size > 32 ? ((long)data[intAlligned+1]<<32)|data[intAlligned] : data[intAlligned];
			result >>>= unaligned;
			result &= (1l<<size)-1;
			return result;
		}
		public void SET(int address, byte size, long value) {
			final int intAlligned = address >>> 5;
			final int unaligned = address & 0b11111;
			gui.debugger.lastAccess = address;
			gui.debugger.lastSize = size;
			gui.debugger.lastSet=true;
			long old = unaligned + size > 32 ? ((long)data[intAlligned+1]<<32)|data[intAlligned] : data[intAlligned];
			
			final long mask = (1l<<size)-1<<unaligned;
			old = (old&~mask) | ((value << unaligned)&mask);
			data[intAlligned] = (int) (old);
			if(unaligned + size > 32)
				data[intAlligned+1] = (int) (old >>> 32);
		}
	}	
	
	class DirectMemoryAccessBufferWriter {
		public DirectMemoryAccessBufferWriter(RandomAccessMemoryUnit ram, int baseAddress, int indexAddress, int length, byte size){
			this.ram = ram;
			this.baseAddress = baseAddress;
			this.indexAddress = indexAddress;
			this.length = length;
			this.size = size;
		}
		final RandomAccessMemoryUnit ram;
		final int baseAddress;
		final int indexAddress;
		final int length;
		final byte size;
		final ArrayList<PinHandler> readyPins = new ArrayList<>();
		
		void addPinHandler(PinHandler... pins) {
			for(PinHandler p : pins)
				readyPins.add(p);
		}
		
		void put(long value) {
			int index = indexAddress==0?0:(int) ram.GET(indexAddress, (byte) 32);
			ram.SET(baseAddress+index, size, value);
			index+=size;
			if(index >= length)
				throw new ArrayIndexOutOfBoundsException("DirectMemoryAccessBuffer out of bounds");
			if(indexAddress!=0)
				ram.SET(indexAddress, size, index);
			if(readyPins.size() > 0)
				new ArrayList<>(readyPins).forEach(p -> p.handle(p));
		}
		void put(long... values) {
			int index = indexAddress==0?0:(int) ram.GET(indexAddress, (byte) 32);
			for(long value : values) {
				ram.SET(baseAddress+index, size, value);
				index+=size;
			}
			if(index >= length)
				throw new ArrayIndexOutOfBoundsException("DirectMemoryAccessBuffer out of bounds");
			if(indexAddress!=0)
				ram.SET(indexAddress, size, index);
			if(readyPins.size() > 0)
				new ArrayList<>(readyPins).forEach(p -> p.handle(p));
		}
		void putAligned(int... alignedValues) {
			int len = alignedValues.length;
			int index = indexAddress==0?0:(int) ram.GET(indexAddress, (byte) 32);
			int newIndex = index + len*32;
			if(newIndex <= length) {
				System.arraycopy(alignedValues, 0, ram.data, (baseAddress+index)/32, len);
				index = newIndex;
			} else
				throw new ArrayIndexOutOfBoundsException("DirectMemoryAccessBuffer out of bounds");
			if(indexAddress!=0)
				ram.SET(indexAddress, size, index);
			if(readyPins.size() > 0)
				new ArrayList<>(readyPins).forEach(p -> p.handle(p));
		}
	}
	class DirectMemoryAccessRingBufferWriter extends DirectMemoryAccessBufferWriter{
		
		public DirectMemoryAccessRingBufferWriter(RandomAccessMemoryUnit ram, int baseAddress, int indexAddress, int length, byte size) {
			super(ram, baseAddress, indexAddress, length, size);
		}
		
		void addPinHandler(PinHandler... pins) {
			for(PinHandler p : pins)
				readyPins.add(p);
		}
		
		void put(long value) {
			int index = indexAddress==0?0:(int) ram.GET(indexAddress, (byte) 32);
			ram.SET(baseAddress+index, size, value);
			index+=size;
			if(index >= length)
				index = 0;
			if(indexAddress!=0)
				ram.SET(indexAddress, size, index);
			if(readyPins.size() > 0)
				new ArrayList<>(readyPins).forEach(p -> p.handle(p));
		}
		void put(long... values) {
			int index = indexAddress==0?0:(int) ram.GET(indexAddress, (byte) 32);
			for(long value : values) {
				ram.SET(baseAddress+index, size, value);
				index+=size;
				if(index >= length)
					index = 0;
			}
			if(indexAddress!=0)
				ram.SET(indexAddress, size, index);
			if(readyPins.size() > 0)
				new ArrayList<>(readyPins).forEach(p -> p.handle(p));
		}
		void putAligned(int... alignedValues) {
			int len = alignedValues.length;
			int index = indexAddress==0?0:(int) ram.GET(indexAddress, (byte) 32);
			int newIndex = index + len*32;
			if(newIndex < length) {
				System.arraycopy(alignedValues, 0, ram.data, baseAddress+index, len);
				index = newIndex;
			} else {
				int overflow = (newIndex - length)/32;
				System.arraycopy(alignedValues, 0, ram.data, baseAddress+index, len - overflow);
				System.arraycopy(alignedValues, len - overflow, ram.data, baseAddress, overflow);
				index = overflow;
			}
			if(indexAddress!=0)
				ram.SET(indexAddress, size, index);
			if(readyPins.size() > 0)
				new ArrayList<>(readyPins).forEach(p -> p.handle(p));
		}
	}
	
	class Pin {
		public Pin(int address){
			this.address = address;
		}
		final int address;
		boolean isOn;
	}

	@FunctionalInterface
	interface KeyHandler {
		public void handle(KeyEvent e);
	}
	@FunctionalInterface
	interface PinHandler {
		public void handle(PinHandler p);
	}
	@FunctionalInterface
	interface TimerHandler {
		public void handle(TimerHandler t);
	}
}

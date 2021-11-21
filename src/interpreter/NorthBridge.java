package interpreter;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

public class NorthBridge {
	
	// Memory protection
	// Memory mapping
	// Packet buffer
	// DMA
	
	Peripheral[] peripherals = new Peripheral[256];
	
	public NorthBridge() {
		peripherals[0] = new NorthBridgeHandler();
	}
	
	class RandomAccessMemoryUnit extends Peripheral{
		final int[] data;
		
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
	class RandomAccessMemoryUnitDebug extends RandomAccessMemoryUnit{
		
		int lastAccess, lastSize;
		boolean lastSet;
		
		RandomAccessMemoryUnitDebug(int bits){
			super(bits);
		}
		
		public long GET(int address, byte size) {
			final int intAlligned = address >>> 5;
			final int unaligned = address & 0b11111;
			lastAccess = address;
			lastSize = size;
			lastSet = false;
			long result = unaligned + size > 32 ? ((long)data[intAlligned+1]<<32)|data[intAlligned] : data[intAlligned];
			result >>>= unaligned;
			result &= (1l<<size)-1;
			return result;
		}
		public void SET(int address, byte size, long value) {
			final int intAlligned = address >>> 5;
			final int unaligned = address & 0b11111;
			lastAccess = address;
			lastSize = size;
			lastSet = true;
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
//		final ArrayList<PinHandler> readyPins = new ArrayList<>();
//		
//		void addPinHandler(PinHandler... pins) {
//			for(PinHandler p : pins)
//				readyPins.add(p);
//		}
		
		void put(long value) {
			int index = indexAddress==0?0:(int) ram.GET(indexAddress, (byte) 32);
			ram.SET(baseAddress+index, size, value);
			index+=size;
			if(index >= length)
				throw new ArrayIndexOutOfBoundsException("DirectMemoryAccessBuffer out of bounds");
			if(indexAddress!=0)
				ram.SET(indexAddress, size, index);
//			if(readyPins.size() > 0)
//				new ArrayList<>(readyPins).forEach(p -> p.handle());
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
//			if(readyPins.size() > 0)
//				new ArrayList<>(readyPins).forEach(p -> p.handle());
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
//			if(readyPins.size() > 0)
//				new ArrayList<>(readyPins).forEach(p -> p.handle());
		}
	}
	class DirectMemoryAccessRingBufferWriter extends DirectMemoryAccessBufferWriter{
		
		public DirectMemoryAccessRingBufferWriter(RandomAccessMemoryUnit ram, int baseAddress, int indexAddress, int length, byte size) {
			super(ram, baseAddress, indexAddress, length, size);
		}
		
//		void addPinHandler(PinHandler... pins) {
//			for(PinHandler p : pins)
//				readyPins.add(p);
//		}
		
		void put(long value) {
			int index = indexAddress==0?0:(int) ram.GET(indexAddress, (byte) 32);
			ram.SET(baseAddress+index, size, value);
			index+=size;
			if(index >= length)
				index = 0;
			if(indexAddress!=0)
				ram.SET(indexAddress, size, index);
//			if(readyPins.size() > 0)
//				new ArrayList<>(readyPins).forEach(p -> p.handle());
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
//			if(readyPins.size() > 0)
//				new ArrayList<>(readyPins).forEach(p -> p.handle());
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
//			if(readyPins.size() > 0)
//				new ArrayList<>(readyPins).forEach(p -> p.handle());
		}
	}
	class DirectMemoryAccessQueueBufferWriter {}
/* ADDRESSES

	0:	NorthbridgeHandler
	1:	CPU
	2:	RAM
	

 */
/* MMAP
0x0000000-0x8000000: RAM
0xffff000-0xfffffff: NBH 
 */
	/*
interrupt steps
1. get PC, save in memory
2. return a jump command

	 */
	void connectPeripheral(Peripheral p, int addr) {
		peripherals[addr] = p; 
	}
	
	abstract class Peripheral {
		int addr;
		
		void connect(int addr) {
			this.addr = addr;
		}
		
		public long GET(int addr, byte size) {
			return 0;
		}
		public void SET(int addr, byte size, long val) {
			
		}
	}
	
	class NorthBridgeHandler extends Peripheral {
		int[] localStorage = new int[0x10000/Integer.SIZE];
		
		public NorthBridgeHandler() {
			SET(255*8, (byte)8, 0);
		}
		
		public long GET(int address, byte size) {
			final int intAlligned = address >>> 5;
			final int unaligned = address & 0b11111;
			long result = unaligned + size > 32 ? ((long)localStorage[intAlligned+1]<<32)|localStorage[intAlligned] : localStorage[intAlligned];
			result >>>= unaligned;
			result &= (1l<<size)-1;
			return result;
		}
		public void SET(int address, byte size, long value) {
			final int intAlligned = address >>> 5;
			final int unaligned = address & 0b11111;
			long old = unaligned + size > 32 ? ((long)localStorage[intAlligned+1]<<32)|localStorage[intAlligned] : localStorage[intAlligned];
			
			final long mask = (1l<<size)-1<<unaligned;
			old = (old&~mask) | ((value << unaligned)&mask);
			localStorage[intAlligned] = (int) (old);
			if(unaligned + size > 32)
				localStorage[intAlligned+1] = (int) (old >>> 32);
		}
	}
	
	class Timer extends Peripheral {
		long startEpoch = 0;
		Timer() {
			startEpoch = System.currentTimeMillis();
		}
		
		@Override
		public long GET(int addr, byte size) {
			return (System.currentTimeMillis() - startEpoch) & 0xffffffff;
		}
//		void fireAt(long pc) {
//			inputQueue.add(pc);
//		}
		
		
		// Store datachunk (arbitrary memory location)
		// Set bit when busy (always 1)
	}
	class TimerInterrupter extends Peripheral {
		
		@Override
		public void SET(int addr, byte size, long val) {
			
		}
	}
	
	
	class KeyBoard extends Peripheral {
		KeyBoard() {
			
		}
		
		// Store datachunk (arbitrary memory location)
		// Set bit when busy (always 1)
	}
	
	class DMABuffer extends Peripheral {
		DMABuffer(int start, int size) {
			
		}
		
		// Load datachunk (arbitrary memory location)
		// Store datachunk (arbitrary memory location)
		// Set bit when busy (busy:1, idle:0)
		// Return index
		// Set index
	}
	
	class StorageBuffer extends Peripheral {
		StorageBuffer() {
			
		}
		
		// Load datachunk (arbitrary memory location)
		// Store datachunk (arbitrary memory location)
		// Set bit when busy (busy:1, idle:0)
	}
	
	class USBBuffer extends Peripheral {
		USBBuffer() {
			
		}
		
		// Load datachunk (arbitrary memory location)
		// Store datachunk (arbitrary memory location)
		// Set bit when busy (busy:1, idle:0)
	}
	
	class NetworkBuffer extends Peripheral {
		NetworkBuffer() {
			
		}
		
		// Load datachunk (arbitrary memory location)
		// Store datachunk (arbitrary memory location)
		// Set bit when busy (busy:1, idle:0)
	}
	
	class PrintInt extends Peripheral {
		PrintInt() {
			
		}
		
		@Override
		public void SET(int addr, byte size, long val) {
			System.out.println("> " + Long.toHexString(val|0x100000000l).substring(1) + "\t" + val);
		}
	}
	class PrintStr extends Peripheral {
		PrintStr() {
			
		}
		
		@Override
		public void SET(int addr, byte size, long val) {
			StringBuilder sb = new StringBuilder();
			for(char c = (char) NorthBridge.this.GET(addr++, (byte)8); c != 0; c = (char) NorthBridge.this.GET(addr++, (byte)8)) {
				sb.append(c);
			}
			System.out.println("> " + sb);
		}
		// Load datachunk (arbitrary memory location)
		// Set bit when busy (busy:1, idle:0)
	}
	
	class LEDlamp extends Peripheral {
		
	}
	
	class DigitalPin extends Peripheral {
		
	}
	
	class AnalogPin extends Peripheral {
		
	}
	
	class GPU extends Peripheral {
		GPU() {
			
		}
		
		// Load datachunk (arbitrary memory location)
		// Store datachunk (arbitrary memory location)
		// Set bit when busy (busy:1, idle:0)
	}
	
	class GridDisplay extends Peripheral {
		
		final int[] data;
		final int bitDepth, bitScale, mode;
		
		public GridDisplay(int bits, int bitDepth, int mode) {
			data = new int[bits/Integer.SIZE];
			this.mode = mode;
			this.bitDepth = bitDepth;
			bitScale = Integer.numberOfTrailingZeros(bitDepth);
			
		}
		
		@Override
		public void SET(int addr, byte size, long val) {
			final int intAlligned = addr >>> 5;
			final int unaligned = addr & 0b11111;
			long old = unaligned + size > 32 ? ((long)data[intAlligned+1]<<32)|data[intAlligned] : data[intAlligned];
			
			final long mask = (1l<<size)-1<<unaligned;
			old = (old&~mask) | ((val << unaligned)&mask);
			data[intAlligned] = (int) (old);
			if(unaligned + size > 32)
				data[intAlligned+1] = (int) (old >>> 32);
		}
		
		// Load datachunk (arbitrary memory location)
		// Event bit when to draw screen
		// Event bit when to initialize screen
		// Config (width/height/bitdepth/mode)
	}
	
	class BootLoader extends Peripheral {
//		BootLoader(File programFile) {
//			RAM.clear();
//			try {
//				Integer rIP = 0;
//				Integer memoffs = 0;
//				byte[] programData = ExecutableLinkableFormat.loadProgramData(programFile, rIP, memoffs);
//
//				IntBuffer intBuf = ByteBuffer.wrap(programData).order(ByteOrder.BIG_ENDIAN).asIntBuffer();
//				int[] arr = new int[intBuf.remaining()];
//				intBuf.get(arr);
//				
//				DirectMemoryAccessBufferWriter programLoadBuffer = new DirectMemoryAccessBufferWriter(RAM, 0, 0, arr.length*32, (byte) 32);
//				programLoadBuffer.putAligned(arr);
//				
//			} catch (IOException e) {
//				System.err.println("Error while loading in " + programFile);
//				e.printStackTrace();
//			}
//			interpreter.initialize();
//		}
		
		// Store datachunk (arbitrary memory location)
		// Set bit when busy (busy:1, idle:0)
	}
	
	class InterruptHandler extends Peripheral {
		
		// Store datachunk (arbitrary memory location)
		
	}
	
	class CPUDebug extends Peripheral {
		Interpreter interpreter = new Interpreter(NorthBridge.this);
		
		AtomicBoolean pause = new AtomicBoolean(true);
		AtomicBoolean stop = new AtomicBoolean(false);
		
		Object notifier = new Object();
		Object pauseNotifier = new Object();
		
		public CPUDebug() {
			executionThread.start();
		}
		
		void pause() {
			pause.set(true);
		}
		void pauseWait() {
			pause.set(true);
			synchronized (peripherals) {
				while(interpreter.interrupted)
					try {
						pauseNotifier.wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
			}
		}
		
		void unpause() {
			if(pause.getAndSet(false)) {
				synchronized (notifier) {
					notifier.notify();
				}
			}
		}
		
		long delay = 0;
		
		long uptime = 0;
		long instructions = 0;
		
		Thread executionThread = new Thread(new Runnable() {
			
			@Override
			public void run() {
				try {
					
					while (!stop.get()) {
						while (pause.get()) {
							synchronized (pauseNotifier) {
								pauseNotifier.notify();
							}
							synchronized (notifier) {
								notifier.wait();
							}
						}
						long start = System.nanoTime();
						int instrCounter;
						Thread.sleep(500);
						for(instrCounter = 0; instrCounter < 1; instrCounter++) {
							interpreter.executeCycle();
							if(interpreter.interrupted) {
								pause.set(true);
								break;
							}
						}
						uptime += System.nanoTime() - start;
						instructions += instrCounter;
						instrCounter = 0;
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				System.out.println("done");
			}
		});
		
		@Override
		public void SET(int addr, byte size, long val) {
			if (pause.get()) {
				interpreter.rIP = (int)val;
				unpause();
			}	else 
			System.err.println("FATAL: INTERRUPT WHILE CPU IS RUNNING");
		}
		
		@Override
		public long GET(int addr, byte size) {
			pauseWait();
			return interpreter.rIP;
		}
	}
	
//	public long GET(int addr, byte size) {
//		return peripherals[addr >>> 24].GET(addr&0xffffff, size);
//	}
//	public void SET(int addr, byte size, long val) {
//		peripherals[addr >>> 24].SET(addr&0xffffff, size, val);
//	}
//	public long loadInstr(int addr) {
//		addr *= 32;
//		return peripherals[addr >>> 24].GET(addr&0xffffff, (byte)32);
//	}
	public long GET(int addr, byte size) {
		int index = (int) peripherals[0].GET((addr >>> 24) << 3, (byte) 8);
		return peripherals[index].GET(addr&0xffffff, size);
	}
	public void SET(int addr, byte size, long val) {
		int index = (int) peripherals[0].GET((addr >>> 24) << 3, (byte) 8);
		peripherals[index].SET(addr&0xffffff, size, val);
	}
	public long loadInstr(int addr) {
		addr *= 32;
		int index = (int) peripherals[0].GET((addr >>> 24) << 3, (byte) 8);
		return peripherals[index].GET(addr&0xffffff, (byte)32);
	}
}

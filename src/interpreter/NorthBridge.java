package interpreter;

import java.util.Arrays;

import interpreter.GUI.Debugger;

public class NorthBridge {
	
	// Memory protection
	// Memory mapping
	// Packet buffer
	// DMA
	
	Peripheral[] peripherals = new Peripheral[256];
	
	Debugger debugger;
	
	public NorthBridge(Debugger debugger) {
		this.debugger = debugger;
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
	class RandomMemoryAccessUnitDebug extends RandomAccessMemoryUnit{
		RandomMemoryAccessUnitDebug(RandomAccessMemoryUnit RAM){
			super(RAM.data);
		}
		
		public long GET(int address, byte size) {
			final int intAlligned = address >>> 5;
			final int unaligned = address & 0b11111;
			debugger.lastAccess = address;
			debugger.lastSize = size;
			debugger.lastSet=false;
			long result = unaligned + size > 32 ? ((long)data[intAlligned+1]<<32)|data[intAlligned] : data[intAlligned];
			result >>>= unaligned;
			result &= (1l<<size)-1;
			return result;
		}
		public void SET(int address, byte size, long value) {
			final int intAlligned = address >>> 5;
			final int unaligned = address & 0b11111;
			debugger.lastAccess = address;
			debugger.lastSize = size;
			debugger.lastSet=true;
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
	class DirectMemoryAccessQueueBufferWriter {
		
	}
	
	class DataPacket {
		int addr;
	}
	class Subscription {
		int start, end;
		int addr;
	}
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
		
	}
	void subscribePeripheralToMemory(int addr, int start, int end) {
		
	}
	
	abstract class Peripheral {

//		ArrayList<Long> inputQueue = new ArrayList<Long>();
		int addr;
		
		void connect(int addr) {
			this.addr = addr;
		}
	}
	
	class NorthBridgeHandler extends Peripheral {
		int[] localStorage = new int[0x10000/Integer.SIZE];
		
		
	}
	
	class Timer extends Peripheral {
		Timer() {
			
		}
		
//		void fireAt(long pc) {
//			inputQueue.add(pc);
//		}
		
		// Store datachunk (arbitrary memory location)
		// Set bit when busy (always 1)
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
	
	class PrintStr extends Peripheral {
		PrintStr() {
			
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
	
	class GridObserver extends Peripheral {
		public GridObserver() {
			// TODO Auto-generated constructor stub
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
	
	class CPU extends Peripheral {
		Interpreter interpreter = new Interpreter(NorthBridge.this);
	}
	
	
	public long GET(int i, byte b) {
		// TODO Auto-generated method stub
		return 0;
	}
	public void SET(int i, byte b, long l) {
		// TODO Auto-generated method stub
		
	}
	public long loadInstr(int rIP) {
		// TODO Auto-generated method stub
		return 0;
	}
}

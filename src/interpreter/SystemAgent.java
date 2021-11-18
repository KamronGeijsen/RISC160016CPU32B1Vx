package interpreter;

/**
 * @author Kamron Geijsen
 *
 */
public class SystemAgent {
	/**
	
//	public SystemAgent(File programFile, GUI gui, TimerUnit timerUnit){
//		this.programFile = programFile;
//		this.gui = gui;
//		this.timerUnit = timerUnit;
//		
//		RAM = new RandomAccessMemoryUnit();
//		interpreter = new Interpreter(new RandomMemoryAccessUnitDebug(RAM), this);
////		disassembler = new Disassembler();
//		
//		reset();
//	}
	Debugger debugger;

	Peripheral[] peripheralMap = new Peripheral[256];
	public SystemAgent(Debugger debugger) {
		this.debugger = debugger;
		
		RAM = new RandomAccessMemoryUnit();
		interpreter = new Interpreter(new RandomMemoryAccessUnitDebug(RAM), this);
		BootLoader bootLoader = new BootLoader(0);
		
		peripheralMap[0] = null;
	}

	public void reset() {
//		RAM.clear();
//		try {
//			Integer rIP = 0;
//			Integer memoffs = 0;
//			byte[] programData = ExecutableLinkableFormat.loadProgramData(programFile, rIP, memoffs);
//
//			IntBuffer intBuf = ByteBuffer.wrap(programData).order(ByteOrder.BIG_ENDIAN).asIntBuffer();
//			int[] arr = new int[intBuf.remaining()];
//			intBuf.get(arr);
//			
//			DirectMemoryAccessBufferWriter programLoadBuffer = new DirectMemoryAccessBufferWriter(RAM, 0, 0, arr.length*32, (byte) 32);
//			programLoadBuffer.putAligned(arr);
//			
//		} catch (IOException e) {
//			System.err.println("Error while loading in " + programFile);
//			e.printStackTrace();
//		}
//		interpreter.initialize();
		
//		gui.panes.removeIf(p -> !gui.defaultPanes.contains(p));
	}

	final static int DEFAULT_RAM_SIZE = 65536; // Bits
	
//	final File programFile;
//	final GUI gui;
//	final TimerUnit timerUnit;
	final RandomAccessMemoryUnit RAM;
	final Interpreter interpreter;
//	final Disassembler disassembler;
	
		
	boolean paused = true;
	
//	public String disassembleCurrentInstructionFormated() {
//		return Long.toHexString(interpreter.rIP | 0x10000).substring(1) + "\t"
//				+ disassembleCurrentInstruction();
//	}
//	public String disassembleCurrentInstruction() {
//		return disassembler.disassemble(RAM.data[interpreter.rIP]);
//	}
	public void timedExecuteHandler(int executeTimems) {
//		new ArrayList<>(timeHandlers).forEach(t -> t.handle(t));
//		new ArrayList<>(pinHandlers).forEach(p -> p.handle(p));
		uncoreHandler();

		long starttime = System.nanoTime();
		long stopExecute = System.currentTimeMillis()+executeTimems;
		if(!paused && !interpreter.interrupted) {
			label: do {
				for(int i = 0; i < 64; i++) {
					debugger.instrCounter++;
					interpreter.executeCycle();
					if(RAM.data[interpreter.rIP] == 0) {
						interpreter.interrupted = true;
						if(interruptHandler())
							break label;
					}
				}
			} while(stopExecute > System.currentTimeMillis());
		}
		debugger.CPUuptimePerSecond += System.nanoTime()-starttime;
	}
	
	/*
	 * @return			true if the processor goes to sleep mode, otherwise false
	 * /
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
//			long fireTime = timerUnit.getMilis() + sleepMilis;
//			timer.fireAt(fireTime);
			interpreter.interrupted = true;
			return true;
		}
		case 2: {
			
			return true;
		}
		case 3: { // Create GridObserver
//			for(GUIPane pane : gui.panes)
//				if(pane instanceof GridObserver)
//					throw new RuntimeException("gridObserver already initialized");
			
			long[] regs = interpreter.reg;
//			GridObserver gridObserver = gui.new GridObserver(RAM, 100, 600, 
//					(int) regs[1], (int) regs[2], (int) regs[3], (int) regs[4], (int) regs[5], (int) regs[6]);
//			gui.panes.add(gridObserver);

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
		case 7: { //Create Keyboard pin
			int address = (int)interpreter.reg[1];
			int keyCode = (int)interpreter.reg[2];
//			keyHandlers.add(e -> {
//				if(e.getKeyCode() == keyCode)
//					RAM.SET(address, (byte)1, e.getID()==KeyEvent.KEY_PRESSED?1:0);
//				return false;
//			});
//			pins.add(new Pin(address));
			
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
//			KeyHandler[] modes = {
//					() -> {
//						if((e.getKeyChar() & ~0x7f) == 0) 
//							keyQueue.put(e.getKeyChar() | (e.getID()==KeyEvent.KEY_PRESSED?0x80:0));
//					},
//					() -> {
//						if((e.getKeyChar() & ~0x7f) == 0) 
//							keyQueue.put(e.getKeyChar() | (e.getID()==KeyEvent.KEY_PRESSED?0x80:0));
//					},
//					() -> {
//						if((e.getKeyChar() & ~0x7f) == 0 && e.getID()==KeyEvent.KEY_PRESSED && !(e.getKeyCode() == KeyEvent.VK_SPACE && paused)) {
//							keyQueue.put(e.getKeyChar());
//						}
//					},
//			};
//			if(mode == 2) {
//				keyQueue.addPinHandler(() -> {
//					RAM.SET(pinAddress, (byte)1, 1);
//					System.out.println("new key!");
//					return false;
//				});
//			}
//			
//			
//			keyHandlers.add(modes[mode]);
			
			interpreter.unblock();
			return false;
		}
		case 9: { // Read
			
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
//				new ArrayList<>(timeHandlers).forEach(t -> t.handle());
//				new ArrayList<>(pinHandlers).forEach(p -> p.handle());
				
//				System.out.println(disassembleCurrentInstructionFormated());
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
//					new ArrayList<>(timeHandlers).forEach(t -> t.handle());
//					new ArrayList<>(pinHandlers).forEach(p -> p.handle());

//					System.out.println(disassembleCurrentInstructionFormated());
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
	*/
}

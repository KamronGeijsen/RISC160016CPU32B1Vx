package interpreter;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

import assembler.Assembler;
import interpreter.GUI.CPUObserver;
import interpreter.GUI.GridObserver;
import interpreter.GUI.MemoryObserver;
import interpreter.GUI.RegObserver;
import interpreter.NorthBridge.CPUDebug;
import interpreter.NorthBridge.GridDisplay;
import interpreter.NorthBridge.InterruptHandler;
import interpreter.NorthBridge.PrintInt;
import interpreter.NorthBridge.RandomAccessMemoryUnitDebug;
import interpreter.NorthBridge.Timer;
import interpreter.NorthBridge.TimerInterrupter;
import lib.ExecutableLinkableFormat;

public class Launcher {
	
	RunConfiguration runConfiguration = new RunConfigurationAsm("examples/test.masm");
	
	public Launcher() {
		runConfiguration.compileGUI(); 
	}
	
	public static void main(String[] args) {
//		File file = defaultFile;
//		if(args != null && args.length == 1 && args[0] != null)
//			file = new File(args[0]);
		new Launcher();
	}
	
	class RunConfiguration {
		
		NorthBridge compile() {
			return null;
			
		}

		GUI compileGUI() {
			return null;
		}
		
	}
	class RunConfigurationAsm extends RunConfiguration {
		final File inputFile;
		
		public RunConfigurationAsm(String path) {
			inputFile = new File(path);
		}
		public RunConfigurationAsm(File file) {
			inputFile = file;
		}
		
		
		
		GUI compileGUI() {
			NorthBridge northBridge = new NorthBridge();
			RandomAccessMemoryUnitDebug ram = northBridge.new RandomAccessMemoryUnitDebug(65536);
			CPUDebug cpu = northBridge.new CPUDebug();
			InterruptHandler interruptHandler = northBridge.new InterruptHandler();
			GridDisplay gridDisplay = northBridge.new GridDisplay(16*16, 1, 0);
			Timer timer = northBridge.new Timer();
			TimerInterrupter timerInterrupter = northBridge.new TimerInterrupter();
			PrintInt printInt = northBridge.new PrintInt();
			
			
			northBridge.connectPeripheral(ram, 1);
			northBridge.connectPeripheral(cpu, 2);
			northBridge.connectPeripheral(timerInterrupter, 3);
			northBridge.connectPeripheral(gridDisplay, 4);
			northBridge.connectPeripheral(timer, 5);
			northBridge.connectPeripheral(timerInterrupter, 6);
			
			northBridge.connectPeripheral(printInt, 8);
			
			
			
			northBridge.SET(0xff000000, (byte)8, 1);
			
			
			{
				
				ram.clear();
				try {
					byte[] assembled = Assembler.assembleMacroBytes(inputFile);
					Integer rIP = 0;
					Integer memoffs = 0;
					byte[] programData = ExecutableLinkableFormat.loadProgramData(assembled, rIP, memoffs);

					IntBuffer intBuf = ByteBuffer.wrap(programData).order(ByteOrder.BIG_ENDIAN).asIntBuffer();
					int[] arr = new int[intBuf.remaining()];
					intBuf.get(arr);
					
//					DirectMemoryAccessBufferWriter programLoadBuffer = northBridge.new DirectMemoryAccessBufferWriter(ram, 0, 0, arr.length*32, (byte) 32);
//					programLoadBuffer.putAligned(arr);
					System.arraycopy(arr, 0, ram.data, 0, arr.length);
					
				} catch (IOException e) {
					System.err.println("Error while loading in " + inputFile);
					e.printStackTrace();
				}
				cpu.interpreter.initialize();
			}

			
			
			
			GUI gui = new GUI(northBridge);
			CPUObserver cpuObserver = gui.new CPUObserver(600, 700, cpu);
			MemoryObserver memoryObserver = gui.new MemoryObserver(ram, 50, 50, 256, 256, 2);
			RegObserver regObserver = gui.new RegObserver(cpu.interpreter, 600, 50, 16);
			GridObserver gridObserver = gui.new GridObserver(gridDisplay, 100, 600, 16, 16, 20);
			
			gui.panes.add(regObserver);
			gui.panes.add(memoryObserver);
			gui.panes.add(cpuObserver);
			gui.panes.add(gridObserver);
			
			
			cpu.unpause();
			
			return gui;
		}
	}
}

package interpreter;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

import assembler.Assembler;
import interpreter.GUI.Debugger;
import interpreter.GUI.MemoryObserver;
import interpreter.GUI.RegObserver;
import interpreter.NorthBridge.CPU;
import interpreter.NorthBridge.DirectMemoryAccessBufferWriter;
import interpreter.NorthBridge.RandomMemoryAccessUnitDebug;
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
			NorthBridge northBridge = new NorthBridge(null);
			RandomMemoryAccessUnitDebug ram = northBridge.new RandomMemoryAccessUnitDebug(
					northBridge.new RandomAccessMemoryUnit(65536));
			CPU cpu = northBridge.new CPU();

			northBridge.connectPeripheral(cpu, 1);
			northBridge.connectPeripheral(ram, 2);
			
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
					
					DirectMemoryAccessBufferWriter programLoadBuffer = northBridge.new DirectMemoryAccessBufferWriter(ram, 0, 0, arr.length*32, (byte) 32);
					programLoadBuffer.putAligned(arr);
					
				} catch (IOException e) {
					System.err.println("Error while loading in " + inputFile);
					e.printStackTrace();
				}
				cpu.interpreter.initialize();
			}

			
			GUI gui = new GUI(northBridge);
			Debugger debugger = gui.new Debugger(600, 700, cpu.interpreter, ram);
			northBridge.debugger = debugger;
			MemoryObserver memoryObserver = gui.new MemoryObserver(ram, 50, 50, 256, 256, 2);
			RegObserver regObserver = gui.new RegObserver(cpu.interpreter, 600, 50, 16);
			
			gui.panes.add(regObserver);
			gui.panes.add(memoryObserver);
			gui.panes.add(debugger);
			
			return gui;
		}
	}
}

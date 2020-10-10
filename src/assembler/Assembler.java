package assembler;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Scanner;

import lib.ExecutableLinkableFormat;

public class Assembler {
	
	final static File dir = new File("examples/");
	
	final static File macroAssembly = new File(dir, "snake.masm");
//	final static File macroAssembly = new File(dir, "fibonacci.masm");
//	final static File macroAssembly = new File(dir, "primes2.masm");
//	final static File macroAssembly = new File(dir, "os.masm");
	final static File microAssembly = new File(dir, "o.uasm");
	final static File binaryText = new File(dir, "o.bin");
	final static File binaryInstructions = new File(dir, "o.exe");
	
	public static void main(String[] args) {
		File input = null;
		File output = null;
		String flag = "";
		if(args == null)
			args = new String[0];
		for(String s : args) {
			if(s.startsWith("-")) {
				if(flag == null)
					throw new RuntimeException("Invalid flag parameter: " + s);
				else
					flag = s.substring(1);
			}
			switch (flag) {
			case "o":
				if(output == null) output = new File(s);
				else throw new RuntimeException("Invalid flag parameter: " + s);
				break;
			case "":
				if(input == null) input = new File(s);
				else if(output == null) output = new File(s);
				else throw new RuntimeException("Invalid flag parameter: " + s);
				break;

			default:
				throw new RuntimeException("Invalid flag parameter: " + s);
			}
		}
		
		if(input == null) input = macroAssembly;
		if(output == null) output = binaryInstructions;
		
		
		if(input.exists())
			try {
				assembleMacro();
			} catch (IOException e) {
				e.printStackTrace();
			}
	}

	private static void assembleMacro() throws IOException {
		Scanner sc = new Scanner(macroAssembly);
		PrintStream fos = new PrintStream(microAssembly);
		DataOutputStream dos = new DataOutputStream(new FileOutputStream(binaryInstructions));
		PrintStream fos3 = new PrintStream(binaryText);
		
		ArrayList<String[]> macroAssemblyLexed = new MacroAssemblerLexer().macroAssemblerLexer(sc);
		ArrayList<String> microAssembly = new MacroAssemblerParser().macroAssemblerParser(macroAssemblyLexed);
		int[] instructions = new MicroAssembler().assemble(microAssembly);
		for(int i = 0; i < microAssembly.size(); i++) {
			fos.println(microAssembly.get(i));
			fos3.println(Long.toBinaryString(Integer.toUnsignedLong(instructions[i]) | 0x1_0000_0000l).substring(1));
		}
		byte[] absoluteFileBytes = ExecutableLinkableFormat.saveToFile(instructions, binaryInstructions);
		dos.write(absoluteFileBytes);
		
		fos3.close();
		fos.close();
		dos.close();
		sc.close();
	}
}

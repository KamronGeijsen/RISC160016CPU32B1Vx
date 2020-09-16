package assembler;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Scanner;

public class Assembler {
	
	final static File dir = new File("examples/");
	
//	final static File macroAssembly = new File(dir, "snake.masm");
	final static File macroAssembly = new File(dir, "fibonacci.masm");
//	final static File macroAssembly = new File(dir, "primes2.masm");
	final static File microAssembly = new File(dir, "o.uasm");
	final static File binaryText = new File(dir, "o.bin");
	final static File binaryInstructions = new File(dir, "o.exe");
	
	public static void main(String[] args) {
		File file = null;
		File output = null;
		try {
			String flag = "";
			if(args != null)
				for(String s : args) {
					if(s.startsWith("-")) {
						if(flag == null)
							throw new IOException("Invalid flag parameter: " + s);
						else
							flag = s.substring(1);
					}
					switch (flag) {
					case "o":
						if(output == null)
							output = new File(s);
						else
							throw new IOException("Invalid flag parameter: " + s);
						break;
					case "":
						if(file == null)
							file = new File(s);
						else
							throw new IOException("Invalid flag parameter: " + s);
						break;
		
					default:
						throw new IOException("Invalid flag parameter: " + s);
					}
				}
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		
		if(file == null)
			file = macroAssembly;
		if(output == null)
			output = binaryInstructions;
		
		
//			new MacroAssemblerParser(file, output);
		if(file.exists())
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
		
		ArrayList<String> macroAssembly = new ArrayList<>();
		while(sc.hasNext()) {
			macroAssembly.add(sc.nextLine());
		}
		ArrayList<String[]> macroAssemblyLexed = new MacroAssemblerLexer().macroAssemblerLexer(macroAssembly);
		ArrayList<String> microAssembly = new MacroAssemblerParser().macroAssemblerParser(macroAssemblyLexed);
		int[] binaryInstructions = new MicroAssembler().assemble(microAssembly);
		
		for(String s : microAssembly)
			fos.println(s);
		for(int instr : binaryInstructions) {
			dos.writeInt(instr);
			fos3.println(Long.toBinaryString(Integer.toUnsignedLong(instr) | 0x1_0000_0000l).substring(1));
		}
		fos3.flush();
		fos3.close();
		fos.flush();
		fos.close();
		dos.flush();
		dos.close();
		sc.close();
	}
}

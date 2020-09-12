package compiler;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Scanner;

import lib.HardcodedLibrary;


@SuppressWarnings("serial")
public class MicroAssembler {
	
	void assemble(Scanner sc, OutputStream os, PrintStream os2) throws IOException {
		DataOutputStream dos = new DataOutputStream(os);
//		DataOutputStream dos2 = new PrintStream(os2);
		while(sc.hasNext()) {
			int instr = parse(sc.nextLine());
			
			dos.writeInt(instr);
			os2.println(Long.toBinaryString(Integer.toUnsignedLong(instr) | 1l<<32).substring(1));
		}
		sc.close();
		dos.flush();
		dos.close();
	}
	
	int parse(String s) {
//		System.out.println(s);
		long instr = 0;

		if (s.startsWith(".")) {
			s = s.substring(1);
			if(s.startsWith("data")) {
				String[] parse = s.split("\t");
//				return Integer.reverse(toImm(parse[1]));
//				System.out.println(parse[1]);
				return toImm(parse[1]);
			}
		} 
		else if (s.contains("[")) {
			String[] parse = s.split("\t");
			String[] args = parse[1].split("\\,");
			String sidd = args[1].substring(1, args[1].length() - 1);
			int SID;
			if (s.contains("*")) {
				String[] parse2 = sidd.split("\\+");
				String[] parse3 = parse2[1].split("\\*");
				SID = (toReg(parse2[0]) << 16) | (toImm(parse2[2]) << 8) | (sizesMap.get(parse3[0]) << 5)
						| (toReg(parse3[1]));
			} else {
				String[] parse2 = sidd.split("\\+");
				SID = (toReg(parse2[0]) << 16) | (toImm(parse2[1]));
			}
			if (parse[0].matches("((GET)|(LEA))\\.\\d+")) {
				String[] parse2 = parse[0].split("\\.");
				int size = sizesMap.get(parse2[1]);
				parse[0] = parse2[0] + ".s";
				instr |= size << 27;
			}
			instr |= (macroOP.get(parse[0].toUpperCase()) << 26) | (toReg(args[0]) << 21) | SID;

		} else if (s.contains("\tJ") || s.startsWith("J")) {
			if (s.startsWith("JMP") || s.startsWith("JAL")) {
				int SIO;

				String[] parse = s.split(" ");
				if (parse[1].contains(",")) {
					parse[1] = parse[1].split("\\,")[1];
				}
				if (parse[1].contains("*")) {
					String[] parse2 = parse[1].split("\\+");
					String[] parse3 = parse2[0].split("\\*");
					SIO = (toImm(parse2[1]) << 8) | (sizesMap.get(parse3[0])) | (toReg(parse3[1]));
				} else {
					SIO = toImm(parse[1]);
				}
				instr |= (macroOP.get(parse[0].toUpperCase()) << 26) | SIO;

			} else {
				int shreg_imm = 0;
				String[] parse = s.split("	");
				String[] args = parse[1].split(",");
				if (args[1].contains("*")) {
					String[] parse2 = args[1].split("\\*");
					shreg_imm = (sizesMap.get(parse2[0])) | (toReg(parse2[1]));
				} else {
					shreg_imm = toImm(args[1]);
				}
				String[] ops = parse[0].split(" ");
				instr = (0x10000000) | (toImm(ops[1]) << 21) | (toReg(args[0]) << 16)
						| (cond.get(ops[0].substring(1).toUpperCase()) << 12) | (compare.get(ops[2].toUpperCase()) << 8)
						| shreg_imm;
			}
		} else {
			int shreg_imm = 0;
			String[] parse = s.split("	");
			String[] args = parse[1].split("\\,");

			if (args[2].contains("*")) {
				String[] parse2 = args[2].split("\\*");
				shreg_imm = (sizesMap.get(parse2[0])) | (toReg(parse2[1]));
			} else {
				shreg_imm = toImm(args[2]);
			}

			String[] opc = parse[0].split("\\.");
			int macroOP = 0b01011;
			if (opc.length > 1) {
				if (opc[1].equals("D"))
					macroOP = 0b01101;
				if (opc[1].equals("INIT"))
					macroOP = 0b01111;
			}
			instr = (macroOP << 26) | (toReg(args[0]) << 21) | (toReg(args[1]) << 16)
					| (lib_op.get(opc[0].toUpperCase()) << 8) | shreg_imm;
		}
		instr |= s.contains("*") ? 0x80000000l : 0;

		return (int) instr;
	}
	
	int toReg(String s) {
		return Integer.parseInt(s.substring(1),16);
	}
	int toImm(String s) {
		if(s.startsWith("0x"))
			return Integer.parseInt(s.substring(2), 16);
		if(s.startsWith("0b"))
			return Integer.parseInt(s.substring(2), 2);
		return Integer.parseInt(s);
	}
	
	
	private final HashMap<String, Integer> sizesMap = new HashMap<String, Integer>() {{
		put("1",	0);
		put("2",	1);
		put("4",	2);
		put("8",	3);
		put("16",	4);
		put("32",	5);
		put("64",	6);
		put("128",	7);
	}};
	private final HashMap<String, Integer> macroOP = new HashMap<String, Integer>(){{
		put("JMP", 		0b00000);
		put("JAL", 		0b00010);
		put("SET", 		0b01000);
		put("GET", 		0b01001);
		put("LEA", 		0b01010);
		put("LEA.D", 	0b01100);
		put("LEA.INIT", 0b01110);
		put("LEA.S", 	0b10000);
		put("GET.S", 	0b10001);
	}};
	
	private final HashMap<String, Integer> lib_op = new HashMap<String, Integer>() {
		{
			try {
				Scanner sc = new Scanner(new File("src/lib/libop.txt"));
				while (sc.hasNextLine()) {
					String[] b = sc.nextLine().split("\t");
					put(b[0], Integer.parseInt(b[1], 16));
				}
				sc.close();
			} catch (FileNotFoundException e) {
//				e.printStackTrace();
				System.err
						.println("Interpreter: Could not find custom library LIBOP, loading standard library instead");
				int LIB = 0;
				for (String[] s1 : HardcodedLibrary.LIB_OP) {
					int OP = 0;
					for (String s : s1)
						put(s, LIB * 16 + OP++);
					LIB++;
				}
			}
		}
	};
	
	private final HashMap<String, Integer> compare = new HashMap<String, Integer>() {
		{
			try {
				Scanner sc = new Scanner(new File("src/lib/cmp.txt"));
				while (sc.hasNextLine()) {
					String[] b = sc.nextLine().split("\t");
					put(b[0], Integer.parseInt(b[1], 16));
				}
				sc.close();
			} catch (FileNotFoundException e) {
//				e.printStackTrace();
				System.err.println("Interpreter: Could not find custom library CMP, loading standard library instead");
				int i = 0;
				for (String s : HardcodedLibrary.CMP)
					put(s, i++);
			}
		}
	};
	private final HashMap<String, Integer> cond = new HashMap<String, Integer>() {
		{
			try {
				Scanner sc = new Scanner(new File("src/lib/cond.txt"));
				while (sc.hasNextLine()) {
					String[] b = sc.nextLine().split("\t");
					put(b[0], Integer.parseInt(b[1], 16));
				}
				sc.close();
			} catch (FileNotFoundException e) {
//				e.printStackTrace();
				System.err.println("Interpreter: Could not find custom library COND, loading standard library instead");
				int i = 0;
				for (String s : HardcodedLibrary.COND)
					put(s, i++);
			}
		}
	};
	
	
	public static void main(String[] args) {
		try {
			Scanner sc = new Scanner(new File("src/compiler/o.uasm"));
			FileOutputStream fos = new FileOutputStream(new File("src/compiler/o.exe"));
			PrintStream fos2 = new PrintStream(new File("src/compiler/o.bin"));
			
			new MicroAssembler().assemble(sc, fos, fos2);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
}

package assembler;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Scanner;

import lib.HardcodedLibrary;

@SuppressWarnings("serial")
public class Disassembler {
	
	public String disassemble(final long instr) {
		final int MOP = (int) ((instr >> 26) & 0xf);

		final int LibOp_CondOp = (int) ((instr >> 8) & 0xff);

		final long imm8 = (instr >> 0) & 0xff;
		final long disp16 = (instr >> 0) & 0xffff;
		final long disp8 = (instr >> 8) & 0xff;

		final long offs27 = (instr >> 0) & 0x7ffffff;
		final long offs19 = (instr >> 8) & 0x7ffff;
		final long offs7 = (instr >> 21) & 0x7f;

		final int rDEST = (int) ((instr >> 21) & 0x1f);
		final int rSRC = (int) ((instr >> 16) & 0x1f);
		final int rINDEX = (int) ((instr >> 0) & 0x1f);
		final int scale = (int) ((instr >> 5) & 0x7);
		final int size = (int) ((instr >> 27) & 0x7);

		final boolean SI = (instr & 0x80000000l) != 0;

		if ((instr & 0x40000000) == 0) {
			switch (MOP) {
			case 0:
			case 1: // JMP
				return "JMP "
						+ (SI ? (1 << scale) + "*r" + Integer.toHexString(rINDEX) + "+0x" + Long.toHexString(offs19)
								: "0x" + Long.toHexString(offs27));
			case 2:
			case 3: // JAL
				return "JAL "
						+ (SI ? (1 << scale) + "*r" + Integer.toHexString(rINDEX) + "+0x" + Long.toHexString(offs19)
								: "0x" + Long.toHexString(offs27));

			case 4:
			case 5:
			case 6:
			case 7: // Jcc
				return "J" + cond.get(LibOp_CondOp >> 4) + " 0x" + Long.toHexString(offs7) + " "
						+ compare.get(LibOp_CondOp & 0xf) + "\tr" + Integer.toHexString(rSRC) + ","
						+ (SI ? (1 << scale) + "*r" + Integer.toHexString(rINDEX) : "0x" + Long.toHexString(imm8));

			case 8: // SET
				return "SET\tr" + Integer.toHexString(rDEST) + ",[r" + Integer.toHexString(rSRC) + "+"
						+ (SI ? (1 << scale) + "*r" + Integer.toHexString(rINDEX) + "+0x" + Long.toHexString(disp8)
								: "0x" + Long.toHexString(disp16))
						+ "]";
			case 9: // GET
				return "GET\tr" + Integer.toHexString(rDEST) + ",[r" + Integer.toHexString(rSRC) + "+"
						+ (SI ? (1 << scale) + "*r" + Integer.toHexString(rINDEX) + "+0x" + Long.toHexString(disp8)
								: "0x" + Long.toHexString(disp16))
						+ "]";

			case 10:// LEA
				return "LEA\tr" + Integer.toHexString(rDEST) + ",[r" + Integer.toHexString(rSRC) + "+"
						+ (SI ? (1 << scale) + "*r" + Integer.toHexString(rINDEX) + "+0x" + Long.toHexString(disp8)
								: "0x" + Long.toHexString(disp16))
						+ "]";
			case 11:// DP
				return lib_op.get(LibOp_CondOp) + "\tr" + Integer.toHexString(rDEST) + ",r" + Integer.toHexString(rSRC)
						+ ","
						+ (SI ? (1 << scale) + "*r" + Integer.toHexString(rINDEX) : "0x" + Long.toHexString(imm8));

			case 12:// LEA.d
				return "LEA\tr" + Integer.toHexString(rDEST) + ",[r" + Integer.toHexString(rSRC) + "+"
						+ (SI ? (1 << scale) + "*r" + Integer.toHexString(rINDEX) + "+0x" + Long.toHexString(disp8)
								: "0x" + Long.toHexString(disp16))
						+ "]";
			case 13:// DP.d
				return lib_op.get(LibOp_CondOp) + ".d\tr" + Integer.toHexString(rDEST) + ",r" + Integer.toHexString(rSRC)
						+ ","
						+ (SI ? (1 << scale) + "*r" + Integer.toHexString(rINDEX) : "0x" + Long.toHexString(imm8));

			case 14:// LEA.init
				return "LEA\tr" + Integer.toHexString(rDEST) + ",[r" + Integer.toHexString(rSRC) + "+"
						+ (SI ? (1 << scale) + "*r" + Integer.toHexString(rINDEX) + "+0x" + Long.toHexString(disp8)
								: "0x" + Long.toHexString(disp16))
						+ "]";
			case 15:// DP.init
				return lib_op.get(LibOp_CondOp) + ".init\tr" + Integer.toHexString(rDEST) + ",r"
						+ Integer.toHexString(rSRC) + ","
						+ (SI ? scale + "*r" + Integer.toHexString(rINDEX) : "0x" + Long.toHexString(imm8));
			default:
				throw new Error(
						"Literally impossible. MOP is masked to 4 bits, all combinations of switch statement are present");
			}
		} else {
			if ((instr & 0x04000000) == 0) {
				return "LEA." + (1 << size) + "\tr" + Integer.toHexString(rDEST) + ",[r" + Integer.toHexString(rSRC)
						+ "+"
						+ (SI ? (1 << scale) + "*r" + Integer.toHexString(rINDEX) + "+0x" + Long.toHexString(disp8)
								: "0x" + Long.toHexString(disp16))
						+ "]";
			} else {
				return "GET." + (1 << size) + "\tr" + Integer.toHexString(rDEST) + ",[r" + Integer.toHexString(rSRC)
						+ "+"
						+ (SI ? (1 << scale) + "*r" + Integer.toHexString(rINDEX) + "+0x" + Long.toHexString(disp8)
								: "0x" + Long.toHexString(disp16))
						+ "]";
			}
		}
	}
	

	private final HashMap<Integer, String> lib_op = new HashMap<Integer, String>() {
		{
			try {
				Scanner sc = new Scanner(new File("src/lib/libop.txt"));
				while (sc.hasNextLine()) {
					String[] b = sc.nextLine().split("\t");
					if(!containsKey(Integer.parseInt(b[1], 16)))
						put(Integer.parseInt(b[1], 16), b[0]);
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
						put(LIB * 16 + OP++, s);
					LIB++;
				}
				System.out.println(this);
			}
		}
	};
	private final HashMap<Integer, String> compare = new HashMap<Integer, String>() {
		{
			try {
				Scanner sc = new Scanner(new File("src/lib/cmp.txt"));
				while (sc.hasNextLine()) {
					String[] b = sc.nextLine().split("\t");
					if(!containsKey(Integer.parseInt(b[1], 16)))
						put(Integer.parseInt(b[1], 16), b[0]);
				}
				sc.close();
			} catch (FileNotFoundException e) {
//				e.printStackTrace();
				System.err.println("Interpreter: Could not find custom library CMP, loading standard library instead");
				int i = 0;
				for (String s : HardcodedLibrary.CMP)
					put(i++, s);
			}
		}
	};
	private final HashMap<Integer, String> cond = new HashMap<Integer, String>() {
		{
			try {
				Scanner sc = new Scanner(new File("src/lib/cond.txt"));
				while (sc.hasNextLine()) {
					String[] b = sc.nextLine().split("\t");
					if(!containsKey(Integer.parseInt(b[1], 16)))
						put(Integer.parseInt(b[1], 16), b[0]);
				}
				sc.close();
			} catch (FileNotFoundException e) {
//				e.printStackTrace();
				System.err.println("Interpreter: Could not find custom library COND, loading standard library instead");
				int i = 0;
				for (String s : HardcodedLibrary.COND)
					put(i++, s);
			}
		}
	};
}

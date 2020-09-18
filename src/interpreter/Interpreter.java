package interpreter;

import java.util.Arrays;

import interpreter.GUI.RandomAccessMemoryUnit;
import interpreter.GUI.SystemAgent;

public class Interpreter {

	private final RandomAccessMemoryUnit RAM;
	final long[] reg = new long[32];
	final byte[] regSize = new byte[32];
	int rIP = 0;

	Interpreter(RandomAccessMemoryUnit RAM, SystemAgent systemAgent) {
		this.RAM = RAM;
	}

	public void initialize() {
		Arrays.fill(reg, 0);
		Arrays.fill(regSize, (byte) 0);
		rIP = 0;
	}

	public void executeCycle() {
		executeInstr(RAM.loadInstr(rIP));
	}

	public void executeInstr(final long instr) {
		final int MOP = (int) ((instr >> 26) & 0xf);

		final int LibOp_CondOp = (int) ((instr >> 8) & 0xff);

		final long imm8 = (instr >> 0) & 0xff;
		final long disp16 = sxt((instr >> 0) & 0xffff, 16);
		final long disp8 = sxt((instr >> 8) & 0xff, 8);

		final long offs27 = sxt((instr >> 0) & 0x7ffffff, 27);
		final long offs19 = sxt((instr >> 8) & 0x7ffff, 19);
		final long offs7 = sxt((instr >> 21) & 0x7f, 7);

		final int rDEST = (int) ((instr >> 21) & 0x1f);
		final int rSRC = (int) ((instr >> 16) & 0x1f);
		final int rINDEX = (int) ((instr >> 0) & 0x1f);
		final int scale = (int) ((instr >> 5) & 0x7);
		final int size = (int) ((instr >> 27) & 0x7);

		final boolean SI = (instr & 0x80000000l) != 0;

		int nextPC = rIP + 1;

		final long shreg = SI ? reg[rINDEX] << scale : imm8;
		final long SIO = SI ? ((reg[rINDEX] << scale) >> 5) + offs19 : offs27;
		final long SID = SI ? (reg[rINDEX] << scale) + disp8 : disp16;

		if ((instr & 0x40000000) == 0) {
			switch (MOP) {
			case 2:
			case 3: // JAL
				reg[30] = rIP << 5;
				regSize[30] = 32;
			case 0:
			case 1: // JMP
				nextPC = SI ? (int) (SIO) : (int) (rIP + SIO);
				break;
			case 4:
			case 5:
			case 6:
			case 7: // Jcc
				if (COND(LibOp_CondOp >> 4, COMP(LibOp_CondOp & 0xf, reg[rSRC], shreg, regSize[rSRC]), regSize[rSRC]))
					nextPC = (int) (rIP + offs7);
				break;

			case 8: // SET
				RAM.SET((int) (reg[rSRC] + SID), regSize[rDEST], reg[rDEST]);
				break;
			case 9: // GET
				reg[rDEST] = RAM.GET((int) (reg[rSRC] + SID), regSize[rDEST]);
				break;

			case 10:// LEA
				reg[rDEST] = maskLowestNBits(reg[rSRC] + SID, regSize[rDEST]);
				break;
			case 11:// DP
				reg[rDEST] = maskLowestNBits(LIB_OP(LibOp_CondOp, reg[rSRC], shreg, regSize[rSRC]), regSize[rDEST]);
				break;

			case 12: {// LEA.d
				final long dest = reg[rSRC] + SID;
				final long mask = ((1 << regSize[rSRC]) - 1);
				reg[rDEST] = maskLowestNBits(reg[rDEST] & ~mask | dest & mask, regSize[rDEST]);
				break;
			}
			case 13: {// DP.d
				final long dest = LIB_OP(LibOp_CondOp, reg[rSRC], shreg, regSize[rSRC]);
				final long mask = ((1 << regSize[rSRC]) - 1);
				reg[rDEST] = maskLowestNBits(reg[rDEST] & ~mask | dest & mask, regSize[rDEST]);
				break;
			}

			case 14:// LEA.init
				regSize[rDEST] = regSize[rSRC];
				reg[rDEST] = maskLowestNBits(reg[rSRC] + SID, regSize[rDEST]);
				break;
			case 15:// DP.init
				regSize[rDEST] = regSize[rSRC];
				reg[rDEST] = maskLowestNBits(LIB_OP(LibOp_CondOp, reg[rSRC], shreg, regSize[rSRC]), regSize[rDEST]);
				break;
			}
		} else {
			regSize[rDEST] = (byte) (1 << size);
			
			if ((instr & 0x04000000) == 0) { // LEA.size
				reg[rDEST] = maskLowestNBits(reg[rSRC] + SID, regSize[rDEST]);
			} else { // GET.size
				reg[rDEST] = RAM.GET((int) (reg[rSRC] + SID), regSize[rDEST]);
			}
		}
		
		reg[rDEST] &= ((1l << regSize[rDEST]) - 1);
		rIP = nextPC;
	}

	private long LIB_OP(int lib_op, long src, long src2, byte xtSize) {
		int lib = lib_op >> 4;
		int op = lib_op & 0xf;
		switch (lib) {
		case 0:
			src = (op & 0b0001) != 0 ? sxt(src, xtSize) : src;
			if ((op & 0b0100) != 0)
				src2 = ~src2;
			if ((op & 0b1000) != 0)
				src = ~src;
			return src + src2 + ((op & 0b0010) != 0 ? 1 : 0);
		case 1:
			return ((op & 8) != 0 ? ~src & ~src2 : 0) | ((op & 4) != 0 ? src & ~src2 : 0)
					| ((op & 2) != 0 ? ~src & src2 : 0) | ((op & 1) != 0 ? src & src2 : 0);
		case 2:
			switch (op) {
			case 0:
				return src << src2;
			case 1:
				return src >> src2;
			default:
				throw new IllegalArgumentException("Not implemented yet");
			}
		case 3:
			switch (op) {
			case 0:
				return src * src2;
			case 6:
				return src % src2;
			default:
				throw new IllegalArgumentException("Not implemented yet");
			}
		case 4:
			throw new IllegalArgumentException("Not implemented yet");
		case 5:
			throw new IllegalArgumentException("Not implemented yet");
		case 6:
			throw new IllegalArgumentException("Not implemented yet");
		case 7:
			throw new IllegalArgumentException("Not implemented yet");
		case 8:
			throw new IllegalArgumentException("Not implemented yet");
		case 9:
			throw new IllegalArgumentException("Not implemented yet");
		case 10:
			throw new IllegalArgumentException("Not implemented yet");
		case 11:
			throw new IllegalArgumentException("Not implemented yet");
		case 12:
			throw new IllegalArgumentException("Not implemented yet");
		case 13:
			return (long) (Math.random() * (src2 - src) + src);
		case 14:
			throw new IllegalArgumentException("Not implemented yet");
		case 15:
			throw new IllegalArgumentException("Not implemented yet");
		}
		throw new IllegalArgumentException();
	}
	private long COMP(int comp, long src, long src2, byte xtSize) {

		switch (comp) {
		case 0:
			return (Integer.toUnsignedLong((int) src) - Integer.toUnsignedLong((int) src2));
		case 1:
			return sxt(src, xtSize) - sxt(src2, xtSize);
		case 2:
			return src + src2;
		case 3:
			return sxt(src, xtSize) + sxt(src2, xtSize);
		case 4:
			return src & src2;
		case 5:
			return src | src2;
		case 6:
			return src & ~src2;
		case 7:
			return src ^ src2;
		case 8:
			throw new IllegalArgumentException("Not implemented yet");
		case 9:
			throw new IllegalArgumentException("Not implemented yet");
		case 10:
			throw new IllegalArgumentException("Not implemented yet");
		case 11:
			throw new IllegalArgumentException("Not implemented yet");
		case 12:
			throw new IllegalArgumentException("Not implemented yet");
		case 13:
			throw new IllegalArgumentException("Not implemented yet");
		case 14:
			throw new IllegalArgumentException("Not implemented yet");
		case 15:
			throw new IllegalArgumentException("Not implemented yet");
		default:
			throw new Error(
					"Impossible. COND is masked to 4 bits, all combinations of switch statement are present");
		}
	}
	private boolean COND(int comp, long result, byte size) {
		boolean not = (comp & 1) != 0;
		int op = comp >> 1;
		switch (op) {
		case 0:
			return (0 == result) != not;
		case 1:
			return (result < 0) != not;
		case 2:
			return (result <= 0) != not;
		case 3:
			return ((result & 0x80000000l) != 0) != not;
		case 4:
			throw new IllegalArgumentException("Not implemented yet");
		case 5:
			throw new IllegalArgumentException("Not implemented yet");
		case 6:
			throw new IllegalArgumentException("Not implemented yet");
		case 7:
			throw new IllegalArgumentException("Not implemented yet");
		default:
			throw new Error(
					"Impossible. OP is masked to 3 bits, all combinations of switch statement are present");
		}
		
	}

	private static long sxt(long field, int bits) {
//		field &= (1l << bits) - 1;  // <= not needed as all instructions are masked to their size
		final long i = 1l << (bits - 1);
		return (field ^ i) - i;
	}
	private static long maskLowestNBits(long field, int bits) {
		return field & ((1l << bits) - 1);
	}
}

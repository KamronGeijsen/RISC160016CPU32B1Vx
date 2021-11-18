package assembler;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;

import lib.HardcodedLibrary;

@SuppressWarnings("serial")
public class MacroAssemblerParser {


	
	private final HashMap<String, Integer> labelNamesToMicroAddresses = new HashMap<String, Integer>();

	private int allocatedRegistersMap = 0;
	
	ArrayList<String> macroAssemblerParser(ArrayList<String[]> macroLexed) {
		ArrayList<String> microASM = new ArrayList<String>();
		for(String[] s : macroLexed) {
			if(s.length == 32) {
				try {
					parse(s, microASM);
				} catch (ParseException e){
					System.err.println(String.join(", ", s));
					throw e;
				}
				
			} else {
				labelNamesToMicroAddresses.put(s[0], microASM.size());
			}
		}
		insertLabels(microASM);
		
		return microASM;
	}
	
	void insertLabels(ArrayList<String> microASM) {
		for (int currentAddr = 0; currentAddr < microASM.size(); currentAddr++) {
			if (microASM.get(currentAddr).contains(":")) {
				String[] parts = microASM.get(currentAddr).split(":", 2);
				Integer addr = labelNamesToMicroAddresses.get(parts[0]);
				if (addr == null) {
					System.out.println(microASM.get(currentAddr));
					throw new ParseException("Label not found: " + parts[0]);
				}
				String name = parts[1];
				if(name.startsWith(".data"))
					addr = addr & -1;
				else if(name.startsWith("JMP") || name.startsWith("JAL")) 
					addr = addr - currentAddr & 0x7ffffff;
				else if(name.startsWith("J")) 
					addr = addr - currentAddr & 0x7f;
				else
					addr = addr*32 & 0xffff;
				
				microASM.set(currentAddr, parts[1].replaceFirst("#", "0x" + (Integer.toHexString(addr))));
			}
		}
	}
	
	void parse(String[] s, ArrayList<String> microASM) {
		
//		System.out.println(java.util.Arrays.toString(s));
		StringBuilder sb = new StringBuilder();
		String command = s[0].toUpperCase();
		if(command.matches("\\$FREE")) {
			for(int i = 1; s[i] != null; i+=2)
				deallocateRegister(s[i]);
		}
		else if(command.matches("\\$INIT")) {
			for(int i = 1; s[i] != null; i+=2) {
//				System.out.println(s[i]);
				if(s[i].contains("=")) {
					String[] parts = s[i].split("=");
					allocateRegister(parts[0], parts[1]);
				}
				else allocateRegister(s[i]);
			}
		}
		else if(command.matches("\\$DATA\\.\\w+")) {
			Integer size = sizesMap.get(command.split("\\.")[1]);
			if(size == null)
				throw new ParseException("Datatype does not exist: " + command.split("\\.")[1]);
			int effectiveSize = 0;
			int tempInt = 0;
			if(size > 32)
				throw new ParseException("Larger than int sizes not supported yet");
			else {
//				
				for(int i = 1; s[i] != null; i+=2) {
					tempInt |= (toImm(s[i]) & ((1l << size) - 1)) << effectiveSize;
					if((effectiveSize += size) == 32) {
						microASM.add(".data	0x" + Integer.toHexString(tempInt));
						tempInt = effectiveSize = 0;
					}
					
				}
				if(effectiveSize != 0) {
					microASM.add(".data	0x" + Integer.toHexString(tempInt));
				}
				
			}
			
		}
		else if(command.contentEquals(".DATA")) {
			microASM.add(".data	0x" + Integer.toHexString(toImm(s[1])));
		}

		else if(command.matches("INIT\\.\\w+|(LEA|GET)\\.\\w+")) {
			if(s[1].contains("=")) {
				String[] parts = s[1].split("=");
				allocateRegister(parts[0], parts[1]);
				s[1]=parts[0];
			}
			else allocateRegister(s[1]);
			boolean bracket = s[3].contentEquals("[");
			if(command.charAt(0) == 'I') {
				sb.append(bracket ? "GET." : "LEA.");
				sb.append(sizesMap.get(command.substring(5)));
			}
			else {
				sb.append(command.charAt(0) == 'L'?"LEA.":"GET.");
				sb.append(sizesMap.get(command.substring(4)));
			}
			sb.append('\t');
			sb.append(register(s[1]));
			
			sb.append(',');
			address(sb, s, bracket?4:3);
			
			String sbs = sb.toString();
			if(sbs.contains(":")) {
				String[] sbss = sbs.split(":", 2);
				sb.setLength(0);
				sb.append(sbss[1]);
				sb.append(':');
				sb.append(sbss[0]);
				microASM.add(sb.toString());
			}
			else
				microASM.add(sbs);
		}
		else if(command.matches("\\$CHAR\\.\\w+") || command.matches("\\$ASCIIZ?")) {
			int size;
			if(command.charAt(1) == 'A') {
				size = 8;
				if(command.length() == "$ASCIIZ".length())
					s[1]+="\\0";
			} else {
				size = sizesMap.get(command.split("\\.")[1]);
			}
			
				
			int effectiveSize = 0;
			int tempInt = 0;
			if(size > 32)
				throw new ParseException("Larger than int sizes not supported yet");
			for(int i = 1; i < s[1].length(); i++) {
				char c = s[1].charAt(i);
				if (c == '\\') {
					
					switch(s[1].charAt(++i)) {
					case '0': c = 0; break;
					case 'n': c = '\n'; break;
					}
					
				}
				tempInt |= ((int)(c) & ((1l << size) - 1)) << effectiveSize;
				if((effectiveSize += size) == 32) {
					microASM.add(".data	0x" + Integer.toHexString(tempInt));
					tempInt = effectiveSize = 0;
				}
				
			}
			if(effectiveSize != 0) {
				microASM.add(".data	0x" + Integer.toHexString(tempInt));
			}
		}
		else if(command.matches("\\$LABEL(\\.\\w+)?") || command.matches("\\$ADDR(\\.\\w+)?")) {
			int size;
			if(command.contains(".")) {
				size = sizesMap.get(command.split("\\.")[1]);
			} else {
				size = 32;
			}
			if(size != 32)
				throw new ParseException("Larger than int sizes not supported yet");
			for(int i = 1; s[i] != null; i+=2) {
				microASM.add(s[i] + ":.data	#");
			}
		}
		else if(command.matches("\\$RES(ERVE)?(\\.\\w+)?") || command.matches("\\$ZERO(\\.\\w+)?")) {
			int size;
			if(command.contains(".")) {
				size = sizesMap.get(command.split("\\.")[1]);
			} else {
				size = 1;
			}
			if(size > 32)
				throw new ParseException("Larger than int sizes not supported yet");
			int totalBitAmount = 0;
			for(int i = 1; s[i] != null; i+=2) {
				totalBitAmount += (toImm(s[i])) * size;
			}
			for(int i = 0; i < totalBitAmount; i+=32) {
				microASM.add(".data	0x0");
			}
		}
		else if(command.matches("(SET|GET|LEA(|\\.D|\\.INIT))")) {
			boolean implicitAddress = command.startsWith("LEA") && !s[3].contentEquals("[");
			sb.append(command);
			sb.append('\t');
			sb.append(register(s[1]));
			sb.append(',');
			address(sb, s, implicitAddress?3:4);

			String sbs = sb.toString();
			if(sbs.contains(":")) {
				String[] sbss = sbs.split(":", 2);
				sb.setLength(0);
				sb.append(sbss[1]);
				sb.append(':');
				sb.append(sbss[0]);
				microASM.add(sb.toString());
			}
			else
				microASM.add(sbs);
		}
		else if(lib_op.contains(command.toUpperCase())) {
//			System.out.println(Arrays.toString(s));
			sb.append(command);
			sb.append("\t");
			sb.append(register(s[1]));
			sb.append(',');
			if(s[4] != null && s[4].contentEquals("*")) {
				sb.append(s[1]);
				sb.append(",");
				if(s[3].matches("\\d+")) {
					sb.append(s[3]);
					sb.append("*");
					sb.append(register(s[5]));
				} else {
					sb.append(s[5]);
					sb.append("*");
					sb.append(register(s[3]));
				}
			} else if(s[6] != null && s[6].contentEquals("*")) {
				sb.append(s[3]);
				sb.append(",");
				if(s[5].matches("\\d+")) {
					sb.append(s[5]);
					sb.append("*");
					sb.append(register(s[7]));
				} else {
					sb.append(s[7]);
					sb.append("*");
					sb.append(register(s[5]));
				}
			}else {
				String src = s[4] == null ? s[1] : s[3];
				String src2 = s[4] == null ? s[3] : s[5];
				sb.append(register(src));
				sb.append(",");
				if(src2.matches("\\d\\w*"))
					sb.append("0x"+Integer.toHexString(toImm(src2)&0xff));
				else {
					sb.append("1*");
					sb.append(register(src2));
				}
			}
			microASM.add(sb.toString());
		}
		else if(command.startsWith("J")) {
			if(command.matches("(JMP|JAL)")) {
				int offs = 0;
				String si = null;	
				String label = null;
				for(int i = 1; i < 32; i+=2) {
					String arg = s[i];
					String separator = s[i+1];
					if(arg.matches("\\-?[\\w#]+")) {
						if(separator == null || separator.contentEquals("+")) {
							if(arg.matches("\\-?\\d\\w*")) {
								offs += toImm(arg);
							}
							else  {
								try {
									String index = register(arg);
									if(si == null)
										si = "1*"+index;
									else
										throw new ParseException("Jumpaddress contains too many scaled indexes: " + arg);
								} catch (ParseException e) {
									if(label == null)
										label = arg; 
									else
										throw new ParseException("Jumpaddress contains too many labels: " + arg);
								}
							}
							if(separator == null)
								break;
						}
						else if (separator.contentEquals("*")) {
							String index;
							Integer scale;
							String arg2;
							String separator2;
							try {
								index = register(arg);
								arg2 = s[i+2];
								separator2 = s[i+3];
								if(separator2 == null || separator2.contentEquals("+")) {
									scale = toImm(arg2);
								} else
									throw new ParseException("Jumpaddress contains invalid separator: " + separator2);
								if(si == null)
									si = scale+"*"+index;
								else
									throw new ParseException("Jumpaddress contains too many scaled indexes: " + arg);
							} catch (ParseException e) {
								scale = toImm(arg);
								arg2 = s[i+2];
								separator2 = s[i+3];
								if(separator2 == null || separator2.contentEquals("+")) {
									index = register(arg2);
								} else
									throw new ParseException("Jumpaddress contains invalid separator: " + separator2);
								if(si == null)
									si = scale+"*"+index;
							}
							i+=2;
							if(separator2 == null)
								break;
						}
						else
							throw new ParseException("Jumpaddress separator does not exist: " + separator);
					}else
						throw new ParseException("Jumpaddress argument invalid: " + separator);
				}
				
				if(label == null) {
					sb.append(command);
					sb.append(' ');
					if(si != null) {
						sb.append(si);
						sb.append('+');
					}
					sb.append(offs);
					
				}
				else {
					sb.append(label);
					sb.append(':');
					sb.append(command);
					sb.append(" #");
				}
				
			} else {
				String src1 = s[3];
				String src2 = s[5];
				String cmp = s[2];
				if(!cond.contains(command)) throw new ParseException("Condition does not exist: " + s[0]);
				if(!compare.contains(cmp.toUpperCase())) {
					if(s[3] == null && command.matches("JN?Z")) {
						cmp = "OR";
						src1 = s[2];
						src2 = "0";
					}else
						throw new ParseException("Compare does not exist: " + s[3]);
				}
//				System.out.println(command + "\t" + Arrays.toString(s));
				if(s[1].matches("\\-?(0x[0-9A-Fa-f]+|0b[01]+|\\d+)")) {
					sb.append(command);
					sb.append(' ');
					sb.append(s[1]);
					sb.append(' ');
					sb.append(cmp);
				}
				else {
					sb.append(s[1]);
					sb.append(':');
					sb.append(command);
					sb.append(" # ");
					sb.append(cmp);
				}
				
				sb.append("\t");
				sb.append(register(src1));
				sb.append(',');
				if(s[6] != null && s[6].equals("*")) {
					if(s[5].matches("\\d+")) {
						sb.append(s[5]);
						sb.append("*");
						sb.append(register(s[7]));
					}else {
						sb.append(s[7]);
						sb.append("*");
						sb.append(register(s[5]));
					}
				} else {
					if(src2.matches("\\d\\w*")) 
						sb.append("0x"+Integer.toHexString(toImm(src2)&0xff));
					else {
						sb.append("1*");
						sb.append(register(src2));
					}
				}
				
			}
			microASM.add(sb.toString());
		}
		else if(command.matches("SYSCALL")) {
			int regNumber = 1;
			for(int i = 3; s[i] != null; i+=2) {
				if(!s[i-1].contentEquals(","))
					throw new ParseException("Invalid arguments");
//				int l = toImm(s[i]);
//				if((l|0x7fff) > 0x7fff)
//					 throw new ParseException("Immediate overload");
//				microASM.add("LEA.32	r"+Integer.toHexString(i/2)+",[r0+0x" + Integer.toHexString(l)+"]");
				/*
				 *  {
			if(s[1].contains("=")) {
				String[] parts = s[1].split("=");
				allocateRegister(parts[0], parts[1]);
				s[1]=parts[0];
			}
			else allocateRegister(s[1]);
			boolean get = s[3].contentEquals("[");
			
			
			sb.append((get ? "GET." : "LEA."));
			sb.append(sizesMap.get(command.substring(5)));
			sb.append('\t');
			sb.append(register(s[1]));
			
			sb.append(',');
			address(sb, s, get?4:3);
			
			String sbs = sb.toString();
			if(sbs.contains(":")) {
				String[] sbss = sbs.split(":", 2);
				sb.setLength(0);
				sb.append(sbss[1]);
				sb.append(':');
				sb.append(sbss[0]);
				microASM.add(sb.toString());
			}
			else
				microASM.add(sbs);
		}
				 */
				if(s[i].contentEquals("[")) {
					sb.append("GET.32	r"+Integer.toHexString(regNumber)+",");
					address(sb, s, i+1);
				} else {
					sb.append("LEA.32	r"+Integer.toHexString(regNumber)+",");
					address(sb, s, i);
					
				}
				while (s[i] != null && !s[i].contentEquals(","))
					i++;
				i--;

				String sbs = sb.toString();
				if(sbs.contains(":")) {
					String[] sbss = sbs.split(":", 2);
					sb.setLength(0);
					sb.append(sbss[1]);
					sb.append(':');
					sb.append(sbss[0]);
					microASM.add(sb.toString());
				}
				else
					microASM.add(sbs);
				sb.setLength(0);
				regNumber++;
			}
			if(s[1] != null) {
				int l = toImm(s[1]);
				if((l|0x7fff) > 0x7fff)
					 throw new ParseException("Immediate overload");
				microASM.add("LEA.32	r0,[r0+0x" + Integer.toHexString(l)+"]");
			}
			microASM.add("JMP 0");
			
		}
		else if(command.matches("RET")) {
			microASM.add("JMP 1*r1e+1");
		}
		else if(command.matches("NOP")) {
			microASM.add("LEFT	r0,r0,0");
		}
		else if(command.matches("(CLR|INC|DEC|NOT|NEG)")) {
			sb.append(new HashMap<String, String>(){{
				put("CLR","RIGHT");
				put("INC","ADD");
				put("DEC","SUB");
				put("NOT","NXOR");
				put("NEG","SUBR");
			}}.get(command));
			sb.append("\t");
			sb.append(register(s[1]));
			sb.append(',');
			sb.append(register(s[3]!=null?s[3]:s[1]));
			sb.append(',');
			sb.append(new HashMap<String, String>(){{
				put("CLR","0");
				put("INC","1");
				put("DEC","1");
				put("NOT","0");
				put("NEG","0");
			}}.get(command));
			microASM.add(sb.toString());
		} 
		else if(command.matches("MOV")) {
			sb.append("RIGHT\t");
			sb.append(register(s[1]));
			sb.append(',');
			sb.append(register(s[1]));
			sb.append(',');
			if(s[3].matches("\\d\\w*")) 
				sb.append(s[3]);
			else {
				sb.append("1*");
				sb.append(register(s[3]));
			}
			microASM.add(sb.toString());
		}
		else
			throw new ParseException(" :: TODO: " + command);
	}



	void address(StringBuilder sb, String[] s, int start) {
		String disp = null;
		String base = null;
		String si = null;
		String label = null;
		boolean plusSeparator = true;
		String current = s[start];
		while(current != null && !current.contentEquals("]") && !current.contentEquals(",")) {
			if(plusSeparator) {
				if(current.matches("\\-?\\d\\w*")) {
					if(s[start+1] != null && s[start+1].contentEquals("*")) {
						Integer scale = sizesMap.get(current);
						if(scale == null) throw new ParseException("Address SI must contain valid scale");
						si = scale + "*" + register(s[start+2]);
						start+=2;
					} else {
						if(disp != null) {
							disp = "0x"+Integer.toHexString(toImm(disp) + toImm(s[start]));
						}
						else
							disp = "0x"+Integer.toHexString(toImm(s[start]));
					}
				}
				else if(current.matches("\\w+")) {
					if(base != null) {
						if(si != null) throw new ParseException("Address can not contain this many variables");
						si = "1*" + register(current);
					}
					else {
						try {
							base = register(current);
						} catch (ParseException e) {
							if(e.getMessage().startsWith("Unknown register:")) {
								disp = "#";
								label = current;
							}
								
							else throw e;
						}
					}
						
				}
				else throw new ParseException("Unrecognized symbol: " + s[start]);
				plusSeparator = false;
			} else {
				if(current.contentEquals("+")) 
					plusSeparator = true;
				else 
					throw new ParseException("Address must contain separators");
			}
			current = s[++start];
		}
		
		if(base == null)
			base = "r0";
		if(disp == null)
			disp = "0";
		
			
		sb.append("[");
		
		if(si == null) {
			if(label == null)
				if((toImm(disp)|0xffff) > 0xffff)
					 throw new ParseException("Immediate overload");
			sb.append(base);
			sb.append('+');
			if(label == null)
				sb.append("0x"+Integer.toHexString(toImm(disp)&0xffff));
			else 
				sb.append("#");
		} else {
			if(label == null)
			if((toImm(disp)|0xff) > 0xff)
				 throw new ParseException("Immediate overload");
			sb.append(base);
			sb.append('+');
			sb.append(si);
			sb.append('+');
			if(label == null)
				sb.append("0x"+Integer.toHexString(toImm(disp)&0xff));
			else 
				sb.append("#");
		}
		sb.append("]");
		if(label != null){
			sb.append(":");
			sb.append(label);
		}
	}
	
	void allocateRegister(String name) {
		if(regsMap.containsKey(name))
			regsMap.remove(name);
		if(!name.matches("r[01]?[0-9a-fA-F]")) {
			int num = Integer.numberOfTrailingZeros(~allocatedRegistersMap);
			if(num == 32)
				throw new ParseException("No allocatable registers available");
			regsMap.put(name, "r"+Integer.toHexString(num));
			allocatedRegistersMap |= 1<<num;
		}
//		System.out.println(regsMap);
	}
	void allocateRegister(String name, String regname) {
//		System.out.println(name);
		if(regsMap.containsKey(name))
			regsMap.remove(name);
		if(regname.matches("r[01]?[0-9a-fA-F]")) {
			int num = Integer.parseInt(register(regname).substring(1),16);
			regsMap.put(name, "r"+Integer.toHexString(num));
			allocatedRegistersMap |= 1<<num;
		}
		else {
			String previous = regsMap.get(regname);
			if(previous == null)
				throw new ParseException("Cannot initialize '" + name + "' to '" + regname + "', it doesn't exist");
			regsMap.put(name, previous);
		}
	}
	void deallocateRegister(String name) {
		String reg = regsMap.remove(name);
		if(!regsMap.containsValue(reg)) {
			int num = Integer.parseInt(register(reg).substring(1),16);
			allocatedRegistersMap &= ~(1<<num);
		}
	}
	
	String register(String s) {
		String reg = regsMap.get(s);
		if(reg != null)
			return reg;
		if(s.matches("r1?[0-9a-fA-F]"))
			return s;
		throw new ParseException("Unknown register: \'" + s + "\'");
	}
	int toImm(String s) {
		if(s.startsWith("-")) {
			if(s.startsWith("-0x"))
				return (int)-Long.parseLong(s.substring(3), 16);
			if(s.startsWith("-0b"))
				return (int)-Long.parseLong(s.substring(3), 2);
			return (int)Long.parseLong(s);
		}
		if(s.startsWith("0x"))
			return (int)Long.parseLong(s.substring(2), 16);
		if(s.startsWith("0b"))
			return (int)Long.parseLong(s.substring(2), 2);
		return (int)Long.parseLong(s);
	}
	
	
	private final HashMap<String, Integer> sizesMap = new HashMap<String, Integer>() {
		{
			put("BIT", 1);
			put("COUPLE", 2);
			put("NIBBLE", 4);
			put("BYTE", 8);
			put("SHORT", 16);
			put("HALFWORD", 16);
			put("INT", 32);
			put("WORD", 32);
			put("LONG", 64);
			put("BULK", 128);

			put("1", 1);
			put("2", 2);
			put("4", 4);
			put("8", 8);
			put("16", 16);
			put("32", 32);
			put("64", 64);
			put("128", 128);
		}
	};	
	private final HashMap<String, String> regsMap = new HashMap<>();
	private final HashSet<String> lib_op = new HashSet<String>() {
		{
			try {
				Scanner sc = new Scanner(new File("src/lib/libop.txt"));
				while (sc.hasNextLine()) {
					String[] b = sc.nextLine().split("\t");
					add(b[0]);
				}
				sc.close();
			} catch (FileNotFoundException e) {
				System.err.println(
						"MacroAssembler: Could not find custom library LIBOP, loading standard library instead");
				for (String[] s1 : HardcodedLibrary.LIB_OP)
					for (String s : s1)
						add(s);

				System.out.println(this);
			}
		}
	};
	private final HashSet<String> compare = new HashSet<String>() {
		{
			try {
				Scanner sc = new Scanner(new File("src/lib/cmp.txt"));
				while (sc.hasNextLine()) {
					String[] b = sc.nextLine().split("\t");
					add(b[0]);
				}
				sc.close();
			} catch (FileNotFoundException e) {
				System.err.println("Interpreter: Could not find custom library CMP, loading standard library instead");
				for (String s : HardcodedLibrary.CMP)
					add(s);
			}
		}
	};
	private final HashSet<String> cond = new HashSet<String>() {
		{
			try {
				Scanner sc = new Scanner(new File("src/lib/cond.txt"));
				while (sc.hasNextLine()) {
					String[] b = sc.nextLine().split("\t");
					add("J"+b[0]);
				}
				sc.close();
			} catch (FileNotFoundException e) {
				System.err.println("Interpreter: Could not find custom library COND, loading standard library instead");
				for (String s : HardcodedLibrary.COND)
					add("J"+s);
			}
		}
	};
	
	
	class ParseException extends RuntimeException {
		public ParseException(String message) {
			super(message);
		}

		private static final long serialVersionUID = 1L;
		
	}
	
	
}

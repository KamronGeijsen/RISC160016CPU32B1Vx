package interpreter;

public class Main {
	public static void main(String[] args) {
		
		new Main();
	}
	
	Main(){
		//int i = 0b0100110101000;
		int i = 0b0011011101111;
		
//		print(~(i|~(i+1)));
//		print((i&(-i)));
//		print(0);
//		print(j);
//		for(byte n = 0; n < 16; n++)
//			System.out.println("INC	" + operation[n] + "	b" + toStr(doOperation(i, i+1, n)));
		for(byte n = 0; n < 16; n++)
			System.out.println("NEG	" + operation[n] + "	b" + toStr(doOperation(~i, (~i)+1, n)));
//		for(byte n = 0; n < 16; n++) {
//			byte b = n;
//			if((b&2)!=0) b^=4;
//			if((b&8)!=0) b^=7;
//			print(b);
//		}
//			System.out.println(toStr(doOperation(0b0011, 0b0101, n)));
			
	}
	
	int doOperation(int a, int b, byte operation) {
		return 
		((operation&8) != 0 ? ~a&~b : 0) | 
		((operation&4) != 0 ? ~a&b : 0) | 
		((operation&2) != 0 ? a&~b : 0) | 
		((operation&1) != 0 ? a&b : 0);
	}
	
	void print(int i) {
		System.out.println(Integer.toBinaryString(0x10000+(i&0xffff)).substring(1));
	}
	
	String toStr(int i) {
		return Integer.toBinaryString(0x10000+(i&0xffff)).substring(1);
	}

	String[] operation = {
			"ZERO",
			"AND",
			"ANDN",
			"RIGHT",
			"NORN",
			"LEFT",
			"XOR",
			"OR",
			"NOR",
			"NXOR",
			"NLEFT",
			"NANDN",
			"NRIGHT",
			"NORN",
			"NAND",
			"ONE",
	};
}

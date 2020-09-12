package lib;

public class HardcodedLibrary {
	
	public static String[] CMP = {
	/*0*/	"CMP",
	/*1*/	"ICMP",
	/*2*/	"ADD",
	/*3*/	"IADD",
	/*4*/	"AND",
	/*5*/	"OR",
	/*6*/	"ANDN",
	/*7*/	"XOR",
	/*8*/	"FCMP",	// Reserved
	/*9*/	"FUCMP",	// Reserved
	/*a*/	"FADD",	// Reserved
	/*b*/	"FUADD",	// Reserved
	/*c*/	"BT",	// Reserved
	/*d*/	"BTS",	// Reserved
	/*e*/	"BTR",	// Reserved
	/*f*/	"BTC",	// Reserved
	};
	
	public static String[] COND = {
	/*0*/	"E",
	/*1*/	"NE",
	/*2*/	"L",
	/*3*/	"NL",
	/*4*/	"LE",
	/*5*/	"NLE",
	/*6*/	"F",
	/*7*/	"NF",
	/*8*/	"S",
	/*9*/	"NS",
	/*a*/	"SE",
	/*b*/	"NSE",
	/*c*/	"P",	// Reserved
	/*d*/	"NP",	// Reserved
	/*e*/	"M",	// Reserved
	/*f*/	"NM",	// Reserved
	};
	
	public static String[][] LIB_OP = {
			{
	/*00*/	"ADD",
	/*01*/	"IADD",
	/*02*/	"ADDC",
	/*03*/	"IADDC",
	/*04*/	"SUBB",
	/*05*/	"ISUBB",
	/*06*/	"SUB",
	/*07*/	"ISUB",
	/*08*/	"SUBBR",
	/*09*/	"ISUBBR",
	/*0a*/	"SUBR",
	/*0b*/	"ISUBR",
	/*0c*/	"ADDNC",
	/*0d*/	"IADDNC",
	/*0e*/	"ADDN",
	/*0f*/	"IADDN",
			},{
	/*10*/	"ZERO",
	/*11*/	"AND",
	/*12*/	"NORN",
	/*13*/	"RIGHT",
	/*14*/	"ANDN",
	/*15*/	"LEFT",
	/*16*/	"XOR",
	/*17*/	"OR",
	/*18*/	"NOR",
	/*19*/	"NXOR",
	/*1a*/	"NLEFT",
	/*1b*/	"ORN",
	/*1c*/	"NRIGHT",
	/*1d*/	"NANDN",
	/*1e*/	"NAND",
	/*1f*/	"ONE",
			},{
	/*20*/	"SHL",
	/*21*/	"SHR",
	/*22*/	"SFL",
	/*23*/	"SAR",
	/*24*/	"ROL",
	/*25*/	"ROR",
	/*26*/	"SRL",
	/*27*/	"SRR",
	/*28*/	"SHLR",
	/*29*/	"SHRR",
	/*2a*/	"SFLR",
	/*2b*/	"SARR",
	/*2c*/	"ROLR",
	/*2d*/	"RORR",
	/*2e*/	"SRLR",
	/*2f*/	"SRRR",
			},{
	/*30*/	"MUL",
	/*31*/	"IMUL",
	/*32*/	"DIV",
	/*33*/	"IDIV",
	/*34*/	"ANDMUL",	// Reserved
	/*35*/	"ORMUL",	// Reserved
	/*36*/	"MOD",
	/*37*/	"IMOD",
	/*38*/	"MULD",
	/*39*/	"IMULD",
	/*3a*/	"DIVD",
	/*3b*/	"IDIVD",
	/*3c*/	"ANDMULD",	// Reserved
	/*3d*/	"ORMULD",	// Reserved
	/*3e*/	"MODD",
	/*3f*/	"IMODD",
			},{
	/*4*/
			},{
	/*5*/
			},{
	/*6*/
			},{
	/*7*/
			},{
	/*8*/
			},{
	/*9*/
			},{
	/*a*/
			},{
	/*b*/
			},{
	/*c*/
			},{
	/*d*/
			"","","","",
	/*d4*/	"RAND",	// Reserved
			},{
	/*e*/
			},{
	/*f*/
			}
	};
}
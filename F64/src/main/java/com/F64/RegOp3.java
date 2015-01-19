package com.F64;

public enum RegOp3 {
	MIN("signed minimum. dest = min(src1, src2)"),
	MAX("signed maximum. dest = max(src1, src2)"),
	ADD("Add. dest = src1 + src2. If either src1 or src2 is register S then a nip operation is appended"),
	ADDI("Add immediate (src2). dest = src1 + src2"),
	ADDC("Add with carry. C,dest = src1 + src2 + C. If either src1 or src2 is register S then a nip operation is appended"),
	SUB("Subtract. dest = src1 - src2. If either src1 or src2 is register S then a nip operation is appended"),
	SUBI("Subtract immediate (src2). dest = src1 - src2"),
	SUBC("Subtract with carry. C,dest = src1 + ~src2 + C. If either src1 or src2 is register S then a nip operation is appended"),
	AND("Bitwise and. dest = src1 & src2. If either src1 or src2 is register S then a nip operation is appended"),
	OR("Bitwise or. dest = src1 | src2. If either src1 or src2 is register S then a nip operation is appended"),
	XOR("Bitwise exclusive or. dest = src1 ^ src2. If either src1 or src2 is register S then a nip operation is appended"),
	XORN("Bitwise exclusive or with inverted second operand. dest = src1 ^ ~src2. If either src1 or src2 is register S then a nip operation is appended"),
	ASL("Arithmetic shift left. dest = src1 << src2. If src2 < 0 then dest = src1. If src2 >= 64 then dest = 0(src1>=0) or MIN_INT(src1<0). If either src1 or src2 is register S then a nip operation is appended"),
	ASR("Arithmetic shift right. dest = src1 >> src2. If src2 < 0 then dest = src1. If src2 >= 64 then dest = 0(src1>=0) or -1(src1<0). If either src1 or src2 is register S then a nip operation is appended"),
	LSL("Logical shift left. dest = src1 <<< src2. If src2 < 0 then dest = src1. If src2 >= 64 then dest = 0. If either src1 or src2 is register S then a nip operation is appended"),
	LSR("Logical shift right. dest = src1 >>> src2. If src2 < 0 then dest = src1. If src2 >= 64 then dest = 0. If either src1 or src2 is register S then a nip operation is appended"),
	ROL("Rotate left. dest = src1 ^<< src2. If src2 < 0 then dest = src1. If either src1 or src2 is register S then a nip operation is appended"),
	ROR("Rotate right. dest = src1 >>^ src2. If src2 < 0 then dest = src1. If either src1 or src2 is register S then a nip operation is appended"),
	RCL("Rotate left with carry. dest = src1 ^.<< src2. If src2 < 0 then dest = src1. If either src1 or src2 is register S then a nip operation is appended"),
	RCR("Rotate right with carry. dest = src1 >>.^ src2. If src2 < 0 then dest = src1. If either src1 or src2 is register S then a nip operation is appended"),
	ASLI("Arithmetic shift left immediate (src2). dest = src1 << src2"),
	ASRI("Arithmetic shift right immediate (src2). dest = src1 >> src2"),
	LSLI("Logical shift left immediate (src2). dest = src1 <<< src2"),
	LSRI("Logical shift right immediate (src2). dest = src1 >>> src2"),
	ROLI("Rotate left immediate (src2). dest = src1 ^<< src2"),
	RORI("Rotate right immediate with carry (src2). dest = src1 >>^ src2"),
	RCLI("Rotate left immediate with carry (src2). dest = src1 ^.<< src2"),
	RCRI("Rotate right immediate with carry (src2). dest = src1 >>.^ src2"),
	BYTECOUNT("Count bytes in a cell. dest = countbytes(src1, src2)"),
	MUL2ADD("*2 and add. dest = src1*2 + src2. If either src1 or src2 is register S then a nip operation is appended"),
	DIV2SUB("/2 and subtract. dest = src1/2 - src2. If either src1 or src2 is register S then a nip operation is appended");


	private String tooltip;

	private RegOp3(String tooltip)
	{
		this.tooltip = tooltip;
	}
	
	public String getTooltip() {return tooltip;}

}
package com.F64;

public enum Ext1 {
	RDROP(2,		"rdrop",	"drop R"),
	RDUP(2,			"rdup",		"dup R"),
	QDUP(2,			"qdup",		"dup if T != 0"),
	CONT(2,			"cont",		"move R to I, pop return stack and jump to slot 0"),
	EXECUTE(2,		"execute",	"Execute word. Move T to I"),
	EXITI(3,		"exiti",	"return from interrupt (interrupt # in next slot)"),
	RCOL(-1,		"rcol",		"Local colon. Continuation address in remaining slots. ( - xt )"),
	LCOL(2,			"lcol",		"Local colon. Continuation address in next cell. ( - xt )"),
	LJMP(2,			"ljmp",		"Long jump. Address in next cell."),
//	RNEXT(-1,		"rnext",	"remaining next"),
	LNEXT(2,		"lnext",	"long next"),
	DO(2,			"do",		"do ( start limit - )"),
	QDO(2,			"?do",		"conditional do ( start limit - flag )"),
	QFOR(2,			"?for",		"conditional for ( n - flag )"),
	LOOP(2,			"loop",		"loop ( - flag ). If flag = 0 then jump"),
	PLOOP(2,		"+loop",	"+loop ( n - flag ). If flag = 0 then jump"),
	MIN(2,			"min",		"minimum"),
	MAX(2,			"max",		"maximum"),
	ADDC(2,			"+y",		"add with carry"),
	SUBC(2,			"-y",		"subtract with carry"),
	ROL(2,			"^<<",		"rotate S left by T and copy most significant bit into carry"),
	ROR(2,			">>^",		"rotate S right by T and copy least significant bit into carry"),
	RCL(2,			"^.<<",		"rotate S left by T with carry"),
	RCR(2,			">>.^",		"rotate S right by T with carry"),
	TUCK(2,			"tuck",		"tuck ( n1 n2 - n2 n1 n2 )"),
	UNDER(2,		"under",	"under ( n1 n2 - n1 n1 n2 )"),
	MULS(2,			"*s",		"multiply step"),
	DIVS(2,			"/s",		"divide step"),
	MDP(2,			"<*",		"multiply & divide prepare"),
	MULF(2,			"*>",		"multiply finished"),
	DIVMODF(2,		"/mod>",	"division & modulo finished"),
	CARRYQ(2,		"y?",		"return true if carry flag is set ( - flag )"),
	EQ0Q(2,			"=0?",		"T == 0 ? ( n - flag )"),
	NE0Q(2,			"!=0?",		"T != 0 ? ( n - flag )"),
	GT0Q(2,			">0?",		"T > 0 ? ( n - flag )"),
	GE0Q(2,			">=0?",		"T >= 0 ? ( n - flag )"),
	LT0Q(2,			"<0?",		"T < 0 ? ( n - flag )"),
	LE0Q(2,			"<=0?",		"T <= 0 ? ( n - flag )"),
	FETCHRES(2,		"<@",		"fetch and reserve memory location ( a - n )"),
	STORECOND(2,	"!>?",		"store conditionally a reserved memory location ( n a - flag"),
	SBITS(2,		"bset",		"set bit T in S and put old bit in carry. Put result in T and pop stack into S"),
	CBITS(2,		"bclr",		"set bit T in S and put old bit in carry. Put result in T and pop stack into S"),
	TBITS(2,		"btgl",		"set bit T in S and put old bit in carry. Put result in T and pop stack into S"),
	RBITS(2,		"b@",		"read bit T from S into carry. Put result in T and pop stack into S"),
	WBITS(2,		"b!",		"write bit T from carry into S and put old bit in carry. Put result in T and pop stack into S"),
	ENTERM(2,		"enterm",	"push the MT to the return stack and load the MT with the method table from SELF"),
	LCALLM(2,		"lcallm",	"long method call. Call a method on SELF (T contains the method #)"),
	LJMPM(2,		"ljmpm",	"long method jump. Jump to a method on SELF (T contains the method #)"),
	TASKFETCH(2,	"task@",	"fetch current task ( - n )"),
	TASKSTORE(2,	"task!",	"reset a task and store startup execution token ( xt n - )"),
	TASKSWITCH(2,	"->task",	"switch to another task ( n - )"),
	;

	private int size;
	private String display;
	private String tooltip;

	private Ext1(int size, String display, String tooltip)
	{
		this.size = size;
		this.display = display;
		this.tooltip = tooltip;
	}
	
	public int size() {return size;}
	public String getTooltip() {return tooltip;}
	public String getDisplay() {return display;}
}

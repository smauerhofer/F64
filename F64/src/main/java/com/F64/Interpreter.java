package com.F64;

public class Interpreter {
	private long[]	register;
	private long[]	memory;
	private long[]	stack;
	private long[]	return_stack;
	private int		slot;
	public static final int BITS_PER_CELL = 64;
	public static final int SLOT_BITS = 6;
	public static final int SLOT_SIZE = 1 << SLOT_BITS;
	public static final int SLOT_MASK = SLOT_SIZE - 1;
	public static final int FINAL_SLOT_BITS = 4;
	public static final int FINAL_SLOT_SIZE = 1 << FINAL_SLOT_BITS;
	public static final int FINAL_SLOT_MASK = FINAL_SLOT_SIZE - 1;
	public static final int FINAL_SLOT = 10;
	public static final int NO_OF_SLOTS = FINAL_SLOT+1;
	
	
	public Interpreter()
	{
		this.register = new long[64];
		this.register[Register.Z.ordinal()] = 0;
	}

	public long getRegister(int reg) {return register[reg];}
	public long getRegister(Register reg) {return register[reg.ordinal()];}
	public void setRegister(int reg, long value) {if (reg > 0) {register[reg] = value;}}
	public void setRegister(Register reg, long value) {if (reg != Register.Z) {register[reg.ordinal()] = value;}}
	
	
	public void pushStack(long value)
	{
		if (this.register[Register.SP.ordinal()] == this.register[Register.S0.ordinal()]) {
			this.register[Register.SP.ordinal()] = this.register[Register.SL.ordinal()];
		}
		else {
			--this.register[Register.SP.ordinal()];
		}
		this.stack[(int)this.register[Register.SP.ordinal()]] = value;
	}
	
	public long popStack()
	{
		long res = this.stack[(int)this.register[Register.SP.ordinal()]];
		if (this.register[Register.SP.ordinal()] == this.register[Register.SL.ordinal()]) {
			this.register[Register.SP.ordinal()] = this.register[Register.S0.ordinal()];
		}
		else {
			++this.register[Register.SP.ordinal()];
		}
		return res;
	}

	public void pushReturnStack(long value)
	{
		if (this.register[Register.RP.ordinal()] == this.register[Register.R0.ordinal()]) {
			this.register[Register.RP.ordinal()] = this.register[Register.RL.ordinal()];
		}
		else {
			--this.register[Register.RP.ordinal()];
		}
		this.return_stack[(int)this.register[Register.RP.ordinal()]] = value;
	}
	
	public long popReturnStack()
	{
		long res = this.return_stack[(int)this.register[Register.RP.ordinal()]];
		if (this.register[Register.RP.ordinal()] == this.register[Register.RL.ordinal()]) {
			this.register[Register.RP.ordinal()] = this.register[Register.R0.ordinal()];
		}
		else {
			++this.register[Register.RP.ordinal()];
		}
		return res;
	}

	
	public void storeRegister(Register reg, long value)
	{
		if (reg != Register.Z) {
			this.register[reg.ordinal()] = value;
		}
	}

	/**
	 * Increment register. Do nothing if the register points not into memory
	 */
	public void incPointer(int reg)
	{
		if (this.register[reg] >= 0) {
			++this.register[reg];
		}
	}


	/**
	 * Increment register. Do nothing if the register points not into memory
	 */
	public void decPointer(int reg)
	{
		if (this.register[reg] >= 0) {
			--this.register[reg];
		}
	}

	/**
	 * Advance P to next location
	 */
	public void nextP()
	{
		incPointer(Register.P.ordinal());
	}

	public long fetchRegister(Register reg)
	{
		return this.register[reg.ordinal()];
	}

	public long remainingSlots()
	{
		long res = this.register[Register.I.ordinal()];
		long mask = -1;
		res &= mask >>> (this.slot*SLOT_BITS);
		this.slot = NO_OF_SLOTS;
		return res;
	}

	public void jumpRemainigSlots()
	{
		// replace to lowest bits of P with the bits in the remaining slots
		long mask = -1;
		mask = mask >>> (this.slot*SLOT_BITS);
		this.register[Register.P.ordinal()] &= ~mask;
		this.register[Register.P.ordinal()] |= this.register[Register.P.ordinal()] & mask;
		this.slot = NO_OF_SLOTS;
	}


	public int nextSlot() throws Exception
	{
		int res = 0;
		if (slot < FINAL_SLOT) {
			res = ((int)(this.register[Register.I.ordinal()] >> (BITS_PER_CELL-(slot*SLOT_BITS)))) & SLOT_MASK;
		}
		else if (slot == FINAL_SLOT) {
			res = ((int)(this.register[Register.I.ordinal()])) & FINAL_SLOT_MASK;
		
		}
		else {
			throw new Exception();
		}
		++slot;
		return res;
	}

	public long replaceNextSlot(long base) throws Exception
	{
		long res = (-1 << SLOT_BITS) & base;
		return res | nextSlot();
	}

	public boolean conditionalJump(int condition)
	{
		switch ((condition >> 4) & 3) {
		case 0: break;	// always
		case 1: if (this.register[Register.T.ordinal()] != 0) {return false;}	// if T == 0
		case 2: if (this.register[Register.T.ordinal()] < 0) {return false;}	// if T >= 0
		case 3: if (!this.getFlag(Flag.Carry)) {return false;}	// if Flags.Carry == 0
		}
		switch (condition & 0xf) {
		case 0: this.slot = 0; break;
		case 1: this.slot = 1; break;
		case 2: this.slot = 2; break;
		case 3: this.slot = 3; break;
		case 4: this.slot = 4; break;
		case 5: this.slot = 5; break;
		case 6: this.slot = 6; break;
		case 7: this.slot = 7; break;
		case 8: this.slot = 8; break;
		case 9: this.slot = 9; break;
		case 10: this.slot = 10; break;
		case 11: nextP(); this.slot = NO_OF_SLOTS; break;
		case 12: nextP(); nextP(); this.slot = NO_OF_SLOTS; break;
		case 13: nextP(); nextP(); nextP(); this.slot = NO_OF_SLOTS; break;
		case 14: nextP(); nextP(); nextP(); nextP(); this.slot = NO_OF_SLOTS; break;
		case 15: this.jumpRemainigSlots(); break;
		}
		return true;
	}
	
	public long drop() throws Exception
	{
		long res = this.register[Register.T.ordinal()];
		this.register[Register.T.ordinal()] = this.register[Register.S.ordinal()];
		this.register[Register.S.ordinal()] = this.popStack();
		return res;
	}

	public void doDrop() throws Exception
	{
		this.register[Register.T.ordinal()] = this.register[Register.S.ordinal()];
		this.register[Register.S.ordinal()] = this.popStack();		
	}

	public void doDup() throws Exception
	{
		this.pushStack(this.register[Register.S.ordinal()]);
		this.register[Register.S.ordinal()] = this.register[Register.T.ordinal()];		
	}

	public void doNext() throws Exception
	{
		if (this.register[Register.R.ordinal()] == 0) {
			this.register[Register.R.ordinal()] = this.popReturnStack();
		}
		else {
			if (conditionalJump(this.nextSlot())) {
				--this.register[Register.R.ordinal()];
			}
		}
	}
	
	public void doCallMethod(long index) throws Exception
	{
		// push P on return stack
		this.pushReturnStack(this.register[Register.R.ordinal()]);
		this.register[Register.R.ordinal()] = this.register[Register.P.ordinal()];
		// check index is in range of method table
		if (index >= this.memory[(int)this.register[Register.MT.ordinal()] + MethodTable.OFFSET_TO_SIZE]) {
			throw new Exception();
		}
//		// check if MT must be loaded
//		if (this.register[Register.MT.ordinal()] == 0) {
//			// load the method table register from SELF
//			this.register[Register.MT.ordinal()] = this.memory[(int)this.register[Register.SELF.ordinal()]];
//		}
		// load address of method
		this.register[Register.P.ordinal()] = this.memory[(int)(this.register[Register.MT.ordinal()] + index) + MethodTable.OFFSET_TO_METHOD_ARRAY];
		this.slot = NO_OF_SLOTS;
	}

	
	public void doJumpMethod(long index) throws Exception
	{
		// check index is in range of method table
		if (index >= this.memory[(int)this.register[Register.MT.ordinal()] + MethodTable.OFFSET_TO_SIZE]) {
			throw new Exception();
		}
//		// check if MT must be loaded
//		if (this.register[Register.MT.ordinal()] == 0) {
//			// load the method table register from SELF
//			this.register[Register.MT.ordinal()] = this.memory[(int)this.register[Register.SELF.ordinal()]];
//		}
		// load address of method
		this.register[Register.P.ordinal()] = this.memory[(int)(this.register[Register.MT.ordinal()] + index) + MethodTable.OFFSET_TO_METHOD_ARRAY];
		this.slot = NO_OF_SLOTS;
	}

	public void doSwap() throws Exception
	{
		int src = nextSlot();
		int dst = nextSlot();
		long tmp = this.register[dst];
		this.register[dst] = this.register[src];
		this.register[src] = tmp;
	}

	public void doMove() throws Exception
	{
		int src = nextSlot();
		int dst = nextSlot();
		this.register[dst] = this.register[src];
	}

	public void doMoveStack() throws Exception
	{
		int src = nextSlot();
		int dst = nextSlot();
		long value = this.register[src];
		if (dst == Register.R.ordinal()) {
			this.pushReturnStack(this.register[Register.R.ordinal()]);			
		}
		else if (dst == Register.S.ordinal()) {
			this.pushStack(this.register[Register.S.ordinal()]);
		}
		else if ((dst == Register.T.ordinal()) && (src == Register.S.ordinal())) {
			this.doDup();
		}
		this.register[dst] = value;
		if (src == Register.R.ordinal()) {
			this.register[Register.R.ordinal()] = this.popReturnStack();
		}
		else if (src == Register.S.ordinal()) {
			this.register[Register.S.ordinal()] = this.popStack();
		}
		else if ((src == Register.T.ordinal()) && (dst == Register.S.ordinal())) {
			this.doDrop();
		}
	}

	public void doRFetch(int reg) throws Exception
	{
		long tmp = this.register[reg];
		this.pushStack(this.register[Register.S.ordinal()]);
		this.register[Register.S.ordinal()] = this.register[Register.T.ordinal()];
		this.register[Register.T.ordinal()] = tmp;
	}

	public void doRStore(int reg) throws Exception
	{
		this.register[reg] = this.register[Register.T.ordinal()];
		this.register[Register.T.ordinal()] = this.register[Register.S.ordinal()];
		this.register[Register.S.ordinal()] = this.popStack();
	}

	public void doFetchR(int reg) throws Exception
	{
		this.doDup();
		this.register[Register.T.ordinal()] = this.memory[(int)this.register[reg]];
	}
	

	public void doStoreR(int reg) throws Exception
	{
		this.memory[(int)this.register[reg]] = this.register[Register.T.ordinal()];
		this.doDrop();
	}

	public void doFetchPInc() throws Exception
	{
		int reg = Register.P.ordinal();
		doDup();
		this.register[Register.T.ordinal()] = this.memory[(int)this.register[reg]];
		incPointer(Register.P.ordinal());
	}
	

	public void doStorePInc() throws Exception
	{
		int reg = Register.P.ordinal();
		this.memory[(int)this.register[reg]] = this.register[Register.T.ordinal()];
		doDrop();
		incPointer(Register.P.ordinal());
	}

	public void doFetchAInc() throws Exception
	{
		int reg = Register.A.ordinal();
		doDup();
		this.register[Register.T.ordinal()] = this.memory[(int)this.register[reg]];
		this.incPointer(reg);
	}
	

	public void doStoreBInc() throws Exception
	{
		int reg = Register.B.ordinal();
		this.memory[(int)this.register[reg]] = this.register[Register.T.ordinal()];
		doDrop();
		this.incPointer(reg);
	}

	public void doExit() throws Exception
	{
		this.register[Register.P.ordinal()] = this.register[Register.R.ordinal()];
		this.register[Register.R.ordinal()] = this.popReturnStack();
		this.slot = NO_OF_SLOTS;
	}

	public void doUNext() throws Exception
	{
		if (this.register[Register.R.ordinal()] == 0) {
			this.register[Register.R.ordinal()] = this.popReturnStack();
		}
		else {
			--this.register[Register.R.ordinal()];
			this.slot = 0;
		}
	}

	public void doCont() throws Exception
	{
		this.register[Register.I.ordinal()] = this.register[Register.T.ordinal()];
		this.doDrop();
//		this.register[Register.R.ordinal()] = this.popReturnStack();
		this.slot = 0;		
	}

	public void doAdd() throws Exception
	{
		this.register[Register.T.ordinal()] += this.register[Register.S.ordinal()];
		this.register[Register.S.ordinal()] = this.popStack();		
	}

	
	public void doSubtract() throws Exception
	{
		this.register[Register.T.ordinal()] = this.register[Register.S.ordinal()] - this.register[Register.T.ordinal()];
		this.register[Register.S.ordinal()] = this.popStack();		
	}

	public void doAnd() throws Exception
	{
		this.register[Register.T.ordinal()] &= this.register[Register.S.ordinal()];
		this.register[Register.S.ordinal()] = this.popStack();		
	}

	public void doOr() throws Exception
	{
		this.register[Register.T.ordinal()] |= this.register[Register.S.ordinal()];
		this.register[Register.S.ordinal()] = this.popStack();
	}

	public void doXor() throws Exception
	{
		this.register[Register.T.ordinal()] ^= this.register[Register.S.ordinal()];
		this.register[Register.S.ordinal()] = this.popStack();
	}

	public void doNot() throws Exception
	{
		this.register[Register.T.ordinal()] = ~this.register[Register.T.ordinal()];
	}

	public void doMul2() throws Exception
	{
		this.register[Register.T.ordinal()] <<= 1;
	}

	public void doDiv2() throws Exception
	{
		this.register[Register.T.ordinal()] >>= 1;
	}

	public void doOver() throws Exception
	{
		this.pushStack(this.register[Register.S.ordinal()]);
		long tmp = this.register[Register.S.ordinal()];
		this.register[Register.S.ordinal()] = this.register[Register.T.ordinal()];
		this.register[Register.T.ordinal()] = tmp;
	}

	public void doNip() throws Exception
	{
		this.register[Register.S.ordinal()] = this.popStack();
	}

	public void doLit() throws Exception
	{
		this.doDup();
		this.register[Register.T.ordinal()] = this.nextSlot();
	}

	public void doBitLit() throws Exception
	{
		this.doDup();
		long tmp = 1;
		this.register[Register.T.ordinal()] = tmp << this.nextSlot();
	}

	public void doExtendLiteral() throws Exception
	{
		this.register[Register.T.ordinal()] <<= SLOT_BITS;
		this.register[Register.T.ordinal()] |= this.nextSlot();
	}

	public void doShortJump() throws Exception
	{
		this.register[Register.P.ordinal()] = this.replaceNextSlot(this.register[Register.P.ordinal()]);
		this.slot = NO_OF_SLOTS;		
	}

	public void doCall() throws Exception
	{
		// push P on return stack
		this.pushReturnStack(this.register[Register.R.ordinal()]);
		this.register[Register.R.ordinal()] = this.register[Register.P.ordinal()];
		// load new P
		this.jumpRemainigSlots();		
	}

	public void doPush() throws Exception
	{
		this.pushReturnStack(this.register[Register.R.ordinal()]);
		this.register[Register.R.ordinal()] = this.register[Register.T.ordinal()];
		this.doDrop();
	}

	public void doPop() throws Exception
	{
		this.doDup();
		this.register[Register.T.ordinal()] = this.register[Register.P.ordinal()];
		this.register[Register.P.ordinal()] = this.popReturnStack();
	}

	public void doLoadSelf() throws Exception
	{
		this.pushReturnStack(this.register[Register.R.ordinal()]);
		this.register[Register.R.ordinal()] = this.register[Register.SELF.ordinal()];
		this.register[Register.SELF.ordinal()] = this.register[Register.T.ordinal()];
		this.doDrop();
	}

	public void doLoadMT() throws Exception
	{
		this.register[Register.MT.ordinal()] = this.memory[(int)this.register[Register.SELF.ordinal()]];
	}

	public void microstep() throws Exception
	{
		switch (ISA.values()[this.nextSlot()]) {
		case NOP:		break;
		case EXIT:		this.doExit(); break;
		case UNEXT:		this.doUNext(); break;
		case CONT:		this.doCont(); break;
		case UJMP0:		this.slot = 0; break;
		case UJMP1:		this.slot = 1; break;
		case UJMP2:		this.slot = 2; break;
		case UJMP3:		this.slot = 3; break;
		case UJMP4:		this.slot = 4; break;
		case UJMP5:		this.slot = 5; break;
		case AND:		this.doAnd(); break;
		case XOR:		this.doXor(); break;
		case DUP:		this.doDup(); break;
		case DROP:		this.doDrop(); break;
		case OVER:		this.doOver(); break;
		case NIP:		this.doNip(); break;
		case LIT:		this.doLit(); break;
		case BLIT:		this.doBitLit(); break;
		case EXT:		this.doExtendLiteral(); break;
		case NEXT:		this.doNext(); break;
		case BRANCH:	this.conditionalJump(this.nextSlot()); break;
		case CALLM:		this.doCallMethod(this.remainingSlots()); break;
		case JMPM:		this.doJumpMethod(this.remainingSlots()); break;
		case SJMP:		this.doShortJump(); break;
		case CALL:		this.doCall(); break;
		case JMP:		this.jumpRemainigSlots(); break;
		case USKIP:		this.slot = NO_OF_SLOTS; break;
		case UJMP6:		this.slot = 6; break;
		case UJMP7:		this.slot = 7; break;
		case UJMP8:		this.slot = 8; break;
		case UJMP9:		this.slot = 9; break;
		case UJMP10:	this.slot = 10; break;
		case SWAP:		this.doSwap(); break;
		case SWAP0:		this.doSwap(); this.slot = 0; break;
		case MOV:		this.doMove(); break;
		case MOVS:		this.doMoveStack(); break;
		case LOADSELF:	this.doLoadSelf(); break;
		case LOADMT:	this.doLoadMT(); break;
		case RFETCH:	this.doRFetch(this.nextSlot()); break;
		case RSTORE:	this.doRStore(this.nextSlot()); break;
		case FETCHR:	this.doFetchR(this.nextSlot()); break;
		case STORER:	this.doStoreR(this.nextSlot()); break;
		case RINC:		++this.register[this.nextSlot()]; break;
		case RDEC:		--this.register[this.nextSlot()]; break;
		case RPINC:		this.incPointer(this.nextSlot()); break;
		case RPDEC:		this.decPointer(this.nextSlot()); break;
		case FETCHPINC:	this.doFetchPInc(); break;
		case STOREPINC:	this.doStorePInc(); break;
		case ADD:		this.doAdd(); break;
		case SUB:		this.doSubtract(); break;
		case OR:		this.doOr(); break;
		case NOT:		this.doNot(); break;
		case MUL2:		this.doMul2(); break;
		case DIV2:		this.doDiv2(); break;
		case PUSH:		this.doPush(); break;
		case POP:		this.doPop(); break;
		case EXT1:		this.doExt1(); break;
		case EXT2:		this.doExt2(); break;
		case EXT3:		this.doExt3(); break;
		case EXT4:		this.doExt4(); break;

		}
	}

	public boolean getFlag(Flag fl)
	{
		return (this.register[Register.FLAGS.ordinal()] & fl.getMask()) != 0;
	}
	
	public void setFlag(Flag fl, boolean value)
	{
		if (value) {
			this.register[Register.FLAGS.ordinal()] |= fl.getMask();
		}
		else {
			this.register[Register.FLAGS.ordinal()] &= ~fl.getMask();			
		}
	}
	
	public void doAddWithCarry() throws Exception
	{
		boolean carry = getFlag(Flag.Carry);
		long a = this.register[Register.S.ordinal()];
		long b = this.register[Register.T.ordinal()];
		long c = a+b;
		boolean overflow = (a > 0 && b > 0 && c < 0) || (a < 0 && b < 0 && c > 0);
		if (carry) {
			if (++c == 0) {overflow = true;}
		}
		this.register[Register.T.ordinal()] = c;
		this.register[Register.S.ordinal()] = this.popStack();
		setFlag(Flag.Carry, overflow);
	}

	public void doSubtractWithCarry() throws Exception
	{
		boolean carry = getFlag(Flag.Carry);
		long a = this.register[Register.S.ordinal()];
		long b = ~this.register[Register.T.ordinal()];
		long c = a+b;
		boolean overflow = (a > 0 && b > 0 && c < 0) || (a < 0 && b < 0 && c > 0);
		if (carry) {
			if (++c == 0) {overflow = true;}
		}
		this.register[Register.T.ordinal()] = c;
		this.register[Register.S.ordinal()] = this.popStack();
		setFlag(Flag.Carry, overflow);
	}

	public void doMultiplyStep() throws Exception
	{
		//TODO
		throw new Exception();
	}

	public void doDivideStep() throws Exception
	{
		//TODO
		throw new Exception();
	}

	public void doRotateLeft() throws Exception
	{
		long value = this.register[Register.T.ordinal()];
		if (value < 0) {
			this.register[Register.T.ordinal()] = (value << 1) | 1;
			setFlag(Flag.Carry, true);
		}
		else {
			this.register[Register.T.ordinal()] = (value << 1);
			setFlag(Flag.Carry, false);
		}
	}

	public void doRotateRight() throws Exception
	{
		long value = this.register[Register.T.ordinal()];
		if ((value & 1) != 0) {
			long mask = 1;
			mask <<= 63;
			this.register[Register.T.ordinal()] = (value >> 1) | mask;
			setFlag(Flag.Carry, true);
		}
		else {
			this.register[Register.T.ordinal()] = (value >>> 1);
			setFlag(Flag.Carry, false);
		}
	}

	public void doRotateLeftWithCarry() throws Exception
	{
		long value = this.register[Register.T.ordinal()];
		if (getFlag(Flag.Carry)) {
			this.register[Register.T.ordinal()] = (value << 1) | 1;
		}
		else {
			this.register[Register.T.ordinal()] = (value << 1);
		}
		setFlag(Flag.Carry, (value < 0));
	}

	public void doRotateRightWithCarry() throws Exception
	{
		long value = this.register[Register.T.ordinal()];
		long mask = 1;
		mask <<= 63;
		if (getFlag(Flag.Carry)) {
			this.register[Register.T.ordinal()] = (value >> 1) | mask;
		}
		else {
			this.register[Register.T.ordinal()] = (value >>> 1);
		}
		setFlag(Flag.Carry, (value & mask) != 0);
	}

	public void doSetBit(int bit) throws Exception
	{
		long mask = 1;
		mask <<= bit & 0x3f;
		long value = this.register[Register.T.ordinal()];
		this.register[Register.T.ordinal()] = value | mask;
		setFlag(Flag.Carry, (value & mask) != 0);
	}

	public void doClearBit(int bit) throws Exception
	{
		long mask = 1;
		mask <<= this.nextSlot();
		long value = this.register[Register.T.ordinal()];
		this.register[Register.T.ordinal()] = value & ~mask;
		setFlag(Flag.Carry, (value & mask) != 0);
	}

	public void doToggleBit(int bit) throws Exception
	{
		long mask = 1;
		mask <<= this.nextSlot();
		long value = this.register[Register.T.ordinal()];
		this.register[Register.T.ordinal()] = value ^ mask;
		setFlag(Flag.Carry, (value & mask) != 0);
	}

	public void doReadBit(int bit) throws Exception
	{
		long mask = 1;
		mask <<= this.nextSlot();
		long value = this.register[Register.T.ordinal()];
		setFlag(Flag.Carry, (value & mask) != 0);
	}

	public void doWriteBit(int bit) throws Exception
	{
		if (getFlag(Flag.Carry)) {
			this.doSetBit(bit);
		}
		else {
			this.doClearBit(bit);
		}
	}

	public void doEnterM() throws Exception
	{
		this.pushReturnStack(this.register[Register.R.ordinal()]);
		this.register[Register.R.ordinal()] = this.register[Register.MT.ordinal()];
		this.register[Register.MT.ordinal()] = this.memory[(int)this.register[Register.SELF.ordinal()]];
	}


	public void doExt1() throws Exception
	{
		switch (Ext1.values()[this.nextSlot()]) {
		case NOP:		break;
		case ADDC:		this.doAddWithCarry(); break;
		case SUBC:		this.doSubtractWithCarry(); break;
		case MULS:		this.doMultiplyStep(); break;
		case DIVS:		this.doDivideStep(); break;
		case ROL:		this.doRotateLeft(); break;
		case ROR:		this.doRotateRight(); break;
		case ROLC:		this.doRotateLeftWithCarry(); break;
		case RORC:		this.doRotateRightWithCarry(); break;
		case SBIT:		this.doSetBit(this.nextSlot()); break;
		case CBIT:		this.doClearBit(this.nextSlot()); break;
		case TBIT:		this.doToggleBit(this.nextSlot()); break;
		case RBIT:		this.doReadBit(this.nextSlot()); break;
		case WBIT:		this.doWriteBit(this.nextSlot()); break;
		case SBITS:		this.doSetBit((int)this.drop()); break;
		case CBITS:		this.doClearBit((int)this.drop()); break;
		case TBITS:		this.doToggleBit((int)this.drop()); break;
		case RBITS:		this.doReadBit((int)this.drop()); break;
		case WBITS:		this.doWriteBit((int)this.drop()); break;
		case ENTERM:	this.doEnterM(); break;
		case LCALLM:	this.doCallMethod(this.drop()); break;
		case LJMPM:		this.doJumpMethod(this.drop()); break;
		case FETCHAINC:	this.doFetchAInc(); break;
		case STOREBINC:	this.doStoreBInc(); break;
		}
	}

	public void doExt2() throws Exception
	{
	}

	public void doExt3() throws Exception
	{
	}

	public void doExt4() throws Exception
	{
	}

	
	
	public void step() throws Exception
	{
		// load new instruction cell
		this.register[Register.I.ordinal()] = this.memory[(int)this.register[Register.P.ordinal()]];
		// increment P
		incPointer(Register.P.ordinal());
		// begin with first slot
		this.slot = 0;
		while (this.slot <= FINAL_SLOT) {
			microstep();
		}
	}

	public void storeMemory(long adr, long value)
	{
		this.memory[(int)adr] = value;
	}

	public long fetchMemory(long adr)
	{
		return this.memory[(int)adr];
	}
	
	public void run(long[] memory, long starting_address, int stack_size, int return_stack_size)
	{
		// initialize stack
		this.stack = new long[stack_size];
		this.storeRegister(Register.SP, 0);
		this.storeRegister(Register.S0, 0);
		this.storeRegister(Register.SL, stack_size-1);
		// initialize return stack
		this.return_stack = new long[return_stack_size];
		this.storeRegister(Register.RP, 0);
		this.storeRegister(Register.R0, 0);
		this.storeRegister(Register.RL, return_stack_size-1);
		// memory
		this.memory = memory;
		this.storeRegister(Register.P, starting_address);
		try {
			for (;;) {
				step();
			}
		}
		catch (Exception ex) {
		}
	}
	
	

}

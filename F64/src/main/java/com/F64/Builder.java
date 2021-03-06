package com.F64;

public class Builder {

	public static boolean fit(int slot, int slot0)
	{
		if (slot < 0) {return false;}
		if (slot == Processor.FIRST_SLOT) {return slot0 < Processor.FIRST_SLOT_SIZE;}
		return slot < Processor.NO_OF_SLOTS;
	}

	public static boolean fit(int slot, int slot0, int slot1)
	{
		if (!fit(slot, slot0)) {return false;}
		if (slot1 == 0) {return true;}
		return slot < Processor.NO_OF_SLOTS-1;
	}

	public static boolean fit(int slot, int slot0, int slot1, int slot2)
	{
		if (!fit(slot, slot0, slot1)) {return false;}
		if (slot2 == 0) {return true;}
		return slot < Processor.NO_OF_SLOTS-2;
	}

	public static boolean fit(int slot, int slot0, int slot1, int slot2, int slot3)
	{
		if (!fit(slot, slot0, slot1, slot2)) {return false;}
		if (slot3 == 0) {return true;}
		return slot < Processor.NO_OF_SLOTS-3;
	}

	public static boolean fit(int slot, int slot0, int slot1, int slot2, int slot3, int slot4)
	{
		if (!fit(slot, slot0, slot1, slot2, slot3)) {return false;}
		if (slot4 == 0) {return true;}
		return slot < Processor.NO_OF_SLOTS-4;
	}

	public static boolean fit(int slot, int slot0, int slot1, int slot2, int slot3, int slot4, int slot5)
	{
		if (!fit(slot, slot0, slot1, slot2, slot3, slot4)) {return false;}
		if (slot5 == 0) {return true;}
		return slot < Processor.NO_OF_SLOTS-5;
	}

	public static int getHighestDifferentBit1(long value1, long value2)
	{
		int diff = 0;
		long mask = -1;
		while ((value1 & mask) != (value2 & mask)) {
			++diff;
			mask <<= 1;
		}
		return diff;
	}
	
	public static int getRemainingBits(int slot)
	{
		return Processor.SLOT_SHIFT[slot];
	}
	
	public static long getAddressMask(int slot)
	{
		return -1 >>> (Processor.BIT_PER_CELL - Processor.SLOT_SHIFT[slot]);
	}

	public static boolean forwardJumpFitsIntoSlot(long P, long target)
	{
		return (target - P) <= Processor.SLOT_SIZE;		
	}

	public static boolean backJumpFitsIntoSlot(long P, long target)
	{
		return (P - target) <= Processor.SLOT_SIZE;		
	}

	public static boolean forwardBranchCanBeImplicit(int slot, int instructionCount)
	{
		return ((instructionCount+slot) < (Processor.NO_OF_SLOTS-2));
	}

	public static boolean backwardBranchCanBeImplicit(int instructionCount)
	{
		return instructionCount < (Processor.NO_OF_SLOTS-1);
	}

	public boolean forwardBranchIsImplicit(Condition cond, Scope block)
	{
		long cp = this.getCurrentPosition();
		this.add(ISA.BRANCH, cond.encode(Branch.SKIP));
		block.generate(this);
		return this.getCurrentP() == cp;
	}

	public boolean forwardBranchIsImplicit(ConditionalBranch br, Scope block)
	{
		long cp = this.getCurrentPosition();
		this.add(ISA.BRANCH, br.getCondition().encode(Branch.SKIP));
		br.set(this, -1);
		block.generate(this);
		return this.getCurrentP() == cp;
	}

	public void forwardBranchImplicit(Condition cond, Scope block)
	{
		long fixup = this.getCurrentPosition();
		int fixup_slot = this.getCurrentSlot()+1;
		this.add(ISA.BRANCH, Branch.SKIP.ordinal());
		block.generate(this);
		int target_slot = this.getCurrentSlot();
		if (target_slot < Processor.NO_OF_SLOTS) {
			com.F64.System s = this.getSystem();
			s.setMemory(fixup, Processor.writeSlot(s.getMemory(fixup), fixup_slot, cond.encode(target_slot)));
		}
	}

	public static boolean forwardBranchCanBeShort(long P, int slot, int instructionCount)
	{
		long target = P + instructionCount / Processor.NO_OF_SLOTS;
		long diff1 = Builder.getHighestDifferentBit1(target, P);
		return diff1 <= Processor.SLOT_BITS;
	}

	public boolean forwardBranchIsShort(ConditionalBranch pre, Scope block, ConditionalBranch fixup)
	{
//		pre.generateBranch(b, Branch.SHORT);
//		long P = b.getCurrentP();
//		block.generate(b);
//		if (fixup != null) {
//			fixup.generateBranch(b, Branch.SHORT);
//		}
//		return shortJumpFitsIntoSlot(P, b.getCurrentPosition());
		pre.generateBranch(this, Branch.FORWARD);
		long P = this.getCurrentP();
		block.generate(this);
		if (fixup != null) {
			fixup.generateBranch(this, Branch.FORWARD);
		}
		return forwardJumpFitsIntoSlot(P, this.getCurrentPosition());
	}

//	public static Location generateForwardBranchShort(Builder b, Condition cond)
//	{
//		b.add(ISA.BRANCH, cond.encode(Branch.SHORT), Processor.SLOT_MASK);
//		return new Location(b.getCurrentP(), b.getCurrentPosition(), b.getCurrentSlot()-1);
//	}
//
//	public static Location generateForwardBranchRem(Builder b, Condition cond)
//	{
//		b.add(ISA.BRANCH, cond.encode(Branch.REM));
//		Location res = new Location(b.getCurrentP(), b.getCurrentPosition(), b.getCurrentSlot());
//		b.flush();
//		return res;
//	}
//

	public long generateForwardBranchLong(Condition cond)
	{
		this.add(ISA.BRANCH, cond.encode(Branch.LONG));
		long res = this.getCurrentP();
		this.addAdditionalCell(0);
		return res;
	}

//	public static boolean forwardForwardBranch(Builder b, ConditionalBranch pre, Scope block, ConditionalBranch post)
//	{
//		pre.generateBranch(b);
//		b.flush();
//		long target1 = b.getCurrentPosition();
//		block.generate(b);
//		if (post != null) {
//			post.generateBranch(b);
//		}
//		b.flush();
//		long target2 = b.getCurrentPosition();
//
//		if (!pre.fixup(b, target1, 0)) {return false;}
//		if (post != null) {
//			if (!post.fixup(b, target2, 0)) {return false;}
//		}
//		return true;
//	}

	public boolean forwardBranchShort(ConditionalBranch pre, Scope block, ConditionalBranch post)
	{
		pre.generateBranch(this, Branch.FORWARD);
		this.flush();
		long target1 = this.getCurrentPosition();
		block.generate(this);
		if (post != null) {
			post.generateBranch(this, Branch.FORWARD);
		}
		this.flush();
		long target2 = this.getCurrentPosition();

		if (!pre.fixup(this, target1, 0)) {return false;}
		if (post != null) {
			if (!post.fixup(this, target2, 0)) {return false;}
		}
		return true;
	}

//	public static boolean forwardBranchCanBeRemaining(long P, int slot, int instructionCount)
//	{
//		long target = P + instructionCount / Processor.NO_OF_SLOTS;
//		long diff1 = Builder.getHighestDifferentBit1(target, P);
//		return diff1 <= Builder.getRemainingBits(slot+1);
//	}
//
//	public static boolean forwardBranchIsRemaining(Builder b, Condition cond, Scope block)
//	{
//		if (b.getCurrentSlot() > (Processor.NO_OF_SLOTS-4)) {
//			b.flush();
//		}
//		b.add(ISA.BRANCH, cond.encode(Branch.REM));
//		long P = b.getCurrentP();
//		block.generate(b);
//		b.flush();
//		long target = b.getCurrentPosition();
//		int diff1 = Builder.getHighestDifferentBit1(target, P);
//		return diff1 <= Processor.SLOT_BITS;
//	}

//	public static Location forwardBranchRemaining(Builder b, Condition cond, Scope block, Condition append_cond)
//	{
//		Location res = null;
//		if (b.getCurrentSlot() > (Processor.NO_OF_SLOTS-4)) {
//			b.flush();
//		}
//		long fixup = b.getCurrentPosition();
//		int fixup_slot = b.getCurrentSlot()+2;
//		b.add(ISA.BRANCH, cond.encode(Branch.REM));
////		long P = b.getCurrentP();
//		block.generate(b);
//		if (append_cond != null) {
//			res = generateForwardBranchRem(b, append_cond);
//		}
//		b.flush();
//		long target = b.getCurrentPosition();
//		long mask = getAddressMask(fixup_slot);
//		com.F64.System s = b.getSystem();
//		long cell = s.getMemory(fixup);
//		cell ^= (cell ^ target) & mask;
//		s.setMemory(fixup, cell);
//		return res;
//	}

	public void fillRemaining(long data)
	{
		long mask = Processor.REMAINING_MASKS[this.current_slot];
		this.current_cell |= mask & data;
		current_slot = Processor.NO_OF_SLOTS;
		flush();
	}
	
	public long forwardBranchLong(Condition cond, Scope block, Condition append_cond)
	{
		long res = 0;
		if (!fit(this.getCurrentSlot(), ISA.BRANCH.ordinal(), cond.encode(Branch.LONG))) {
			this.flush();
		}
		this.add(ISA.BRANCH, cond.encode(Branch.LONG));
		this.addAdditionalCell(0);
		this.flush();
		long fixup = this.getCurrentPosition()-1;
		block.generate(this);
		this.flush();
		long target = this.getCurrentPosition();
		com.F64.System s = this.getSystem();
		s.setMemory(fixup, target);
		return res;
	}
	
//	public static boolean fixupShort(System s, Location loc, long target)
//	{
//		int diff1 = Builder.getHighestDifferentBit1(target, loc.getPAdr());
//		if (diff1 <= Processor.SLOT_BITS) {
//			long fixup = loc.getAdr();
//			int fixup_slot = loc.getSlot();
//			s.setMemory(fixup, Processor.writeSlot(s.getMemory(fixup), fixup_slot, (int)(Processor.SLOT_MASK & target)));
//			return true;
//		}
//		return false;
//	}
//	
//	public static boolean fixupRem(System s, Location loc, long target)
//	{
//		int diff1 = Builder.getHighestDifferentBit1(target, loc.getPAdr());
//		int fixup_slot = loc.getSlot();
//		if (diff1 <= getRemainingBits(fixup_slot)) {
//			long fixup = loc.getAdr();
//			long data = s.getMemory(fixup);
//			long mask = getAddressMask(fixup_slot);
//			s.setMemory(fixup, data ^ ((data ^ target) & mask));
//			return true;
//		}
//		return false;
//	}

	public static void fixupLong(System s, long loc, long target)
	{
		s.setMemory(loc, target);
	}

	
	private System				system;
	private long				start_position;
	private int					start_slot;
	private	long				current_pos;
	private	long				current_cell;
	private	long[]				additional_cells;
	private int					current_slot;
	private int					addtional_cnt;
	private boolean				generate;
	private boolean				call_generated;

	public Builder(System value)
	{
		system = value;
		additional_cells = new long[Processor.NO_OF_SLOTS];
	}

	public System getSystem() {return system;}
	public int getCurrentSlot() {return current_slot;}
	public long getCurrentPosition() {return current_pos;}
	public long getCurrentP() {return current_pos+addtional_cnt;}
	public boolean doesGenerate() {return generate;}
	public boolean hasAdditionalData() {return addtional_cnt > 0;}
	public int getAdditionalDataSize() {return addtional_cnt;}
	public long getCurrentCell() {return current_cell;}
	public void setCurrentCell(long value) {current_cell = value;}

//	public boolean fixupShort(Location loc)
//	{
//		flush();
//		return fixupShort(system, loc, current_cell);
//	}
//	
//	public boolean fixupRem(Location loc)
//	{
//		flush();
//		return fixupRem(system, loc, current_cell);
//	}
//	
//	public void fixupLong(long loc)
//	{
//		flush();
//		fixupLong(system, loc, current_cell);
//	}
//
//	public int getRemainingBits()
//	{
//		return getRemainingBits(current_slot);
//	}
	
	public Builder fork(boolean flush)
	{
		Builder res = new Builder(system);
		res.current_pos = current_pos;
		res.current_cell = current_cell;
		res.current_slot = current_slot;
		res.addtional_cnt = addtional_cnt;
		res.generate = false;
		res.call_generated = false;
		if (flush) {res.flush();}
		res.start_position = res.current_pos;
		res.start_slot = res.current_slot;
		return res;
	}

	public Builder fork(boolean flush, Builder res)
	{
		if (res == null) {res = new Builder(system);}
		res.current_pos = current_pos;
		res.current_cell = current_cell;
		res.current_slot = current_slot;
		res.addtional_cnt = addtional_cnt;
		res.generate = false;
		res.call_generated = false;
		if (flush) {res.flush();}
		res.start_position = res.current_pos;
		res.start_slot = res.current_slot;
		return res;
	}
	

	public boolean exceed1Cell()
	{
		if (addtional_cnt > 0) {return true;}
		if (start_position == current_pos) {return false;}
		if ((current_pos == start_position+1) && (current_slot == 0)) {return false;}
		return true;
	}
	
	public void start(boolean generate)
	{
		current_pos = start_position = system.getCodePosition();
		addtional_cnt = current_slot = start_slot = 0;
		current_cell = 0;
		call_generated = false;
		this.generate = generate;
	}
	
	/**
	 * 
	 * @return true if generated code fits into a single cell and can be inlined
	 */
	public boolean stop()
	{
		boolean res = (
				(current_pos == start_position) || ((current_pos == start_position+1) && (current_slot == 0))
			) && (addtional_cnt == 0) && !call_generated;
		flush();
		return res;
	}

	public void flush()
	{
		if (current_slot > 0) {
			if (generate) {
				if (current_slot < (Processor.NO_OF_SLOTS-1)) {
					// add a skip instruction if cell is not full
					current_cell = Processor.writeSlot(current_cell, current_slot++, ISA.USKIP.ordinal());
				}
				if ((current_pos == start_position) && (start_slot > 0)) {
					system.compileCode(current_cell | system.getMemory(current_pos));
				}
				else {
					system.compileCode(current_cell);
				}
				for (int i=0; i<addtional_cnt; ++i) {
					system.compileCode(additional_cells[i]);
				}
			}
			current_pos += addtional_cnt+1;
			current_cell = 0;
			current_slot = 0;
			addtional_cnt = 0;
		}
	}
	
	public void add(ISA op)
	{
		if (!fit(current_slot, op.ordinal())) {flush();}
		if ((current_slot == Processor.FIRST_SLOT) && (op.ordinal() >= Processor.FIRST_SLOT_SIZE)) {
			current_cell = Processor.writeSlot(current_cell, current_slot++, ISA.NOP.ordinal());
		}
		current_cell = Processor.writeSlot(current_cell, current_slot++, op.ordinal());
	}
	
	public void add(ISA op, int slot0)
	{
		if (!fit(current_slot, op.ordinal(), slot0)) {flush();}
		if ((current_slot == Processor.FIRST_SLOT) && (op.ordinal() >= Processor.FIRST_SLOT_SIZE)) {
			current_cell = Processor.writeSlot(current_cell, current_slot++, ISA.NOP.ordinal());
		}
	current_cell = Processor.writeSlot(current_cell, current_slot++, op.ordinal());
		current_cell = Processor.writeSlot(current_cell, current_slot++, slot0);
	}

	public void add(Ext1 op) {add(ISA.EXT1, op.ordinal());}
	public void add(Ext2 op) {add(ISA.EXT2, op.ordinal());}
	public void add(Ext3 op) {add(ISA.EXT3, op.ordinal());}
	public void add(Ext4 op) {add(ISA.EXT4, op.ordinal());}
	public void add(Ext5 op) {add(ISA.EXT5, op.ordinal());}

	public void add(ISA op, int slot0, int slot1)
	{
		if (!fit(current_slot, op.ordinal(), slot0, slot1)) {flush();}
		if ((current_slot == Processor.FIRST_SLOT) && (op.ordinal() >= Processor.FIRST_SLOT_SIZE)) {
			current_cell = Processor.writeSlot(current_cell, current_slot++, ISA.NOP.ordinal());
		}
		current_cell = Processor.writeSlot(current_cell, current_slot++, op.ordinal());
		current_cell = Processor.writeSlot(current_cell, current_slot++, slot0);
		current_cell = Processor.writeSlot(current_cell, current_slot++, slot1);
	}

	public void add(Ext1 op, int slot0) {add(ISA.EXT1, op.ordinal(), slot0);}
	public void add(Ext2 op, int slot0) {add(ISA.EXT2, op.ordinal(), slot0);}
	public void add(Ext3 op, int slot0) {add(ISA.EXT3, op.ordinal(), slot0);}
	public void add(Ext4 op, int slot0) {add(ISA.EXT4, op.ordinal(), slot0);}
	public void add(Ext5 op, int slot0) {add(ISA.EXT5, op.ordinal(), slot0);}
	public void add(RegOp1 op, int slot0) {add(ISA.REGOP1, op.ordinal(), slot0);}

	public void add(ISA op, int slot0, int slot1, int slot2)
	{
		if (!fit(current_slot, op.ordinal(), slot0, slot1, slot2)) {flush();}
		if ((current_slot == Processor.FIRST_SLOT) && (op.ordinal() >= Processor.FIRST_SLOT_SIZE)) {
			current_cell = Processor.writeSlot(current_cell, current_slot++, ISA.NOP.ordinal());
		}
		current_cell = Processor.writeSlot(current_cell, current_slot++, op.ordinal());
		current_cell = Processor.writeSlot(current_cell, current_slot++, slot0);
		current_cell = Processor.writeSlot(current_cell, current_slot++, slot1);
		current_cell = Processor.writeSlot(current_cell, current_slot++, slot2);
	}

	public void add(Ext1 op, int slot0, int slot1) {add(ISA.EXT1, op.ordinal(), slot0, slot1);}
	public void add(Ext2 op, int slot0, int slot1) {add(ISA.EXT2, op.ordinal(), slot0, slot1);}
	public void add(Ext3 op, int slot0, int slot1) {add(ISA.EXT3, op.ordinal(), slot0, slot1);}
	public void add(Ext4 op, int slot0, int slot1) {add(ISA.EXT4, op.ordinal(), slot0, slot1);}
	public void add(Ext5 op, int slot0, int slot1) {add(ISA.EXT5, op.ordinal(), slot0, slot1);}
	public void add(RegOp2 op, int slot0, int slot1) {add(ISA.REGOP2, op.ordinal(), slot0, slot1);}

	public void add(ISA op, int slot0, int slot1, int slot2, int slot3)
	{
		if (!fit(current_slot, op.ordinal(), slot0, slot1, slot2, slot3)) {flush();}
		if ((current_slot == Processor.FIRST_SLOT) && (op.ordinal() >= Processor.FIRST_SLOT_SIZE)) {
			current_cell = Processor.writeSlot(current_cell, current_slot++, ISA.NOP.ordinal());
		}
		current_cell = Processor.writeSlot(current_cell, current_slot++, op.ordinal());
		current_cell = Processor.writeSlot(current_cell, current_slot++, slot0);
		current_cell = Processor.writeSlot(current_cell, current_slot++, slot1);
		current_cell = Processor.writeSlot(current_cell, current_slot++, slot2);
		current_cell = Processor.writeSlot(current_cell, current_slot++, slot3);
	}

	public void add(Ext1 op, int slot0, int slot1, int slot2) {add(ISA.EXT1, op.ordinal(), slot0, slot1, slot2);}
	public void add(Ext2 op, int slot0, int slot1, int slot2) {add(ISA.EXT2, op.ordinal(), slot0, slot1, slot2);}
	public void add(Ext3 op, int slot0, int slot1, int slot2) {add(ISA.EXT3, op.ordinal(), slot0, slot1, slot2);}
	public void add(Ext4 op, int slot0, int slot1, int slot2) {add(ISA.EXT4, op.ordinal(), slot0, slot1, slot2);}
	public void add(Ext5 op, int slot0, int slot1, int slot2) {add(ISA.EXT5, op.ordinal(), slot0, slot1, slot2);}
	public void add(RegOp3 op, int slot0, int slot1, int slot2) {add(ISA.REGOP3, op.ordinal(), slot0, slot1, slot2);}

	public void add(ISA op, int slot0, int slot1, int slot2, int slot3, int slot4)
	{
		if (!fit(current_slot, op.ordinal(), slot0, slot1, slot2, slot3, slot4)) {flush();}
		if ((current_slot == Processor.FIRST_SLOT) && (op.ordinal() >= Processor.FIRST_SLOT_SIZE)) {
			current_cell = Processor.writeSlot(current_cell, current_slot++, ISA.NOP.ordinal());
		}
		current_cell = Processor.writeSlot(current_cell, current_slot++, op.ordinal());
		current_cell = Processor.writeSlot(current_cell, current_slot++, slot0);
		current_cell = Processor.writeSlot(current_cell, current_slot++, slot1);
		current_cell = Processor.writeSlot(current_cell, current_slot++, slot2);
		current_cell = Processor.writeSlot(current_cell, current_slot++, slot3);
		current_cell = Processor.writeSlot(current_cell, current_slot++, slot4);
	}

	public void finishCell(long bits)
	{
		if (generate) {
			system.compileCode(current_cell | bits);
			for (int i=0; i<addtional_cnt; ++i) {
				system.compileCode(additional_cells[i]);
			}
		}
		current_pos += addtional_cnt+1;
		current_cell = 0;
		current_slot = 0;
		addtional_cnt = 0;
	}

	public void addCall(long dest_adr, boolean is_jump)
	{
		long source_adr = this.getCurrentP();
		long mask = Processor.REMAINING_MASKS[current_slot+1];
		if ((source_adr ^ dest_adr) > mask) {
			flush();			
		}
		if (is_jump) {
			current_cell = Processor.writeSlot(current_cell, current_slot++, ISA.JUMP.ordinal());
		}
		else {
			current_cell = Processor.writeSlot(current_cell, current_slot++, ISA.CALL.ordinal());
		}
		fillRemaining(dest_adr);
		call_generated = true;
	}

	public void addLiteral(long value)
	{
		if (value >= 0) {
			// positive
			if (value < Processor.SLOT_SIZE) {
				add(ISA.LIT, (int)value);
				return;
			}
//			if (value < Processor.SLOT_SIZE * Processor.SLOT_SIZE) {
//				if (fit(current_slot, ISA.LIT.ordinal(), (int)(value >> Processor.SLOT_BITS), ISA.EXT.ordinal(), (int)(value & Processor.SLOT_MASK))) {
//					add(ISA.LIT, (int)(value >> Processor.SLOT_BITS));
//					add(ISA.EXT, (int)(value & Processor.SLOT_MASK));
//					return;
//				}
//			}
		}
		else {
			// negative
			long abs = ~value;
			if (abs < Processor.SLOT_SIZE) {
				add(ISA.NLIT, (int)abs);
				return;
			}
//			if (abs < Processor.SLOT_SIZE * Processor.SLOT_SIZE) {
//				if (fit(current_slot, ISA.NLIT.ordinal(), (int)(abs >> Processor.SLOT_BITS), ISA.EXT.ordinal(), (int)(value & Processor.SLOT_MASK))) {
//					add(ISA.NLIT, (int)(abs >> Processor.SLOT_BITS));
//					add(ISA.EXT, (int)(value & Processor.SLOT_MASK));
//					return;
//				}
//			}
		}
		if (!fit(current_slot, ISA.FETCHPINC.ordinal())) {flush();}
		add(ISA.FETCHPINC);
		addAdditionalCell(value);
	}
	

	public void addAdditionalCell(long value)
	{
		additional_cells[addtional_cnt++] = value;
	}

	public void forwardBranch(ConditionalBranch br, Scope block)
	{
		Builder probe = null;
		int instr_cnt = block.countInstructions();
		if (Builder.forwardBranchCanBeImplicit(getCurrentSlot(), instr_cnt)) {
			probe = fork(false);
			if (probe.forwardBranchIsImplicit(br.getCondition(), block)) {
				forwardBranchImplicit(br.getCondition(), block);
				return;
			}
		}
		if (Builder.forwardBranchCanBeShort(getCurrentP(), getCurrentSlot(), instr_cnt)) {
			probe = fork(false, probe);
			if (probe.forwardBranchIsShort(br, block, null)) {
				forwardBranchShort(br, block, null);
				probe.flush();
				return;
			}
		}
//		if (Builder.forwardBranchCanBeRemaining(getCurrentP(), getCurrentSlot(), instr_cnt)) {
//			probe = fork(false, probe);
//			if (Builder.forwardBranchIsRemaining(probe, br.getCondition(), block)) {
//				return;
//			}
//		}
		forwardBranchLong(br.getCondition(), block, null);

	}

	public void forwardBranch(Condition cond, Scope block)
	{
		Builder probe = null;
		int instr_cnt = block.countInstructions();
		if (Builder.forwardBranchCanBeImplicit(getCurrentSlot(), instr_cnt)) {
			probe = fork(false);
			if (probe.forwardBranchIsImplicit(Condition.EQ0, block)) {
				forwardBranchImplicit(Condition.EQ0, block);
				return;
			}
		}
		ConditionalBranch pre = new ConditionalBranch(Condition.EQ0);
		ConditionalBranch post = new ConditionalBranch(Condition.ALWAYS);
		if (Builder.forwardBranchCanBeShort(getCurrentP(), getCurrentSlot(), instr_cnt)) {
			probe = fork(false, probe);
			if (probe.forwardBranchIsShort(pre, block, post)) {
				forwardBranchShort(pre, block, post);
				probe.flush();
				return;
			}
		}
//		if (Builder.forwardBranchCanBeRemaining(getCurrentP(), getCurrentSlot(), instr_cnt)) {
//			probe = fork(false, probe);
//			if (Builder.forwardBranchIsRemaining(probe, Condition.EQ0, block)) {
//				return;
//			}
//		}
		forwardBranchLong(Condition.EQ0, block, null);
	}

}

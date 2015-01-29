package com.F64.scope;

import com.F64.Branch;
import com.F64.Builder;
import com.F64.Compiler;
import com.F64.Condition;
import com.F64.Ext1;
import com.F64.ISA;
import com.F64.Optimization;
import com.F64.Processor;
import com.F64.codepoint.Literal;

public class If extends com.F64.Block implements java.lang.Cloneable {
	private com.F64.ConditionalBranch	branch_to_false;
	private com.F64.Block				true_part;
	private com.F64.ConditionalBranch	branch_to_end;
	private com.F64.Block				false_part;

	public If(Compiler c, Condition cond)
	{
		super(c.getScope());
		branch_to_false = new com.F64.ConditionalBranch(cond);
		true_part = new com.F64.Block(this);
		c.setScope(true_part);	
	}

	public If clone() throws CloneNotSupportedException
	{
		If res = (If)super.clone();
		if (true_part != null) {
			res.true_part = true_part.clone();
			res.true_part.setOwner(res);
		}
		if (false_part != null) {
			res.false_part = false_part.clone();
			res.false_part.setOwner(res);
		}
		return res;
	}

	public void doElse(Compiler c)
	{
		branch_to_end = new com.F64.ConditionalBranch(Condition.ALWAYS);
		false_part = new com.F64.Block(this);
		c.setScope(false_part);	
	}

	public void doThen(Compiler c)
	{
		c.setScope(this.getOwner());	
	}
	
	@Override
	public boolean optimize(Compiler c, Optimization opt)
	{
		boolean res = false;
		if (opt == Optimization.DEAD_CODE_ELIMINATION) {
			Condition cond = branch_to_false.getCondition();
			if (cond == Condition.ALWAYS) {
				if ((false_part == null) || false_part.isEmpty()) {
					this.remove();
				}
				else {
					false_part.optimize(c, opt);
					this.replaceWithScope(false_part);
				}
				res = true;
			}
			else if (cond == Condition.NEVER) {
				true_part.optimize(c, opt);
				this.replaceWithScope(true_part);
				res = true;
			}
		}
		else if (opt == Optimization.CONSTANT_FOLDING) {
			Condition cond = branch_to_false.getCondition();
			if ((cond == Condition.EQ0) || (cond == Condition.GE0)) {
				com.F64.Codepoint p = this.getPrevious();
				if (p != null) {
					if (p instanceof Literal) {
						Literal lit = (Literal) p;
						long data = lit.getValue();
						if (cond == Condition.EQ0) {
							if (data == 0) {
								cond = Condition.ALWAYS;
							}
							else {
								cond = Condition.NEVER;
							}
						}
						else {
							if (data >= 0) {
								cond = Condition.ALWAYS;
							}
							else {
								cond = Condition.NEVER;
							}
						}
						lit.remove();
						res = true;
					}
				}
			}
		}
		if (false_part != null) {
			if (false_part.optimize(c, opt)) {res = true;}
		}
		if (true_part.optimize(c, opt)) {res = true;}
		return res;
	}
	
	@Override
	public void generate(Builder b)
	{
		int additional, slot, bits, bits1, bits2, diff1, diff2, slot1, slot2, target_slot, fixup_slot, true_target_slot, false_target_slot;
		long target, fixup, target1, target2, cpos;
		Condition cond = branch_to_false.getCondition();
		if (cond == Condition.ALWAYS) {
			if (false_part != null) {
				false_part.generate(b);
			}
			return;
		}
		if (cond == Condition.NEVER) {
			true_part.generate(b);
			return;
		}
		int t_cnt = true_part.countInstructions();
		int f_cnt = false_part == null ? 0 : false_part.countInstructions();
		com.F64.System s = b.getSystem();
		if (t_cnt == 0) {
			// there is no true part
			if (f_cnt == 0) {return;}
			switch (cond) {
			case CARRY:
				b.add(Ext1.CARRYQ);
				branch_to_false.setCondition(Condition.NE0);
				break;
			case EQ0:
				branch_to_false.setCondition(Condition.NE0);
				break;
			case GE0:
				b.add(Ext1.LT0Q);
				branch_to_false.setCondition(Condition.EQ0);
				break;
			default:
				break;
			}
			b.forwardBranch(branch_to_false, false_part);
			return;
		}
		if (f_cnt == 0) {
			// only true part
			// first we test if the if statement fits into current cell
			b.forwardBranch(branch_to_false, true_part);
			return;
		}
		// non-empty true and false part

		// try pattern 1 (implicit + implicit)
		//												+-------------------------------+
		//												|								v
		// -+-------+-------+-------+-------+-------+-------+-------+-------+-------+-------+
		//	|BRANCH	| JMP	|	+	|	+	|	+	| UJMPn	|	-	|	-	|	-	|		|
		// -+-------+-------+-------+-------+-------+-------+-------+-------+-------+-------+
		//				|										^
		//				+---------------------------------------+

		Builder probe = null;

		if (Builder.forwardBranchCanBeImplicit(b.getCurrentSlot(), t_cnt + f_cnt + 1)) {
			probe = b.fork(false);
			cpos = probe.getCurrentPosition();
			additional = probe.getAdditionalDataSize();
			branch_to_false.generateBranch(probe, Branch.SKIP);
			true_part.generate(probe);
			branch_to_end.generateBranch(probe, Branch.SKIP);
			false_part.generate(probe);
			// test if code is still in same cell and no more additional data has been added
			if ((cpos == probe.getCurrentPosition()) && (additional == probe.getAdditionalDataSize())) {
				// pattern 1 fit
				branch_to_false.generateBranch(b, Branch.SKIP);
				true_part.generate(b);
				branch_to_end.generateBranch(b, Branch.SKIP);
				slot1 = b.getCurrentSlot();
				false_part.generate(b);
				slot2 = b.getCurrentSlot();
				branch_to_false.fixup(b, cpos, slot1);
				branch_to_end.fixup(b, cpos, slot2);
				return;
			}
		}

		// try pattern 2 (implicit + short)
		//	
		// -+-------+-------+-------+-------+-------+-------+-------+-------+-------+-------+
		//	|BRANCH	| SKIP	|	+	|	+	|	+	| 	+	|	+	| SJMP	|	N	|		|
		// -+-------+-------+-------+-------+-------+-------+-------+-------+-------+-------+
		//				|														|
		//		+-------+														+---------------+
		//		v																				|
		// -+-------+-------+-------+-------+-------+-------+-------+-------+-------+-------+	|
		//	|	-	|	-	|	-	|	-	|	-	| SKIP	|		|		|		|		|	|
		// -+-------+-------+-------+-------+-------+-------+-------+-------+-------+-------+	v

		if (Builder.forwardBranchCanBeImplicit(b.getCurrentSlot(), t_cnt + 2)) {
			probe = b.fork(false);
			cpos = probe.getCurrentPosition();
			additional = probe.getAdditionalDataSize();
			branch_to_false.generateBranch(probe, Branch.SKIP);
			true_part.generate(probe);
			branch_to_end.generateBranch(probe, Branch.SHORT);
			if ((cpos == probe.getCurrentPosition()) && (additional == probe.getAdditionalDataSize())) {
				probe.flush();
				false_part.generate(probe);
				probe.flush();
				// pattern 1 fit
				diff2 = Builder.getHighestDifferentBit1(probe.getCurrentPosition(), probe.getCurrentPosition());
				bits2 = Processor.getSlotBits(branch_to_end.getFixupSlot());
				if (diff2 <= bits2) {
					branch_to_false.generateBranch(b, Branch.SKIP);
					true_part.generate(b);
					branch_to_end.generateBranch(b, Branch.SHORT);
					b.flush();
					false_part.generate(b);
					b.flush();
					branch_to_end.fixup(b, b.getCurrentPosition(), 0);
					return;
				}
			}
		}
		
		// try pattern 3 (implicit + remaining)
		//	
		// -+-------+-------+-------+-------+-------+-------+-------+-------+-------+-------+
		//	|BRANCH	| SKIP	|	+	|	+	|	+	| 	+	|	+	| RJMP	|	N			|
		// -+-------+-------+-------+-------+-------+-------+-------+-------+-------+-------+
		//				|														|
		//		+-------+														+---------------+
		//		v																				|
		// -+-------+-------+-------+-------+-------+-------+-------+-------+-------+-------+	|
		//	|	-	|	-	|	-	|	-	|	-	| SKIP	|		|		|		|		|	|
		// -+-------+-------+-------+-------+-------+-------+-------+-------+-------+-------+	v

		if (Builder.forwardBranchCanBeImplicit(b.getCurrentSlot(), t_cnt + 2)) {
			probe = b.fork(false);
			cpos = probe.getCurrentPosition();
			additional = probe.getAdditionalDataSize();
			branch_to_false.generateBranch(probe, Branch.SKIP);
			true_part.generate(probe);
			branch_to_end.generateBranch(probe, Branch.REM);
			if ((cpos == probe.getCurrentPosition()) && (additional == probe.getAdditionalDataSize())) {
				probe.flush();
				false_part.generate(probe);
				probe.flush();
				// pattern 1 fit
				diff2 = Builder.getHighestDifferentBit1(probe.getCurrentPosition(), probe.getCurrentPosition());
				bits2 = Builder.getRemainingBits(branch_to_end.getFixupSlot());
				if (diff2 <= bits2) {
					branch_to_false.generateBranch(b, Branch.SKIP);
					true_part.generate(b);
					branch_to_end.generateBranch(b, Branch.REM);
					b.flush();
					false_part.generate(b);
					b.flush();
					branch_to_end.fixup(b, b.getCurrentPosition(), 0);
					return;
				}
			}
		}

		
		// try pattern 4 (implicit + long)
		//	
		// -+-------+-------+-------+-------+-------+-------+-------+-------+-------+-------+
		//	|BRANCH	| SKIP	|	+	|	+	|	+	| 	+	|	+	| EXT1	| LJMP	|		|
		// -+-------+-------+-------+-------+-------+-------+-------+-------+-------+-------+
		//	|			|						N											|
		// -+-------+---|---+-------+-------+-------+-------+-------+-------+-------+-------+
		//				|						|
		//		+-------+						+-----------------------------------------------+
		//		v																				|
		// -+-------+-------+-------+-------+-------+-------+-------+-------+-------+-------+	|
		//	|	-	|	-	|	-	|	-	|	-	|	-	|	-	|	-	|	-	|	-	|	|
		// -+-------+-------+-------+-------+-------+-------+-------+-------+-------+-------+	|
		// -+-------+-------+-------+-------+-------+-------+-------+-------+-------+-------+	|
		// -+-------+-------+-------+-------+-------+-------+-------+-------+-------+-------+	|
		// -+-------+-------+-------+-------+-------+-------+-------+-------+-------+-------+	|
		// -+-------+-------+-------+-------+-------+-------+-------+-------+-------+-------+	v

////		if (Builder.forwardBranchCanBeImplicit(b.getCurrentSlot(), t_cnt + 2)) {
////			probe = b.fork(false);
////			long cpos = probe.getCurrentPosition();
////			branch_to_false.generateBranch(probe, Branch.SKIP);
////			true_part.generate(probe);
////			branch_to_end.generateBranch(probe, Branch.LONG);
////			if (cpos == probe.getCurrentPosition()) {
////				branch_to_false.generateBranch(b, Branch.SKIP);
////				true_part.generate(b);
////				branch_to_end.generateBranch(b, Branch.LONG);
////				b.flush();
////				false_part.generate(b);
////				b.flush();
////				branch_to_end.fixup(b, b.getCurrentPosition(), 0);
////				return;
////			}
////		}
//

		// try pattern 5 (short + short)
		//	
		// -+-------+-------+-------+-------+-------+-------+-------+-------+-------+-------+
		//	|BRANCH	| SHORT	|	N	|	+	|	+	| 	+	|	+	|	+	|	+	|	+	|
		// -+-------+-------+-------+-------+-------+-------+-------+-------+-------+-------+
		// -+-------+-------+---|---+-------+-------+-------+-------+-------+-------+-------+
		// -+-------+-------+---|---+-------+-------+-------+-------+-------+-------+-------+
		//	|	+	|	+	|	|	|	+	|	+	| 	+	|	+	| SJMP	|	N	|		|
		// -+-------+-------+---|---+-------+-------+-------+-------+-------+-------+-------+
		//						|												|
		//		+---------------+												+---------------+
		//		v																				|
		// -+-------+-------+-------+-------+-------+-------+-------+-------+-------+-------+	|
		//	|	-	|	-	|	-	|	-	|	-	| SKIP	|		|		|		|		|	|
		// -+-------+-------+-------+-------+-------+-------+-------+-------+-------+-------+	v

		probe = b.fork(false);
		branch_to_false.generateBranch(probe, Branch.SHORT);
		true_part.generate(probe);
		branch_to_end.generateBranch(probe, Branch.SHORT);
		probe.flush();
		target1 = probe.getCurrentPosition();
		false_part.generate(probe);
		probe.flush();
		target2 = probe.getCurrentPosition();
		diff1 = Builder.getHighestDifferentBit1(branch_to_false.getPAdr(), target1);
		diff2 = Builder.getHighestDifferentBit1(branch_to_end.getPAdr(), target2);
		bits1 = Processor.getSlotBits(branch_to_false.getFixupSlot());
		bits2 = Processor.getSlotBits(branch_to_end.getFixupSlot());

		if ((diff1 <= bits1) && (diff2 <= bits2)) {
			// we can use this pattern
			branch_to_false.generateBranch(b, Branch.SHORT);
			true_part.generate(b);
			branch_to_end.generateBranch(b, Branch.SHORT);
			b.flush();
			target1 = b.getCurrentPosition();
			false_part.generate(b);
			b.flush();
			target2 = b.getCurrentPosition();
			branch_to_false.fixup(b, target1, 0);
			branch_to_end.fixup(b, target2, 0);
			return;
		}

		// try pattern 6 (short + remaining)
		//	
		// -+-------+-------+-------+-------+-------+-------+-------+-------+-------+-------+
		//	|BRANCH	| SHORT	|	N	|	+	|	+	| 	+	|	+	|	+	|	+	|	+	|
		// -+-------+-------+-------+-------+-------+-------+-------+-------+-------+-------+
		// -+-------+-------+---|---+-------+-------+-------+-------+-------+-------+-------+
		// -+-------+-------+---|---+-------+-------+-------+-------+-------+-------+-------+
		//	|	+	|	+	|	|	|	+	|	+	| 	+	|	+	| RJMP	|	N			|
		// -+-------+-------+---|---+-------+-------+-------+-------+-------+-------+-------+
		//						|												|
		//		+---------------+												+---------------+
		//		v																				|
		// -+-------+-------+-------+-------+-------+-------+-------+-------+-------+-------+	|
		//	|	-	|	-	|	-	|	-	|	-	|	-	|	-	|	-	|	-	|	-	|	|
		// -+-------+-------+-------+-------+-------+-------+-------+-------+-------+-------+	|
		// -+-------+-------+-------+-------+-------+-------+-------+-------+-------+-------+	|
		// -+-------+-------+-------+-------+-------+-------+-------+-------+-------+-------+	|
		// -+-------+-------+-------+-------+-------+-------+-------+-------+-------+-------+	|
		// -+-------+-------+-------+-------+-------+-------+-------+-------+-------+-------+	v

		probe = b.fork(false);
		branch_to_false.generateBranch(probe, Branch.SHORT);
		true_part.generate(probe);
		branch_to_end.generateBranch(probe, Branch.REM);
		probe.flush();
		target1 = probe.getCurrentPosition();
		false_part.generate(probe);
		probe.flush();
		target2 = probe.getCurrentPosition();
		diff1 = Builder.getHighestDifferentBit1(branch_to_false.getPAdr(), target1);
		diff2 = Builder.getHighestDifferentBit1(branch_to_end.getPAdr(), target2);
		bits1 = Processor.getSlotBits(branch_to_false.getFixupSlot());
		bits2 = Builder.getRemainingBits(branch_to_end.getFixupSlot());

		if ((diff1 <= bits1) && (diff2 <= bits2)) {
			// we can use this pattern
			branch_to_false.generateBranch(b, Branch.SHORT);
			true_part.generate(b);
			branch_to_end.generateBranch(b, Branch.REM);
			b.flush();
			target1 = b.getCurrentPosition();
			false_part.generate(b);
			b.flush();
			target2 = b.getCurrentPosition();
			branch_to_false.fixup(b, target1, 0);
			branch_to_end.fixup(b, target2, 0);
			return;
		}

		
		// try pattern 7 (short + long)
		//	
		// -+-------+-------+-------+-------+-------+-------+-------+-------+-------+-------+
		//	|BRANCH	| SHORT	|	N	|	+	|	+	| 	+	|	+	|	+	|	+	|	+	|
		// -+-------+-------+-------+-------+-------+-------+-------+-------+-------+-------+
		// -+-------+-------+---|---+-------+-------+-------+-------+-------+-------+-------+
		// -+-------+-------+---|---+-------+-------+-------+-------+-------+-------+-------+
		//	|	+	|	+	|	|	|	+	|	+	| 	+	|	+	| EXT1	| LJMP	|		|
		// -+-------+-------+---|---+-------+-------+-------+-------+-------+-------+-------+
		//	|					|				N											|
		// -+-------+-------+---|---+-------+-------+-------+-------+-------+-------+-------+
		//						|				|
		//		+---------------+				+-----------------------------------------------+
		//		v																				|
		// -+-------+-------+-------+-------+-------+-------+-------+-------+-------+-------+	|
		//	|	-	|	-	|	-	|	-	|	-	|	-	|	-	|	-	|	-	|	-	|	|
		// -+-------+-------+-------+-------+-------+-------+-------+-------+-------+-------+	|
		// -+-------+-------+-------+-------+-------+-------+-------+-------+-------+-------+	|
		// -+-------+-------+-------+-------+-------+-------+-------+-------+-------+-------+	|
		// -+-------+-------+-------+-------+-------+-------+-------+-------+-------+-------+	|
		// -+-------+-------+-------+-------+-------+-------+-------+-------+-------+-------+	v

		probe = b.fork(false);
		branch_to_false.generateBranch(probe, Branch.SHORT);
		true_part.generate(probe);
		branch_to_end.generateBranch(probe, Branch.LONG);
		probe.flush();
		target1 = probe.getCurrentPosition();
		false_part.generate(probe);
		probe.flush();
		target2 = probe.getCurrentPosition();
		diff1 = Builder.getHighestDifferentBit1(branch_to_false.getPAdr(), target1);
		bits1 = Processor.getSlotBits(branch_to_false.getFixupSlot());

		if (diff1 <= bits1) {
			// we can use this pattern
			branch_to_false.generateBranch(b, Branch.SHORT);
			true_part.generate(b);
			branch_to_end.generateBranch(b, Branch.LONG);
			b.flush();
			target1 = b.getCurrentPosition();
			false_part.generate(b);
			b.flush();
			target2 = b.getCurrentPosition();
			branch_to_false.fixup(b, target1, 0);
			branch_to_end.fixup(b, target2, 0);
			return;
		}

		// try pattern 8 (remaining + short)
		//	
		// -+-------+-------+-------+-------+-------+-------+-------+-------+-------+-------+
		//	|BRANCH	| REM	|	N															|
		// -+-------+-------+-------+-------+-------+-------+-------+-------+-------+-------+
		//	|	+	|	+	|	|	|	+	|	+	|	+	|	+	|	+	|	+	|	+	|
		// -+-------+-------+---|---+-------+-------+-------+-------+-------+-------+-------+
		// -+-------+-------+---|---+-------+-------+-------+-------+-------+-------+-------+
		//	|	+	|	+	|	|	|	+	|	+	| 	+	|	+	| SJMP	|	N	|		|
		// -+-------+-------+---|---+-------+-------+-------+-------+-------+-------+-------+
		//						|												|
		//		+---------------+												+---------------+
		//		v																				|
		// -+-------+-------+-------+-------+-------+-------+-------+-------+-------+-------+	|
		//	|	-	|	-	|	-	|	-	|	-	| SKIP	|		|		|		|		|	|
		// -+-------+-------+-------+-------+-------+-------+-------+-------+-------+-------+	v

		probe = b.fork(false);
		branch_to_false.generateBranch(probe, Branch.REM);
		probe.flush();
		true_part.generate(probe);
		branch_to_end.generateBranch(probe, Branch.SHORT);
		probe.flush();
		target1 = probe.getCurrentPosition();
		false_part.generate(probe);
		probe.flush();
		target2 = probe.getCurrentPosition();
		diff1 = Builder.getHighestDifferentBit1(branch_to_false.getPAdr(), target1);
		diff2 = Builder.getHighestDifferentBit1(branch_to_end.getPAdr(), target2);
		bits1 = Builder.getRemainingBits(branch_to_false.getFixupSlot());
		bits2 = Processor.getSlotBits(branch_to_end.getFixupSlot());

		if ((diff1 <= bits1) && (diff2 <= bits2)) {
			// we can use this pattern
			branch_to_false.generateBranch(b, Branch.REM);
			b.flush();
			true_part.generate(b);
			branch_to_end.generateBranch(b, Branch.SHORT);
			b.flush();
			target1 = b.getCurrentPosition();
			false_part.generate(b);
			b.flush();
			target2 = b.getCurrentPosition();
			branch_to_false.fixup(b, target1, 0);
			branch_to_end.fixup(b, target2, 0);
			return;
		}


		// try pattern 9 (remaining + remaining)
		//	
		// -+-------+-------+-------+-------+-------+-------+-------+-------+-------+-------+
		//	|BRANCH	| REM	|	N															|
		// -+-------+-------+-------+-------+-------+-------+-------+-------+-------+-------+
		//	|	+	|	+	|	|	|	+	|	+	|	+	|	+	|	+	|	+	|	+	|
		// -+-------+-------+---|---+-------+-------+-------+-------+-------+-------+-------+
		// -+-------+-------+---|---+-------+-------+-------+-------+-------+-------+-------+
		//	|	+	|	+	|	|	|	+	|	+	| 	+	|	+	| RJMP	|	N			|
		// -+-------+-------+---|---+-------+-------+-------+-------+-------+-------+-------+
		//						|												|
		//		+---------------+												+---------------+
		//		v																				|
		// -+-------+-------+-------+-------+-------+-------+-------+-------+-------+-------+	|
		//	|	-	|	-	|	-	|	-	|	-	|	-	|	-	|	-	|	-	|	-	|	|
		// -+-------+-------+-------+-------+-------+-------+-------+-------+-------+-------+	|
		// -+-------+-------+-------+-------+-------+-------+-------+-------+-------+-------+	|
		// -+-------+-------+-------+-------+-------+-------+-------+-------+-------+-------+	|
		// -+-------+-------+-------+-------+-------+-------+-------+-------+-------+-------+	|
		// -+-------+-------+-------+-------+-------+-------+-------+-------+-------+-------+	v

		probe = b.fork(false);
		branch_to_false.generateBranch(probe, Branch.REM);
		probe.flush();
		true_part.generate(probe);
		branch_to_end.generateBranch(probe, Branch.REM);
		probe.flush();
		target1 = probe.getCurrentPosition();
		false_part.generate(probe);
		probe.flush();
		target2 = probe.getCurrentPosition();
		diff1 = Builder.getHighestDifferentBit1(branch_to_false.getPAdr(), target1);
		diff2 = Builder.getHighestDifferentBit1(branch_to_end.getPAdr(), target2);
		bits1 = Builder.getRemainingBits(branch_to_false.getFixupSlot());
		bits2 = Builder.getRemainingBits(branch_to_end.getFixupSlot());

		if ((diff1 <= bits1) && (diff2 <= bits2)) {
			// we can use this pattern
			branch_to_false.generateBranch(b, Branch.REM);
			b.flush();
			true_part.generate(b);
			branch_to_end.generateBranch(b, Branch.REM);
			b.flush();
			target1 = b.getCurrentPosition();
			false_part.generate(b);
			b.flush();
			target2 = b.getCurrentPosition();
			branch_to_false.fixup(b, target1, 0);
			branch_to_end.fixup(b, target2, 0);
			return;
		}

		// try pattern 10 (remaining + long)
		//	
		// -+-------+-------+-------+-------+-------+-------+-------+-------+-------+-------+
		//	|BRANCH	| REM	|	N															|
		// -+-------+-------+-------+-------+-------+-------+-------+-------+-------+-------+
		//	|	+	|	+	|	|	|	+	|	+	|	+	|	+	|	+	|	+	|	+	|
		// -+-------+-------+---|---+-------+-------+-------+-------+-------+-------+-------+
		// -+-------+-------+---|---+-------+-------+-------+-------+-------+-------+-------+
		//	|	+	|	+	|	|	|	+	|	+	| 	+	|	+	| EXT1	| LJMP	|		|
		// -+-------+-------+---|---+-------+-------+-------+-------+-------+-------+-------+
		//	|					|				N											|
		// -+-------+-------+---|---+-------+-------+-------+-------+-------+-------+-------+
		//						|				|
		//		+---------------+				+-----------------------------------------------+
		//		v																				|
		// -+-------+-------+-------+-------+-------+-------+-------+-------+-------+-------+	|
		//	|	-	|	-	|	-	|	-	|	-	|	-	|	-	|	-	|	-	|	-	|	|
		// -+-------+-------+-------+-------+-------+-------+-------+-------+-------+-------+	|
		// -+-------+-------+-------+-------+-------+-------+-------+-------+-------+-------+	|
		// -+-------+-------+-------+-------+-------+-------+-------+-------+-------+-------+	|
		// -+-------+-------+-------+-------+-------+-------+-------+-------+-------+-------+	|
		// -+-------+-------+-------+-------+-------+-------+-------+-------+-------+-------+	v

		probe = b.fork(false);
		branch_to_false.generateBranch(probe, Branch.REM);
		probe.flush();
		true_part.generate(probe);
		branch_to_end.generateBranch(probe, Branch.LONG);
		probe.flush();
		target1 = probe.getCurrentPosition();
		false_part.generate(probe);
		probe.flush();
		target2 = probe.getCurrentPosition();
		diff1 = Builder.getHighestDifferentBit1(branch_to_false.getPAdr(), target1);
		bits1 = Builder.getRemainingBits(branch_to_false.getFixupSlot());

		if (diff1 <= bits1) {
			// we can use this pattern
			branch_to_false.generateBranch(b, Branch.REM);
			b.flush();
			true_part.generate(b);
			branch_to_end.generateBranch(b, Branch.LONG);
			b.flush();
			target1 = b.getCurrentPosition();
			false_part.generate(b);
			b.flush();
			target2 = b.getCurrentPosition();
			branch_to_false.fixup(b, target1, 0);
			branch_to_end.fixup(b, target2, 0);
			return;
		}

		
		// try pattern 11 (long + short)
		//	
		// -+-------+-------+-------+-------+-------+-------+-------+-------+-------+-------+
		//	|BRANCH	| LONG	|		|		|		|		|		|		|		|		|
		// -+-------+-------+-------+-------+-------+-------+-------+-------+-------+-------+
		//	|					N															|
		// -+-------+-------+-------+-------+-------+-------+-------+-------+-------+-------+
		//	|	+	|	+	|	|	|	+	|	+	|	+	|	+	|	+	|	+	|	+	|
		// -+-------+-------+---|---+-------+-------+-------+-------+-------+-------+-------+
		// -+-------+-------+---|---+-------+-------+-------+-------+-------+-------+-------+
		//	|	+	|	+	|	|	|	+	|	+	| 	+	|	+	| SJMP	|	N	|		|
		// -+-------+-------+---|---+-------+-------+-------+-------+-------+-------+-------+
		//						|												|
		//		+---------------+												+---------------+
		//		v																				|
		// -+-------+-------+-------+-------+-------+-------+-------+-------+-------+-------+	|
		//	|	-	|	-	|	-	|	-	|	-	| SKIP	|		|		|		|		|	|
		// -+-------+-------+-------+-------+-------+-------+-------+-------+-------+-------+	v

		probe = b.fork(false);
		branch_to_false.generateBranch(probe, Branch.LONG);
		true_part.generate(probe);
		branch_to_end.generateBranch(probe, Branch.SHORT);
		probe.flush();
		target1 = probe.getCurrentPosition();
		false_part.generate(probe);
		probe.flush();
		target2 = probe.getCurrentPosition();
		diff2 = Builder.getHighestDifferentBit1(branch_to_end.getPAdr(), target2);
		bits2 = Processor.getSlotBits(branch_to_end.getFixupSlot());

		if (diff2 <= bits2) {
			// we can use this pattern
			branch_to_false.generateBranch(b, Branch.LONG);
			true_part.generate(b);
			branch_to_end.generateBranch(b, Branch.SHORT);
			b.flush();
			target1 = b.getCurrentPosition();
			false_part.generate(b);
			b.flush();
			target2 = b.getCurrentPosition();
			branch_to_false.fixup(b, target1, 0);
			branch_to_end.fixup(b, target2, 0);
			return;
		}

		
		// try pattern 12 (long + remaining)
		//	
		// -+-------+-------+-------+-------+-------+-------+-------+-------+-------+-------+
		//	|BRANCH	| LONG	|		|		|		|		|		|		|		|		|
		// -+-------+-------+-------+-------+-------+-------+-------+-------+-------+-------+
		//	|					N															|
		// -+-------+-------+-------+-------+-------+-------+-------+-------+-------+-------+
		//	|	+	|	+	|	|	|	+	|	+	|	+	|	+	|	+	|	+	|	+	|
		// -+-------+-------+---|---+-------+-------+-------+-------+-------+-------+-------+
		// -+-------+-------+---|---+-------+-------+-------+-------+-------+-------+-------+
		//	|	+	|	+	|	|	|	+	|	+	| 	+	|	+	| RJMP	|	N			|
		// -+-------+-------+---|---+-------+-------+-------+-------+-------+-------+-------+
		//						|												|
		//		+---------------+												+---------------+
		//		v																				|
		// -+-------+-------+-------+-------+-------+-------+-------+-------+-------+-------+	|
		//	|	-	|	-	|	-	|	-	|	-	|	-	|	-	|	-	|	-	|	-	|	|
		// -+-------+-------+-------+-------+-------+-------+-------+-------+-------+-------+	|
		// -+-------+-------+-------+-------+-------+-------+-------+-------+-------+-------+	|
		// -+-------+-------+-------+-------+-------+-------+-------+-------+-------+-------+	|
		// -+-------+-------+-------+-------+-------+-------+-------+-------+-------+-------+	|
		// -+-------+-------+-------+-------+-------+-------+-------+-------+-------+-------+	v

		probe = b.fork(false);
		branch_to_false.generateBranch(probe, Branch.LONG);
		true_part.generate(probe);
		branch_to_end.generateBranch(probe, Branch.REM);
		probe.flush();
		target1 = probe.getCurrentPosition();
		false_part.generate(probe);
		probe.flush();
		target2 = probe.getCurrentPosition();
		diff2 = Builder.getHighestDifferentBit1(branch_to_end.getPAdr(), target2);
		bits2 = Builder.getRemainingBits(branch_to_end.getFixupSlot());

		if (diff2 <= bits2) {
			// we can use this pattern
			branch_to_false.generateBranch(b, Branch.LONG);
			true_part.generate(b);
			branch_to_end.generateBranch(b, Branch.REM);
			b.flush();
			target1 = b.getCurrentPosition();
			false_part.generate(b);
			b.flush();
			target2 = b.getCurrentPosition();
			branch_to_false.fixup(b, target1, 0);
			branch_to_end.fixup(b, target2, 0);
			return;
		}

		// pattern 13 (long + long)
		// this works always
		//	
		// -+-------+-------+-------+-------+-------+-------+-------+-------+-------+-------+
		//	|BRANCH	| LONG	|		|		|		|		|		|		|		|		|
		// -+-------+-------+-------+-------+-------+-------+-------+-------+-------+-------+
		//	|					N															|
		// -+-------+-------+-------+-------+-------+-------+-------+-------+-------+-------+
		//	|	+	|	+	|	|	|	+	|	+	|	+	|	+	|	+	|	+	|	+	|
		// -+-------+-------+---|---+-------+-------+-------+-------+-------+-------+-------+
		// -+-------+-------+---|---+-------+-------+-------+-------+-------+-------+-------+
		//	|	+	|	+	|	|	|	+	|	+	| 	+	|	+	| EXT1	| LJMP	|		|
		// -+-------+-------+---|---+-------+-------+-------+-------+-------+-------+-------+
		//	|					|				N											|
		// -+-------+-------+---|---+-------+-------+-------+-------+-------+-------+-------+
		//						|				|
		//		+---------------+				+-----------------------------------------------+
		//		v																				|
		// -+-------+-------+-------+-------+-------+-------+-------+-------+-------+-------+	|
		//	|	-	|	-	|	-	|	-	|	-	|	-	|	-	|	-	|	-	|	-	|	|
		// -+-------+-------+-------+-------+-------+-------+-------+-------+-------+-------+	|
		// -+-------+-------+-------+-------+-------+-------+-------+-------+-------+-------+	|
		// -+-------+-------+-------+-------+-------+-------+-------+-------+-------+-------+	|
		// -+-------+-------+-------+-------+-------+-------+-------+-------+-------+-------+	|
		// -+-------+-------+-------+-------+-------+-------+-------+-------+-------+-------+	v

		branch_to_false.generateBranch(b, Branch.LONG);
		true_part.generate(b);
		branch_to_end.generateBranch(b, Branch.LONG);
		b.flush();
		target1 = b.getCurrentPosition();
		false_part.generate(b);
		b.flush();
		target2 = b.getCurrentPosition();
		branch_to_false.fixup(b, target1, 0);
		branch_to_end.fixup(b, target2, 0);

	}

}

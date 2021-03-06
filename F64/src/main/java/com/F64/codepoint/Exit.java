package com.F64.codepoint;

import com.F64.Builder;
import com.F64.Compiler;
import com.F64.ISA;
import com.F64.Optimization;
import com.F64.Scope;

public class Exit extends com.F64.Codepoint {

	@Override
	public boolean optimize(Compiler c, Optimization opt)
	{
		if (this.getPrevious() == null) {return false;}
		com.F64.Codepoint p = this.getPrevious();
		if (p != null) {
			switch (opt) {
			case PEEPHOLE:
				if (p instanceof Secondary) {
					// We can eliminate the exit if we convert a call into a jump
					Secondary sec = (Secondary) p;
					sec.setUseJump(true);
					this.remove();
					return true;
				}
				if (p instanceof Execute) {
					Execute exc = (Execute) p;
					if (exc.optimizeExit()) {
						this.remove();
						return true;
					}
				}
				break;

			default:
				break;
			}
		}
		if (opt == Optimization.DEAD_CODE_ELIMINATION) {
			// all code after an exit is dead code
			com.F64.Codepoint n = this.getNext();
			if (n != null) {
				while (n != null) {
					n.remove();
					n = this.getNext();
				}
				return true;
			}
		}
		return false;
	}
	
	@Override
	public void generate(Builder b)
	{
		Scope s = getOwner();
		while (s != null) {
			if (s instanceof com.F64.Block) {
				com.F64.Block blk = (com.F64.Block)s;
				blk.generateLeaveLocals(b);
				if (s instanceof com.F64.scope.Main) {
					break;
				}
			}
			s = s.getOwner();
		}
		b.add(ISA.EXIT);
	}

}

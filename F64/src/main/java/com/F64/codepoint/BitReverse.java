package com.F64.codepoint;

import com.F64.Compiler;
import com.F64.Optimization;
import com.F64.Processor;
import com.F64.RegOp1;
import com.F64.RegOp2;
import com.F64.Register;

public class BitReverse extends com.F64.Codepoint {
	private int src;
	private int dest;

	public BitReverse()
	{
		src = dest = -1;
	}

	@Override
	public boolean optimize(Processor processor, Optimization opt)
	{
		if (this.getPrevious() == null) {return false;}
		com.F64.Codepoint p = this.getPrevious();
		if (p != null) {
			switch (opt) {
			case CONSTANT_FOLDING:
				if (p instanceof Literal) {
					// top of stack is multiplied with a constant
					// this gives a lot of opportunities for optimization
					Literal lit = (Literal) p;
					lit.setValue(Processor.reverseBits(lit.getValue()));
					this.remove();
					return true;
				}
				break;

			case PEEPHOLE:
				if (p instanceof BitReverse) {
					// 2 reverse do nothing
					p.remove();
					this.remove();
					return true;
				}
				break;

			default:
				break;
			}
		}
		return false;
	}
	
	@Override
	public void generate(Compiler c)
	{
		if (src == dest) {
			c.generate(RegOp1.REVERSE, dest < 0 ? Register.T.ordinal() : dest);
		}
		else {
			c.generate(RegOp2.REVERSE, dest < 0 ? Register.T.ordinal() : dest, src < 0 ? Register.T.ordinal() : src);
		}
	}


}
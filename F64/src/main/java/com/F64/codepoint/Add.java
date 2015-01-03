package com.F64.codepoint;

import com.F64.Compiler;
import com.F64.ISA;
import com.F64.Optimization;
import com.F64.Processor;
import com.F64.RegOp1;
import com.F64.Register;

public class Add extends com.F64.Codepoint {

	@Override
	public boolean optimize(Optimization opt)
	{
		if (this.getPrevious() == null) {return false;}
		com.F64.Codepoint p = this.getPrevious();
		if (p != null) {
			switch (opt) {
			case CONSTANT_FOLDING:
				com.F64.Codepoint pp = this.getPrevious();
				if (pp != null) {
					if ((p instanceof Literal) && (pp instanceof Literal)) {
						// constant
						Literal lit1 = (Literal) pp;
						Literal lit2 = (Literal) p;
						lit1.setValue(lit1.getValue() + lit2.getValue());
						this.getOwner().remove(lit2);
						this.getOwner().remove(this);
						return true;
					}
				}
				break;

			case PEEPHOLE:
				if (p instanceof Literal) {
					// top of stack is multiplied with a constant
					// this gives a lot of opportunities for optimization
					Literal lit = (Literal) p;
					long data = lit.getValue();
					if (data == 0) {
						// add by 0 does nothing
						this.getOwner().remove(lit);
						this.getOwner().remove(this);
						return true;
					}
					if (data > 0) {
						if (data == 1) {
							// increment
							this.getOwner().replace(lit, new Inc());
							this.getOwner().remove(this);
							return true;
						}
						if (data < Processor.SLOT_SIZE) {
							this.getOwner().replace(lit, new RegOpCode(RegOp1.ADDI, Register.T.ordinal(), Register.T.ordinal(), (int)data));
							this.getOwner().remove(this);
							return true;
						}
					}
					else {
						if (data == -1) {
							// decrement
							this.getOwner().replace(lit, new Dec());
							this.getOwner().remove(this);
							return true;
						}
						if (data > -Processor.SLOT_SIZE) {
							this.getOwner().replace(lit, new RegOpCode(RegOp1.SUBI, Register.T.ordinal(), Register.T.ordinal(), -(int)data));
							this.getOwner().remove(this);
							return true;
						}
					}
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
		c.generate(ISA.ADD);
	}

}
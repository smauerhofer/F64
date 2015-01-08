package com.F64.codepoint;

import com.F64.Compiler;
import com.F64.ISA;
import com.F64.Optimization;

public class Drop extends com.F64.Codepoint {

	@Override
	public boolean optimize(Optimization opt)
	{
		if (this.getPrevious() == null) {return false;}
		com.F64.Codepoint p = this.getPrevious();
		if (p != null) {
			switch (opt) {
			case CONSTANT_FOLDING:
				if (p instanceof Literal) {
					p.remove();
					this.remove();
					return true;
				}
				break;


			case PEEPHOLE:
				if (p instanceof Dup) {
					// the sequence dup drop does nothing and can be deleted
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
		c.generate(ISA.DROP);
	}

}

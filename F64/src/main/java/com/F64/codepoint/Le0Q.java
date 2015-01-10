package com.F64.codepoint;

import com.F64.Compiler;
import com.F64.Ext2;
import com.F64.Optimization;
import com.F64.Processor;

public class Le0Q extends com.F64.Codepoint {

	@Override
	public boolean optimize(Processor processor, Optimization opt)
	{
		if (this.getPrevious() == null) {return false;}
		com.F64.Codepoint p = this.getPrevious();
		if (p != null) {
			switch (opt) {
			case CONSTANT_FOLDING:
				if (p instanceof Literal) {
					Literal lit = (Literal) p;
					lit.setValue(lit.getValue() <= 0 ? Processor.TRUE : Processor.FALSE);
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
		c.generate(Ext2.LE0Q);
	}


}

package com.F64.codepoint;

import com.F64.Builder;
import com.F64.Compiler;
import com.F64.Optimization;


public class Literal extends com.F64.Codepoint {
	private long	value;

	public Literal(long value)
	{
		this.value = value;
	}

	public long getValue() {return value;}
	public void setValue(long value) {this.value = value;}

	@Override
	public boolean optimize(Compiler c, Optimization opt)
	{
//		if (this.getPrevious() == null) {return false;}
//		com.F64.Codepoint p = this.getPrevious();
//		if (p != null) {
//			switch (opt) {
//			case CONSTANT_FOLDING:
//				if (p instanceof Literal) {
//					// constant
//					Literal lit = (Literal) p;
//					if (lit.getValue() == this.value) {
//						this.replaceWith(new Dup());
//						return true;
//					}
//				}
//				break;
//
//			default:
//				break;
//			}
//		}
		return false;
	}
	
//	private boolean generateOptimized(Compiler c, long data)
//	{
//		if ((data >= 0) && (data < Processor.SLOT_SIZE)) {
//			// constant fits into a slot
//			b.add(ISA.LIT, (int)data);
//			return true;
//		}
//		if ((data & (data-1)) == 0) {
//			// 1 bit set constant
//			b.add(Ext1.BLIT, Processor.findFirstBit1(data));
//			return true;
//		}
//		data = ~data;
//		if ((data >= 0) && (data < Processor.SLOT_SIZE)) {
//			// inverted constant fits into a slot
//			b.add(ISA.NLIT, (int)data);
//			return true;
//		}
//		return false;
//		
//	}
	
	@Override
	public void generate(Builder b)
	{
		b.addLiteral(value);
//		if (generateOptimized(c, value)) {return;}
//		if (value >= 0) {
//			// positive number
//			if (!c.doesFit(ISA.FETCHPINC.ordinal())) {c.flush();}
//			b.add(ISA.FETCHPINC);
//			c.addAdditional(value);
//		}
//		else {
//			// negative number
//			if (!c.doesFit(ISA.FETCHPINC.ordinal())) {c.flush();}
//			b.add(ISA.FETCHPINC);
//			c.addAdditional(value);
//		}
//		
	}


	
}

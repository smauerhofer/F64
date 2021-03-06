package com.F64.codepoint;

import com.F64.Builder;
import com.F64.Compiler;
import com.F64.ISA;
import com.F64.Optimization;
import com.F64.Processor;
import com.F64.RegOp2;
import com.F64.RegOp3;
import com.F64.Register;

public class Lsl extends com.F64.Codepoint {
	private int		src1;
	private int 	src2;
	private int 	dest;
	private	int		cnt;

	public Lsl()
	{
		src1 = src2 = dest = -1;
		cnt = -1;
	}

	public Lsl(int value)
	{
		src1 = src2 = dest = -1;
		cnt = value;
	}

	@Override
	public boolean optimize(Compiler c, Optimization opt)
	{
		if (this.getPrevious() == null) {return false;}
		if (this.cnt >= 0) {return false;}
		com.F64.Codepoint p = this.getPrevious();
		if ((p != null) && (dest==-1)) {
			switch (opt) {
			case CONSTANT_FOLDING:
				com.F64.Codepoint pp = p.getPrevious();
				if (pp != null) {
					if ((p instanceof Literal) && (pp instanceof Literal)) {
						// constant
						Literal lit1 = (Literal) pp;
						Literal lit2 = (Literal) p;
						lit1.setValue(lit1.getValue() << lit2.getValue());
						lit2.remove();
						this.remove();
						return true;
					}
				}
				break;

			case PEEPHOLE:
				if (p instanceof Literal) {
					Literal lit = (Literal) p;
					long data = lit.getValue();
					if (data == 0) {
						lit.remove();
						this.remove();
						return true;
					}
					if (data > 0) {
						if (data < Processor.SLOT_SIZE) {
							lit.replaceWith(new Lsl((int)data));
							this.remove();
							return true;
						}
						else {
							this.getOwner().replace(lit, new Zero());
							this.getOwner().remove(this);
							return true;
						}
					}
					else {
						if (data == -0x8000_0000_0000_0000L) {
							assert(false);
							lit.remove();
							this.remove();
							return true;
						}
						lit.setValue(-data);
						this.replaceWith(new Lsr());
						return true;
					}
				}
				if (p instanceof Lsl) {
					Lsl prev = (Lsl)p;
					if ((this.cnt >= 0) && (prev.cnt >= 0)) {
						prev.cnt += this.cnt;
						this.remove();
						return true;
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
	public void generate(Builder b)
	{
		if (dest == -1) {
			if (cnt == -1) {
				b.add(RegOp3.LSL, Register.T.ordinal(), Register.S.ordinal(), Register.T.ordinal());
				b.add(ISA.NIP);
			}
			else {
				b.add(RegOp3.LSLI, Register.T.ordinal(), Register.T.ordinal(), cnt);
			}
		}
		else if (dest == src1) {
			if (src2 >= 0) {
				b.add(RegOp2.LSL, dest, src2);
			}
			else {
				b.add(RegOp2.LSLI, dest, cnt);
			}
		}
		else if (src2 >= 0) {
			b.add(RegOp3.LSL, dest, src1, src2);
		}
		else {
			b.add(RegOp3.LSLI, dest, src1, cnt);
		}
	}


}

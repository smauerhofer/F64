package com.F64.codepoint;

import com.F64.Builder;
import com.F64.Compiler;
import com.F64.ISA;
import com.F64.Optimization;
import com.F64.Processor;
import com.F64.RegOp2;
import com.F64.RegOp3;
import com.F64.Register;

public class Asl extends com.F64.Codepoint {
	private int		src1;
	private int 	src2;
	private int 	dest;
	private	int		cnt;

	public Asl()
	{
		src1 = src2 = dest = -1;
		cnt = -1;
	}

	public Asl(int value)
	{
		src1 = src2 = dest = -1;
		cnt = value;
	}

	public boolean isStandardConstant() {return (dest == -1) && (cnt >= 0);}
	public int getConstant() {return cnt;}
	public void setConstant(int value) {cnt = value;}

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
					// top of stack is multiplied with a constant
					// this gives a lot of opportunities for optimization
					Literal lit = (Literal) p;
					long data = lit.getValue();
					if (data == 0) {
						lit.remove();
						this.remove();
						return true;
					}
					if (data > 0) {
						if (data < Processor.SLOT_SIZE) {
							lit.replaceWith(new Asl((int)data));
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
						this.replaceWith(new Asr());
						return true;
					}
				}
				if (p instanceof Asl) {
					Asl prev = (Asl)p;
					if ((this.isStandardConstant()) && (prev.isStandardConstant())) {
						prev.cnt += this.cnt;
						this.remove();
						return true;
					}
				}
				if (p instanceof Mul2) {
					if (this.cnt >= 0) {
						this.cnt += 1;
						p.remove();
						return true;
					}
				}
//				if (p instanceof Asr) {
//					Asr prev = (Asr)p;
//					if ((this.isStandardConstant()) && (prev.isStandardConstant())) {
//						if (this.getConstant() == prev.getConstant()) {
//							prev.remove();
//							this.remove();
//							return true;
//						}
//					}
//				}
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
		if (cnt == 0) {return;}
		if (dest == -1) {
			if (cnt == -1) {
				b.add(RegOp3.ASL, Register.T.ordinal(), Register.S.ordinal(), Register.T.ordinal());
				b.add(ISA.NIP);
			}
			else {
				if (cnt < 4) {
					for (int i=0; i<cnt; ++i) {
						b.add(ISA.MUL2);
					}
				}
				else {
					b.add(RegOp3.ASLI, Register.T.ordinal(), Register.T.ordinal(), cnt);
				}
			}
		}
		else if (dest == src1) {
			if (src2 >= 0) {
				b.add(RegOp2.ASL, dest, src2);
			}
			else {
				b.add(RegOp2.ASLI, dest, cnt);
			}
		}
		else if (src2 >= 0) {
			b.add(RegOp3.ASL, dest, src1, src2);
		}
		else {
			b.add(RegOp3.ASLI, dest, src1, cnt);
		}
	}


}

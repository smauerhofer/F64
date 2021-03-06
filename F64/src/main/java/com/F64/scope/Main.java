package com.F64.scope;

import com.F64.Block;
import com.F64.Builder;
import com.F64.Codepoint;
import com.F64.Compiler;
import com.F64.Optimization;
import com.F64.Word;

public class Main extends Block {
	private Word	word;
	private boolean	has_internal_exit;
	private boolean	has_internal_call;

	public Main(Word w)
	{
		super(null);
		word = w;
	}

	public Main(com.F64.Scope s)
	{
		super(s);
	}

	public void internalExit() {has_internal_exit = true;}
	public void internalCall() {has_internal_call = true;}
	
	public Word getWord() {return word;}
	
	@Override
	public boolean optimize(Compiler c, Optimization opt)
	{
		boolean res = super.optimize(c, opt);
		if (opt == Optimization.ENTER_EXIT_ELIMINATION) {
			if (!has_internal_exit && !has_internal_call
				&& (head != null)
				&& (head instanceof com.F64.codepoint.Enter)
				&& (tail instanceof com.F64.codepoint.Exit)
			) {
				Codepoint curr = head.getNext();
				if (curr == tail) {
					// no code inside colon definition
					head = tail = new com.F64.codepoint.Skip();
					res = true;
				}
				else {
					Builder b = c.getBuilder();
					b.start(false);
					super.generate(b);
					if (!b.exceed1Cell()) {
						b.stop();
						head.remove();
						tail.remove();
						res = true;
					}
					b.stop();
				}
			}
		}
		return res;
	}

}

package com.F64.word;

import com.F64.Compiler;
import com.F64.ISA;
import com.F64.Interpreter;
import com.F64.Processor;

public class Dup extends com.F64.Word {

	@Override
	public void execute(Interpreter i)
	{
		Processor p = i.getProcessor();
		p.doDup();
	}

	@Override
	public void compile(Interpreter i)
	{
		Compiler c = i.getCompiler();
		c.compile(new com.F64.codepoint.Dup());
	}


}

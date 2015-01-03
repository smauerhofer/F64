package com.F64.word;

import com.F64.Compiler;
import com.F64.ISA;
import com.F64.Interpreter;
import com.F64.Processor;
import com.F64.Register;

public class Add extends com.F64.Word {

	@Override
	public void execute(Interpreter i)
	{
		Processor p = i.getProcessor();
		p.setRegister(Register.T, p.getRegister(Register.S) + p.getRegister(Register.T));
		p.doNip();
	}

	@Override
	public void compile(Interpreter i)
	{
		Compiler c = i.getCompiler();
		c.compile(ISA.ADD);
	}

}
use crate::{
    chunk::{Chunk, OpCode},
    compiler::compile,
    value::{print_value, Value},
};

pub enum InterpretResult {
    Ok,
    CompileError,
    RuntimeError,
}

pub struct VM<'a> {
    chunk: &'a Chunk,
    ip: usize,
    stack: Vec<Value>,
    pub debug: bool,
}

macro_rules! pop {
    ($stack:expr) => {
        match $stack.pop() {
            Some(operand) => operand,
            None => return InterpretResult::RuntimeError,
        }
    };
}

macro_rules! binary_op {
    ($stack:expr, $op:tt) => {
        {
            let b = pop!($stack);
            let a = pop!($stack);
            $stack.push(a $op b);
        }
    };
}

impl<'a> VM<'a> {
    pub fn new(chunk: &'a Chunk) -> VM {
        VM {
            chunk,
            ip: 0,
            stack: vec![],
            debug: false,
        }
    }

    pub fn interpret(&mut self, line: String) -> InterpretResult {
        compile(line);
        InterpretResult::Ok
    }

    pub fn run(&mut self) -> InterpretResult {
        loop {
            let instruction = self.chunk.get_instruction(self.ip);
            if self.debug {
                print!("          ");
                for value in self.stack.iter() {
                    print!("[ ");
                    print_value(value);
                    print!(" ]");
                }
                print!("\n");
                instruction.disassemble(self.chunk, self.ip);
            }

            match instruction {
                OpCode::Constant(constant_index) => {
                    let value = self.chunk.get_constant(constant_index);
                    self.stack.push(value);
                }
                OpCode::Add => binary_op!(self.stack, +),
                OpCode::Subtract => binary_op!(self.stack, -),
                OpCode::Multiply => binary_op!(self.stack, *),
                OpCode::Divide => binary_op!(self.stack, /),
                OpCode::Negate => match self.stack.pop() {
                    Some(value) => self.stack.push(-value),
                    None => return InterpretResult::RuntimeError,
                },
                OpCode::Return => {
                    if let Some(final_value) = self.stack.pop() {
                        print_value(&final_value);
                        print!("\n");
                    }
                    return InterpretResult::Ok;
                }
            }
            self.ip += 1;
        }
    }
}

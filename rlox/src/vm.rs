use crate::{chunk::Chunk, value::print_value};

pub enum InterpretResult {
    Ok,
    CompileError,
    RuntimeError,
}

pub struct VM<'a> {
    chunk: &'a Chunk,
    ip: usize,
    pub debug: bool,
}

impl<'a> VM<'a> {
    pub fn new(chunk: &'a Chunk) -> VM {
        VM {
            chunk,
            ip: 0,
            debug: false,
        }
    }

    pub fn run(&mut self) -> InterpretResult {
        loop {
            let instruction = self.chunk.get_instruction(self.ip);
            if self.debug {
                instruction.disassemble(self.chunk, self.ip);
            }
            match instruction {
                crate::chunk::OpCode::Constant(constant_index) => {
                    let value = self.chunk.get_constant(constant_index);
                    print_value(&value);
                    print!("\n");
                    self.ip += 1;
                }
                crate::chunk::OpCode::Return => return InterpretResult::Ok,
            }
        }
    }
}

use crate::{
    chunk::Chunk,
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

impl<'a> VM<'a> {
    pub fn new(chunk: &'a Chunk) -> VM {
        VM {
            chunk,
            ip: 0,
            stack: vec![],
            debug: false,
        }
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
                crate::chunk::OpCode::Constant(constant_index) => {
                    let value = self.chunk.get_constant(constant_index);
                    self.stack.push(value);
                    self.ip += 1;
                }
                crate::chunk::OpCode::Return => {
                    if let Some(final_value) = self.stack.pop() {
                        print_value(&final_value);
                        print!("\n");
                    }
                    return InterpretResult::Ok;
                }
            }
        }
    }
}

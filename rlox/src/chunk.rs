use crate::value::Value;

#[repr(u8)]
pub enum OpCode {
    Constant(usize),
    Return,
}

impl OpCode {
    fn disassemble(&self, chunk: &Chunk, offset: usize) {
        print!("{:04} ", offset);

        if offset > 0 && chunk.lines[offset] == chunk.lines[offset - 1] {
            print!("   | ");
        } else {
            print!("{:04} ", chunk.lines[offset]);
        }

        match self {
            OpCode::Return => OpCode::simple_instruction("RETURN"),
            OpCode::Constant(index) => {
                OpCode::constant_instruction("CONSTANT", chunk, index.clone())
            }
        }
    }

    fn simple_instruction(name: &str) {
        println!("{}", name);
    }

    fn constant_instruction(name: &str, chunk: &Chunk, index: usize) {
        let value = chunk.constants[index];
        println!("{:16} {:4} {}", name, index, value);
    }
}

pub struct Chunk {
    code: Vec<OpCode>,
    lines: Vec<usize>,
    constants: Vec<Value>,
}

impl Chunk {
    pub fn new() -> Chunk {
        Chunk {
            code: vec![],
            lines: vec![],
            constants: vec![],
        }
    }

    pub fn write_code(&mut self, code: OpCode, line: usize) {
        self.code.push(code);
        self.lines.push(line);
    }

    pub fn add_constant(&mut self, value: Value) -> usize {
        self.constants.push(value);
        self.constants.len() - 1
    }

    pub fn disassemble(&self, name: &str) {
        println!("== {} ==", name);

        for (offset, instruction) in self.code.iter().enumerate() {
            instruction.disassemble(self, offset);
        }
    }
}

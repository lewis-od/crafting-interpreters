#[repr(u8)]
pub enum OpCode {
    Return,
}

impl OpCode {
    fn disassemble(&self, chunk: &Chunk, offset: usize) -> usize {
        print!("{:04} ", offset);

        let instruction = chunk.code.get(offset).unwrap();
        match instruction {
            OpCode::Return => OpCode::simple_instruction("RETURN", offset),
        }
    }

    fn simple_instruction(name: &str, offset: usize) -> usize {
        println!("{}", name);
        offset + 1
    }
}

pub struct Chunk {
    code: Vec<OpCode>,
}

impl Chunk {
    pub fn new() -> Chunk {
        Chunk { code: vec![] }
    }

    pub fn write_code(&mut self, code: OpCode) {
        self.code.push(code);
    }

    pub fn disassemble(&self, name: &str) {
        println!("== {} ==", name);

        let mut offset = 0;
        for instruction in self.code.iter() {
            offset = instruction.disassemble(self, offset);
        }
    }
}

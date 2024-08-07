mod chunk;
mod value;

use chunk::{Chunk, OpCode};

fn main() {
    let mut chunk = Chunk::new();

    let constant_index = chunk.add_constant(1.2);
    chunk.write_code(OpCode::Constant(constant_index));

    chunk.write_code(OpCode::Return);
    chunk.disassemble("test chunk");
}

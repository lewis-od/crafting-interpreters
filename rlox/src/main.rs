mod chunk;
mod value;

use chunk::{Chunk, OpCode};

fn main() {
    let mut chunk = Chunk::new();

    let constant_index = chunk.add_constant(1.2);
    chunk.write_code(OpCode::Constant(constant_index), 123);

    chunk.write_code(OpCode::Return, 123);
    chunk.disassemble("test chunk");
}

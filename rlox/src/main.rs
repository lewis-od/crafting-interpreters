mod chunk;

use chunk::{Chunk, OpCode};

fn main() {
    let mut chunk = Chunk::new();
    chunk.write_code(OpCode::Return);
    chunk.disassemble("test chunk");
}

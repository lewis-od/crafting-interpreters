mod chunk;
mod value;
mod vm;

use chunk::{Chunk, OpCode};
use std;
use vm::VM;

fn main() {
    let mut chunk = Chunk::new();

    let constant_index = chunk.add_constant(1.2);
    chunk.write_code(OpCode::Constant(constant_index), 123);

    let constant_index = chunk.add_constant(3.4);
    chunk.write_code(OpCode::Constant(constant_index), 123);

    chunk.write_code(OpCode::Add, 123);

    let constant_index = chunk.add_constant(5.6);
    chunk.write_code(OpCode::Constant(constant_index), 123);

    chunk.write_code(OpCode::Divide, 123);
    chunk.write_code(OpCode::Negate, 123);

    chunk.write_code(OpCode::Return, 123);
    chunk.disassemble("test chunk");

    let mut vm = VM::new(&chunk);
    vm.debug = true;
    let exit_code = match vm.run() {
        vm::InterpretResult::Ok => 0,
        vm::InterpretResult::CompileError => 1,
        vm::InterpretResult::RuntimeError => 2,
    };
    std::process::exit(exit_code);
}

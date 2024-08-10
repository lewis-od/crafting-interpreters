mod chunk;
mod compiler;
mod scanner;
mod value;
mod vm;

use chunk::Chunk;
use std;
use std::io::{stdin, stdout, Write};
use vm::VM;

fn main() {
    let chunk = Chunk::new();
    let mut vm = VM::new(&chunk);
    vm.debug = true;

    let num_args = std::env::args().len();
    if num_args == 1 {
        repl(&mut vm);
    } else if num_args == 2 {
        let file_name = std::env::args().nth(1).unwrap();
        run_file(&mut vm, file_name);
    } else {
        eprintln!("Usage: rlox [path]");
        std::process::exit(64);
    }
}

fn repl(vm: &mut VM) {
    loop {
        print!("> ");
        let line = read_input();
        vm.interpret(line);
    }
}

fn read_input() -> String {
    let mut input = String::new();
    let _ = stdout().flush();
    stdin()
        .read_line(&mut input)
        .expect("Did not enter a string");
    input.trim().to_string()
}

fn run_file(vm: &mut VM, file_name: String) {
    let file_contents = std::fs::read_to_string(file_name).expect("Unable to read file");
    let exit_code = match vm.interpret(file_contents) {
        vm::InterpretResult::Ok => 0,
        vm::InterpretResult::CompileError => 65,
        vm::InterpretResult::RuntimeError => 70,
    };
    std::process::exit(exit_code);
}

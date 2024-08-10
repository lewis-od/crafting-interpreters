use crate::scanner::{Scanner, TokenType};

pub fn compile(source: String) {
    let mut scanner = Scanner::new(&source);
    let mut line: Option<usize> = None;
    loop {
        let token = scanner.scan_token();
        if line.is_none() || token.line != line.unwrap() {
            print!("{:4} ", token.line);
            line = Some(token.line);
        } else {
            print!("   | ");
        }
        print!("{:?} '{}'\n", token.r#type, token.lexeme);

        if token.r#type == TokenType::Eof {
            break;
        }
    }
}

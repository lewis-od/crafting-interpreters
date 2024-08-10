use core::str;

use phf::phf_map;

pub struct Token<'a> {
    pub r#type: TokenType,
    pub lexeme: &'a str,
    pub start: usize,
    pub length: usize,
    pub line: usize,
}

#[derive(Debug, PartialEq, Eq, Clone, Copy)]
pub enum TokenType {
    // Single character
    LeftParen,
    RightParen,
    LeftBrace,
    RightBrace,
    Comma,
    Dot,
    Minus,
    Plus,
    Semicolon,
    Slash,
    Star,

    // One or 2 characters
    Bang,
    BangEqual,
    Equal,
    EqualEqual,
    Greater,
    GreaterEqual,
    Less,
    LessEqual,

    // Literals
    Identifier,
    String,
    Number,

    // Keywords
    And,
    Class,
    Else,
    False,
    For,
    Fun,
    If,
    Nil,
    Or,
    Print,
    Return,
    Super,
    This,
    True,
    Var,
    While,

    // Other
    Error,
    Eof,
}

static KEYWORDS: phf::Map<&'static str, TokenType> = phf_map! {
    "and" => TokenType::And,
    "class" => TokenType::Class,
    "else" => TokenType::Else,
    "false" => TokenType::False,
    "for" => TokenType::For,
    "fun" => TokenType::Fun,
    "if" => TokenType::If,
    "nil" => TokenType::Nil,
    "or" => TokenType::Or,
    "print" => TokenType::Print,
    "return" => TokenType::Return,
    "super" => TokenType::Super,
    "this" => TokenType::This,
    "true" => TokenType::True,
    "var" => TokenType::Var,
    "while" => TokenType::While,
};

pub struct Scanner<'a> {
    source: &'a [u8],
    start: usize,
    current: usize,
    line: usize,
}

impl<'a> Scanner<'a> {
    pub fn new(source: &String) -> Scanner {
        Scanner {
            source: source.as_bytes(),
            start: 0,
            current: 0,
            line: 1,
        }
    }

    pub fn scan_token(&mut self) -> Token {
        self.skip_whitespace();
        self.start = self.current;

        if self.is_at_end() {
            return self.make_token(TokenType::Eof);
        }

        let character = self.advance();
        if character.is_alphabetic() {
            return self.identifier();
        }
        if character.is_digit(10) {
            return self.number();
        }

        match character {
            '(' => return self.make_token(TokenType::LeftParen),
            ')' => return self.make_token(TokenType::RightParen),
            '{' => return self.make_token(TokenType::LeftBrace),
            '}' => return self.make_token(TokenType::RightBrace),
            ';' => return self.make_token(TokenType::Semicolon),
            ',' => return self.make_token(TokenType::Comma),
            '.' => return self.make_token(TokenType::Dot),
            '-' => return self.make_token(TokenType::Minus),
            '+' => return self.make_token(TokenType::Plus),
            '/' => return self.make_token(TokenType::Slash),
            '*' => return self.make_token(TokenType::Star),
            '!' => return self.if_match('=', TokenType::BangEqual, TokenType::Bang),
            '=' => return self.if_match('=', TokenType::EqualEqual, TokenType::Equal),
            '<' => return self.if_match('=', TokenType::LessEqual, TokenType::Less),
            '>' => return self.if_match('=', TokenType::GreaterEqual, TokenType::Greater),
            '"' => return self.string(),
            _ => return self.error_token("Unexpected character."),
        }
    }

    fn skip_whitespace(&mut self) {
        loop {
            let c = self.current();
            if c.is_whitespace() {
                if c == '\n' {
                    self.line += 1;
                }
                self.advance();
            } else if c == '/' {
                if self.peek_next() == '/' {
                    // Found a comment - treat as whitespace
                    while self.current() != '\n' && !self.is_at_end() {
                        self.advance();
                    }
                } else {
                    return;
                }
            } else {
                return;
            }
        }
    }

    fn if_match(&mut self, to_match: char, a: TokenType, b: TokenType) -> Token {
        let token_type = if self.r#match(to_match) { a } else { b };
        self.make_token(token_type)
    }

    fn r#match(&mut self, expected: char) -> bool {
        if self.is_at_end() {
            return false;
        }

        if self.current() != expected {
            return false;
        }

        self.current += 1;
        return true;
    }

    fn identifier(&mut self) -> Token {
        while self.current().is_alphanumeric() {
            self.advance();
        }
        self.make_token(self.identifier_type())
    }

    fn identifier_type(&self) -> TokenType {
        let lexeme = str::from_utf8(&self.source[self.start..self.current]).unwrap();
        match KEYWORDS.get(lexeme) {
            Some(keyword_type) => keyword_type.to_owned(),
            None => TokenType::Identifier,
        }
    }

    fn number(&mut self) -> Token {
        while self.current().is_digit(10) {
            self.advance();
        }

        if self.current() == '.' && self.peek_next().is_digit(10) {
            self.advance(); // Consume '.'
            while self.current().is_digit(10) {
                self.advance();
            }
        }

        self.make_token(TokenType::Number)
    }

    fn string(&mut self) -> Token {
        while self.current() != '"' && !self.is_at_end() {
            if self.current() == '\n' {
                self.line += 1;
            }
            self.advance();
        }

        if self.is_at_end() {
            return self.error_token("Unterminated string.");
        }

        self.advance(); // Closing quote
        self.make_token(TokenType::String)
    }

    fn is_at_end(&self) -> bool {
        self.current == self.source.len()
    }

    fn current(&self) -> char {
        if self.current < self.source.len() {
            self.source[self.current] as char
        } else {
            // Mimic null-terminated strings
            '\0'
        }
    }

    fn peek_next(&self) -> char {
        if self.is_at_end() {
            '\0'
        } else {
            self.source[self.current + 1] as char
        }
    }

    fn advance(&mut self) -> char {
        let cur = self.current();
        self.current += 1;
        cur
    }

    fn make_token(&self, token_type: TokenType) -> Token {
        let lexeme = str::from_utf8(&self.source[self.start..self.current]).unwrap();
        Token {
            r#type: token_type,
            start: self.start,
            length: self.current - self.start,
            line: self.line,
            lexeme,
        }
    }

    fn error_token(&self, message: &'a str) -> Token {
        Token {
            r#type: TokenType::Error,
            start: self.start,
            length: message.len(),
            line: self.line,
            lexeme: message.into(),
        }
    }
}

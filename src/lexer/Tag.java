package lexer;

// A classe Tag define constantes para os tokens da linguagem
public class Tag {

    // Palavras reservadas
    public static final int 
        PROGRAM = 256,
        BEGIN = 257,
        END = 258,
        TYPE = 259,
        INT = 260,
        FLOAT = 261,
        CHAR = 262,
        BOOL = 263,
        IF = 264,
        THEN = 265,
        ELSE = 266,
        WHILE = 267,
        DO = 268,
        REPEAT = 269,
        UNTIL = 270,
        IN = 271,
        OUT = 272,

        // Operadores e pontuação
        EQ = 273,       // ==
        GT = 274,       // >
        GE = 275,       // >=
        LT = 276,       // <
        LE = 277,       // <=
        NE = 278,       // !=
        ASSIGN = 279,   // =
        PLUS = 280,     // +
        MINUS = 281,    // -
        OR = 282,       // ||
        TIMES = 283,    // *
        DIV = 284,      // /
        AND = 285,      // &&

        // Tokens diversos
        NUM = 286,          // numero inteiro
        REAL = 287,         // numero float
        CHAR_CONST = 288,   // caractere entre aspas simples
        LITERAL = 289,      // string entre aspas duplas
        ID = 290,           // identificadores

        // Delimitadores
        SEMICOLON = 291,    // ;
        COLON = 292,        // :
        COMMA = 293,        // ,
        LPAREN = 294,       // (
        RPAREN = 295;       // )
}

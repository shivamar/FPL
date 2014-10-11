import java.util.ArrayList;
import java.util.HashMap;
/* 		OO PARSER AND BYTE-CODE GENERATOR FOR TINY PL
 
 Grammar for TinyPL (using EBNF notation) is as follows:

 program ->  decls stmts end
 decls   ->  int idlist ;
 idlist  ->  id { , id } 
 stmts   ->  stmt [ stmts ]
 cmpdstmt->  '{' stmts '}'
 stmt    ->  assign | cond | loop
 assign  ->  id = expr ;
 cond    ->  if '(' rexp ')' cmpdstmt [ else cmpdstmt ]
 loop    ->  while '(' rexp ')' cmpdstmt  
 rexp    ->  expr (< | > | =) expr
 expr    ->  term   [ (+ | -) expr ]
 term    ->  factor [ (* | /) term ]
 factor  ->  int_lit | id | '(' expr ')'
 
 Lexical:   id is a single character; 
 int_lit is an unsigned integer;
 equality operator is =, not ==

 Sample Program: Factorial
 
 int n, i, f;
 n = 4;
 i = 1;
 f = 1;
 while (i < n) {
 i = i + 1;
 f= f * i;
 }
 end

 Sample Program:  GCD
   
 int x, y;
 x = 121;
 y = 132;
 while (x != y) {
 if (x > y) 
 { x = x - y; }
 else { y = y - x; }
 }
 end

 */
public class Parser {

    public static void main(String[] args) {
    	Code.initialise();
        System.out.println("Enter program and terminate with 'end'!\n");
        Lexer.lex();
        new Program();
        Code.output();
    }
}

class Program {

    Decls decls;
    Stmts stmts;

    public Program() { //program -> decls stmts end Note: skip end
        decls = new Decls();
        //Lexer.lex();
        stmts = new Stmts();
        
        Code.gen("return"); // END of program
        /* TODO: Check with TA which approach is to be used. If this how do we attain recurrsion in Stmts?
         do {
         stmts = new Stmts();
         } while(Lexer.nextToken != Token.KEY_END);
         */
    }

}

class Decls {

    Idlist idlist;

    public Decls() {
        if (Lexer.nextToken == Token.KEY_INT) {
            Lexer.lex(); //This eats up the literal word 'int'
            idlist = new Idlist();
            if (Lexer.nextToken == Token.SEMICOLON) {
                Lexer.lex(); //Eats up the semi colon after idlist
            }
        } //There is no need of else as the program is syntactically correct

    }

}

class Idlist {

    public static ArrayList<Character> ids = new ArrayList<Character>(); //TODO: check if this implementation is okay with Prof and TA

    public Idlist() {
        do {
            if (Lexer.nextToken == Token.COMMA) //This is to consume the id if there is more than one.
            {
                Lexer.lex();
            }

            ids.add(Lexer.ident);
            Lexer.lex();            
        } while (Lexer.nextToken == Token.COMMA);

    }

}

class Stmt { // stmt    ->  assign | cond | loop
    Assign asgn;
    Loop loop;
    Cond cond;

    public Stmt() {

        switch (Lexer.nextToken) {
            case Token.ID: //assign
                asgn = new Assign();
                break;
            case Token.KEY_WHILE: // loop
                loop = new Loop();
                break;
            case Token.KEY_IF: // cond
                cond = new Cond();
                break;
            default:
                break;
        }

    }

}

class Stmts {
    Stmt stmt;
    Stmts stmts;

    public Stmts() { // stmts -> stmt stmts | stmt
        stmt = new Stmt();
        if (!(Lexer.nextToken == Token.KEY_END || Lexer.nextToken == Token.RIGHT_BRACE)) { //TODO: Confirm if the END check can be done here or should be done only at Program? Note: The RIGHT_BRACE check remains unaffected
//            Lexer.lex();
            stmts = new Stmts();
        }
    }
}

class Assign {
    char id;
    Expr e;
    
    public Assign() { //Expects the pointer to be on <id>/<variable> Ex: 'a'
        id = Lexer.ident;
        Lexer.lex();
        if(Lexer.nextToken == Token.ASSIGN_OP){ //This if is not even required assuming the program is syntactically correct.
            Lexer.lex();
            e = new Expr();
//            Lexer.lex();
            if(Lexer.nextToken == Token.SEMICOLON){//This if is not even required assuming the program is syntactically correct. 
            	Code.gen("istore_"+ Idlist.ids.indexOf(id));
                Lexer.lex(); //eats up the semicolon.
            }
        }
    }

}

class Cond {
    Rexpr rexpr;
    Cmpdstmt cmpdstmt_if;
    Cmpdstmt cmpdstmt_else;
    int ifCondtnStatementStartPtr;
    int ifFailsNextInstruction;
    int elseCondtnStatementStartPtr;
    int elseConditionStatementEndPtr;
    
    public Cond() { //Expects the pointer to be on 'if' keyword
                    //On completion it will eat out till the last '}' of if/else based on whichever is present
        Lexer.lex(); //eats up the 'if' keyword
        //if(Lexer.nextToken == Token.LEFT_PAREN) { //Not req. assum. its synt. correct
        Lexer.lex(); //eats up '('
        rexpr = new Rexpr(); //Assumes Rexpr doesnt eat up ')'
        Lexer.lex(); //eats up ')'
        
        ifCondtnStatementStartPtr = Code.codeptr - 3;// Has to mention the end of if-block machine instruction number         
        //if(Lexer.nextToken == Token.LEFT_BRACE){ //Not req. assum. its synt. correct
        cmpdstmt_if = new Cmpdstmt();                             
        
        //After Cmpdstmt the pointer will be at 'else' (if 'else' is present)
        if(Lexer.nextToken == Token.KEY_ELSE){ //This if check is essential as 'else' is optional in grammar         	                	
        	elseCondtnStatementStartPtr = Code.codeptr;
        	Code.gen("goto");
        	Code.gen("");
        	Code.gen("");
        	
        	ifFailsNextInstruction = Code.codeptr;
        	
            Lexer.lex(); //eats up the 'else' keyword
            cmpdstmt_else = new Cmpdstmt();
            
        	elseConditionStatementEndPtr = Code.codeptr; 
            Code.setConditionFail(elseCondtnStatementStartPtr, elseConditionStatementEndPtr);
        }        
        
        else {
        	ifFailsNextInstruction = Code.codeptr;
        }
        
        Code.setConditionFail(ifCondtnStatementStartPtr, ifFailsNextInstruction);//Handles if control doesnt enter if block.
    }
}

class Loop { //Expects the pointer to be on 'while' keyword //loop    ->  while '(' rexp ')' cmpdstmt 
    Rexpr rexpr;
    Cmpdstmt cmpdstmt;
    int whileLoopStartPtr;
    int conditionPosition;
    int whileLoopEndPtr;
    
    public Loop() {
    	whileLoopStartPtr = Code.codeptr; 
    	

    	
    	Lexer.lex(); //eats up the 'while' keyword
        if(Lexer.nextToken == Token.LEFT_PAREN) { //Not req. assum. its synt. correct
            Lexer.lex(); //eats up '('
            rexpr = new Rexpr(); //Assumes Rexpr doesnt eat up ')'
            Lexer.lex(); //eats up ')' 
            
            conditionPosition = Code.codeptr - 3;
            
            if(Lexer.nextToken == Token.LEFT_BRACE){ //TODO: Check with TA if this check can be done here or it is necessary for the Cmpdstmt class to check this?
                cmpdstmt = new Cmpdstmt();
            }            
        }
        
     // conditional operators take three bytes in total. Thats the reason for two more extra empty bytes generated here
    Code.gen("goto "+whileLoopStartPtr);
    Code.gen("");
    Code.gen("");
    whileLoopEndPtr = Code.codeptr;
    //function which sets the point where loop started to points to the end of the loop incase of loop condition fails
    Code.setConditionFail (conditionPosition, whileLoopEndPtr);
    }      
}

class Cmpdstmt { //Expects the pointer to be on '{'
                 //While complete the pointer will be on the token next to '}' i.e Cmpdstmt will eat up '}'
                 //TODO: Confirm with TA if the pointer can start at '{' or should it be just b4 '{' and this class should move the pointer?
    Stmts stmts;
    public Cmpdstmt() {
        Lexer.lex(); //eats up the '{'
        stmts = new Stmts();
        Lexer.lex(); //eats up the '}'
    }

}

class Rexpr {
    Expr expr_left;
    Expr expr_right;
    int comparison_op; //Its kept as int purposefully, applying this to the toString of Token the symbol can easily be found  
    
    public Rexpr() {////Expects the pointer to be on left Expression
        expr_left = new Expr();
        //Lexer.lex();
        comparison_op = Lexer.nextToken;
        Lexer.lex();
        expr_right = new Expr();
        
        // conditional operators take three bytes in total. Thats the reason for two more extra empty bytes generated here
        Code.gen(Code.opcode(Code.comparisonOperators.get(comparison_op)));        
        Code.gen("");         
        Code.gen("");       
    }   
}

class Expr {
    Term t;
    Expr e;
    char op;

    public Expr() { // expr -> term (+ | -) expr | term

        t = new Term();
        if (Lexer.nextToken == Token.ADD_OP || Lexer.nextToken == Token.SUB_OP) {
            op = Lexer.nextChar;
            Lexer.lex();
            e = new Expr();
			Code.gen(Code.opcode(op));
        }

    }

}

class Term {
    Factor f;
    Term t;
    char op;

    public Term() { // term -> factor (* | /) term | factor
        f = new Factor();
        if (Lexer.nextToken == Token.MULT_OP || Lexer.nextToken == Token.DIV_OP) {
            op = Lexer.nextChar;
            Lexer.lex();
            t = new Term();
			Code.gen(Code.opcode(op));
        }
    }

}

class Factor {
    Expr e;
    int i;
    char id;

    public Factor() { // factor -> number | '(' expr ')'
        switch (Lexer.nextToken) {
            case Token.INT_LIT: // number
                i = Lexer.intValue;
                Lexer.lex(); //TODO: Confirm with TA as this is Prof code but Sankar commented it.
                
                String intLitRepresentation = (i < 6) ? "iconst_" :  i < 128 ? "bipush " : "sipush "; 
                Code.gen(intLitRepresentation + i);
              
                switch(intLitRepresentation)
                {
                case "bipush ":
                	Code.gen("");
                	break;
                case "sipush ":
                	Code.gen("");
                	Code.gen("");
                	break;
                }
                                
                break;
            case Token.ID: // id
                id = Lexer.ident;
                Lexer.lex();
                Code.gen("iload_" + Idlist.ids.indexOf(id));//shiva
                break;    
            case Token.LEFT_PAREN: // '('
                Lexer.lex();
                e = new Expr();
                Lexer.lex(); // skip over ')'
                break;
            default:
                break;
        }
    }

}

class Code {
	static String[] code = new String[100];
	static int codeptr = 0;
    static HashMap<Integer,Character> comparisonOperators = new HashMap<Integer,Character>();
    
    static void setConditionFail(int conditionStartPosition, int whileLoopEndPtr)
    {
    	code[conditionStartPosition] = code[conditionStartPosition] +" "+ String.valueOf(whileLoopEndPtr);
    }
    
    
    static void initialise()
    {
    	comparisonOperators.put(Token.GREATER_OP,'>');
    	comparisonOperators.put(Token.LESSER_OP,'<');
    	comparisonOperators.put(Token.NOT_EQ, '!');
    	comparisonOperators.put(Token.ASSIGN_OP, '=');    	    	
    }
    
	public static void gen(String s) {
		code[codeptr] = s;
		codeptr++;
	}
	
	public static String opcode(char op) {
		switch(op) {
		case '+' : return "iadd";
		case '-':  return "isub";
		case '*':  return "imul";
		case '/':  return "idiv";
		case '<':  return "if_icmpge";
		case '>':  return "if_icmple";
		case '=':  return "if_icmpne";
		case '!': return "if_icmpeq";
		default: return "";		
		}
	}
	
	public static void output() {
		for (int i=0; i<codeptr; i++)
		{
			if(code[i]=="") continue; // to handle (rExp, loop ,condition) which requires skipping two extra bytes of code
			System.out.println(i + ": " + code[i]);
		}
	}
}

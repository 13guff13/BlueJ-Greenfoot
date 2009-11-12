/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009  Michael Kolling and John Rosenberg 
 
 This program is free software; you can redistribute it and/or 
 modify it under the terms of the GNU General Public License 
 as published by the Free Software Foundation; either version 2 
 of the License, or (at your option) any later version. 
 
 This program is distributed in the hope that it will be useful, 
 but WITHOUT ANY WARRANTY; without even the implied warranty of 
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 GNU General Public License for more details. 
 
 You should have received a copy of the GNU General Public License 
 along with this program; if not, write to the Free Software 
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA. 
 
 This file is subject to the Classpath exception as provided in the  
 LICENSE.txt file that accompanied this code.
 */
package bluej.parser;

import java.io.Reader;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import bluej.parser.ast.LocatableToken;
import bluej.parser.ast.gen.BlueJJavaLexer;
import bluej.parser.ast.gen.JavaTokenTypes;

/**
 * Base class for Java parsers.
 * 
 * @author Davin McCall
 */
public class NewParser
{
    protected JavaTokenFilter tokenStream;
    //protected JavaLexer lexer;

    public static TokenStream getLexer(Reader r)
    {
        //EscapedUnicodeReader euReader = new EscapedUnicodeReader(r);
        //JavaLexer lexer = new JavaLexer(euReader);
        BlueJJavaLexer lexer = new BlueJJavaLexer(r);
        //lexer.setTokenObjectClass("bluej.parser.ast.LocatableToken");
        lexer.setTabSize(1);
        //euReader.setAttachedScanner(lexer);
        return lexer;
    }
    
    public NewParser(Reader r)
    {
        TokenStream lexer = getLexer(r);
        tokenStream = new JavaTokenFilter(lexer, this);
    }

    /**
     * An error occurred during parsing. Override this method to control error behaviour.
     * @param msg A message describing the error
     */
    protected void error(String msg)
    {
        //throw new RuntimeException("Parse error: (" + lexer.getLine() + ":" + lexer.getColumn() + ") " + msg);
        throw new RuntimeException("Parse error: " + msg);
    }

    /**
     * Parse a compilation unit (from the beginning).
     */
    public void parseCU()
    {
        parseCU(0);
    }
	
    protected void beginPackageStatement(LocatableToken token) {  }

    /** We have the package name for this source */
    protected void gotPackage(List<LocatableToken> pkgTokens) { }

    /** We've seen the semicolon at the end of a "package" statement. */
    protected void gotPackageSemi(LocatableToken token) { }

    /** Beginning of some arbitrary grammatical element */
    protected void beginElement(LocatableToken token) { }

    /** End of some arbitrary grammatical element.
     * 
     * @param token  The end token 
     * @param included  True if the end token is part of the element; false if it is part of the next element.
     */
    protected void endElement(LocatableToken token, boolean included) { }

    /**
     * Got the beginning (opening brace) of a method or constructor body.
     */
    protected void beginMethodBody(LocatableToken token) { }
    
    /**
     * End of a method or constructor body reached.
     */
    protected void endMethodBody(LocatableToken token, boolean included) { }
    
    protected void endMethodDecl(LocatableToken token, boolean included)
    {
        endElement(token, included);
    }
        
    /** reached a compilation unit state */
    protected void reachedCUstate(int i) { }

    /** We've seen the semicolon at the end of an "import" statement */
    protected void gotImportStmtSemi(LocatableToken token) {
        endElement(token, true);
    }

    protected void beginForLoop(LocatableToken token) { }
    
    protected void beginForLoopBody(LocatableToken token) { }
    
    protected void endForLoopBody(LocatableToken token, boolean included) { }

    protected void endForLoop(LocatableToken token, boolean included) { }
    
    protected void beginWhileLoop(LocatableToken token) { }

    protected void beginWhileLoopBody(LocatableToken token) { }

    protected void endWhileLoopBody(LocatableToken token, boolean included) { }
    
    protected void endWhileLoop(LocatableToken token, boolean included) { }
    
    protected void beginIfStmt(LocatableToken token) { }
    
    protected void beginIfCondBlock(LocatableToken token) { }
    
    protected void endIfCondBlock(LocatableToken token, boolean included) { }
    
    protected void endIfStmt(LocatableToken token, boolean included) { }
    
    protected void beginDoWhile(LocatableToken token) { }
    
    protected void beginDoWhileBody(LocatableToken token) { }
    
    protected void endDoWhileBody(LocatableToken token, boolean included) { }
    
    protected void endDoWhile(LocatableToken token, boolean included) { }
    
    protected void beginTryCatchSmt(LocatableToken token) { }
    
    protected void beginTryBlock(LocatableToken token) { }
    
    protected void endTryBlock(LocatableToken token, boolean included) { }
    
    protected void endTryCatchStmt(LocatableToken token, boolean included) { }
    
    /** A list of a parameters to a method or constructor */
    protected void beginArgumentList(LocatableToken token) { }
    
    /** An individual argument has ended */
    protected void endArgument() { }
    
    /** The end of the argument list has been reached. */
    protected void endArgumentList(LocatableToken token) { }
    
    /**
     * got a "new ..." expression. Will be followed by a type spec (gotTypeSpec())
     * and possibly by array size declarations, then endExprNew()
     */
    protected void gotExprNew(LocatableToken token) { }
    
    protected void endExprNew(LocatableToken token, boolean included) { }
    
    /**
     * Beginning of a statement block. This includes anonymous statement blocks, and static
     * initializer blocks
     */
    protected void beginStmtblockBody(LocatableToken token)
    {
        beginElement(token);
    }
    
    protected void endStmtblockBody(LocatableToken token, boolean included)
    {
        endElement(token, included);
    }
    
    /**
     * Begin a (possibly static) initialisation block.
     * @param first   The first token (should be either "static" or the "{")
     * @param lcurly  The "{" token which opens the block body
     */
    protected void beginInitBlock(LocatableToken first, LocatableToken lcurly) { }
    
    /**
     * End of a (possibly static) initialisation block
     * @param rcurly    The last token (should be "}")
     * @param included  True if the last token is actually a "}"
     */
    protected void endInitBlock(LocatableToken rcurly, boolean included) { }

    /** Begin the type definition body. */
    protected void beginTypeBody(LocatableToken leftCurlyToken) { }
    
    /** End of type definition body. This should be a '}' unless an error occurred */
    protected void endTypeBody(LocatableToken endCurlyToken, boolean included) { }
    
    protected void gotTypeDefEnd(LocatableToken token, boolean included)
    {
        endElement(token, included);
    }
    
    /**
     * Got a field (inside a type definition). The beginning is marked by the previous beginElement().
     * @param idToken   The token with the name of the field.
     */
    protected void gotField(LocatableToken idToken) { }

    protected void endField(LocatableToken token, boolean included)
    {
        endElement(token, included);
    }

    /** We've seen a type specification or something that looks a lot like one. */
    protected void gotTypeSpec(List<LocatableToken> tokens) { }

    /** Seen a type cast operator. The tokens list contains the type to which is cast. */
    protected void gotTypeCast(List<LocatableToken> tokens)
    {
        gotTypeSpec(tokens);
    }
    
    protected void beginExpression(LocatableToken token) { }
    
    /** Saw a literal as part of an expression */
    protected void gotLiteral(LocatableToken token) { }
    
    /** Saw a binary operator as part of an expression */
    protected void gotBinaryOperator(LocatableToken token) { }
    
    /**
     * Check whether a particular token is a type declaration initiator, i.e "class", "interface"
     * or "enum"
     */
    public boolean isTypeDeclarator(LocatableToken token)
    {
        return token.getType() == JavaTokenTypes.LITERAL_class
        || token.getType() == JavaTokenTypes.LITERAL_enum
        || token.getType() == JavaTokenTypes.LITERAL_interface;
    }

    /**
     * Check whether a token is a primitive type - "int" "float" etc
     */
    public static boolean isPrimitiveType(LocatableToken token)
    {
        return token.getType() == JavaTokenTypes.LITERAL_void
        || token.getType() == JavaTokenTypes.LITERAL_boolean
        || token.getType() == JavaTokenTypes.LITERAL_byte
        || token.getType() == JavaTokenTypes.LITERAL_char
        || token.getType() == JavaTokenTypes.LITERAL_short
        || token.getType() == JavaTokenTypes.LITERAL_int
        || token.getType() == JavaTokenTypes.LITERAL_long
        || token.getType() == JavaTokenTypes.LITERAL_float
        || token.getType() == JavaTokenTypes.LITERAL_double;
    }

    public static int TYPEDEF_CLASS = 0;
    public static int TYPEDEF_INTERFACE = 1;
    public static int TYPEDEF_ENUM = 2;
    public static int TYPEDEF_ANNOTATION=3;

    /**
     * Parse a compilation unit.
     * @param state  The current parse state
     */
    public void parseCU(int state)
    {
        while (true) {              // optional: package statement
            LocatableToken token = tokenStream.nextToken();
            if (token.getType() == JavaTokenTypes.LITERAL_package) {
                beginPackageStatement(token);
                token = tokenStream.nextToken();
                List<LocatableToken> pkgTokens = parseDottedIdent(token);
                gotPackage(pkgTokens);
                token = tokenStream.nextToken();
                if (token.getType() != JavaTokenTypes.SEMI) {
                    error("Expecting ';' at end of package declaration");
                    tokenStream.pushBack(token);
                }
                else {
                    gotPackageSemi(token);
                }
                reachedCUstate(1); state = 1;
            }
            else if (token.getType() == JavaTokenTypes.LITERAL_import) {
                beginElement(token);
                token = tokenStream.nextToken();
                parseDottedIdent(token);
                if (tokenStream.LA(1).getType() == JavaTokenTypes.DOT) {
                    tokenStream.nextToken();
                    token = tokenStream.nextToken();
                    if (token.getType() == JavaTokenTypes.SEMI) {
                        error("Trailing '.' in import statement");
                    }
                    else if (token.getType() == JavaTokenTypes.STAR) {
                        token = tokenStream.nextToken();
                        if (token.getType() != JavaTokenTypes.SEMI) {
                            error("Expected ';' following import statement");
                            tokenStream.pushBack(token);
                        }
                        else {
                            gotImportStmtSemi(token);
                        }
                    }
                    else {
                        error("Expected package/class identifier, or '*', in import statement.");
                        if (tokenStream.LA(1).getType() == JavaTokenTypes.SEMI) {
                            tokenStream.nextToken();
                        }
                    }
                }
                else {
                    token = tokenStream.nextToken();
                    if (token.getType() != JavaTokenTypes.SEMI) {
                        error("Expected ';' following import statement");
                        tokenStream.pushBack(token);
                    }
                    else {
                        gotImportStmtSemi(token);
                    }
                }
            }
            else if (isModifier(token) || isTypeDeclarator(token)) {
                // optional: class/interface/enum
                beginElement(token);
                tokenStream.pushBack(token);
                parseTypeDef();
                reachedCUstate(2); state = 2;
            }
            else if (token.getType() == JavaTokenTypes.EOF) {
                break;
            }
            else {
                // TODO give different diagnostic depending on state
                error("Expected: Type definition (class, interface or enum)");
            }
        }
    }
    
    /**
     * Parse a type definition (class, interface, enum).
     */
    public void parseTypeDef()
    {
        // possibly, modifiers: [public|private|protected] [static]
        parseModifiers();
        LocatableToken token = tokenStream.nextToken();					

        boolean isAnnotation = token.getType() == JavaTokenTypes.AT;
        if (isAnnotation) {
            token = tokenStream.nextToken();
        }

        // [class|interface|enum]						
        if (isTypeDeclarator(token)) {
            int tdType = -1;
            String typeDesc;			
            if (token.getType() == JavaTokenTypes.LITERAL_class) {
                typeDesc = "class";
                tdType = TYPEDEF_CLASS;
            }
            else if (token.getType() == JavaTokenTypes.LITERAL_interface) {
                typeDesc = "interface";
                tdType = TYPEDEF_INTERFACE;
                //check for annotation type
                if(isAnnotation) {
                    tdType = TYPEDEF_ANNOTATION;						 
                }
            }
            else {
                typeDesc = "enum";
                tdType = TYPEDEF_ENUM;
            }

            gotTypeDef(tdType);

            // Class name
            token = tokenStream.nextToken();
            if (token.getType() != JavaTokenTypes.IDENT) {
                error("Expected identifier (in " + typeDesc + " definition)");
                tokenStream.pushBack(token);
                return;
            }
            gotTypeDefName(token);

            // template arguments
            token = tokenStream.nextToken();
            if (token.getType() == JavaTokenTypes.LT) {
                parseTemplateParams();
                token = tokenStream.nextToken();
            }

            // extends...
            if (token.getType() == JavaTokenTypes.LITERAL_extends) {
                gotTypeDefExtends(token);
                parseTypeSpec();
                token = tokenStream.nextToken();
            }

            // implements...
            if (token.getType() == JavaTokenTypes.LITERAL_implements) {
                gotTypeDefImplements(token);
                parseTypeSpec();
                token = tokenStream.nextToken();
                while (token.getType() == JavaTokenTypes.COMMA) {
                    parseTypeSpec();
                    token = tokenStream.nextToken();
                }
            }

            // Body!
            if (token.getType() != JavaTokenTypes.LCURLY) {
                error("Expected '{' (in class definition)");
                tokenStream.pushBack(token);
                gotTypeDefEnd(token, false);
                return;
            }

            beginTypeBody(token);

            if (tdType == TYPEDEF_ENUM) {
                parseEnumConstants();
            }

            if (tdType == TYPEDEF_ANNOTATION) {
                parseAnnotationBody();
            }
            else { 
                parseClassBody();
            }

            token = tokenStream.nextToken();
            if (token.getType() != JavaTokenTypes.RCURLY) {
                error("Expected '}' (in class definition)");
                endTypeBody(token, false);
                gotTypeDefEnd(token, false);
                return;
            }
            endTypeBody(token, true);
            gotTypeDefEnd(token, true);
        }
        else {
            error("Expected type declarator: 'class', 'interface', or 'enum'");
        }
    }
	
    public void parseEnumConstants()
    {
        LocatableToken token = tokenStream.nextToken();
        while (token.getType() == JavaTokenTypes.IDENT) {
            // The identifier is the constant name - there may be constructor arguments as well
            token = tokenStream.nextToken();
            if (token.getType() == JavaTokenTypes.LPAREN) {
                parseArgumentList(token);
                token = tokenStream.nextToken();
                if (token.getType() != JavaTokenTypes.RPAREN) {
                    error("Expecting ')' at end of enum constant constructor arguments");
                    if (token.getType() != JavaTokenTypes.COMMA
                            && token.getType() != JavaTokenTypes.SEMI) {
                        tokenStream.pushBack(token);
                        return;
                    }
                }
                else {
                    token = tokenStream.nextToken();
                }
            }

            if (token.getType() == JavaTokenTypes.SEMI) {
                return;
            }

            if (token.getType() == JavaTokenTypes.RCURLY) {
                // This is valid
                tokenStream.pushBack(token);
                return;
            }

            if (token.getType() != JavaTokenTypes.COMMA) {
                error("Expecting ',' or ';' after enum constant declaration");
                tokenStream.pushBack(token);
                return;
            }
            token = tokenStream.nextToken();
        }
    }
	
    // Parse template parameters. The '<' should have been read already.
    public void parseTemplateParams()
    {
        DepthRef dr = new DepthRef();
        dr.depth = 1;

        while (true) {
            LocatableToken token = tokenStream.nextToken();
            if (token.getType() != JavaTokenTypes.IDENT) {
                error("Expected identifier (in type parameter list)");
                tokenStream.pushBack(token);
                return;
            }

            token = tokenStream.nextToken();
            if (token.getType() == JavaTokenTypes.LITERAL_extends) {
                do {
                    parseTargType(false, new LinkedList<LocatableToken>(), dr);
                    if (dr.depth <= 0) {
                        return;
                    }
                    token = tokenStream.nextToken();
                } while (token.getType() == JavaTokenTypes.BAND);
            }

            if (token.getType() != JavaTokenTypes.COMMA) {
                if (token.getType() != JavaTokenTypes.GT) {
                    error("Expecting '>' at end of type parameter list");
                    tokenStream.pushBack(token);
                }
                break;
            }
        }
    }

    /**
     * Called when the current element is recognised as a type definition.
     * @param tdType  one of TYPEDEF_CLASS, _INTERFACE, _ANNOTATION or _ENUM
     */
    protected void gotTypeDef(int tdType) { }

    /** Called when we have the identifier token for a class/interface/enum definition */
    protected void gotTypeDefName(LocatableToken nameToken) { }

    /** Called when we have seen the "extends" literal token */
    protected void gotTypeDefExtends(LocatableToken extendsToken) { }

    /** Called when we have seen the "implements" literal token */
    protected void gotTypeDefImplements(LocatableToken implementsToken) { }

    /**
     * Check whether a token represents a modifier (or an "at" symbol,
     * denoting an annotation).
     */
    public static boolean isModifier(LocatableToken token)
    {
        int tokType = token.getType();
        return (tokType == JavaTokenTypes.LITERAL_public
                || tokType == JavaTokenTypes.LITERAL_private
                || tokType == JavaTokenTypes.LITERAL_protected
                || tokType == JavaTokenTypes.ABSTRACT
                || tokType == JavaTokenTypes.FINAL
                || tokType == JavaTokenTypes.LITERAL_static
                || tokType == JavaTokenTypes.LITERAL_volatile
                || tokType == JavaTokenTypes.LITERAL_native
                || tokType == JavaTokenTypes.STRICTFP
                || tokType == JavaTokenTypes.AT);
    }

    /**
     * Parse a modifier list (and return all modifier tokens in a list)
     */
    public List<LocatableToken> parseModifiers()
    {
        List<LocatableToken> rval = new LinkedList<LocatableToken>();
        
        LocatableToken token = tokenStream.nextToken();
        while (isModifier(token)) {
            if (token.getType()==JavaTokenTypes.AT) {
                if( tokenStream.LA(1).getType() != JavaTokenTypes.LITERAL_interface) {					
                    parseAnnotation();
                }
                else {
                    tokenStream.pushBack(token);
                    return rval;
                }
            }
            rval.add(token);
            token = tokenStream.nextToken();
        }			
        tokenStream.pushBack(token);
        
        return rval;
    }
	
    public void parseClassBody()
    {
        LocatableToken token = tokenStream.nextToken();
        while (token.getType() != JavaTokenTypes.RCURLY) {
            if (token.getType() == JavaTokenTypes.EOF) {
                error("Unexpected end-of-file in type body; missing '}'");
                return;
            }

            beginElement(token);
            tokenStream.pushBack(token);
            LocatableToken hiddenToken = (LocatableToken) token.getHiddenBefore();
            // field declaration, method declaration, inner class
            List<LocatableToken> modifiers = parseModifiers();
            token = tokenStream.nextToken();
            if (token.getType() == JavaTokenTypes.LITERAL_class
                    || token.getType() == JavaTokenTypes.LITERAL_interface
                    || token.getType() == JavaTokenTypes.LITERAL_enum
                    || token.getType() == JavaTokenTypes.AT) {
                tokenStream.pushBack(token);
                pushBackAll(modifiers);
                parseTypeDef();
            }
            else {
                // Not an inner type: should be a method/constructor or field,
                // or (possibly static) a initialisation block
                if (token.getType() == JavaTokenTypes.SEMI) {
                    // A spurious semicolon.
                    endElement(token, true);
                }
                else if (token.getType() == JavaTokenTypes.LCURLY) {
                    // initialisation block
                    LocatableToken firstToken = token;
                    if (! modifiers.isEmpty()) {
                        firstToken = modifiers.get(0);
                    }
                    beginInitBlock(firstToken, token);
                    parseStmtBlock();
                    token = tokenStream.nextToken();
                    if (token.getType() != JavaTokenTypes.RCURLY) {
                        error("Expecting '}' (at end of initialisation block)");
                        tokenStream.pushBack(token);
                        endInitBlock(token, false);
                        endElement(token, false);
                    }
                    else {
                        endInitBlock(token, true);
                        endElement(token, true);
                    }
                }
                else if (token.getType() == JavaTokenTypes.IDENT
                        && tokenStream.LA(1).getType() == JavaTokenTypes.LPAREN) {
                    // constructor
                    gotConstructorDecl(token, hiddenToken);
                    tokenStream.nextToken();
                    parseMethodParamsBody();
                }
                else if (token.getType() == JavaTokenTypes.LT
                        || token.getType() == JavaTokenTypes.IDENT
                        || isPrimitiveType(token)) {
                    // method, field
                    if (token.getType() == JavaTokenTypes.LT) {
                        // generic method
                        parseTemplateParams();
                    }
                    else {
                        tokenStream.pushBack(token);
                    }
                    parseTypeSpec();
                    LocatableToken idToken = tokenStream.nextToken(); // identifier
                    if (idToken.getType() != JavaTokenTypes.IDENT) {
                        error("Expected identifier (method or field name).");
                        token = idToken;
                        continue;
                    }
                    parseArrayDeclarators();

                    token = tokenStream.nextToken();
                    if (token.getType() == JavaTokenTypes.SEMI) {
                        // field declaration: done
                        gotField(idToken);
                        endField(token, true);
                        token = tokenStream.nextToken();
                        continue;
                    }
                    else if (token.getType() == JavaTokenTypes.ASSIGN) {
                        // field declaration
                        gotField(idToken);
                        parseExpression();
                        parseSubsequentDeclarations(true);
                        token = tokenStream.nextToken();
                        continue;
                    }
                    else if (token.getType() == JavaTokenTypes.LPAREN) {
                        // method declaration
                        gotMethodDeclaration(idToken, hiddenToken);
                        parseMethodParamsBody();
                    }
                    else if (token.getType() == JavaTokenTypes.COMMA) {
                        tokenStream.pushBack(token);
                        parseSubsequentDeclarations(true);
                    }
                    else {
                        error("Expected ';' or '=' or '(' (in field or method declaration), got token type: " + token.getType());
                        tokenStream.pushBack(token);
                        endElement(token, false);
                    }
                }
                else {
                    error("Unexpected token \"" + token.getText() + "\" in type declaration body");
                }
            }
            token = tokenStream.nextToken();
        }
        tokenStream.pushBack(token);
    }

    protected void parseArrayDeclarators()
    {
        if (tokenStream.LA(1).getType() != JavaTokenTypes.LBRACK) {
            return;
        }

        LocatableToken token = tokenStream.nextToken();
        while (token.getType() == JavaTokenTypes.LBRACK) {
            token = tokenStream.nextToken();
            if (token.getType() != JavaTokenTypes.RBRACK) {
                error("Expecting ']' (to match '[')");
                if (tokenStream.LA(1).getType() == JavaTokenTypes.RBRACK) {
                    // Try and recover
                    token = tokenStream.nextToken(); // ']'
                }
                else {
                    tokenStream.pushBack(token);
                    return;
                }
            }
            token = tokenStream.nextToken();
        }
        tokenStream.pushBack(token);
    }
	
    /**
     * We've got the return type, name, and opening parenthesis of a method/constructor
     * declaration. Parse the rest.
     */
    public void parseMethodParamsBody()
    {
        parseParameterList();
        gotAllMethodParameters();
        LocatableToken token = tokenStream.nextToken();
        if (token.getType() != JavaTokenTypes.RPAREN) {
            error("Expected ')' at end of parameter list (in method declaration)");
            tokenStream.pushBack(token);
            endMethodDecl(token, false);
            return;
        }
        token = tokenStream.nextToken();
        if (token.getType() == JavaTokenTypes.LITERAL_throws) {
            do {
                parseTypeSpec();
                token = tokenStream.nextToken();
            } while (token.getType() == JavaTokenTypes.COMMA);
        }
        if (token.getType() == JavaTokenTypes.LCURLY) {
            // method body
            beginMethodBody(token);
            parseStmtBlock();
            token = tokenStream.nextToken();
            if (token.getType() != JavaTokenTypes.RCURLY) {
                error("Expected '}' at end of method body");
                tokenStream.pushBack(token);
                endMethodBody(token, false);
                endMethodDecl(token, false);
            }
            else {
                endMethodBody(token, true);
                endMethodDecl(token, true);
            }
        }
        else if (token.getType() != JavaTokenTypes.SEMI) {
            error("Expected ';' or '{' following parameter list in method declaration");
            tokenStream.pushBack(token);
            endMethodDecl(token, false);
        }
        else {
            endMethodDecl(token, true);
        }
    }
	
    /**
     * Parse a statement block - such as a method body
     */
    public void parseStmtBlock()
    {
        while(true) {
            LocatableToken token = tokenStream.nextToken();
            if (token.getType() == JavaTokenTypes.EOF
                    || token.getType() == JavaTokenTypes.RCURLY
                    || token.getType() == JavaTokenTypes.RPAREN) {
                tokenStream.pushBack(token);
                return;
            }
            beginElement(token);
            LocatableToken ntoken = parseStatement(token);
            if (ntoken != null) {
                endElement(ntoken, true);
            }
            else {
                ntoken = tokenStream.LA(1);
                endElement(tokenStream.LA(1), false);
                if (ntoken == token) {
                    break; // we're not getting anywhere - time to bail
                    // TODO we can just skip the token and keep processing, but we should be
                    // context aware. For instance if token is "catch" and we are in a try block,
                    // should bail out altogether now so that processing can continue upstream.
                }
            }
        }
    }

    public void parseStatement()
    {
        parseStatement(tokenStream.nextToken());
    }

    /**
     * Parse a statement. Return the last token that is part of the statement (i.e the ';' or '{'
     * terminator), or null if an error was encountered.
     * @param token  The first token of the statement
     */
    public LocatableToken parseStatement(LocatableToken token)
    {
        if (token.getType() == JavaTokenTypes.SEMI) {
            return token; // empty statement
        }
        else if (token.getType() == JavaTokenTypes.LITERAL_return) {
            token = tokenStream.nextToken();
            if (token.getType() != JavaTokenTypes.SEMI) {
                tokenStream.pushBack(token);
                parseExpression();
                token = tokenStream.nextToken();
            }
            if (token.getType() != JavaTokenTypes.SEMI) {
                error("Expecting ';' after 'return' statement");
                tokenStream.pushBack(token);
                return null;
            }
            return token;
        }
        else if (token.getType() == JavaTokenTypes.LITERAL_for) {
            return parseForStatement(token);
        }
        else if (token.getType() == JavaTokenTypes.LITERAL_while) {
            return parseWhileStatement(token);
        }
        else if (token.getType() == JavaTokenTypes.LITERAL_if) {
            return parseIfStatement(token);
        }
        else if (token.getType() == JavaTokenTypes.LITERAL_do) {
            return parseDoWhileStatement(token);
        }
        else if (token.getType() == JavaTokenTypes.LITERAL_assert) {
            return parseAssertStatement(token);
        }
        else if (token.getType() == JavaTokenTypes.LITERAL_switch) {
            return parseSwitchStatement(token);
        }
        else if (token.getType() == JavaTokenTypes.LITERAL_case) {
            parseExpression();
            token = tokenStream.nextToken();
            if (token.getType() != JavaTokenTypes.COLON) {
                error("Expecting ':' at end of case expression");
                tokenStream.pushBack(token);
                return null;
            }
            return token;
        }
        else if (token.getType() == JavaTokenTypes.LITERAL_default) {
            token = tokenStream.nextToken();
            if (token.getType() != JavaTokenTypes.COLON) {
                error("Expecting ':' at end of case expression");
                tokenStream.pushBack(token);
                return null;
            }
            return token;
        }
        else if (token.getType() == JavaTokenTypes.LITERAL_continue
                || token.getType() == JavaTokenTypes.LITERAL_break) {
            // There might be a label afterwards
            token = tokenStream.nextToken();
            if (token.getType() == JavaTokenTypes.IDENT) {
                token = tokenStream.nextToken();
            }
            if (token.getType() != JavaTokenTypes.SEMI) {
                error("Expecting ';' at end of " + token.getText() + " statement");
                tokenStream.pushBack(token);
                return null;
            }
            return token;
        }
        else if (token.getType() == JavaTokenTypes.LITERAL_throw) {
            parseExpression();
            token = tokenStream.nextToken();
            if (token.getType() != JavaTokenTypes.SEMI) {
                error("Expecting ';' at end of 'throw' statement");
                tokenStream.pushBack(token);
                return null;
            }
            return token;
        }
        else if (token.getType() == JavaTokenTypes.LITERAL_try) {
            return parseTryCatchStmt(token);
        }
        else if (token.getType() == JavaTokenTypes.IDENT) {
            // A label?
            LocatableToken ctoken = tokenStream.nextToken();
            if (ctoken.getType() == JavaTokenTypes.COLON) {
                return ctoken;
            }
            tokenStream.pushBack(ctoken);
            tokenStream.pushBack(token);

            // A declaration of a variable?
            List<LocatableToken> tlist = new LinkedList<LocatableToken>();
            boolean isTypeSpec = parseTypeSpec(true, tlist);
            token = tokenStream.nextToken();
            tokenStream.pushBack(token);
            pushBackAll(tlist);
            if (isTypeSpec && token.getType() == JavaTokenTypes.IDENT) {
                gotTypeSpec(tlist);
                parseVariableDeclarations();
                return null; // DAV
            }
            else {
                parseExpression();						
                token = tokenStream.nextToken();
                if (token.getType() != JavaTokenTypes.SEMI) {
                    error("Expected ';' at end of previous statement");
                    tokenStream.pushBack(token);
                    return null;
                }
                return token;
            }
        }
        else if (isModifier(token)) {	
            tokenStream.pushBack(token);
            parseModifiers();
            if (isTypeDeclarator(tokenStream.LA(1)) || tokenStream.LA(1).getType() == JavaTokenTypes.AT) {
                parseTypeDef();
            }
            else {
                parseVariableDeclarations();
            }
            return null;
        }
        else if (isTypeDeclarator(token)) {
            tokenStream.pushBack(token);
            parseTypeDef();
            return null;
        }
        else if (isPrimitiveType(token)) {
            tokenStream.pushBack(token);
            List<LocatableToken> tlist = new LinkedList<LocatableToken>();
            parseTypeSpec(false, tlist);

            if (tokenStream.LA(1).getType() == JavaTokenTypes.DOT) {
                // int.class, or int[].class are possible
                pushBackAll(tlist);
                parseExpression();
            }
            else {
                pushBackAll(tlist);
                parseVariableDeclarations();
            }
            return null;
        }
        else if (token.getType() == JavaTokenTypes.LCURLY) {
            beginStmtblockBody(token);
            parseStmtBlock();
            token = tokenStream.nextToken();
            if (token.getType() != JavaTokenTypes.RCURLY) {
                error("Expecting '}' at end of statement block");
                if (token.getType() != JavaTokenTypes.RPAREN) {
                    tokenStream.pushBack(token);
                }
                endStmtblockBody(token, false);
                return null;
            }
            endStmtblockBody(token, true);
            return token;
        }
        else {
            tokenStream.pushBack(token);
            parseExpression();
            token = tokenStream.nextToken();
            if (token.getType() != JavaTokenTypes.SEMI) {
                error("Expected ';' at end of previous statement");
                tokenStream.pushBack(token);
                return null;
            }
            return token;
        }
    }
    
    public LocatableToken parseTryCatchStmt(LocatableToken token)
    {
        beginTryCatchSmt(token);
        token = tokenStream.nextToken();
        if (token.getType() != JavaTokenTypes.LCURLY) {
            error ("Expecting '{' after 'try'");
            tokenStream.pushBack(token);
            endTryCatchStmt(token, false);
            return null;
        }
        beginTryBlock(token);
        parseStmtBlock();
        token = tokenStream.nextToken();
        if (token.getType() == JavaTokenTypes.RCURLY) {
            endTryBlock(token, true);
        }
        else if (token.getType() == JavaTokenTypes.LITERAL_catch
                || token.getType() == JavaTokenTypes.LITERAL_finally) {
            // Invalid, but we can recover
            tokenStream.pushBack(token);
            error("Missing '}' at end of 'try' block");
            endTryBlock(token, false);
        }
        else {
            tokenStream.pushBack(token);
            error("Missing '}' at end of 'try' block");
            endTryBlock(token, false);
            endTryCatchStmt(token, false);
            return null;
        }

        int laType = tokenStream.LA(1).getType();
        while (laType == JavaTokenTypes.LITERAL_catch
                || laType == JavaTokenTypes.LITERAL_finally) {
            token = tokenStream.nextToken();
            if (laType == JavaTokenTypes.LITERAL_catch) {
                token = tokenStream.nextToken();
                if (token.getType() != JavaTokenTypes.LPAREN) {
                    error("Expecting '(' after 'catch'");
                    tokenStream.pushBack(token);
                    endTryCatchStmt(token, false);
                    return null;
                }
                parseTypeSpec();
                token = tokenStream.nextToken();
                if (token.getType() != JavaTokenTypes.IDENT) {
                    error("Expecting identifier after type (in 'catch' expression)");
                    tokenStream.pushBack(token);
                    endTryCatchStmt(token, false);
                    return null;
                }
                token = tokenStream.nextToken();
                if (token.getType() != JavaTokenTypes.RPAREN) {
                    error("Expecting ')' after identifier (in 'catch' expression)");
                    tokenStream.pushBack(token);
                    endTryCatchStmt(token, false);
                    return null;
                }
            }
            token = tokenStream.nextToken();
            if (token.getType() != JavaTokenTypes.LCURLY) {
                error("Expecting '{' after 'catch'/'finally'");
                tokenStream.pushBack(token);
                endTryCatchStmt(token, false);
                return null;
            }
            token = parseStatement(token); // parse as a statement block
            laType = tokenStream.LA(1).getType();
        }
        if (token != null) {
            endTryCatchStmt(token, true);
        }
        else {
            endTryCatchStmt(tokenStream.LA(1), false);
        }
        return token;
    }
	
    public LocatableToken parseAssertStatement(LocatableToken token)
    {
        parseExpression();
        token = tokenStream.nextToken();
        if (token.getType() == JavaTokenTypes.COLON) {
            // Should be followed by a string
            parseExpression();
            token = tokenStream.nextToken();
        }
        if (token.getType() != JavaTokenTypes.SEMI) {
            error("Expected ';' at end of assertion statement");
            tokenStream.pushBack(token);
            return null;
        }
        return token;
    }

    public LocatableToken parseSwitchStatement(LocatableToken token)
    {
        token = tokenStream.nextToken();
        if (token.getType() != JavaTokenTypes.LPAREN) {
            error("Expected '(' after 'switch'");
            tokenStream.pushBack(token);
            return null;
        }
        parseExpression();
        token = tokenStream.nextToken();
        if (token.getType() != JavaTokenTypes.RPAREN) {
            error("Expected ')' at end of expression (in 'switch(...)')");
            tokenStream.pushBack(token);
            return null;
        }
        token = tokenStream.nextToken();
        if (token.getType() != JavaTokenTypes.LCURLY) {
            error("Expected '{' after 'switch(...)'");
            tokenStream.pushBack(token);
            return null;
        }
        parseStmtBlock();
        token = tokenStream.nextToken();
        if (token.getType() != JavaTokenTypes.RCURLY) {
            error("Missing '}' at end of 'switch' statement block");
            tokenStream.pushBack(token);
            return null;
        }
        return token;
    }
	
    public LocatableToken parseDoWhileStatement(LocatableToken token)
    {
        beginDoWhile(token);
        token = tokenStream.nextToken(); // '{' or a statement
        LocatableToken ntoken = parseStatement(token);
        if (ntoken != null || token != tokenStream.LA(1)) {
            beginDoWhileBody(token);
            if (ntoken == null) {
                endDoWhileBody(tokenStream.LA(1), false);
            }
            else {
                endDoWhileBody(ntoken, true);
            }
        }

        token = tokenStream.nextToken();
        if (token.getType() != JavaTokenTypes.LITERAL_while) {
            error("Expecting 'while' after statement block (in 'do ... while')");
            tokenStream.pushBack(token);
            endDoWhile(token, false);
            return null;
        }
        token = tokenStream.nextToken();
        if (token.getType() != JavaTokenTypes.LPAREN) {
            error("Expecting '(' after 'while'");
            tokenStream.pushBack(token);
            endDoWhile(token, false);
            return null;
        }
        parseExpression();
        token = tokenStream.nextToken();
        if (token.getType() != JavaTokenTypes.RPAREN) {
            error("Expecting ')' after conditional expression (in 'while' statement)");
            tokenStream.pushBack(token);
            endDoWhile(token, false);
            return null;
        }
        token = tokenStream.nextToken(); // should be ';'
        endDoWhile(token, true);
        return token;
    }
	
    public LocatableToken parseWhileStatement(LocatableToken token)
    {
        beginWhileLoop(token);
        token = tokenStream.nextToken();
        if (token.getType() != JavaTokenTypes.LPAREN) {
            error("Expecting '(' after 'while'");
            tokenStream.pushBack(token);
            endWhileLoop(token, false);
            return null;
        }
        parseExpression();
        token = tokenStream.nextToken();
        if (token.getType() != JavaTokenTypes.RPAREN) {
            error("Expecting ')' after conditional expression (in 'while' statement)");
            tokenStream.pushBack(token);
            endWhileLoop(token, false);
            return null;
        }
        token = tokenStream.nextToken();
        beginWhileLoopBody(token);
        token = parseStatement(token);
        if (token != null) {
            endWhileLoopBody(token, true);
            endWhileLoop(token, true);
        }
        else {
            token = tokenStream.LA(1);
            endWhileLoopBody(token, false);
            endWhileLoop(token, false);
            token = null;
        }
        return token;
    }
	
    public LocatableToken parseForStatement(LocatableToken forToken)
    {
        // TODO: if we get an unexpected token in the part between '(' and ')' check
        // if it is ')'. If so we might still expect a loop body to follow.
        beginForLoop(forToken);
        LocatableToken token = tokenStream.nextToken();
        if (token.getType() != JavaTokenTypes.LPAREN) {
            error("Expecting '(' after 'for'");
            tokenStream.pushBack(token);
            endForLoop(token, false);
            return null;
        }
        if (tokenStream.LA(1).getType() != JavaTokenTypes.SEMI) {
            // Could be an old or new style for-loop.
            List<LocatableToken> tlist = new LinkedList<LocatableToken>();
            boolean isTypeSpec = parseTypeSpec(true, tlist);
            if (isTypeSpec && tokenStream.LA(1).getType() == JavaTokenTypes.IDENT) {
                // for (type var ...
                gotTypeSpec(tlist);
                token = tokenStream.nextToken(); // identifier
                token = tokenStream.nextToken();
                if (token.getType() == JavaTokenTypes.COLON) {
                    // This is a "new" for loop (Java 5)
                    parseExpression();
                    token = tokenStream.nextToken();
                    if (token.getType() != JavaTokenTypes.RPAREN) {
                        error("Expecting ')' (in for statement)");
                        tokenStream.pushBack(token);
                        endForLoop(token, false);
                        return null;
                    }
                    token = tokenStream.nextToken();
                    beginForLoopBody(token);
                    token = parseStatement(token); // loop body
                    endForLoopBody(token);
                    endForLoop(token);
                    return token;
                }
                else {
                    // Old style loop with initialiser
                    if (token.getType() != JavaTokenTypes.ASSIGN) {
                        error("Expecting '=' to complete initializer (in for loop)");
                        tokenStream.pushBack(token);
                        endForLoop(token, false);
                        return null;
                    }
                    parseExpression();
                    token = tokenStream.nextToken();
                    if (token.getType() != JavaTokenTypes.SEMI) {
                        error("Expecting ';' after initialiser (in for statement)");
                        tokenStream.pushBack(token);
                        endForLoop(token, false);
                        return null;
                    }
                }
            }
            else {
                // Not a type spec, so, we might have a general statement
                pushBackAll(tlist);
                token = tokenStream.nextToken();
                parseStatement(token);
            }
        }
        else {
            token = tokenStream.nextToken(); // SEMI
        }

        // We're expecting a regular (old-style) statement at this point
        if (tokenStream.LA(1).getType() != JavaTokenTypes.SEMI) {
            // test expression
            parseExpression();
        }
        token = tokenStream.nextToken();
        if (token.getType() != JavaTokenTypes.SEMI) {
            error("Expecting ';' after test expression (in for statement)");
            tokenStream.pushBack(token);
            endForLoop(token, false);
            return null;
        }
        if (tokenStream.LA(1).getType() != JavaTokenTypes.RPAREN) {
            // loop increment expression
            parseExpression();
        }
        token = tokenStream.nextToken();
        if (token.getType() != JavaTokenTypes.RPAREN) {
            error("Expecting ')' at end of 'for(...'");
            tokenStream.pushBack(token);
            endForLoop(token, false);
            return null;
        }
        token = tokenStream.nextToken();
        if (token.getType() == JavaTokenTypes.RCURLY
                || token.getType() == JavaTokenTypes.EOF) {
            error("Expecting statement after 'for(...)'");
            tokenStream.pushBack(token);
            endForLoop(token, false);
            return null;
        }
        beginForLoopBody(token);
        token = parseStatement(token);
        endForLoopBody(token);
        endForLoop(token);
        return token;
    }
    
    private void endForLoop(LocatableToken token)
    {
        if (token == null) {
            endForLoop(tokenStream.LA(1), false);
        }
        else {
            endForLoop(token, true);
        }
    }
    
    private void endForLoopBody(LocatableToken token)
    {
        if (token == null) {
            endForLoopBody(tokenStream.LA(1), false);
        }
        else {
            endForLoopBody(token, true);
        }
    }
	
    /**
     * Parse an "if" statement.
     * @param token  The token corresponding to the "if" literal.
     */
    public LocatableToken parseIfStatement(LocatableToken token)
    {
        beginIfStmt(token);
        token = tokenStream.nextToken();
        if (token.getType() != JavaTokenTypes.LPAREN) {
            error("Expecting '(' after 'if'");
            tokenStream.pushBack(token);
            endIfStmt(token, false);
            return null;
        }
        parseExpression();
        token = tokenStream.nextToken();
        if (token.getType() != JavaTokenTypes.RPAREN) {
            error("Expecting ')' after conditional expression (in 'if' statement)");
            tokenStream.pushBack(token);
            if (token.getType() != JavaTokenTypes.LCURLY) {
                endIfStmt(token, false);
                return null;
            }
        }
        token = tokenStream.nextToken();
        beginIfCondBlock(token);
        token = parseStatement(token);
        endIfCondBlock(token);
        while (tokenStream.LA(1).getType() == JavaTokenTypes.LITERAL_else) {
            tokenStream.nextToken(); // else
            token = tokenStream.nextToken();
            beginIfCondBlock(token);
            token = parseStatement(token);
            endIfCondBlock(token);
        }
        endIfStmt(token);
        return token;
    }
    
    private void endIfCondBlock(LocatableToken token)
    {
        if (token != null) {
            endIfCondBlock(token, true);
        }
        else {
            endIfCondBlock(tokenStream.LA(1), false);
        }
    }
    
    private void endIfStmt(LocatableToken token)
    {
        if (token != null) {
            endIfStmt(token, true);
        }
        else {
            endIfStmt(tokenStream.LA(1), false);
        }
    }
	
    /**
     * Parse a variable declaration, possibly with an initialiser, always followed by ';'
     */
    public void parseVariableDeclarations()
    {
        parseModifiers();
        parseVariableDeclaration();
        parseSubsequentDeclarations(false);
    }

    /**
     * After seeing a type and identifier declaration, this will parse any
     * the subsequent declarations, and check for a terminating semicolon.
     */
    protected void parseSubsequentDeclarations(boolean isField)
    {
        LocatableToken token = tokenStream.nextToken();
        while (token.getType() == JavaTokenTypes.COMMA) {
            token = tokenStream.nextToken();
            if (token.getType() != JavaTokenTypes.IDENT) {
                error("Expecting variable identifier (or change ',' to ';')");
                return;
            }
            parseArrayDeclarators();
            token = tokenStream.nextToken();
            if (token.getType() == JavaTokenTypes.ASSIGN) {
                parseExpression();
                token = tokenStream.nextToken();
            }
        }

        if (token.getType() != JavaTokenTypes.SEMI) {
            error("Expecting ';' at end of variable declaration");
            tokenStream.pushBack(token);
            if (isField) {
                endField(token, false);
            }
            else {
                endElement(token, false);
            }
        }
        else {
            if (isField) {
                endField(token, true);
            }
            else {
                endElement(token, true);
            }
        }
    }

    /**
     * Parse a variable (or field or parameter) declaration, possibly including an initialiser
     * (but not including modifiers)
     */
    public void parseVariableDeclaration()
    {
        parseTypeSpec();
        LocatableToken token = tokenStream.nextToken();
        if (token.getType() != JavaTokenTypes.IDENT) {
            error("Expecting identifier (in variable/field declaration)");
            tokenStream.pushBack(token);
            return;
        }

        // Array declarators can follow name
        parseArrayDeclarators();

        token = tokenStream.nextToken();
        if (token.getType() == JavaTokenTypes.ASSIGN) {
            parseExpression();
        }
        else {
            tokenStream.pushBack(token);
        }
    }
	
    /**
     * Parse a type specification. This includes class name(s) (Xyz.Abc), type arguments
     * to generic types, and array declarators.
     * 
     * <p>The final set of array declarators will not be parsed if they contain a dimension value.
     * Eg for "Abc[][][10]" this method will leave "[10]" unprocessed and still in the token stream. 
     */
    public boolean parseTypeSpec()
    {
        List<LocatableToken> tokens = new LinkedList<LocatableToken>();
        boolean rval = parseTypeSpec(false, tokens);
        if (rval) {
            gotTypeSpec(tokens);
        }
        return rval;
    }
	
    /**
     * Parse a type specification. This could be a primitive type (including void),
     * or a class type (qualified or not, possibly with type parameters). This can
     * do a speculative parse if the following tokens might either be a type specification
     * or a statement-expression.
     * 
     * @param speculative  Whether this is a speculative parse, i.e. we might not actually
     *                     have a type specification. If this is set some parse errors will
     *                     simply return false.
     * @param ttokens   A list which will be filled with tokens. If the return is true, the tokens
     *                  make up a possible type specification; otherwise the tokens should be
     *                  pushed back on the token stream.
     * 
     * @return true if we saw what might be a type specification (even if it
     * 		               contains errors), or false if it does not appear to be
     *                     a type specification. (only meaningful if speculative == true).
     */
    public boolean parseTypeSpec(boolean speculative, List<LocatableToken> ttokens)
    {
        ttokens.addAll(parseModifiers());
        int ttype = parseBaseType(speculative, ttokens);
        if (ttype == TYPE_ERROR) {
            return false;
        }
        else if (ttype == TYPE_PRIMITIVE) {
            speculative = false;
        }
        else {
            LocatableToken token = tokenStream.nextToken();
            if (token.getType() == JavaTokenTypes.LT) {
                ttokens.add(token);

                // Type parameters? (or is it a "less than" comparison?)
                DepthRef dr = new DepthRef();
                dr.depth = 1;
                if (!parseTpars(speculative, ttokens, dr)) {
                    return false;
                }
            }
            else {
                tokenStream.pushBack(token);
            }
        }

        // check for inner type
        LocatableToken token = tokenStream.nextToken();
        if (token.getType() == JavaTokenTypes.DOT) {
            if (tokenStream.LA(1).getType() == JavaTokenTypes.IDENT) {
                ttokens.add(token);
                return parseTypeSpec(speculative, ttokens);
            }
            else {
                tokenStream.pushBack(token);
                return true;
            }
        }
        else

            // check for array declarators
            while (token.getType() == JavaTokenTypes.LBRACK
                    && tokenStream.LA(1).getType() == JavaTokenTypes.RBRACK) {
                ttokens.add(token);
                token = tokenStream.nextToken(); // RBRACK
                ttokens.add(token);
                token = tokenStream.nextToken();
            }

        tokenStream.pushBack(token);
        return true;
    }

    private static final int TYPE_PRIMITIVE = 0;
    private static final int TYPE_OTHER = 1;
    private static final int TYPE_ERROR = 2;

    /**
     * Parse a type "base" - a primitive type or a class type without type parameters.
     * The type parameters may follow.
     * 
     * @param speculative
     * @param ttokens
     * @return
     * @throws TokenStreamException
     */
    private int parseBaseType(boolean speculative, List<LocatableToken> ttokens)
    {
        LocatableToken token = tokenStream.nextToken();
        if (isPrimitiveType(token)) {
            // Ok, we have a base type
            ttokens.add(token);
            return TYPE_PRIMITIVE;
        }
        else {
            if (token.getType() != JavaTokenTypes.IDENT) {
                if (! speculative) {
                    error("Expected type identifier");
                }
                tokenStream.pushBack(token);
                return TYPE_ERROR;
            }

            ttokens.addAll(parseDottedIdent(token));
        }
        return TYPE_OTHER;
    }

    private boolean parseTpars(boolean speculative, List<LocatableToken> ttokens, DepthRef dr)
    {
        // We already have opening '<' and depth reflects this.

        int beginDepth = dr.depth;
        LocatableToken token;
        boolean needBaseType = true;

        while (dr.depth >= beginDepth) {

            if (tokenStream.LA(1).getType() == JavaTokenTypes.QUESTION) {
                // Wildcard
                token = tokenStream.nextToken();
                ttokens.add(token);
                token = tokenStream.nextToken();
                if (token.getType() == JavaTokenTypes.LITERAL_extends
                        || token.getType() == JavaTokenTypes.LITERAL_super) {
                    ttokens.add(token);
                    needBaseType = true;
                }
                else {
                    tokenStream.pushBack(token);
                    needBaseType = false;
                }
            }

            if (needBaseType) {
                boolean r = parseTargType(speculative, ttokens, dr);
                if (!r) {
                    return false;
                }
                if (dr.depth < beginDepth) {
                    break;
                }
            }

            token = tokenStream.nextToken();
            // Type parameters being closed
            if (token.getType() == JavaTokenTypes.GT
                    || token.getType() == JavaTokenTypes.SR
                    || token.getType() == JavaTokenTypes.BSR) {
                ttokens.add(token);
                if (token.getType() == JavaTokenTypes.GT) {
                    dr.depth--;
                }
                else if (token.getType() == JavaTokenTypes.SR) {
                    dr.depth -= 2;
                }
                else if (token.getType() == JavaTokenTypes.BSR) {
                    dr.depth -= 3;
                }
            }
            else if (token.getType() == JavaTokenTypes.COMMA) {
                ttokens.add(token);
            }
            else {
                if (! speculative) {
                    error("Expected '>' to close type parameter list");
                }
                tokenStream.pushBack(token);
                return false;
            }
        }
        return true;
    }

    // TODO comments
    private boolean parseTargType(boolean speculative, List<LocatableToken> ttokens, DepthRef dr)
    {
        int beginDepth = dr.depth;
        int ttype = parseBaseType(speculative, ttokens);
        if (ttype == TYPE_ERROR) {
            return false;
        }

        if (ttype == TYPE_OTHER) {
            // May be type parameters
            if (tokenStream.LA(1).getType() == JavaTokenTypes.LT) {
                dr.depth++;
                ttokens.add(tokenStream.nextToken());
                if (!parseTpars(speculative, ttokens, dr)) {
                    return false;
                }
                if (dr.depth < beginDepth) {
                    return true;
                }
            }

            LocatableToken token = tokenStream.nextToken();
            if (token.getType() == JavaTokenTypes.DOT && tokenStream.LA(1).getType() == JavaTokenTypes.IDENT) {
                ttokens.add(token);
                if (!parseTargType(speculative, ttokens, dr)) {
                    return false;
                }
            }
            else 
                // Array declarators?
                while (token.getType() == JavaTokenTypes.LBRACK
                        && tokenStream.LA(1).getType() == JavaTokenTypes.RBRACK) {
                    ttokens.add(token);
                    token = tokenStream.nextToken(); // RBRACK
                    ttokens.add(token);
                    token = tokenStream.nextToken();
                }

            tokenStream.pushBack(token);
        }

        return true;
    }
	
    /**
     * Parse a dotted identifier. This could be a variable, method or type name.
     * @param first The first token in the dotted identifier (should be an IDENT)
     * @return A list of tokens making up the dotted identifier
     */
    public List<LocatableToken> parseDottedIdent(LocatableToken first)
    {
        List<LocatableToken> rval = new LinkedList<LocatableToken>();
        rval.add(first);
        LocatableToken token = tokenStream.nextToken();
        while (token.getType() == JavaTokenTypes.DOT) {
            LocatableToken ntoken = tokenStream.nextToken();
            if (ntoken.getType() != JavaTokenTypes.IDENT) {
                // This could be for example "xyz.class"
                tokenStream.pushBack(ntoken);
                break;
            }
            rval.add(token);
            rval.add(ntoken);
            token = tokenStream.nextToken();
        }
        tokenStream.pushBack(token);
        return rval;
    }
	
    /**
     * Check whether a token is an operator. Note that the LPAREN token can be an operator
     * (method call) or value (parenthesized expression).
     * 
     * "new" is not classified as an operator here (an operator operates on a value).
     */
    public static boolean isOperator(LocatableToken token)
    {
        int ttype = token.getType();
        return ttype == JavaTokenTypes.PLUS
        || ttype == JavaTokenTypes.MINUS
        || ttype == JavaTokenTypes.STAR
        || ttype == JavaTokenTypes.DIV
        || ttype == JavaTokenTypes.LBRACK
        || ttype == JavaTokenTypes.LPAREN
        || ttype == JavaTokenTypes.PLUS_ASSIGN
        || ttype == JavaTokenTypes.STAR_ASSIGN
        || ttype == JavaTokenTypes.MINUS_ASSIGN
        || ttype == JavaTokenTypes.DIV_ASSIGN
        || ttype == JavaTokenTypes.DOT
        || ttype == JavaTokenTypes.EQUAL
        || ttype == JavaTokenTypes.NOT_EQUAL
        || ttype == JavaTokenTypes.ASSIGN
        || ttype == JavaTokenTypes.BNOT
        || ttype == JavaTokenTypes.LNOT
        || ttype == JavaTokenTypes.INC
        || ttype == JavaTokenTypes.DEC
        || ttype == JavaTokenTypes.BOR
        || ttype == JavaTokenTypes.BOR_ASSIGN
        || ttype == JavaTokenTypes.BAND
        || ttype == JavaTokenTypes.BAND_ASSIGN
        || ttype == JavaTokenTypes.BXOR
        || ttype == JavaTokenTypes.BXOR_ASSIGN
        || ttype == JavaTokenTypes.SL
        || ttype == JavaTokenTypes.SL_ASSIGN
        || ttype == JavaTokenTypes.SR
        || ttype == JavaTokenTypes.SR_ASSIGN
        || ttype == JavaTokenTypes.BSR
        || ttype == JavaTokenTypes.BSR_ASSIGN
        || ttype == JavaTokenTypes.MOD
        || ttype == JavaTokenTypes.MOD_ASSIGN
        || ttype == JavaTokenTypes.LITERAL_instanceof;
    }
	
    /**
     * Check whether an operator is a binary operator.
     * 
     * "instanceof" is not considered to be a binary operator (operates on only one value).
     */
    public boolean isBinaryOperator(LocatableToken token)
    {
        int ttype = token.getType();
        return ttype == JavaTokenTypes.PLUS
        || ttype == JavaTokenTypes.MINUS
        || ttype == JavaTokenTypes.STAR
        || ttype == JavaTokenTypes.DIV
        || ttype == JavaTokenTypes.MOD
        || ttype == JavaTokenTypes.BOR
        || ttype == JavaTokenTypes.BXOR
        || ttype == JavaTokenTypes.BAND
        || ttype == JavaTokenTypes.SL
        || ttype == JavaTokenTypes.SR
        || ttype == JavaTokenTypes.BSR
        || ttype == JavaTokenTypes.BSR_ASSIGN
        || ttype == JavaTokenTypes.SR_ASSIGN
        || ttype == JavaTokenTypes.SL_ASSIGN
        || ttype == JavaTokenTypes.BAND_ASSIGN
        || ttype == JavaTokenTypes.BXOR_ASSIGN
        || ttype == JavaTokenTypes.BOR_ASSIGN
        || ttype == JavaTokenTypes.MOD_ASSIGN
        || ttype == JavaTokenTypes.DIV_ASSIGN
        || ttype == JavaTokenTypes.STAR_ASSIGN
        || ttype == JavaTokenTypes.MINUS_ASSIGN
        || ttype == JavaTokenTypes.PLUS_ASSIGN
        || ttype == JavaTokenTypes.ASSIGN
        || ttype == JavaTokenTypes.DOT
        || ttype == JavaTokenTypes.EQUAL
        || ttype == JavaTokenTypes.NOT_EQUAL
        || ttype == JavaTokenTypes.LT
        || ttype == JavaTokenTypes.LE
        || ttype == JavaTokenTypes.GT
        || ttype == JavaTokenTypes.GE
        || ttype == JavaTokenTypes.LAND
        || ttype == JavaTokenTypes.LOR;
    }
	
    public boolean isUnaryOperator(LocatableToken token)
    {
        int ttype = token.getType();
        return ttype == JavaTokenTypes.PLUS
        || ttype == JavaTokenTypes.MINUS
        || ttype == JavaTokenTypes.LNOT
        || ttype == JavaTokenTypes.BNOT
        || ttype == JavaTokenTypes.INC
        || ttype == JavaTokenTypes.DEC;
    }

    /**
     * Parse an annotation
     */
    public boolean parseAnnotation()
    {
        boolean parsed=false;
        LocatableToken token = tokenStream.nextToken();
        if (token.getType()==JavaTokenTypes.IDENT){                
            token = tokenStream.nextToken();
            if (token.getType()==JavaTokenTypes.DOT){
                parseDottedIdent(token);
                parsed=true;                    
            }
            //arguments
            else if (token.getType()==JavaTokenTypes.LPAREN){
                parseArgumentList(token);
                token = tokenStream.nextToken();
            }
            else  tokenStream.pushBack(token);                
        }
        else{
            error("Expecting identifier after an @");
            tokenStream.pushBack(token);
        }
        return parsed;
    }
	
    /**
     * Parse an annotation body
     */
    public void parseAnnotationBody()
    {
        LocatableToken token = tokenStream.nextToken();
        while (token.getType() != JavaTokenTypes.RCURLY) {
            LocatableToken hiddenToken = (LocatableToken) token.getHiddenBefore();
            LocatableToken idToken = tokenStream.nextToken(); // identifier
            if (idToken.getType() != JavaTokenTypes.IDENT) {
                error("Expected identifier (method or field name).");
                return;
            }

            token = tokenStream.nextToken();

            if (token.getType() == JavaTokenTypes.LPAREN) {
                // method declaration
                gotMethodDeclaration(idToken, hiddenToken);
                parseMethodParamsBody();
            }
            else {
                error("Expected ';' or '=' or '(' (in field or method declaration), got token type: " + token.getType());
                tokenStream.pushBack(token);
            }
            token = tokenStream.nextToken();
            if (token.getType()==JavaTokenTypes.LITERAL_default){
                parseExpression();
                token = tokenStream.nextToken();
                if (token.getType()!= JavaTokenTypes.SEMI){
                    error("Expected ';' or '=' or '(' (in field or method declaration), got token type: " + token.getType());
                    tokenStream.pushBack(token);
                }
                token = tokenStream.nextToken();
            }

        }   
        tokenStream.pushBack(token);
    }
	
    /**
     * Parse an expression
     */
    public void parseExpression()
    {
        LocatableToken token = tokenStream.nextToken();

        while (true) {
            if (token.getType() == JavaTokenTypes.LITERAL_new) {
                // new XYZ(...)
                parseNewExpression(token);
            }
            else if (token.getType() == JavaTokenTypes.LCURLY) {
                // an initialiser list for an array
                do {
                    if (tokenStream.LA(1).getType() == JavaTokenTypes.RCURLY) {
                        token = tokenStream.nextToken(); // RCURLY
                        break;
                    }
                    parseExpression();
                    token = tokenStream.nextToken();
                } while (token.getType() == JavaTokenTypes.COMMA);
                if (token.getType() != JavaTokenTypes.RCURLY) {
                    error("Expected '}' at end of initialiser list expression");
                    tokenStream.pushBack(token);
                }
            }
            else if (token.getType() == JavaTokenTypes.IDENT) {
                // tokenStream.pushBack(token);
                parseDottedIdent(token);
                //parseTypeSpec(false); // call it a type, it might actually be a value
            }
            else if (token.getType() == JavaTokenTypes.STRING_LITERAL
                    || token.getType() == JavaTokenTypes.CHAR_LITERAL
                    || token.getType() == JavaTokenTypes.NUM_INT
                    || token.getType() == JavaTokenTypes.NUM_LONG
                    || token.getType() == JavaTokenTypes.NUM_DOUBLE
                    || token.getType() == JavaTokenTypes.NUM_FLOAT
                    || token.getType() == JavaTokenTypes.LITERAL_null
                    || token.getType() == JavaTokenTypes.LITERAL_this
                    || token.getType() == JavaTokenTypes.LITERAL_super
                    || token.getType() == JavaTokenTypes.LITERAL_true
                    || token.getType() == JavaTokenTypes.LITERAL_false) {
                // Literals need no further processing
                gotLiteral(token);
            }
            else if (isPrimitiveType(token)) {
                // Not really part of an expression, but may be followed by
                // .class or [].class  (eg int.class, int[][].class)
            }
            else if (isUnaryOperator(token)) {
                // Unary operator
                token = tokenStream.nextToken();
                continue;
            }
            else if (token.getType() == JavaTokenTypes.LPAREN) {
                // Either a parenthesised expression, or a type cast
                // We handle cast to primitive specially - it can be followed by +, ++, -, --
                // and yet be a cast.
                boolean isPrimitive = isPrimitiveType(tokenStream.LA(1));

                List<LocatableToken> tlist = new LinkedList<LocatableToken>();
                boolean isTypeSpec = parseTypeSpec(true, tlist);
                // if
                // -it's a type spec
                // -it's followed by ')'
                // -it's not followed by an operator OR
                //  the type is primitive and the following operator is a unary operator
                // -it's not followed by an expression terminator - ; , )

                int tt2 = tokenStream.LA(2).getType();
                boolean isCast = isTypeSpec && tokenStream.LA(1).getType() == JavaTokenTypes.RPAREN;
                isCast &= !isOperator(tokenStream.LA(2)) || (isPrimitive
                        && isUnaryOperator(tokenStream.LA(2)));
                isCast &= tt2 != JavaTokenTypes.SEMI && tt2 != JavaTokenTypes.RPAREN
                && tt2 != JavaTokenTypes.RCURLY && tt2 != JavaTokenTypes.EOF;

                if (isCast) {
                    // This surely must be type cast
                    gotTypeCast(tlist);
                    token = tokenStream.nextToken(); // RPAREN
                    token = tokenStream.nextToken();
                    continue;
                }
                else {
                    pushBackAll(tlist);
                    parseExpression();
                    token = tokenStream.nextToken();
                    if (token.getType() != JavaTokenTypes.RPAREN) {
                        error("Unmatched '(' in expression; expecting ')'");
                        tokenStream.pushBack(token);
                        return;
                    }
                }
            }
            else {
                error("Invalid expression token=" + token);
                tokenStream.pushBack(token);
                return;
            }

            // Now we get an operator, or end of expression
            while (true) {
                token = tokenStream.nextToken();
                if (token.getType() == JavaTokenTypes.RPAREN
                        || token.getType() == JavaTokenTypes.SEMI
                        || token.getType() == JavaTokenTypes.RBRACK
                        || token.getType() == JavaTokenTypes.COMMA
                        || token.getType() == JavaTokenTypes.COLON
                        || token.getType() == JavaTokenTypes.EOF
                        || token.getType() == JavaTokenTypes.RCURLY)
                {
                    // These are all legitimate expression endings
                    tokenStream.pushBack(token);
                    return;
                }
                else if (token.getType() == JavaTokenTypes.LPAREN) {
                    // Method call
                    parseArgumentList(token);
                    tokenStream.nextToken(); // remove the ')'
                }
                else if (token.getType() == JavaTokenTypes.LBRACK) {
                    // Arrary subscript?
                    if (tokenStream.LA(1).getType() == JavaTokenTypes.RBRACK) {
                        // No subscript means that this is a type - must be followed by
                        // ".class" normally. Eg Object[].class
                        token = tokenStream.nextToken(); // RBRACK
                        continue;
                    }
                    parseExpression();
                    token = tokenStream.nextToken();
                    if (token.getType() != JavaTokenTypes.RBRACK) {
                        error("Expected ']' after array subscript expression");
                        tokenStream.pushBack(token);
                    }
                }
                else if (token.getType() == JavaTokenTypes.LITERAL_instanceof) {
                    parseTypeSpec();
                }
                else if (token.getType() == JavaTokenTypes.DOT) {
                    // Handle dot operator specially, as there are some special cases
                    token = tokenStream.nextToken();
                    if (token.getType() != JavaTokenTypes.LITERAL_class) {
                        break;
                    }
                    // Class literal: continue and look for another operator
                    // (which should really only be '.')
                }
                else if (isBinaryOperator(token)) {
                    // Binary operators - need another operand
                    gotBinaryOperator(token);
                    token = tokenStream.nextToken();
                    break;
                }
                else if (token.getType() == JavaTokenTypes.INC
                        || token.getType() == JavaTokenTypes.DEC) {
                    // post operators (unary)
                    continue;
                }
                else if (token.getType() == JavaTokenTypes.QUESTION) {
                    parseExpression();
                    token = tokenStream.nextToken();
                    if (token.getType() != JavaTokenTypes.COLON) {
                        error("Expecting ':' (in ?: operator)");
                        tokenStream.pushBack(token);
                        return;
                    }
                    token = tokenStream.nextToken();
                    break;
                }
                else {
                    // TODO
                    error("Expected operator, got '" + token + "'");
                    tokenStream.pushBack(token);
                    return;
                }
            }
        }
    }
    
    public void parseNewExpression(LocatableToken token)
    {
        // new XYZ(...)
        gotExprNew(token);
        token = tokenStream.nextToken();
        if (token.getType() != JavaTokenTypes.IDENT && !isPrimitiveType(token)) {
            error("Expected type identifier after \"new\" (in expression)");
            tokenStream.pushBack(token);
            endExprNew(token, false);
            return;
        }
        tokenStream.pushBack(token);
        parseTypeSpec();
        token = tokenStream.nextToken();

        if (token.getType() == JavaTokenTypes.LBRACK) {
            while (token.getType() == JavaTokenTypes.LBRACK) {
                // array dimensions
                if (tokenStream.LA(1).getType() != JavaTokenTypes.RBRACK) {
                    parseExpression();
                }
                token = tokenStream.nextToken();
                if (token.getType() != JavaTokenTypes.RBRACK) {
                    error("Expecting ']' after array dimension (in new ... expression)");
                    tokenStream.pushBack(token);
                    endExprNew(token, false);
                }
            }

            endExprNew(token, true);
            return;
        }
        
        if (token.getType() == JavaTokenTypes.LCURLY) {
            do {
                if (tokenStream.LA(1).getType() == JavaTokenTypes.RCURLY) {
                    token = tokenStream.nextToken();
                    break;
                }
                parseExpression();
                token = tokenStream.nextToken();
            }
            while (token.getType() == JavaTokenTypes.COMMA);
            
            if (token.getType() != JavaTokenTypes.RCURLY) {
                error("Expecting '}' at end of array initializer list");
                tokenStream.pushBack(token);
                endExprNew(token, false);
                return;
            }
            
            endExprNew(token, true);
            return;
        }

        if (token.getType() != JavaTokenTypes.LPAREN) {
            error("Expected '(' or '[' after type name (in 'new ...' expression)");
            tokenStream.pushBack(token);
            endExprNew(token, false);
            return;
        }
        parseArgumentList(token);
        token = tokenStream.nextToken();
        if (token.getType() != JavaTokenTypes.RPAREN) {
            error("Expected ')' at end of argument list (in 'new ...' expression)");
            tokenStream.pushBack(token);
            endExprNew(token, false);
            return;
        }

        if (tokenStream.LA(1).getType() == JavaTokenTypes.LCURLY) {
            // a class body (anonymous inner class)
            tokenStream.nextToken(); // LCURLY
            parseClassBody();
            token = tokenStream.nextToken();
            if (token.getType() != JavaTokenTypes.RCURLY) {
                error("Expected '}' at end of inner class body");
                tokenStream.pushBack(token);
                tokenStream.pushBack(token);
                endExprNew(token, false);
                return;
            }
        }
        endExprNew(token, true);
    }
    
    /**
     * Parse a comma-separated, possibly empty list of arguments to a method/constructor.
     * Returns with the closing ')' token still in the token stream.
     * @param token   the '(' token
     */
    public void parseArgumentList(LocatableToken token)
    {
        beginArgumentList(token);
        token = tokenStream.nextToken();
        if (token.getType() != JavaTokenTypes.RPAREN) {
            tokenStream.pushBack(token);
            do  {
                parseExpression();
                token = tokenStream.nextToken();
                endArgument();
            } while (token.getType() == JavaTokenTypes.COMMA);
            if (token.getType() != JavaTokenTypes.RPAREN) {
                error("Expecting ',' or ')' (in argument list)");
            }
        }
        endArgumentList(token);
        tokenStream.pushBack(token); // push back the ')' or erroneous token
        return;
    }
	
    /**
     * Parse a list of formal parameters (possibly empty)
     */
    public void parseParameterList()
    {
        LocatableToken token = tokenStream.nextToken();
        while (token.getType() != JavaTokenTypes.RPAREN
                && token.getType() != JavaTokenTypes.RCURLY) {
            tokenStream.pushBack(token);

            parseTypeSpec();
            LocatableToken idToken = tokenStream.nextToken(); // identifier
            if (idToken.getType() != JavaTokenTypes.IDENT) {
                error("Expected parameter identifier (in method parameter)");
                // TODO skip to next ',', ')' or '}' if there is one soon (LA(3)?)
                tokenStream.pushBack(idToken);
                return;
            }
            gotMethodParameter(idToken);
            parseArrayDeclarators();
            token = tokenStream.nextToken();
            if (token.getType() != JavaTokenTypes.COMMA) {
                break;
            }
            token = tokenStream.nextToken();
        }
        tokenStream.pushBack(token);
    }
	
    /**
     * We've seen a constructor declaration. The token supplied is the constructor name.
     * The hiddenToken is the comment before the constructor.
     */
    protected void gotConstructorDecl(LocatableToken token, LocatableToken hiddenToken) {}

    /**
     * We've seen a method declaration; the token parameter is the method name;
     * the hiddenToken parameter is the comment before the method
     */
    protected void gotMethodDeclaration(LocatableToken token, LocatableToken hiddenToken) {}

    /** 
     * We saw a method (or constructor) parameter. The given token specifies the parameter name. 
     * The last type parsed by parseTypeSpec(boolean) is the parameter type, however, there may
     * be array declarators (after the identifier) yet to be parsed.
     */
    protected void gotMethodParameter(LocatableToken token) { }

    protected void gotAllMethodParameters() { }

    /**
     * Called by the lexer when it sees a comment.
     */
    public void gotComment(LocatableToken token) { }

    private void pushBackAll(List<LocatableToken> tokens)
    {
        ListIterator<LocatableToken> i = tokens.listIterator(tokens.size());
        while (i.hasPrevious()) {
            tokenStream.pushBack(i.previous());
        }
    }

    private class DepthRef
    {
        int depth;
    }
}

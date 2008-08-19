lexer grammar AccessPred;
@header {
package edu.cmu.cs.plural.perm.parser;
}

T21 : '(' ;
T22 : ')' ;
T23 : ',' ;
T24 : '#' ;
T25 : '!fr' ;
T26 : '=' ;
T27 : '/' ;

// $ANTLR src "C:\Documents and Settings\Kevin\My Documents\workspace\Plural\permission-parser\AccessPred.g" 193
ALT	:	'alt' | '+';
	// $ANTLR src "C:\Documents and Settings\Kevin\My Documents\workspace\Plural\permission-parser\AccessPred.g" 194
TENS	:	'tens' | '*';
	// $ANTLR src "C:\Documents and Settings\Kevin\My Documents\workspace\Plural\permission-parser\AccessPred.g" 195
WITH	:	'with' | '&';
	// $ANTLR src "C:\Documents and Settings\Kevin\My Documents\workspace\Plural\permission-parser\AccessPred.g" 196
DEQ	:	'==';
	// $ANTLR src "C:\Documents and Settings\Kevin\My Documents\workspace\Plural\permission-parser\AccessPred.g" 197
NEQ	:	'!=';
	// $ANTLR src "C:\Documents and Settings\Kevin\My Documents\workspace\Plural\permission-parser\AccessPred.g" 198
IN 	:	'in';
	// $ANTLR src "C:\Documents and Settings\Kevin\My Documents\workspace\Plural\permission-parser\AccessPred.g" 199
NULL	:	'null';
	// $ANTLR src "C:\Documents and Settings\Kevin\My Documents\workspace\Plural\permission-parser\AccessPred.g" 200
IMPLIES	:	'=>' | 'implies';
	// $ANTLR src "C:\Documents and Settings\Kevin\My Documents\workspace\Plural\permission-parser\AccessPred.g" 201
TRUE	:	'true';
	// $ANTLR src "C:\Documents and Settings\Kevin\My Documents\workspace\Plural\permission-parser\AccessPred.g" 202
FALSE	:	'false';
	// $ANTLR src "C:\Documents and Settings\Kevin\My Documents\workspace\Plural\permission-parser\AccessPred.g" 203
ONE		:	'one';
	// $ANTLR src "C:\Documents and Settings\Kevin\My Documents\workspace\Plural\permission-parser\AccessPred.g" 204
ZERO	:	'zero';

// $ANTLR src "C:\Documents and Settings\Kevin\My Documents\workspace\Plural\permission-parser\AccessPred.g" 206
ID 	:	(LETTER|'_')(LETTER|DIGIT|'_')*;

// $ANTLR src "C:\Documents and Settings\Kevin\My Documents\workspace\Plural\permission-parser\AccessPred.g" 208
fragment LETTER: ('a'..'z' | 'A'..'Z');

// $ANTLR src "C:\Documents and Settings\Kevin\My Documents\workspace\Plural\permission-parser\AccessPred.g" 210
NUMBER	: (DIGIT)+ ;

// $ANTLR src "C:\Documents and Settings\Kevin\My Documents\workspace\Plural\permission-parser\AccessPred.g" 212
WHITESPACE : ( '\t' | ' ' | '\r' | '\n'| '\u000C' )+ 	{ $channel = HIDDEN; } ;

// $ANTLR src "C:\Documents and Settings\Kevin\My Documents\workspace\Plural\permission-parser\AccessPred.g" 214
fragment DIGIT	: '0'..'9' ;

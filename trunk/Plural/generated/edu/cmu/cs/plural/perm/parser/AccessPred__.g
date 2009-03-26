lexer grammar AccessPred;
@header {
package edu.cmu.cs.plural.perm.parser;
}

T21 : '(' ;
T22 : ')' ;
T23 : ',' ;
T24 : '#' ;
T25 : '!' ;
T26 : '.super' ;
T27 : '.this' ;
T28 : 'fr' ;
T29 : 'fl' ;
T30 : 'dp' ;
T31 : 'df' ;
T32 : '=' ;
T33 : '/' ;

// $ANTLR src "C:\Documents and Settings\kbierhof\My Documents\workspace\Plural\permission-parser\AccessPred.g" 251
ALT	:	'alt' | '+';
	// $ANTLR src "C:\Documents and Settings\kbierhof\My Documents\workspace\Plural\permission-parser\AccessPred.g" 252
TENS	:	'tens' | '*';
	// $ANTLR src "C:\Documents and Settings\kbierhof\My Documents\workspace\Plural\permission-parser\AccessPred.g" 253
WITH	:	'with' | '&';
	// $ANTLR src "C:\Documents and Settings\kbierhof\My Documents\workspace\Plural\permission-parser\AccessPred.g" 254
DEQ	:	'==';
	// $ANTLR src "C:\Documents and Settings\kbierhof\My Documents\workspace\Plural\permission-parser\AccessPred.g" 255
NEQ	:	'!=';
	// $ANTLR src "C:\Documents and Settings\kbierhof\My Documents\workspace\Plural\permission-parser\AccessPred.g" 256
IN 	:	'in';
	// $ANTLR src "C:\Documents and Settings\kbierhof\My Documents\workspace\Plural\permission-parser\AccessPred.g" 257
NULL	:	'null';
	// $ANTLR src "C:\Documents and Settings\kbierhof\My Documents\workspace\Plural\permission-parser\AccessPred.g" 258
IMPLIES	:	'=>' | 'implies';
	// $ANTLR src "C:\Documents and Settings\kbierhof\My Documents\workspace\Plural\permission-parser\AccessPred.g" 259
TRUE	:	'true';
	// $ANTLR src "C:\Documents and Settings\kbierhof\My Documents\workspace\Plural\permission-parser\AccessPred.g" 260
FALSE	:	'false';
	// $ANTLR src "C:\Documents and Settings\kbierhof\My Documents\workspace\Plural\permission-parser\AccessPred.g" 261
ONE		:	'one';
	// $ANTLR src "C:\Documents and Settings\kbierhof\My Documents\workspace\Plural\permission-parser\AccessPred.g" 262
ZERO	:	'zero';

// $ANTLR src "C:\Documents and Settings\kbierhof\My Documents\workspace\Plural\permission-parser\AccessPred.g" 264
ID 	:	(LETTER|'_')(LETTER|DIGIT|'_')*;

// $ANTLR src "C:\Documents and Settings\kbierhof\My Documents\workspace\Plural\permission-parser\AccessPred.g" 266
fragment LETTER: ('a'..'z' | 'A'..'Z');

// $ANTLR src "C:\Documents and Settings\kbierhof\My Documents\workspace\Plural\permission-parser\AccessPred.g" 268
NUMBER	: (DIGIT)+ ;

// $ANTLR src "C:\Documents and Settings\kbierhof\My Documents\workspace\Plural\permission-parser\AccessPred.g" 270
WHITESPACE : ( '\t' | ' ' | '\r' | '\n'| '\u000C' )+ 	{ $channel = HIDDEN; } ;

// $ANTLR src "C:\Documents and Settings\kbierhof\My Documents\workspace\Plural\permission-parser\AccessPred.g" 272
fragment DIGIT	: '0'..'9' ;

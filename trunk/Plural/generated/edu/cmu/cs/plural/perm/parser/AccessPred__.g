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

// $ANTLR src "C:\Documents and Settings\kbierhof\My Documents\workspace\Plural\permission-parser\AccessPred.g" 234
ALT	:	'alt' | '+';
	// $ANTLR src "C:\Documents and Settings\kbierhof\My Documents\workspace\Plural\permission-parser\AccessPred.g" 235
TENS	:	'tens' | '*';
	// $ANTLR src "C:\Documents and Settings\kbierhof\My Documents\workspace\Plural\permission-parser\AccessPred.g" 236
WITH	:	'with' | '&';
	// $ANTLR src "C:\Documents and Settings\kbierhof\My Documents\workspace\Plural\permission-parser\AccessPred.g" 237
DEQ	:	'==';
	// $ANTLR src "C:\Documents and Settings\kbierhof\My Documents\workspace\Plural\permission-parser\AccessPred.g" 238
NEQ	:	'!=';
	// $ANTLR src "C:\Documents and Settings\kbierhof\My Documents\workspace\Plural\permission-parser\AccessPred.g" 239
IN 	:	'in';
	// $ANTLR src "C:\Documents and Settings\kbierhof\My Documents\workspace\Plural\permission-parser\AccessPred.g" 240
NULL	:	'null';
	// $ANTLR src "C:\Documents and Settings\kbierhof\My Documents\workspace\Plural\permission-parser\AccessPred.g" 241
IMPLIES	:	'=>' | 'implies';
	// $ANTLR src "C:\Documents and Settings\kbierhof\My Documents\workspace\Plural\permission-parser\AccessPred.g" 242
TRUE	:	'true';
	// $ANTLR src "C:\Documents and Settings\kbierhof\My Documents\workspace\Plural\permission-parser\AccessPred.g" 243
FALSE	:	'false';
	// $ANTLR src "C:\Documents and Settings\kbierhof\My Documents\workspace\Plural\permission-parser\AccessPred.g" 244
ONE		:	'one';
	// $ANTLR src "C:\Documents and Settings\kbierhof\My Documents\workspace\Plural\permission-parser\AccessPred.g" 245
ZERO	:	'zero';

// $ANTLR src "C:\Documents and Settings\kbierhof\My Documents\workspace\Plural\permission-parser\AccessPred.g" 247
ID 	:	(LETTER|'_')(LETTER|DIGIT|'_')*;

// $ANTLR src "C:\Documents and Settings\kbierhof\My Documents\workspace\Plural\permission-parser\AccessPred.g" 249
fragment LETTER: ('a'..'z' | 'A'..'Z');

// $ANTLR src "C:\Documents and Settings\kbierhof\My Documents\workspace\Plural\permission-parser\AccessPred.g" 251
NUMBER	: (DIGIT)+ ;

// $ANTLR src "C:\Documents and Settings\kbierhof\My Documents\workspace\Plural\permission-parser\AccessPred.g" 253
WHITESPACE : ( '\t' | ' ' | '\r' | '\n'| '\u000C' )+ 	{ $channel = HIDDEN; } ;

// $ANTLR src "C:\Documents and Settings\kbierhof\My Documents\workspace\Plural\permission-parser\AccessPred.g" 255
fragment DIGIT	: '0'..'9' ;

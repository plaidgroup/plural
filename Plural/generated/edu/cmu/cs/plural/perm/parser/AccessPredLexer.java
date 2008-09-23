// $ANTLR 3.0.1 C:\\Documents and Settings\\kbierhof\\My Documents\\workspace\\Plural\\permission-parser\\AccessPred.g 2008-09-22 17:56:53

package edu.cmu.cs.plural.perm.parser;


import org.antlr.runtime.*;
import java.util.Stack;
import java.util.List;
import java.util.ArrayList;

public class AccessPredLexer extends Lexer {
    public static final int IMPLIES=8;
    public static final int LETTER=18;
    public static final int NULL=12;
    public static final int NUMBER=11;
    public static final int DEQ=9;
    public static final int WHITESPACE=20;
    public static final int ONE=16;
    public static final int T27=27;
    public static final int T26=26;
    public static final int T25=25;
    public static final int ID=6;
    public static final int Tokens=28;
    public static final int T24=24;
    public static final int EOF=-1;
    public static final int T23=23;
    public static final int TRUE=4;
    public static final int T22=22;
    public static final int T21=21;
    public static final int ZERO=17;
    public static final int NEQ=10;
    public static final int IN=7;
    public static final int ALT=13;
    public static final int TENS=15;
    public static final int DIGIT=19;
    public static final int FALSE=5;
    public static final int WITH=14;
    public AccessPredLexer() {;} 
    public AccessPredLexer(CharStream input) {
        super(input);
    }
    public String getGrammarFileName() { return "C:\\Documents and Settings\\kbierhof\\My Documents\\workspace\\Plural\\permission-parser\\AccessPred.g"; }

    // $ANTLR start T21
    public final void mT21() throws RecognitionException {
        try {
            int _type = T21;
            // C:\\Documents and Settings\\kbierhof\\My Documents\\workspace\\Plural\\permission-parser\\AccessPred.g:6:5: ( '(' )
            // C:\\Documents and Settings\\kbierhof\\My Documents\\workspace\\Plural\\permission-parser\\AccessPred.g:6:7: '('
            {
            match('('); 

            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end T21

    // $ANTLR start T22
    public final void mT22() throws RecognitionException {
        try {
            int _type = T22;
            // C:\\Documents and Settings\\kbierhof\\My Documents\\workspace\\Plural\\permission-parser\\AccessPred.g:7:5: ( ')' )
            // C:\\Documents and Settings\\kbierhof\\My Documents\\workspace\\Plural\\permission-parser\\AccessPred.g:7:7: ')'
            {
            match(')'); 

            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end T22

    // $ANTLR start T23
    public final void mT23() throws RecognitionException {
        try {
            int _type = T23;
            // C:\\Documents and Settings\\kbierhof\\My Documents\\workspace\\Plural\\permission-parser\\AccessPred.g:8:5: ( ',' )
            // C:\\Documents and Settings\\kbierhof\\My Documents\\workspace\\Plural\\permission-parser\\AccessPred.g:8:7: ','
            {
            match(','); 

            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end T23

    // $ANTLR start T24
    public final void mT24() throws RecognitionException {
        try {
            int _type = T24;
            // C:\\Documents and Settings\\kbierhof\\My Documents\\workspace\\Plural\\permission-parser\\AccessPred.g:9:5: ( '#' )
            // C:\\Documents and Settings\\kbierhof\\My Documents\\workspace\\Plural\\permission-parser\\AccessPred.g:9:7: '#'
            {
            match('#'); 

            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end T24

    // $ANTLR start T25
    public final void mT25() throws RecognitionException {
        try {
            int _type = T25;
            // C:\\Documents and Settings\\kbierhof\\My Documents\\workspace\\Plural\\permission-parser\\AccessPred.g:10:5: ( '!fr' )
            // C:\\Documents and Settings\\kbierhof\\My Documents\\workspace\\Plural\\permission-parser\\AccessPred.g:10:7: '!fr'
            {
            match("!fr"); 


            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end T25

    // $ANTLR start T26
    public final void mT26() throws RecognitionException {
        try {
            int _type = T26;
            // C:\\Documents and Settings\\kbierhof\\My Documents\\workspace\\Plural\\permission-parser\\AccessPred.g:11:5: ( '=' )
            // C:\\Documents and Settings\\kbierhof\\My Documents\\workspace\\Plural\\permission-parser\\AccessPred.g:11:7: '='
            {
            match('='); 

            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end T26

    // $ANTLR start T27
    public final void mT27() throws RecognitionException {
        try {
            int _type = T27;
            // C:\\Documents and Settings\\kbierhof\\My Documents\\workspace\\Plural\\permission-parser\\AccessPred.g:12:5: ( '/' )
            // C:\\Documents and Settings\\kbierhof\\My Documents\\workspace\\Plural\\permission-parser\\AccessPred.g:12:7: '/'
            {
            match('/'); 

            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end T27

    // $ANTLR start ALT
    public final void mALT() throws RecognitionException {
        try {
            int _type = ALT;
            // C:\\Documents and Settings\\kbierhof\\My Documents\\workspace\\Plural\\permission-parser\\AccessPred.g:234:5: ( 'alt' | '+' )
            int alt1=2;
            int LA1_0 = input.LA(1);

            if ( (LA1_0=='a') ) {
                alt1=1;
            }
            else if ( (LA1_0=='+') ) {
                alt1=2;
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("234:1: ALT : ( 'alt' | '+' );", 1, 0, input);

                throw nvae;
            }
            switch (alt1) {
                case 1 :
                    // C:\\Documents and Settings\\kbierhof\\My Documents\\workspace\\Plural\\permission-parser\\AccessPred.g:234:7: 'alt'
                    {
                    match("alt"); 


                    }
                    break;
                case 2 :
                    // C:\\Documents and Settings\\kbierhof\\My Documents\\workspace\\Plural\\permission-parser\\AccessPred.g:234:15: '+'
                    {
                    match('+'); 

                    }
                    break;

            }
            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end ALT

    // $ANTLR start TENS
    public final void mTENS() throws RecognitionException {
        try {
            int _type = TENS;
            // C:\\Documents and Settings\\kbierhof\\My Documents\\workspace\\Plural\\permission-parser\\AccessPred.g:235:6: ( 'tens' | '*' )
            int alt2=2;
            int LA2_0 = input.LA(1);

            if ( (LA2_0=='t') ) {
                alt2=1;
            }
            else if ( (LA2_0=='*') ) {
                alt2=2;
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("235:1: TENS : ( 'tens' | '*' );", 2, 0, input);

                throw nvae;
            }
            switch (alt2) {
                case 1 :
                    // C:\\Documents and Settings\\kbierhof\\My Documents\\workspace\\Plural\\permission-parser\\AccessPred.g:235:8: 'tens'
                    {
                    match("tens"); 


                    }
                    break;
                case 2 :
                    // C:\\Documents and Settings\\kbierhof\\My Documents\\workspace\\Plural\\permission-parser\\AccessPred.g:235:17: '*'
                    {
                    match('*'); 

                    }
                    break;

            }
            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end TENS

    // $ANTLR start WITH
    public final void mWITH() throws RecognitionException {
        try {
            int _type = WITH;
            // C:\\Documents and Settings\\kbierhof\\My Documents\\workspace\\Plural\\permission-parser\\AccessPred.g:236:6: ( 'with' | '&' )
            int alt3=2;
            int LA3_0 = input.LA(1);

            if ( (LA3_0=='w') ) {
                alt3=1;
            }
            else if ( (LA3_0=='&') ) {
                alt3=2;
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("236:1: WITH : ( 'with' | '&' );", 3, 0, input);

                throw nvae;
            }
            switch (alt3) {
                case 1 :
                    // C:\\Documents and Settings\\kbierhof\\My Documents\\workspace\\Plural\\permission-parser\\AccessPred.g:236:8: 'with'
                    {
                    match("with"); 


                    }
                    break;
                case 2 :
                    // C:\\Documents and Settings\\kbierhof\\My Documents\\workspace\\Plural\\permission-parser\\AccessPred.g:236:17: '&'
                    {
                    match('&'); 

                    }
                    break;

            }
            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end WITH

    // $ANTLR start DEQ
    public final void mDEQ() throws RecognitionException {
        try {
            int _type = DEQ;
            // C:\\Documents and Settings\\kbierhof\\My Documents\\workspace\\Plural\\permission-parser\\AccessPred.g:237:5: ( '==' )
            // C:\\Documents and Settings\\kbierhof\\My Documents\\workspace\\Plural\\permission-parser\\AccessPred.g:237:7: '=='
            {
            match("=="); 


            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end DEQ

    // $ANTLR start NEQ
    public final void mNEQ() throws RecognitionException {
        try {
            int _type = NEQ;
            // C:\\Documents and Settings\\kbierhof\\My Documents\\workspace\\Plural\\permission-parser\\AccessPred.g:238:5: ( '!=' )
            // C:\\Documents and Settings\\kbierhof\\My Documents\\workspace\\Plural\\permission-parser\\AccessPred.g:238:7: '!='
            {
            match("!="); 


            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end NEQ

    // $ANTLR start IN
    public final void mIN() throws RecognitionException {
        try {
            int _type = IN;
            // C:\\Documents and Settings\\kbierhof\\My Documents\\workspace\\Plural\\permission-parser\\AccessPred.g:239:5: ( 'in' )
            // C:\\Documents and Settings\\kbierhof\\My Documents\\workspace\\Plural\\permission-parser\\AccessPred.g:239:7: 'in'
            {
            match("in"); 


            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end IN

    // $ANTLR start NULL
    public final void mNULL() throws RecognitionException {
        try {
            int _type = NULL;
            // C:\\Documents and Settings\\kbierhof\\My Documents\\workspace\\Plural\\permission-parser\\AccessPred.g:240:6: ( 'null' )
            // C:\\Documents and Settings\\kbierhof\\My Documents\\workspace\\Plural\\permission-parser\\AccessPred.g:240:8: 'null'
            {
            match("null"); 


            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end NULL

    // $ANTLR start IMPLIES
    public final void mIMPLIES() throws RecognitionException {
        try {
            int _type = IMPLIES;
            // C:\\Documents and Settings\\kbierhof\\My Documents\\workspace\\Plural\\permission-parser\\AccessPred.g:241:9: ( '=>' | 'implies' )
            int alt4=2;
            int LA4_0 = input.LA(1);

            if ( (LA4_0=='=') ) {
                alt4=1;
            }
            else if ( (LA4_0=='i') ) {
                alt4=2;
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("241:1: IMPLIES : ( '=>' | 'implies' );", 4, 0, input);

                throw nvae;
            }
            switch (alt4) {
                case 1 :
                    // C:\\Documents and Settings\\kbierhof\\My Documents\\workspace\\Plural\\permission-parser\\AccessPred.g:241:11: '=>'
                    {
                    match("=>"); 


                    }
                    break;
                case 2 :
                    // C:\\Documents and Settings\\kbierhof\\My Documents\\workspace\\Plural\\permission-parser\\AccessPred.g:241:18: 'implies'
                    {
                    match("implies"); 


                    }
                    break;

            }
            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end IMPLIES

    // $ANTLR start TRUE
    public final void mTRUE() throws RecognitionException {
        try {
            int _type = TRUE;
            // C:\\Documents and Settings\\kbierhof\\My Documents\\workspace\\Plural\\permission-parser\\AccessPred.g:242:6: ( 'true' )
            // C:\\Documents and Settings\\kbierhof\\My Documents\\workspace\\Plural\\permission-parser\\AccessPred.g:242:8: 'true'
            {
            match("true"); 


            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end TRUE

    // $ANTLR start FALSE
    public final void mFALSE() throws RecognitionException {
        try {
            int _type = FALSE;
            // C:\\Documents and Settings\\kbierhof\\My Documents\\workspace\\Plural\\permission-parser\\AccessPred.g:243:7: ( 'false' )
            // C:\\Documents and Settings\\kbierhof\\My Documents\\workspace\\Plural\\permission-parser\\AccessPred.g:243:9: 'false'
            {
            match("false"); 


            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end FALSE

    // $ANTLR start ONE
    public final void mONE() throws RecognitionException {
        try {
            int _type = ONE;
            // C:\\Documents and Settings\\kbierhof\\My Documents\\workspace\\Plural\\permission-parser\\AccessPred.g:244:6: ( 'one' )
            // C:\\Documents and Settings\\kbierhof\\My Documents\\workspace\\Plural\\permission-parser\\AccessPred.g:244:8: 'one'
            {
            match("one"); 


            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end ONE

    // $ANTLR start ZERO
    public final void mZERO() throws RecognitionException {
        try {
            int _type = ZERO;
            // C:\\Documents and Settings\\kbierhof\\My Documents\\workspace\\Plural\\permission-parser\\AccessPred.g:245:6: ( 'zero' )
            // C:\\Documents and Settings\\kbierhof\\My Documents\\workspace\\Plural\\permission-parser\\AccessPred.g:245:8: 'zero'
            {
            match("zero"); 


            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end ZERO

    // $ANTLR start ID
    public final void mID() throws RecognitionException {
        try {
            int _type = ID;
            // C:\\Documents and Settings\\kbierhof\\My Documents\\workspace\\Plural\\permission-parser\\AccessPred.g:247:5: ( ( LETTER | '_' ) ( LETTER | DIGIT | '_' )* )
            // C:\\Documents and Settings\\kbierhof\\My Documents\\workspace\\Plural\\permission-parser\\AccessPred.g:247:7: ( LETTER | '_' ) ( LETTER | DIGIT | '_' )*
            {
            if ( (input.LA(1)>='A' && input.LA(1)<='Z')||input.LA(1)=='_'||(input.LA(1)>='a' && input.LA(1)<='z') ) {
                input.consume();

            }
            else {
                MismatchedSetException mse =
                    new MismatchedSetException(null,input);
                recover(mse);    throw mse;
            }

            // C:\\Documents and Settings\\kbierhof\\My Documents\\workspace\\Plural\\permission-parser\\AccessPred.g:247:19: ( LETTER | DIGIT | '_' )*
            loop5:
            do {
                int alt5=2;
                int LA5_0 = input.LA(1);

                if ( ((LA5_0>='0' && LA5_0<='9')||(LA5_0>='A' && LA5_0<='Z')||LA5_0=='_'||(LA5_0>='a' && LA5_0<='z')) ) {
                    alt5=1;
                }


                switch (alt5) {
            	case 1 :
            	    // C:\\Documents and Settings\\kbierhof\\My Documents\\workspace\\Plural\\permission-parser\\AccessPred.g:
            	    {
            	    if ( (input.LA(1)>='0' && input.LA(1)<='9')||(input.LA(1)>='A' && input.LA(1)<='Z')||input.LA(1)=='_'||(input.LA(1)>='a' && input.LA(1)<='z') ) {
            	        input.consume();

            	    }
            	    else {
            	        MismatchedSetException mse =
            	            new MismatchedSetException(null,input);
            	        recover(mse);    throw mse;
            	    }


            	    }
            	    break;

            	default :
            	    break loop5;
                }
            } while (true);


            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end ID

    // $ANTLR start LETTER
    public final void mLETTER() throws RecognitionException {
        try {
            // C:\\Documents and Settings\\kbierhof\\My Documents\\workspace\\Plural\\permission-parser\\AccessPred.g:249:16: ( ( 'a' .. 'z' | 'A' .. 'Z' ) )
            // C:\\Documents and Settings\\kbierhof\\My Documents\\workspace\\Plural\\permission-parser\\AccessPred.g:249:18: ( 'a' .. 'z' | 'A' .. 'Z' )
            {
            if ( (input.LA(1)>='A' && input.LA(1)<='Z')||(input.LA(1)>='a' && input.LA(1)<='z') ) {
                input.consume();

            }
            else {
                MismatchedSetException mse =
                    new MismatchedSetException(null,input);
                recover(mse);    throw mse;
            }


            }

        }
        finally {
        }
    }
    // $ANTLR end LETTER

    // $ANTLR start NUMBER
    public final void mNUMBER() throws RecognitionException {
        try {
            int _type = NUMBER;
            // C:\\Documents and Settings\\kbierhof\\My Documents\\workspace\\Plural\\permission-parser\\AccessPred.g:251:8: ( ( DIGIT )+ )
            // C:\\Documents and Settings\\kbierhof\\My Documents\\workspace\\Plural\\permission-parser\\AccessPred.g:251:10: ( DIGIT )+
            {
            // C:\\Documents and Settings\\kbierhof\\My Documents\\workspace\\Plural\\permission-parser\\AccessPred.g:251:10: ( DIGIT )+
            int cnt6=0;
            loop6:
            do {
                int alt6=2;
                int LA6_0 = input.LA(1);

                if ( ((LA6_0>='0' && LA6_0<='9')) ) {
                    alt6=1;
                }


                switch (alt6) {
            	case 1 :
            	    // C:\\Documents and Settings\\kbierhof\\My Documents\\workspace\\Plural\\permission-parser\\AccessPred.g:251:11: DIGIT
            	    {
            	    mDIGIT(); 

            	    }
            	    break;

            	default :
            	    if ( cnt6 >= 1 ) break loop6;
                        EarlyExitException eee =
                            new EarlyExitException(6, input);
                        throw eee;
                }
                cnt6++;
            } while (true);


            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end NUMBER

    // $ANTLR start WHITESPACE
    public final void mWHITESPACE() throws RecognitionException {
        try {
            int _type = WHITESPACE;
            // C:\\Documents and Settings\\kbierhof\\My Documents\\workspace\\Plural\\permission-parser\\AccessPred.g:253:12: ( ( '\\t' | ' ' | '\\r' | '\\n' | '\\u000C' )+ )
            // C:\\Documents and Settings\\kbierhof\\My Documents\\workspace\\Plural\\permission-parser\\AccessPred.g:253:14: ( '\\t' | ' ' | '\\r' | '\\n' | '\\u000C' )+
            {
            // C:\\Documents and Settings\\kbierhof\\My Documents\\workspace\\Plural\\permission-parser\\AccessPred.g:253:14: ( '\\t' | ' ' | '\\r' | '\\n' | '\\u000C' )+
            int cnt7=0;
            loop7:
            do {
                int alt7=2;
                int LA7_0 = input.LA(1);

                if ( ((LA7_0>='\t' && LA7_0<='\n')||(LA7_0>='\f' && LA7_0<='\r')||LA7_0==' ') ) {
                    alt7=1;
                }


                switch (alt7) {
            	case 1 :
            	    // C:\\Documents and Settings\\kbierhof\\My Documents\\workspace\\Plural\\permission-parser\\AccessPred.g:
            	    {
            	    if ( (input.LA(1)>='\t' && input.LA(1)<='\n')||(input.LA(1)>='\f' && input.LA(1)<='\r')||input.LA(1)==' ' ) {
            	        input.consume();

            	    }
            	    else {
            	        MismatchedSetException mse =
            	            new MismatchedSetException(null,input);
            	        recover(mse);    throw mse;
            	    }


            	    }
            	    break;

            	default :
            	    if ( cnt7 >= 1 ) break loop7;
                        EarlyExitException eee =
                            new EarlyExitException(7, input);
                        throw eee;
                }
                cnt7++;
            } while (true);

             channel = HIDDEN; 

            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end WHITESPACE

    // $ANTLR start DIGIT
    public final void mDIGIT() throws RecognitionException {
        try {
            // C:\\Documents and Settings\\kbierhof\\My Documents\\workspace\\Plural\\permission-parser\\AccessPred.g:255:16: ( '0' .. '9' )
            // C:\\Documents and Settings\\kbierhof\\My Documents\\workspace\\Plural\\permission-parser\\AccessPred.g:255:18: '0' .. '9'
            {
            matchRange('0','9'); 

            }

        }
        finally {
        }
    }
    // $ANTLR end DIGIT

    public void mTokens() throws RecognitionException {
        // C:\\Documents and Settings\\kbierhof\\My Documents\\workspace\\Plural\\permission-parser\\AccessPred.g:1:8: ( T21 | T22 | T23 | T24 | T25 | T26 | T27 | ALT | TENS | WITH | DEQ | NEQ | IN | NULL | IMPLIES | TRUE | FALSE | ONE | ZERO | ID | NUMBER | WHITESPACE )
        int alt8=22;
        switch ( input.LA(1) ) {
        case '(':
            {
            alt8=1;
            }
            break;
        case ')':
            {
            alt8=2;
            }
            break;
        case ',':
            {
            alt8=3;
            }
            break;
        case '#':
            {
            alt8=4;
            }
            break;
        case '!':
            {
            int LA8_5 = input.LA(2);

            if ( (LA8_5=='f') ) {
                alt8=5;
            }
            else if ( (LA8_5=='=') ) {
                alt8=12;
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("1:1: Tokens : ( T21 | T22 | T23 | T24 | T25 | T26 | T27 | ALT | TENS | WITH | DEQ | NEQ | IN | NULL | IMPLIES | TRUE | FALSE | ONE | ZERO | ID | NUMBER | WHITESPACE );", 8, 5, input);

                throw nvae;
            }
            }
            break;
        case '=':
            {
            switch ( input.LA(2) ) {
            case '=':
                {
                alt8=11;
                }
                break;
            case '>':
                {
                alt8=15;
                }
                break;
            default:
                alt8=6;}

            }
            break;
        case '/':
            {
            alt8=7;
            }
            break;
        case 'a':
            {
            int LA8_8 = input.LA(2);

            if ( (LA8_8=='l') ) {
                int LA8_27 = input.LA(3);

                if ( (LA8_27=='t') ) {
                    int LA8_37 = input.LA(4);

                    if ( ((LA8_37>='0' && LA8_37<='9')||(LA8_37>='A' && LA8_37<='Z')||LA8_37=='_'||(LA8_37>='a' && LA8_37<='z')) ) {
                        alt8=20;
                    }
                    else {
                        alt8=8;}
                }
                else {
                    alt8=20;}
            }
            else {
                alt8=20;}
            }
            break;
        case '+':
            {
            alt8=8;
            }
            break;
        case 't':
            {
            switch ( input.LA(2) ) {
            case 'e':
                {
                int LA8_28 = input.LA(3);

                if ( (LA8_28=='n') ) {
                    int LA8_38 = input.LA(4);

                    if ( (LA8_38=='s') ) {
                        int LA8_47 = input.LA(5);

                        if ( ((LA8_47>='0' && LA8_47<='9')||(LA8_47>='A' && LA8_47<='Z')||LA8_47=='_'||(LA8_47>='a' && LA8_47<='z')) ) {
                            alt8=20;
                        }
                        else {
                            alt8=9;}
                    }
                    else {
                        alt8=20;}
                }
                else {
                    alt8=20;}
                }
                break;
            case 'r':
                {
                int LA8_29 = input.LA(3);

                if ( (LA8_29=='u') ) {
                    int LA8_39 = input.LA(4);

                    if ( (LA8_39=='e') ) {
                        int LA8_48 = input.LA(5);

                        if ( ((LA8_48>='0' && LA8_48<='9')||(LA8_48>='A' && LA8_48<='Z')||LA8_48=='_'||(LA8_48>='a' && LA8_48<='z')) ) {
                            alt8=20;
                        }
                        else {
                            alt8=16;}
                    }
                    else {
                        alt8=20;}
                }
                else {
                    alt8=20;}
                }
                break;
            default:
                alt8=20;}

            }
            break;
        case '*':
            {
            alt8=9;
            }
            break;
        case 'w':
            {
            int LA8_12 = input.LA(2);

            if ( (LA8_12=='i') ) {
                int LA8_30 = input.LA(3);

                if ( (LA8_30=='t') ) {
                    int LA8_40 = input.LA(4);

                    if ( (LA8_40=='h') ) {
                        int LA8_49 = input.LA(5);

                        if ( ((LA8_49>='0' && LA8_49<='9')||(LA8_49>='A' && LA8_49<='Z')||LA8_49=='_'||(LA8_49>='a' && LA8_49<='z')) ) {
                            alt8=20;
                        }
                        else {
                            alt8=10;}
                    }
                    else {
                        alt8=20;}
                }
                else {
                    alt8=20;}
            }
            else {
                alt8=20;}
            }
            break;
        case '&':
            {
            alt8=10;
            }
            break;
        case 'i':
            {
            switch ( input.LA(2) ) {
            case 'n':
                {
                int LA8_31 = input.LA(3);

                if ( ((LA8_31>='0' && LA8_31<='9')||(LA8_31>='A' && LA8_31<='Z')||LA8_31=='_'||(LA8_31>='a' && LA8_31<='z')) ) {
                    alt8=20;
                }
                else {
                    alt8=13;}
                }
                break;
            case 'm':
                {
                int LA8_32 = input.LA(3);

                if ( (LA8_32=='p') ) {
                    int LA8_42 = input.LA(4);

                    if ( (LA8_42=='l') ) {
                        int LA8_50 = input.LA(5);

                        if ( (LA8_50=='i') ) {
                            int LA8_56 = input.LA(6);

                            if ( (LA8_56=='e') ) {
                                int LA8_60 = input.LA(7);

                                if ( (LA8_60=='s') ) {
                                    int LA8_62 = input.LA(8);

                                    if ( ((LA8_62>='0' && LA8_62<='9')||(LA8_62>='A' && LA8_62<='Z')||LA8_62=='_'||(LA8_62>='a' && LA8_62<='z')) ) {
                                        alt8=20;
                                    }
                                    else {
                                        alt8=15;}
                                }
                                else {
                                    alt8=20;}
                            }
                            else {
                                alt8=20;}
                        }
                        else {
                            alt8=20;}
                    }
                    else {
                        alt8=20;}
                }
                else {
                    alt8=20;}
                }
                break;
            default:
                alt8=20;}

            }
            break;
        case 'n':
            {
            int LA8_15 = input.LA(2);

            if ( (LA8_15=='u') ) {
                int LA8_33 = input.LA(3);

                if ( (LA8_33=='l') ) {
                    int LA8_43 = input.LA(4);

                    if ( (LA8_43=='l') ) {
                        int LA8_51 = input.LA(5);

                        if ( ((LA8_51>='0' && LA8_51<='9')||(LA8_51>='A' && LA8_51<='Z')||LA8_51=='_'||(LA8_51>='a' && LA8_51<='z')) ) {
                            alt8=20;
                        }
                        else {
                            alt8=14;}
                    }
                    else {
                        alt8=20;}
                }
                else {
                    alt8=20;}
            }
            else {
                alt8=20;}
            }
            break;
        case 'f':
            {
            int LA8_16 = input.LA(2);

            if ( (LA8_16=='a') ) {
                int LA8_34 = input.LA(3);

                if ( (LA8_34=='l') ) {
                    int LA8_44 = input.LA(4);

                    if ( (LA8_44=='s') ) {
                        int LA8_52 = input.LA(5);

                        if ( (LA8_52=='e') ) {
                            int LA8_58 = input.LA(6);

                            if ( ((LA8_58>='0' && LA8_58<='9')||(LA8_58>='A' && LA8_58<='Z')||LA8_58=='_'||(LA8_58>='a' && LA8_58<='z')) ) {
                                alt8=20;
                            }
                            else {
                                alt8=17;}
                        }
                        else {
                            alt8=20;}
                    }
                    else {
                        alt8=20;}
                }
                else {
                    alt8=20;}
            }
            else {
                alt8=20;}
            }
            break;
        case 'o':
            {
            int LA8_17 = input.LA(2);

            if ( (LA8_17=='n') ) {
                int LA8_35 = input.LA(3);

                if ( (LA8_35=='e') ) {
                    int LA8_45 = input.LA(4);

                    if ( ((LA8_45>='0' && LA8_45<='9')||(LA8_45>='A' && LA8_45<='Z')||LA8_45=='_'||(LA8_45>='a' && LA8_45<='z')) ) {
                        alt8=20;
                    }
                    else {
                        alt8=18;}
                }
                else {
                    alt8=20;}
            }
            else {
                alt8=20;}
            }
            break;
        case 'z':
            {
            int LA8_18 = input.LA(2);

            if ( (LA8_18=='e') ) {
                int LA8_36 = input.LA(3);

                if ( (LA8_36=='r') ) {
                    int LA8_46 = input.LA(4);

                    if ( (LA8_46=='o') ) {
                        int LA8_54 = input.LA(5);

                        if ( ((LA8_54>='0' && LA8_54<='9')||(LA8_54>='A' && LA8_54<='Z')||LA8_54=='_'||(LA8_54>='a' && LA8_54<='z')) ) {
                            alt8=20;
                        }
                        else {
                            alt8=19;}
                    }
                    else {
                        alt8=20;}
                }
                else {
                    alt8=20;}
            }
            else {
                alt8=20;}
            }
            break;
        case 'A':
        case 'B':
        case 'C':
        case 'D':
        case 'E':
        case 'F':
        case 'G':
        case 'H':
        case 'I':
        case 'J':
        case 'K':
        case 'L':
        case 'M':
        case 'N':
        case 'O':
        case 'P':
        case 'Q':
        case 'R':
        case 'S':
        case 'T':
        case 'U':
        case 'V':
        case 'W':
        case 'X':
        case 'Y':
        case 'Z':
        case '_':
        case 'b':
        case 'c':
        case 'd':
        case 'e':
        case 'g':
        case 'h':
        case 'j':
        case 'k':
        case 'l':
        case 'm':
        case 'p':
        case 'q':
        case 'r':
        case 's':
        case 'u':
        case 'v':
        case 'x':
        case 'y':
            {
            alt8=20;
            }
            break;
        case '0':
        case '1':
        case '2':
        case '3':
        case '4':
        case '5':
        case '6':
        case '7':
        case '8':
        case '9':
            {
            alt8=21;
            }
            break;
        case '\t':
        case '\n':
        case '\f':
        case '\r':
        case ' ':
            {
            alt8=22;
            }
            break;
        default:
            NoViableAltException nvae =
                new NoViableAltException("1:1: Tokens : ( T21 | T22 | T23 | T24 | T25 | T26 | T27 | ALT | TENS | WITH | DEQ | NEQ | IN | NULL | IMPLIES | TRUE | FALSE | ONE | ZERO | ID | NUMBER | WHITESPACE );", 8, 0, input);

            throw nvae;
        }

        switch (alt8) {
            case 1 :
                // C:\\Documents and Settings\\kbierhof\\My Documents\\workspace\\Plural\\permission-parser\\AccessPred.g:1:10: T21
                {
                mT21(); 

                }
                break;
            case 2 :
                // C:\\Documents and Settings\\kbierhof\\My Documents\\workspace\\Plural\\permission-parser\\AccessPred.g:1:14: T22
                {
                mT22(); 

                }
                break;
            case 3 :
                // C:\\Documents and Settings\\kbierhof\\My Documents\\workspace\\Plural\\permission-parser\\AccessPred.g:1:18: T23
                {
                mT23(); 

                }
                break;
            case 4 :
                // C:\\Documents and Settings\\kbierhof\\My Documents\\workspace\\Plural\\permission-parser\\AccessPred.g:1:22: T24
                {
                mT24(); 

                }
                break;
            case 5 :
                // C:\\Documents and Settings\\kbierhof\\My Documents\\workspace\\Plural\\permission-parser\\AccessPred.g:1:26: T25
                {
                mT25(); 

                }
                break;
            case 6 :
                // C:\\Documents and Settings\\kbierhof\\My Documents\\workspace\\Plural\\permission-parser\\AccessPred.g:1:30: T26
                {
                mT26(); 

                }
                break;
            case 7 :
                // C:\\Documents and Settings\\kbierhof\\My Documents\\workspace\\Plural\\permission-parser\\AccessPred.g:1:34: T27
                {
                mT27(); 

                }
                break;
            case 8 :
                // C:\\Documents and Settings\\kbierhof\\My Documents\\workspace\\Plural\\permission-parser\\AccessPred.g:1:38: ALT
                {
                mALT(); 

                }
                break;
            case 9 :
                // C:\\Documents and Settings\\kbierhof\\My Documents\\workspace\\Plural\\permission-parser\\AccessPred.g:1:42: TENS
                {
                mTENS(); 

                }
                break;
            case 10 :
                // C:\\Documents and Settings\\kbierhof\\My Documents\\workspace\\Plural\\permission-parser\\AccessPred.g:1:47: WITH
                {
                mWITH(); 

                }
                break;
            case 11 :
                // C:\\Documents and Settings\\kbierhof\\My Documents\\workspace\\Plural\\permission-parser\\AccessPred.g:1:52: DEQ
                {
                mDEQ(); 

                }
                break;
            case 12 :
                // C:\\Documents and Settings\\kbierhof\\My Documents\\workspace\\Plural\\permission-parser\\AccessPred.g:1:56: NEQ
                {
                mNEQ(); 

                }
                break;
            case 13 :
                // C:\\Documents and Settings\\kbierhof\\My Documents\\workspace\\Plural\\permission-parser\\AccessPred.g:1:60: IN
                {
                mIN(); 

                }
                break;
            case 14 :
                // C:\\Documents and Settings\\kbierhof\\My Documents\\workspace\\Plural\\permission-parser\\AccessPred.g:1:63: NULL
                {
                mNULL(); 

                }
                break;
            case 15 :
                // C:\\Documents and Settings\\kbierhof\\My Documents\\workspace\\Plural\\permission-parser\\AccessPred.g:1:68: IMPLIES
                {
                mIMPLIES(); 

                }
                break;
            case 16 :
                // C:\\Documents and Settings\\kbierhof\\My Documents\\workspace\\Plural\\permission-parser\\AccessPred.g:1:76: TRUE
                {
                mTRUE(); 

                }
                break;
            case 17 :
                // C:\\Documents and Settings\\kbierhof\\My Documents\\workspace\\Plural\\permission-parser\\AccessPred.g:1:81: FALSE
                {
                mFALSE(); 

                }
                break;
            case 18 :
                // C:\\Documents and Settings\\kbierhof\\My Documents\\workspace\\Plural\\permission-parser\\AccessPred.g:1:87: ONE
                {
                mONE(); 

                }
                break;
            case 19 :
                // C:\\Documents and Settings\\kbierhof\\My Documents\\workspace\\Plural\\permission-parser\\AccessPred.g:1:91: ZERO
                {
                mZERO(); 

                }
                break;
            case 20 :
                // C:\\Documents and Settings\\kbierhof\\My Documents\\workspace\\Plural\\permission-parser\\AccessPred.g:1:96: ID
                {
                mID(); 

                }
                break;
            case 21 :
                // C:\\Documents and Settings\\kbierhof\\My Documents\\workspace\\Plural\\permission-parser\\AccessPred.g:1:99: NUMBER
                {
                mNUMBER(); 

                }
                break;
            case 22 :
                // C:\\Documents and Settings\\kbierhof\\My Documents\\workspace\\Plural\\permission-parser\\AccessPred.g:1:106: WHITESPACE
                {
                mWHITESPACE(); 

                }
                break;

        }

    }


 

}
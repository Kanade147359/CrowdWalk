// -*- mode: java; indent-tabs-mode: nil -*-
/** Think Formula, Logical and Control functions
 * @author:: Itsuki Noda
 * @version:: 0.0 2015/04/16 I.Noda
 * <B>History:</B>
 * <UL>
 *   <LI> [2015/04/16]: Create This File. </LI>
 *   <LI> [YYYY/MM/DD]: add more </LI>
 * </UL>
 * <B>Usage:</B>
 * ...
 */

package nodagumi.ananPJ.Agents.Think;

import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

import java.lang.reflect.InvocationTargetException ;

import nodagumi.ananPJ.Agents.AgentBase;
import nodagumi.ananPJ.Agents.RationalAgent;
import nodagumi.ananPJ.Agents.Think.ThinkEngine;
import nodagumi.ananPJ.Agents.Think.ThinkFormula;
import nodagumi.Itk.* ;

//======================================================================
/**
 * 論理演算および実行制御を表す式の処理系 (logica. functions).
 */
public class ThinkFormulaLogical extends ThinkFormula {
    //============================================================
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 代表インスタンス
     * (複数あっても無駄なので、単一である方が良い)
     * registerFormulas で登録。
     */
    static ThinkFormula singleton ;

    //============================================================
    //------------------------------------------------------------
    /**
     * 登録
     */
    static public boolean registerFormulas(Lexicon lexicon)
    {
	try {
	    Class currentClass =
		new Object(){}.getClass().getEnclosingClass() ;
	    singleton =
		(ThinkFormula)(currentClass.newInstance()) ;
	} catch(Exception ex) {
	    Itk.logError("wrong class definition") ;
	    ex.printStackTrace() ;
	}
        
	lexicon.register("true", singleton) ;
	lexicon.register("false", singleton) ;
	lexicon.register("not", singleton) ;
	lexicon.register("and", singleton) ;
	lexicon.register("or", singleton) ;
	lexicon.register("proc", singleton) ;
	lexicon.register("if", singleton) ;

	return true ;
    }

    //------------------------------------------------------------
    /**
     * 呼び出し.
     * <pre>
     * { "" : "true" } || "true"
     * { "" : "false" } || "false"
     * </pre>
     * あるいは以下のフォーマット
     * <ul>
     *   <li>{@link #call_not "not"}</li>
     *   <li>{@link #call_and "and"}</li>
     *   <li>{@link #call_or "or"}</li>
     *   <li>{@link #call_proc "proc"}</li>
     *   <li>{@link #call_if "if"}</li>
     * </ul>
     */
    @Override
    public Term call(String head, Term expr, ThinkEngine engine) {
	if(head.equals("true")) {
	    return Term_True ;
	} else if(head.equals("false")) {
	    return Term_False ;
	} else if(head.equals("not")) {
	    return call_not(expr, engine) ;
	} else if(head.equals("and")) {
	    return call_and(expr, engine) ;
	} else if(head.equals("or")) {
	    return call_or(expr, engine) ;
	} else if(head.equals("proc")) {
	    return call_proc(expr, engine) ;
	} else if(head.equals("if")) {
	    return call_if(expr, engine) ;
	} else {
	    Itk.logError("unknown expression", "expr=", expr) ;
	    return Term_Null ;
	}
    }

    //------------------------------------------------------------
    /**
     * 推論(not)。
     * <pre>
     * { "" : "not",
     *   "body" : _expr_ }
     * </pre>
     */
    public Term call_not(Term expr, ThinkEngine engine) {
        Term result = engine.think(expr.getArgTerm("body")) ;
        if(checkFalse(result)) {
            return Term_True ;
        } else {
            return Term_False ;
        }
    }

    //------------------------------------------------------------
    /**
     * 推論(And)。
     * <pre>
     * { "" : "and",
     *   "body" : [_expr_, _expr_, ...] }
     * </pre>
     */
    public Term call_and(Term expr, ThinkEngine engine) {
        Term body = expr.getArgTerm("body") ;
        if(!body.isArray()) {
	    Itk.logError("Illegal and body",
			 "expr=", expr) ;
            System.exit(1) ;
        }

        Term result = Term_True ;
        for(int i = 0 ; i < body.getArraySize() ; i++) {
            Term subExpr = body.getNthTerm(i) ;
            result = engine.think(subExpr) ;
            if(checkFalse(result)) break ;
        }
        return result ;
    }

    //------------------------------------------------------------
    /**
     * 推論(Or)。
     * <pre>
     * { "" : "or",
     *   "body" : [ _expr_, _expr_, ...] }
     * </pre>
     */
    public Term call_or(Term expr, ThinkEngine engine) {
        Term body = expr.getArgTerm("body") ;
        if(!body.isArray()) {
            Itk.logError("Illegal proc body") ;
            Itk.logError_("expr:", expr) ;
            System.exit(1) ;
        }

        Term result = Term_False ;
        for(int i = 0 ; i < body.getArraySize() ; i++) {
            Term subExpr = body.getNthTerm(i) ;
            result = engine.think(subExpr) ;
            if(!checkFalse(result)) break ;
        }
        return result ;
    }

    //------------------------------------------------------------
    /**
     * 推論(proc)。
     * <pre>
     * { "" : "proc",
     *   "body" : [ _expr_, _expr_, ...] }
     * OR
     * [ _expr, _expr_, ...]
     * </pre>
     */
    public Term call_proc(Term expr, ThinkEngine engine) {
        Term body = expr.getArgTerm("body") ;
	return call_procBody(body, engine) ;
    }

    //------------------------------------------------------------
    /**
     * 推論(proc)の本体。
     */
    public Term call_procBody(Term body, ThinkEngine engine) {
        if(!body.isArray()) {
            Itk.logError("Illegal proc body") ;
            Itk.logError_("body:", body) ;
            System.exit(1) ;
        }

        Term result = Term_Null ;
        for(int i = 0 ; i < body.getArraySize() ; i++) {
            Term subExpr = body.getNthTerm(i) ;
            result = engine.think(subExpr) ;
        }
        return result ;
    }

    //------------------------------------------------------------
    /**
     * 推論(if)。
     * <pre>
     * { "" : "if",
     *   "condition" : _expr_,
     *   "then" : _expr_,
     *   "else" : _expr_ }
     * </pre>
     */
    public Term call_if(Term expr, ThinkEngine engine) {
        Term condition = expr.getArgTerm("condition") ;
        Term thenExpr = expr.getArgTerm("then") ;
        Term elseExpr = expr.getArgTerm("else") ;

        if(condition == null || thenExpr == null) {
            Itk.logError("wrong if expr", expr) ;
            System.exit(1) ;
        }

        Term cond = engine.think(condition) ;

        if(!checkFalse(cond)) {
            return engine.think(thenExpr) ;
        } else {
            if(elseExpr != null) {
                return engine.think(elseExpr) ;
            } else {
                return Term_False ;
            }
        }
    }

    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    //------------------------------------------------------------
    //============================================================
} // class ThinkFormulaLogical


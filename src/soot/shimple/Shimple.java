/* Soot - a J*va Optimization Framework
 * Copyright (C) 2003 Navindra Umanee
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 * Boston, MA 02111-1307, USA.
 */

package soot.shimple;

import soot.*;
import soot.jimple.*;
import soot.shimple.internal.*;
import soot.util.*;
import java.util.*;
import java.io.*;

/**
 * Contains the constructors for the components of the Shimple (SSA
 * Jimple) grammar.  Methods are available to construct Shimple from
 * Jimple/Shimple, create Phi nodes, and converting back from
 * Shimple to Jimple.
 *
 * <p> We did not replicate those elements already available from
 * Jimple.v().
 *
 * @author Navindra Umanee
 * @see soot.shimple.Shimple
 * @see <a
 * href="http://citeseer.nj.nec.com/cytron91efficiently.html">Efficiently
 * Computing Static Single Assignment Form and the Control Dependence
 * Graph</a>
**/
public class Shimple
{
    private static Shimple shimpleRepresentation = new Shimple();

    protected Shimple()
    {
    }

    public static Shimple v()
    {
        return shimpleRepresentation;
    }

    /**
     * Returns an empty ShimpleBody associated with method m.
     **/
    public ShimpleBody newBody(SootMethod m)
    {
        return new ShimpleBody(m);
    }

    /**
     * Returns a ShimpleBody constructed from b, assumes phasename for
     * Shimple options is "shimple".
     **/
    public ShimpleBody newBody(Body b)
    {
        Map options = Scene.v().getPhaseOptions("shimple");
        return new ShimpleBody(b, options);
    }

    /**
     * Returns a ShimpleBody constructed from b.
     **/
    public ShimpleBody newBody(Body b, String phase)
    {
        Map options = Scene.v().getPhaseOptions(phase);
        return new ShimpleBody(b, options);
    }

    /**
     * Returns a ShimpleBody constructed from b with given options.
     *
     * <p> Currently available option is "naive-phi-elimination",
     * typically specified in the "shimple" phase (eg, -p shimple
     * naive-phi-elimination) which skips the dead code elimination
     * and register allocation phase before eliminating Phi nodes.
     * This can be useful for understanding the effect of analyses.
     **/
    public ShimpleBody newBody(Body b, String phase, String optionsString)
    {
        Map options = Scene.v().computePhaseOptions(phase, optionsString);
        return new ShimpleBody(b, options);
    }

    /**
     * Create a trivial Phi expression, where preds are an ordered
     * list of the control predecessor Blocks of the Phi expression.
     **/
    public PhiExpr newPhiExpr(Local leftLocal, List preds)
    {
        return new SPhiExpr(leftLocal, preds);
    }

    /**
     * Create a Phi expression with the provided list of Values
     * (Locals or Constants).
     **/
    public PhiExpr newPhiExpr(List args, List preds)
    {
        return new SPhiExpr(args, preds);
    }

    /**
     * Constructs a JimpleBody from a ShimpleBody.
     *
     * <p> Currently available option is "naive-phi-elimination",
     * typically specified in the "shimple" phase (eg, -p shimple
     * naive-phi-elimination) which skips the dead code elimination
     * and register allocation phase before eliminating Phi nodes.
     * This can be useful for understanding the effect of analyses.
     **/
    public JimpleBody newJimpleBody(ShimpleBody body)
    {
        return body.toJimpleBody();
    }

    /**
     * Misc utility function.  Returns true if the unit is a Phi node,
     * false otherwise.
     **/
    public static boolean isPhiNode(Unit unit)
    {
        if(getPhiExpr(unit) == null)
            return false;

        return true;
    }

    /**
     * Misc utility function.  Returns the corresponding PhiExpr if
     * the unit is a Phi node, null otherwise.
     **/
    public static PhiExpr getPhiExpr(Unit unit)
    {
        if(!(unit instanceof AssignStmt))
            return null;

        Value right = ((AssignStmt)unit).getRightOp();
        
        if(right instanceof PhiExpr)
            return (PhiExpr) right;

        return null;
    }

    /**
     * Misc utility function.  Returns the corresponding left Local if
     * the unit is a Phi node, null otherwise.
     **/
    public static Local getLhsLocal(Unit unit)
    {
        if(!(unit instanceof AssignStmt))
            return null;

        Value right = ((AssignStmt)unit).getRightOp();
        
        if(right instanceof PhiExpr){
            Value left = ((AssignStmt)unit).getLeftOp();
            return (Local) left;
        }

        return null;
    }

    /**
     * Misc utility function.  Perform the necessary fix ups when
     * removing a CFG block from the Unit chain.
     *
     * @see soot.PatchingChain
     **/
    public static void redirectToPreds(Unit remove, Chain unitChain)
    {
        /* Determine whether we should continue processing or not. */

        Iterator pointersIt = remove.getBoxesPointingToThis().iterator();
        
        if(!pointersIt.hasNext())
            return;
        
        while(pointersIt.hasNext()){
            UnitBox pointer = (UnitBox) pointersIt.next();

            // a PhiExpr may be involved, hence continue processing.
            // note that we will use the value of "pointer" and
            // continue iteration from where we left off.
            if(!pointer.isBranchTarget())
                break;

            // no PhiExpr's are involved, abort
            if(!pointersIt.hasNext())
                return;
        }

        /* Ok, continuing... */
            
        Set preds = new HashSet();
        Set phis  = new HashSet();
        
        // find fall-through pred
        if(!remove.equals(unitChain.getFirst())){
            Unit possiblePred = (Unit) unitChain.getPredOf(remove);
            if(!possiblePred.branches())
                preds.add(possiblePred);
        }

        // find the rest of the preds and all Phi's that point to remove
        Iterator unitsIt = unitChain.iterator();
        while(unitsIt.hasNext()){
            Unit unit = (Unit) unitsIt.next();
            Iterator targetsIt = unit.getUnitBoxes().iterator();
            while(targetsIt.hasNext()){
                UnitBox targetBox = (UnitBox) targetsIt.next();
                
                if(remove.equals(targetBox.getUnit())){
                    if(targetBox.isBranchTarget())
                        preds.add(unit);
                    else{
                        PhiExpr phiExpr = getPhiExpr(unit);
                        if(phiExpr != null)
                            phis.add(unit);
                    }
                }
            }
        }
        
        /* At this point we have found all the preds and relevant Phi's */

        /* Each Phi needs an argument for each pred. */
        Iterator phiIt = phis.iterator();
        while(phiIt.hasNext()){
            PhiExpr phiExpr = (PhiExpr) phiIt.next();
            int argIndex = phiExpr.getArgIndex(remove);

            if(argIndex == -1)
                throw new RuntimeException("Dazed and confused.");

            // now we've got the value!
            Value argValue = phiExpr.getArgBox(argIndex).getValue();
            phiExpr.removeArg(argIndex);

            // add new arguments to Phi
            Iterator predsIt = preds.iterator();
            while(predsIt.hasNext()){
                Unit pred = (Unit) predsIt.next();
                phiExpr.addArg(argValue, pred);
            }
        }
    }
}

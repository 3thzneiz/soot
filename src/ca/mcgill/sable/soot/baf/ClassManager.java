/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 * Baf, a Java(TM) bytecode analyzer framework.                      *
 * Copyright (C) 1997, 1998 Raja Vallee-Rai (kor@sable.mcgill.ca)    *
 * All rights reserved.                                              *
 *                                                                   *
 * This work was done as a project of the Sable Research Group,      *
 * School of Computer Science, McGill University, Canada             *
 * (http://www.sable.mcgill.ca/).  It is understood that any         *
 * modification not identified as such is not covered by the         *
 * preceding statement.                                              *
 *                                                                   *
 * This work is free software; you can redistribute it and/or        *
 * modify it under the terms of the GNU Library General Public       *
 * License as published by the Free Software Foundation; either      *
 * version 2 of the License, or (at your option) any later version.  *
 *                                                                   *
 * This work is distributed in the hope that it will be useful,      *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of    *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU *
 * Library General Public License for more details.                  *
 *                                                                   *
 * You should have received a copy of the GNU Library General Public *
 * License along with this library; if not, write to the             *
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330,      *
 * Boston, MA  02111-1307, USA.                                      *
 *                                                                   *
 * Java is a trademark of Sun Microsystems, Inc.                     *
 *                                                                   *
 * To submit a bug report, send a comment, or get the latest news on *
 * this project and other Sable Research Group projects, please      *
 * visit the web site: http://www.sable.mcgill.ca/                   *
 * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

/*
 Reference Version
 -----------------
 This is the latest official version on which this file is based.
 The reference version is: $BafVersion: 0.4 $

 Change History
 --------------
 A) Notes:

 Please use the following template.  Most recent changes should
 appear at the top of the list.

 - Modified on [date (March 1, 1900)] by [name]. [(*) if appropriate]
   [description of modification].

 Any Modification flagged with "(*)" was done as a project of the
 Sable Research Group, School of Computer Science,
 McGill University, Canada (http://www.sable.mcgill.ca/).

 You should add your copyright, using the following template, at
 the top of this file, along with other copyrights.

 *                                                                   *
 * Modifications by [name] are                                       *
 * Copyright (C) [year(s)] [your name (or company)].  All rights     *
 * reserved.                                                         *
 *                                                                   *

 B) Changes:

 - Modified on 15-Jun-1998 by Raja Vallee-Rai (kor@sable.mcgill.ca). (*)
   First internal release (Version 0.1).
*/
 
package ca.mcgill.sable.soot.baf;

import ca.mcgill.sable.util.*;

/**
 * The ClassManager is an object which keeps track of classes which have been
 * transformed to their Baf form.  Classes are sometimes automatically loaded
 * because they are referred to by another class which has been loaded.
 * Please note that referring to a class as a type will not cause the
 * class to be loaded.
 */
 
public class ClassManager
{
    List classes = new ArrayList(); 
    
    public ClassManager()
    {
    }
    
    public void addClass(BClass c) throws AlreadyManagedException, DuplicateNameException
    {
        if(c.isManaged())
            throw new AlreadyManagedException(c.getName());
        
        if(managesClass(c.getName()))
            throw new DuplicateNameException(c.getName());
            
        classes.add(c);
        c.isManaged = true;
        c.manager = this;
    }
    
    public void removeClass(BClass c) throws IncorrectManagerException
    {
        if(!c.isManaged() || c.getManager() != this)
            throw new IncorrectManagerException(c.getName());
        
        classes.remove(c);
        c.isManaged = false;
    }
    
    public boolean managesClass(String className)
    {
        Object[] elements = classes.toArray();
        
        for(int i = 0; i < elements.length; i++)
        {
            BClass c = (BClass) elements[i];
            
            if(c.getName().equals(className))
                return true;
        }
        
        return false;
    }
    
    /**
     * Returns the BClass with the given className.  Loads it if it is not present.
     */
     
    public BClass getClass(String className) throws ClassFileNotFoundException, 
                                             CorruptClassFileException,
                                             DuplicateNameException
    {
        Object[] elements = classes.toArray();
        
        for(int i = 0; i < elements.length; i++)
        {
            BClass c = (BClass) elements[i];
            
            if(c.getName().equals(className))
                return c;
        }

        // Not there, create an unresolved class.
        {
            BClass BClass = new BClass(className);
            
            addClass(BClass);
            
            return BClass;
        }
    }
    
    public List getClasses()
    {
        return Collections.unmodifiableList(classes);
    }
}

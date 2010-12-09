//
// $Id: $
//
// Depot library - a Java relational persistence library
// Copyright (C) 2006-2009 Michael Bayne and Pär Winzell
//
// This library is free software; you can redistribute it and/or modify it
// under the terms of the GNU Lesser General Public License as published
// by the Free Software Foundation; either version 2.1 of the License, or
// (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA

package com.samskivert.depot.impl.expression;

import java.util.Collection;

import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.expression.FluentExp;
import com.samskivert.depot.expression.SQLExpression;
import com.samskivert.util.StringUtil;

public interface Function
{
    String getCanonicalFunctionName ();

    public static abstract class NoArgFun<T> extends FluentExp<T> implements Function
    {
        public void addClasses (Collection<Class<? extends PersistentRecord>> classSet)
        {
            // nothing to add
        }
    }

    public static abstract class OneArgFun<T> extends FluentExp<T> implements Function
    {
        protected OneArgFun (SQLExpression<?> argument)
        {
            _arg = argument;
        }

        public void addClasses (Collection<Class<? extends PersistentRecord>> classSet)
        {
            _arg.addClasses(classSet);
        }

        public SQLExpression<?> getArg ()
        {
            return _arg;
        }

        @Override
        public String toString ()
        {
            return getCanonicalFunctionName() + "(" + _arg + ")";
        }

        protected SQLExpression<?> _arg;
    }

    public static abstract class TwoArgFun<T> extends FluentExp<T> implements Function
    {
        protected TwoArgFun (SQLExpression<?> arg1, SQLExpression<?> arg2)
        {
            _arg1 = arg1;
            _arg2 = arg2;
        }

        public void addClasses (Collection<Class<? extends PersistentRecord>> classSet)
        {
            _arg1.addClasses(classSet);
            _arg2.addClasses(classSet);
        }

        @Override
        public String toString ()
        {
            return getCanonicalFunctionName() + "(" + _arg1 + ", " + _arg2 + ")";
        }

        protected SQLExpression<?> _arg1, _arg2;
    }

    public static abstract class ManyArgFun<T> extends ArgumentExp<T> implements Function
    {
        protected ManyArgFun (SQLExpression<?>... args)
        {
            super(args);
        }

        // we specialize for arity two to allow subtypes who require proper generic types to
        // provide arity two constructors for use in 99% of the cases where you don't need
        // arbitrary arguments and don't want to have to suppress the annoying use-site generic
        // varargs array creation warning
        protected ManyArgFun (SQLExpression<?> arg1, SQLExpression<?> arg2)
        {
            super(arg1, arg2);
        }

        @Override
        public String toString ()
        {
            return getCanonicalFunctionName() + "(" + StringUtil.join(_args, ", ") + ")";
        }
    }
}

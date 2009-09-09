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

package com.samskivert.depot.function;

import com.samskivert.depot.expression.ArgumentExp;
import com.samskivert.depot.expression.SQLExpression;
import com.samskivert.depot.impl.ExpressionVisitor;

public abstract class ConditionalFun
{
    public static class Coalesce extends ArgumentExp {
        public Coalesce (SQLExpression... args) {
            super(args);
        }
        public Object accept (ExpressionVisitor<?> visitor) {
            return visitor.visit(this);
        }
    }

    public static class Greatest extends ArgumentExp {
        public Greatest (SQLExpression... args) {
            super(args);
        }
        public Object accept (ExpressionVisitor<?> visitor) {
            return visitor.visit(this);
        }
    }

    public static class Least extends ArgumentExp {
        public Least (SQLExpression... args) {
            super(args);
        }
        public Object accept (ExpressionVisitor<?> visitor) {
            return visitor.visit(this);
        }
    }
}

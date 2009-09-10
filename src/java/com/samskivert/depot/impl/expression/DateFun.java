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

import com.samskivert.depot.expression.SQLExpression;
import com.samskivert.depot.impl.ExpressionVisitor;

public abstract class DateFun
{
    public static class DatePart extends OneArgFun {
        public enum Part {
            DAY_OF_MONTH, DAY_OF_WEEK, DAY_OF_YEAR, HOUR, MINUTE, MONTH,
            SECOND, WEEK, YEAR, EPOCH
        };
        public DatePart (SQLExpression date, Part part) {
            super(date);
            _part = part;
        }
        public Object accept (ExpressionVisitor<?> visitor) {
            return visitor.visit(this);
        }
        public Part getPart () {
            return _part;
        }
        protected Part _part;
    }

    public static class DateTruncate extends OneArgFun {
        /**
         * The degree of truncation to perform, in time units. Currently only DAY, due to lacking
         * MySQL support, but we hope for future versions to match PostgreSQL.
         */
        public enum Truncation {
            DAY,
        };
        /**
         * Truncate a SQL timestamp value, currently only to the nearest day (Truncation.DAY) due
         * to lacking MySQL support, but we hope for future versions to match PostgreSQL.
         */
        public DateTruncate (SQLExpression date, Truncation truncation) {
            super(date);
            _truncation= truncation;
        }
        public Object accept (ExpressionVisitor<?> visitor) {
            return visitor.visit(this);
        }
        public Truncation getTruncation () {
            return _truncation;
        }
        protected Truncation _truncation;
    }

    public static class Now extends NoArgFun {
        public Object accept (ExpressionVisitor<?> visitor) {
            return visitor.visit(this);
        }
    }
}

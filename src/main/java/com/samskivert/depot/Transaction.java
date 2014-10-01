//
// Depot library - a Java relational persistence library
// https://github.com/threerings/depot/blob/master/LICENSE

package com.samskivert.depot;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.ConnectionProvider;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Allows database operations to be performed in a transaction. Transactions can be performed
 * manually as follows:
 * <pre>{@code
 * PersistenceContext ctx = ...;
 * Transaction tx = Transaction.start(ctx);
 * try {
 *   _someRepo.updateFoo(...);
 *   _someOtherRepo.updateBar(...);
 *   tx.commit();
 * } catch (RuntimeException re)  {
 *   tx.rollback();
 *   throw re;
 * }
 * }</pre>
 *
 * Or one can make use of the {@link Transaction#perform} helper method:
 * <pre>{@code
 * PersistenceContext ctx = ...;
 * Transaction.perform(ctx, new Runnable() {
 *   public void run () {
 *     _someRepo.updateFoo(...);
 *     _someOtherRepo.updateBar(...);
 *   }
 * });
 * }</pre>
 *
 * <p><b>Caveats:</b></p>
 *
 * <p>All repositories involved in a transaction must use the same {@code PersistenceContext}
 * supplied when starting the transaction. Attempting to use a repository which references a
 * different context will cause an exception to be thrown (and the transaction to be aborted,
 * assuming you've structured your code correctly).</p>
 *
 * <p>Transactions may not be nested.</p>
 */
public class Transaction {

    /**
     * Starts a transaction using the supplied persistence context. You <em>must</em> eventually
     * call {@link #commit} or {@link #rollback} on the returned transaction.
     */
    public static Transaction start (PersistenceContext ctx)
    {
        Transaction tx = get();
        if (tx != null) throw new DatabaseException("Nested transactions not supported.");
        tx = new Transaction(ctx);
        _activeTx.set(tx);
        return tx;
    }

    /**
     * Returns the currently active transaction (on the caller's thread), or null.
     */
    public static Transaction get ()
    {
        return _activeTx.get();
    }

    /**
     * Returns true if a transaction is currently active on the caller's thread.
     */
    public static boolean inTransaction ()
    {
        return get() != null;
    }

    /**
     * Performs {@code op} inside a transaction, committing it if {@code op} completes
     * successfully, rolling it back if {@code op} throws any exceptions.
     */
    public static void perform (PersistenceContext ctx, Runnable op)
    {
        Transaction tx = start(ctx);
        try {
            op.run();
            tx.commit();
        } catch (RuntimeException re) {
            tx.rollback();
            throw re;
        }
    }

    /**
     * Commits this transaction.
     */
    public void commit ()
    {
        if (_conn != null) {
            checkActive("commit");
            try {
                _conn.commit();
            } catch (SQLException sqe) {
                throw new DatabaseException("Transaction commit failure", sqe);
            } finally {
                release();
            }
        }
    }

    /**
     * Aborts this transaction, rolling back any database operations performed thus far.
     */
    public void rollback ()
    {
        if (_conn != null) {
            checkActive("rollback");
            try {
                _conn.rollback();
            } catch (SQLException sqe) {
                throw new DatabaseException("Transaction rollback failure", sqe);
            } finally {
                release();
            }
        }
    }

    /** The persistence context in which this transaction is operating. */
    public final PersistenceContext ctx;

    Connection getConnection ()
        throws PersistenceException
    {
        if (_conn == null) _conn = ctx._conprov.getTxConnection(ctx._ident);
        return _conn;
    }

    void connectionFailed (SQLException sqe)
    {
        ctx._conprov.txConnectionFailed(ctx._ident, _conn, sqe);
    }

    protected Transaction (PersistenceContext ctx)
    {
        this.ctx = ctx;
    }

    protected void checkActive (String action) {
        if (_activeTx.get() != this) throw new IllegalStateException(
            "Attempted to " + action + " non-active transaction");
    }

    protected void release () {
        ctx._conprov.releaseTxConnection(ctx._ident, _conn);
        _conn = null;
        _activeTx.set(null);
    }

    protected static final ThreadLocal<Transaction> _activeTx = new ThreadLocal<Transaction>();

    /** The connection being used for this transaction. */
    protected Connection _conn;
}

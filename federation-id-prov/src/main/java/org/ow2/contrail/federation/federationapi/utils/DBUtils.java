package org.ow2.contrail.federation.federationapi.utils;

import com.mysql.jdbc.exceptions.MySQLIntegrityConstraintViolationException;

import javax.persistence.RollbackException;
import java.sql.SQLIntegrityConstraintViolationException;

public class DBUtils {

    public static boolean isIntegrityConstraintException(RollbackException e) {
        Throwable t = e.getCause();
        if (t != null) {
            Throwable t1 = t.getCause();
            if (t1 instanceof MySQLIntegrityConstraintViolationException ||
                    t1 instanceof SQLIntegrityConstraintViolationException) {
                return true;
            }
        }
        return false;
    }
}

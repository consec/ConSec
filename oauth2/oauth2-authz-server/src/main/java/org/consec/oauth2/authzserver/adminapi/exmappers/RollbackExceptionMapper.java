package org.consec.oauth2.authzserver.adminapi.exmappers;

import com.mysql.jdbc.exceptions.MySQLIntegrityConstraintViolationException;

import javax.persistence.RollbackException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.sql.SQLIntegrityConstraintViolationException;

@Provider
public class RollbackExceptionMapper implements ExceptionMapper<RollbackException> {

    public Response toResponse(RollbackException e) {
        if (isIntegrityConstraintException(e)) {
            return Response.status(Response.Status.CONFLICT).build();
        }
        else {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    public boolean isIntegrityConstraintException(RollbackException e) {
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

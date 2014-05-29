package org.herasaf.xacml.core.function.impl.specialMatchFunctions;

import org.herasaf.xacml.core.function.AbstractFunction;
import org.herasaf.xacml.core.function.FunctionProcessingException;

public class DescendantOrSelfMatchFunction extends AbstractFunction {

    /**
     * XACML function ID.
     */
    public static final String ID = "urn:contrail:function:descendant-or-self-match";

    private static final long serialVersionUID = 1L;

    /**
     * {@inheritDoc} <br>
     * <br>
     * Returns true if the first argument matches the second argument according
     * the specification on page 125 (A.3.14, special match functions) in <a
     * href=
     * "http://www.oasis-open.org/committees/tc_home.php?wg_abbrev=xacml#XACML20"
     * > OASIS eXtensible Access Control Markup Langugage (XACML) 2.0, Errata 29
     * June 2006</a>
     */
    public Object handle(Object... args) throws FunctionProcessingException {
        try {
            if (args.length != 2) {
                throw new FunctionProcessingException(
                        "Invalid number of parameters.");
            }
            String parentPath = trimPath(((String) args[0]));
            String childPath = trimPath(((String) args[1]));
            if (parentPath.equals(childPath)) {
                return true;
            }
            else {
                return childPath.startsWith(parentPath + "/");
            }
        }
        catch (ClassCastException e) {
            throw new FunctionProcessingException(
                    "The arguments were of the wrong datatype.");
        }
        catch (FunctionProcessingException e) {
            throw e;
        }
        catch (Exception e) {
            throw new FunctionProcessingException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getFunctionId() {
        return ID;
    }

    private String trimPath(String path) {
        path = path.trim();
        if (path.endsWith("/")) {
            return path.substring(0, path.length() - 1);
        }
        else {
            return path;
        }
    }
}

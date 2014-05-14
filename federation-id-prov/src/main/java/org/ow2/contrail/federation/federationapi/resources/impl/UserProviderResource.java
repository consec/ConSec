/**
 *
 */
package org.ow2.contrail.federation.federationapi.resources.impl;

import org.apache.log4j.Logger;
import org.ow2.contrail.federation.federationapi.interfaces.BaseSingle;
import org.ow2.contrail.federation.federationapi.utils.JSONObject;
import org.ow2.contrail.federation.federationdb.jpa.entities.Provider;
import org.ow2.contrail.federation.federationdb.jpa.entities.User;

import javax.ws.rs.core.Response;
import java.util.ArrayList;

/**
 * @author ales
 */
public class UserProviderResource implements BaseSingle {

    protected static Logger logger =
            Logger.getLogger(UserProviderResource.class);

    Provider provider = null;
    User user = null;

    public UserProviderResource(User user, Provider provider) {
        this.provider = provider;
        this.user = user;
    }

    /* (non-Javadoc)
     * @see org.ow2.contrail.federation.federationapi.interfaces.BaseSingle#getSubresources()
     */
    @Override
    public ArrayList<String> getSubresources() throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Response get() throws Exception {
        logger.debug("entering get");
        JSONObject ovf = new JSONObject();
        ovf.put("link", String.format("/providers/%d", this.provider));
        logger.debug("Exiting get");
        return Response.ok(ovf.toString()).build();
    }

    @Override
    public Response delete() throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Response put(String content) throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

}

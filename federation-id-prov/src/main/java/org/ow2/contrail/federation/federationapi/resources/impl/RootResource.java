package org.ow2.contrail.federation.federationapi.resources.impl;

import org.json.JSONObject;
import org.ow2.contrail.federation.federationapi.resources.IRootResource;

import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import java.util.ArrayList;

@Path("/")
public class RootResource implements IRootResource {

    public ArrayList<String> getSubresources() throws Exception {
        ArrayList<String> subresources = new ArrayList<String>();
        subresources.add("users");
        subresources.add("attributes");
        subresources.add("idps");
        subresources.add("roles");
        subresources.add("groups");
        subresources.add("ovfs");
        subresources.add("providers");
        return subresources;
    }

    @Override
    public Response get() throws Exception {
        JSONObject ret = new JSONObject();
        ret.put("resources", this.getSubresources());
        return Response.ok(ret).build();
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

/**
 *
 */
package org.ow2.contrail.federation.federationapi.resources.impl;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.ow2.contrail.federation.federationapi.interfaces.BaseSingle;
import org.ow2.contrail.federation.federationdb.jpa.dao.ApplicationDAO;
import org.ow2.contrail.federation.federationdb.jpa.entities.Application;
import org.ow2.contrail.federation.federationdb.utils.PersistenceUtils;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.List;

/**
 * @author ales
 */
@Path("/applications")
public class ApplicationsResource {

    protected static Logger logger =
            Logger.getLogger(ApplicationsResource.class);

    /**
     * Returns list of collection.
     *
     * @return
     */
    @GET
    @Produces("application/json")
    public Response get() throws Exception {
        logger.debug("Entering get");
        EntityManager em = PersistenceUtils.getInstance().getEntityManager();
        try {
            Query query = em.createNamedQuery("Application.findAll");
            List<Application> appList = query.getResultList();
            JSONArray UriList = new JSONArray();
            for (Application app : appList) {
                String uri = String.format("/applications/%d", app.getApplicationId());
                JSONObject o = new JSONObject();
                o.put("name", app.getName());
                o.put("uri", uri);
                UriList.put(o);
            }
            logger.debug("Exiting get");
            return Response.ok(UriList.toString()).build();
        }
        finally {
            logger.debug("Exiting get");
            PersistenceUtils.getInstance().closeEntityManager(em);
        }


    }

    /**
     * Sub-resource locator method. Returns the sub-resource object that can handle the remainder
     * of the request.
     *
     * @param application id
     * @return
     */
    @Path("/{id}")
    public BaseSingle subResource(@PathParam("id") int id) throws Exception {
        logger.debug("Entering subResource.");
        Application app = ApplicationDAO.findById(id);
        if (app == null) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        else {
            return new ApplicationResource(app);
        }
    }

}

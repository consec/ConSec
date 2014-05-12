package org.consec.oauth2.authzserver.adminapi;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.consec.oauth2.authzserver.jpa.dao.OwnerDao;
import org.consec.oauth2.authzserver.jpa.entities.CountryTrust;
import org.consec.oauth2.authzserver.jpa.entities.CountryTrustPK;
import org.consec.oauth2.authzserver.jpa.entities.Owner;
import org.consec.oauth2.authzserver.utils.PersistenceUtils;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URISyntaxException;
import java.util.List;

@Path("/owners/{ownerUuid}/trust/countries")
public class CountryTrustResource {

    private String ownerUuid;

    @Context
    UriInfo uriInfo;

    public CountryTrustResource(@PathParam("ownerUuid") String ownerUuid) {
        this.ownerUuid = ownerUuid;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public JSONArray getOrganizationsTrust() throws JSONException {
        EntityManager em = PersistenceUtils.getInstance().getEntityManager();
        try {
            Owner owner = new OwnerDao(em).findByUuid(ownerUuid);
            if (owner == null) {
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }

            TypedQuery<CountryTrust> q = em.createNamedQuery("CountryTrust.findByOwnerId", CountryTrust.class);
            q.setParameter("ownerId", owner.getId());
            List<CountryTrust> countryTrustList = q.getResultList();

            JSONArray countryTrustArr = new JSONArray();
            for (CountryTrust countryTrust : countryTrustList) {
                JSONObject o = new JSONObject();
                o.put("country_code", countryTrust.getCountry().getCode());
                o.put("is_trusted", countryTrust.getIsTrusted());

                countryTrustArr.put(o);
            }
            return countryTrustArr;
        }
        finally {
            PersistenceUtils.getInstance().closeEntityManager(em);
        }
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addCountryTrust(JSONArray data) throws JSONException, URISyntaxException {
        EntityManager em = PersistenceUtils.getInstance().getEntityManager();
        try {
            Owner owner = new OwnerDao(em).findByUuid(ownerUuid);
            if (owner == null) {
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }

            if (data.length() == 0) {
                return Response.noContent().build();
            }

            try {
                em.getTransaction().begin();
                for (int i = 0; i < data.length(); i++) {
                    JSONObject countryData = data.getJSONObject(i);
                    String countryCode = countryData.getString("country_code");
                    boolean isTrusted = countryData.getBoolean("is_trusted");

                    CountryTrust countryTrust = em.find(CountryTrust.class,
                            new CountryTrustPK(owner.getId(), countryCode));
                    if (countryTrust != null) {
                        countryTrust.setIsTrusted(isTrusted);
                        em.merge(countryTrust);
                    }
                    else {
                        countryTrust = new CountryTrust(owner.getId(), countryCode);
                        countryTrust.setIsTrusted(isTrusted);
                        em.persist(countryTrust);
                    }
                }
                em.getTransaction().commit();
            }
            catch (JSONException e) {
                em.getTransaction().rollback();
                throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                        .entity(e.getMessage()).build());
            }

            return Response.noContent().build();
        }
        finally {
            PersistenceUtils.getInstance().closeEntityManager(em);
        }
    }
}

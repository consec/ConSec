package org.consec.authz.xacml.filter.utils;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.log4j.Logger;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.codehaus.jettison.json.JSONObject;
import org.consec.authz.xacml.common.XACMLDecision;
import org.consec.authz.xacml.common.xacmlrequest.Action;
import org.consec.authz.xacml.common.xacmlrequest.Subject;
import org.consec.authz.xacml.common.xacmlrequest.SubjectList;
import org.consec.authz.xacml.common.xacmlrequest.XACMLRequest;
import org.consec.common.authn.Principal;

import javax.servlet.http.HttpServletRequest;
import java.io.FileInputStream;
import java.net.URI;
import java.security.KeyStore;
import java.security.Security;
import java.util.Scanner;

public class PDPClient {
    private static Logger log = Logger.getLogger(PDPClient.class);

    private URI pdpEndpoint;
    private String keystoreFile;
    private String keystorePass;
    private String truststoreFile;
    private String truststorePass;

    public PDPClient(URI pdpEndpoint, String keystoreFile, String keystorePass,
                     String truststoreFile, String truststorePass) {
        this.pdpEndpoint = pdpEndpoint;
        this.keystoreFile = keystoreFile;
        this.keystorePass = keystorePass;
        this.truststoreFile = truststoreFile;
        this.truststorePass = truststorePass;

        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    public XACMLDecision evaluate(HttpServletRequest request) throws Exception {

        Principal principal = (Principal) request.getAttribute("CONSEC_PRINCIPAL");
        if (principal == null) {
            throw new Exception("No authentication data found.");
        }

        // XACML request subject
        SubjectList subjectList = new SubjectList();
        subjectList.addSubject(new Subject(Subject.Type.USER, principal.getUserId()));

        for (String roleId : principal.getUserRoles()) {
            subjectList.addSubject(new Subject(Subject.Type.ROLE, roleId));
        }

        for (String groupId : principal.getUserGroups()) {
            subjectList.addSubject(new Subject(Subject.Type.GROUP, groupId));
        }

        // XACML request resource
        String resourceURI = request.getPathInfo();

        // XACML request action
        String method = request.getMethod();
        Action action;
        if (method.equals("GET") || method.equals("OPTIONS") || method.equals("HEAD")) {
            action = Action.READ;
        }
        else if (method.equals("POST")) {
            action = Action.WRITE;
        }
        else if (method.equals("PUT")) {
            action = Action.WRITE;
        }
        else if (method.equals("DELETE")) {
            action = Action.WRITE;
        }
        else {
            throw new Exception("Invalid HTTP method: " + method);
        }

        XACMLRequest xacmlRequest = new XACMLRequest(subjectList, resourceURI, action);
        HttpPost httpPost = new HttpPost(pdpEndpoint);
        HttpEntity entity = new StringEntity(xacmlRequest.toJson().toString());
        httpPost.setEntity(entity);
        httpPost.setHeader("Content-Type", "application/json");
        HttpClient httpClient = createHttpClient(pdpEndpoint);
        HttpResponse httpResponse = httpClient.execute(httpPost);

        if (httpResponse.getStatusLine().getStatusCode() != 200) {
            throw new Exception("Invalid response received from the PDP: " + httpResponse.getStatusLine());
        }

        Scanner scanner = new Scanner(httpResponse.getEntity().getContent(), "UTF-8").useDelimiter("\\A");
        String content = scanner.next();
        JSONObject result = new JSONObject(content);
        XACMLDecision xacmlDecision = XACMLDecision.valueOf(result.getString("decision"));
        return xacmlDecision;
    }

    private HttpClient createHttpClient(URI uri) throws Exception {
        // read in the keystore from the filesystem, this should contain a single keypair
        KeyStore clientKeyStore = KeyStore.getInstance("JKS");
        clientKeyStore.load(new FileInputStream(keystoreFile), keystorePass.toCharArray());

        // read in the truststore from the filesystem, this should contain a single keypair
        KeyStore trustStore = KeyStore.getInstance("JKS");
        trustStore.load(new FileInputStream(truststoreFile), truststorePass.toCharArray());

        // set up the socketfactory, to use our keystore for client authentication.
        SSLSocketFactory socketFactory = new SSLSocketFactory(
                SSLSocketFactory.TLS,
                clientKeyStore,
                keystorePass,
                trustStore,
                null,
                null,
                SSLSocketFactory.STRICT_HOSTNAME_VERIFIER);

        // create and configure scheme registry
        SchemeRegistry registry = new SchemeRegistry();
        registry.register(new Scheme("https", uri.getPort(), socketFactory));

        // create a client connection manager to use in creating httpclients
        ThreadSafeClientConnManager mgr = new ThreadSafeClientConnManager(registry);

        // create the client based on the manager, and use it to make the call
        return new DefaultHttpClient(mgr);
    }
}

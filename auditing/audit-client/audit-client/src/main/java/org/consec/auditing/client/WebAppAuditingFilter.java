package org.consec.auditing.client;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.consec.auditing.common.auditevent.*;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.*;
import java.net.InetAddress;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WebAppAuditingFilter implements Filter {
    private static Logger log = Logger.getLogger(WebAppAuditingFilter.class);

    private Auditor auditor;
    private boolean auditRequestData;
    private boolean auditResponseData;
    private int auditRequestDataSizeLimit;
    private int auditResponseDataSizeLimit;
    private List<String> responseMimeTypesToAudit;
    private String localID;
    private String localHostName;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        log.debug("Initializing WebAppAuditingFilter...");

        String configFilePath = filterConfig.getInitParameter("configuration-file");
        if (configFilePath == null) {
            throw new ServletException("WebAppAuditingFilter: missing parameter 'configuration-file'.");
        }
        Properties props = new Properties();
        try {
            props.load(new FileInputStream(configFilePath));
            log.debug(String.format("Properties loaded successfully from file '%s'.", configFilePath));
        }
        catch (IOException e) {
            String message = String.format("Failed to read properties file '%s': %s", configFilePath, e.getMessage());
            log.error(message, e);
            throw new ServletException(message, e);
        }

        try {
            AuditorFactory.init(props);
            auditor = AuditorFactory.getAuditor();

            auditRequestData = Boolean.valueOf(props.getProperty("auditing.auditRequestData"));
            auditResponseData = Boolean.valueOf(props.getProperty("auditing.auditResponseData"));
            auditRequestDataSizeLimit =
                    Integer.parseInt(props.getProperty("auditing.auditRequestData.sizeLimit"));
            auditResponseDataSizeLimit =
                    Integer.parseInt(props.getProperty("auditing.auditResponseData.sizeLimit"));

            // which MIME types of response content to audit
            String responseMimeTypes = props.getProperty("auditing.auditResponseData.mimeTypes");
            responseMimeTypesToAudit = new ArrayList<String>();
            if (responseMimeTypes != null && !responseMimeTypes.equals("")) {
                String[] responseMimeTypesArr = responseMimeTypes.split(",");
                Collections.addAll(responseMimeTypesToAudit, responseMimeTypesArr);
            }

            localID = props.getProperty("auditing.localID");
            localHostName = InetAddress.getLocalHost().getCanonicalHostName();

            log.info("WebAppAuditingFilter initialized successfully.");
        }
        catch (Exception e) {
            log.error("Failed to initialize WebAppAuditingFilter: " + e.getMessage(), e);
            throw new ServletException("Failed to initialize WebAppAuditingFilter: " + e.getMessage(), e);
        }
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) servletRequest;
        HttpServletResponse httpResponse = (HttpServletResponse) servletResponse;

        BufferedRequestWrapper bufferedRequest = null;
        HttpServletResponseCopier responseCopier = null;
        if (auditRequestData) {
            bufferedRequest = new BufferedRequestWrapper(httpRequest);
        }
        if (auditResponseData) {
            responseCopier = new HttpServletResponseCopier(httpResponse);
        }

        try {
            filterChain.doFilter(
                    auditRequestData ? bufferedRequest : httpRequest,
                    auditResponseData ? responseCopier : httpResponse);
            if (responseCopier != null) {
                responseCopier.flushBuffer();
            }
        }
        finally {
            try {
                log.debug("Auditing HTTP request...");
                String requestContent = null;
                if (bufferedRequest != null) {
                    requestContent = new String(bufferedRequest.getBuffer());
                    if (auditRequestDataSizeLimit > 0 &&
                            requestContent.length() > auditRequestDataSizeLimit) {
                        requestContent = requestContent.substring(0, auditRequestDataSizeLimit) + "...";
                    }
                }
                String responseContent = null;
                if (responseCopier != null &&
                        responseMimeTypesToAudit.contains(httpResponse.getContentType())) {
                    byte[] copy = responseCopier.getCopy();
                    responseContent = new String(copy, httpResponse.getCharacterEncoding());
                    if (auditResponseDataSizeLimit > 0 &&
                            responseContent.length() > auditResponseDataSizeLimit) {
                        responseContent = responseContent.substring(0, auditResponseDataSizeLimit) + "...";
                    }
                }

                AuditEvent auditEvent = createAuditEvent(httpRequest, httpResponse, requestContent, responseContent);
                auditor.audit(auditEvent);
            }
            catch (Exception e) {
                log.error("Failed to audit HTTP request: " + e.getMessage(), e);
            }
        }
    }

    @Override
    public void destroy() {
        log.debug("Destroying WebAppAuditingFilter...");
        try {
            auditor.close();
        }
        catch (Exception e) {
            log.error("Failed to destroy WebAppAuditingFilter: " + e.getMessage(), e);
        }
        log.info("WebAppAuditingFilter destroyed successfully.");
    }

    private AuditEvent createAuditEvent(HttpServletRequest httpRequest, HttpServletResponse httpResponse,
                                        String requestContent, String responseContent) throws JSONException {
        AuditEvent event = new AuditEvent();
        event.setEventType("API_CALL");
        event.setEventTime(new Date());

        // action
        String action = null;
        if (httpRequest.getMethod().equals("GET")) {
            action = "read";
        }
        else if (httpRequest.getMethod().equals("POST")) {
            action = "create";
        }
        else if (httpRequest.getMethod().equals("PUT")) {
            action = "update";
        }
        else if (httpRequest.getMethod().equals("DELETE")) {
            action = "delete";
        }
        else if (httpRequest.getMethod().equals("OPTIONS")) {
            action = "read";
        }
        else if (httpRequest.getMethod().equals("HEAD")) {
            action = "read";
        }
        event.setAction(action);

        // outcome
        Outcome outcome;
        if (httpResponse.getStatus() < 400) {
            outcome = Outcome.SUCCESS;
        }
        else {
            outcome = Outcome.FAILURE;
        }
        event.setOutcome(outcome);

        // initiator
        Initiator initiator = new Initiator();
        if (httpRequest.isSecure()) {
            X509Certificate[] certs =
                    (X509Certificate[]) httpRequest.getAttribute("javax.servlet.request.X509Certificate");
            if (certs != null) {
                String clientDN = certs[0].getSubjectDN().getName();
                initiator.setId(clientDN);
            }
            else {
                initiator.setId(httpRequest.getRemoteAddr());
            }
        }
        else {
            initiator.setId(httpRequest.getRemoteAddr());
        }
        // extract OAuth2 access token if exists
        String authzHeader = httpRequest.getHeader("Authorization");
        if (authzHeader != null) {
            Pattern bearerTokenPattern = Pattern.compile("^Bearer ([\\w-]+)$");
            Matcher m = bearerTokenPattern.matcher(authzHeader);
            if (m.find()) {
                String accessToken = m.group(1);
                initiator.setOauthAccessToken(accessToken);
            }
        }
        event.setInitiator(initiator);

        // target
        Target target = new Target();
        target.setId(localID);
        target.setHost(localHostName);
        event.setTarget(target);

        // HTTP request data
        JSONObject httpRequestData = new JSONObject();
        httpRequestData.put("method", httpRequest.getMethod());
        httpRequestData.put("contentType", httpRequest.getContentType());
        httpRequestData.put("url", httpRequest.getRequestURL().toString());
        if (requestContent != null) {
            httpRequestData.put("content", requestContent);
        }
        Attachment httpRequestAtt = new Attachment("httpRequestData", "application/json", httpRequestData.toString());
        event.addAttachment(httpRequestAtt);

        // HTTP response data
        JSONObject httpResponseData = new JSONObject();
        httpResponseData.put("statusCode", httpResponse.getStatus());
        httpResponseData.put("contentType", httpResponse.getContentType());
        if (responseContent != null) {
            httpResponseData.put("content", responseContent);
        }
        Attachment httpResponseAtt = new Attachment("httpResponseData", "application/json", httpResponseData.toString());
        event.addAttachment(httpResponseAtt);

        return event;
    }

    public class HttpServletResponseCopier extends HttpServletResponseWrapper {

        private ServletOutputStream outputStream;
        private PrintWriter writer;
        private ServletOutputStreamCopier copier;

        public HttpServletResponseCopier(HttpServletResponse response) throws IOException {
            super(response);
        }

        @Override
        public ServletOutputStream getOutputStream() throws IOException {
            if (writer != null) {
                throw new IllegalStateException("getWriter() has already been called on this response.");
            }

            if (outputStream == null) {
                outputStream = getResponse().getOutputStream();
                copier = new ServletOutputStreamCopier(outputStream);
            }

            return copier;
        }

        @Override
        public PrintWriter getWriter() throws IOException {
            if (outputStream != null) {
                throw new IllegalStateException("getOutputStream() has already been called on this response.");
            }

            if (writer == null) {
                copier = new ServletOutputStreamCopier(getResponse().getOutputStream());
                writer = new PrintWriter(new OutputStreamWriter(copier, getResponse().getCharacterEncoding()), true);
            }

            return writer;
        }

        @Override
        public void flushBuffer() throws IOException {
            if (writer != null) {
                writer.flush();
            }
            else if (outputStream != null) {
                copier.flush();
            }
        }

        @Override
        public void sendError(int sc) throws IOException {
            super.sendError(sc);
        }

        @Override
        public void sendError(int sc, String msg) throws IOException {
            super.sendError(sc, msg);
        }

        public byte[] getCopy() {
            if (copier != null) {
                return copier.getCopy();
            }
            else {
                return new byte[0];
            }
        }
    }

    public class ServletOutputStreamCopier extends ServletOutputStream {

        private OutputStream outputStream;
        private ByteArrayOutputStream copy;

        public ServletOutputStreamCopier(OutputStream outputStream) {
            this.outputStream = outputStream;
            this.copy = new ByteArrayOutputStream(1024);
        }

        @Override
        public void write(int b) throws IOException {
            outputStream.write(b);
            copy.write(b);
        }

        public byte[] getCopy() {
            return copy.toByteArray();
        }
    }

    private class BufferedServletInputStream extends ServletInputStream {

        ByteArrayInputStream bais;

        public BufferedServletInputStream(ByteArrayInputStream bais) {
            this.bais = bais;
        }

        public int available() {
            return bais.available();
        }

        public int read() {
            return bais.read();
        }

        public int read(byte[] buf, int off, int len) {
            return bais.read(buf, off, len);
        }

    }

    private class BufferedRequestWrapper extends HttpServletRequestWrapper {

        ByteArrayInputStream bais;

        ByteArrayOutputStream baos;

        BufferedServletInputStream bsis;

        byte[] buffer;

        public BufferedRequestWrapper(HttpServletRequest req) throws IOException {
            super(req);
            InputStream is = req.getInputStream();
            baos = new ByteArrayOutputStream();
            byte buf[] = new byte[1024];
            int letti;
            while ((letti = is.read(buf)) > 0) {
                baos.write(buf, 0, letti);
            }
            buffer = baos.toByteArray();
        }

        public ServletInputStream getInputStream() {
            try {
                bais = new ByteArrayInputStream(buffer);
                bsis = new BufferedServletInputStream(bais);
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }

            return bsis;
        }

        public byte[] getBuffer() {
            return buffer;
        }

    }
}

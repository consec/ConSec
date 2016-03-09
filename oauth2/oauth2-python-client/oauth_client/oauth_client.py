#!/usr/bin/env python
'''
Created on Feb 3, 2013

@author: ales
'''

from wsgiref.simple_server import make_server
import requests, json
from oauth_client_conf import OAuthClientConfiguration
import logging, urllib
import uuid
from OpenSSL import SSL, crypto

PRIKEY_NBITS = 2048

logging.basicConfig(filename='oauth_client.log',level=logging.DEBUG)
logger = logging.getLogger("oauth_client")
oauth_client_conf = OAuthClientConfiguration("../conf/oauth_client.cfg")

class State:
    oauth_state = None

client_state=State()

class DelegatedCertificate(object):
    """ Represents user's delegated certificate
    """    
    raw = None
    subject=None
    issuer=None
    valid_not_after=None
    serial_num=None
    version=None
    
    def __init__(self, cert,cert_raw):
        self.raw=cert_raw       
        self.subject = str(cert.get_subject())
        self.issuer=str(cert.get_issuer());
        self.valid_not_after=str(cert.get_notAfter())
        self.serial_num=str(cert.get_serial_number())
        self.version=str(cert.get_version())
                
    def __str__(self):
        return json.dumps({'issuer':self.issuer, 'subject':self.subject,'valid_not_after':self.valid_not_after,'version':self.version,'serial_num':self.serial_num,'raw':self.raw}, sort_keys=True, indent=4)


def get_authorization_request_uri():
    state=uuid.uuid1()
    uri='%s/authorize?response_type=%s&client_id=%s&redirect_uri=%s&state=%s&scope=%s' % (oauth_client_conf.authz_server_auth_endpoint,
                                                                        oauth_client_conf.authz_server_response_type, 
                                                                        oauth_client_conf.client_id,
                                                                        urllib.quote(oauth_client_conf.client_callback_url, safe="%=&?~#!$,;'@()*[]"),
                                                                        str(state),
                                                                        urllib.quote(oauth_client_conf.client_scope, safe="%=&?~#!$,;'@()*[]"),
                                                                        )
    #logger.debug("HTTP call %s :" % uri)
    #uri = urllib.quote(uri, safe="%/:=&?~#+!$,;'@()*[]")
    logger.debug("HTTP call %s :" % uri)
    return uri, state

def _state_err(start_response):
    response_body = "State error"
    response_headers = [('Content-Type', 'text/html'),
                       ('Content-Length', str(len(response_body)))]
    status = '401 Unauthorized'
    start_response(status, response_headers)
    return []

def create_key_pair(n_bits_for_key=PRIKEY_NBITS):
        """Generate key pair and return as PEM encoded string
        @type n_bits_for_key: int
        @param n_bits_for_key: number of bits for private key generation - 
        default is 2048
        @rtype: OpenSSL.crypto.PKey
        @return: public/private key pair
        """
        key_pair = crypto.PKey()
        key_pair.generate_key(crypto.TYPE_RSA, n_bits_for_key)
        
        return key_pair

def create_cert_req(key_pair, cn ,message_digest="md5"):
        """Create a certificate request.
        
        @type CN: basestring
        @param CN: Common Name for certificate - effectively the same as the
        username for the MyProxy credential
        @type keyPair: string/None
        @param keyPair: public/private key pair
        @type messageDigest: basestring
        @param messageDigest: message digest type - default is MD5
        @rtype: base string
        @return certificate request PEM text and private key PEM text
        """
        
        # Check all required certifcate request DN parameters are set                
        # Create certificate request
        cert_req = crypto.X509Req()        
        # Create public key object
        cert_req.set_pubkey(key_pair)
        cert_req.get_subject().CN=cn
        
        # Add the public key to the request
        cert_req.sign(key_pair, message_digest)
        
        cert_req = crypto.dump_certificate_request(crypto.FILETYPE_PEM, 
                                                   cert_req)
        
        return cert_req

def get_cert(access_token):
    logger.debug('Calling CA with access_token')
    key_pair = create_key_pair()
    cert_req = create_cert_req(key_pair, "TestUser")
    encoded_cert_req = cert_req.replace('+', '%2B')
    req = "%s=%s\n" % ('certificate_request', encoded_cert_req)
    uri = (("%s?%s") % (oauth_client_conf.ca_server_url,req))
    logger.debug("URI for CA req: %s" % (uri))
    tokenheader = ("Bearer %s" % access_token)            
    resp=requests.post(
                               url=uri, 
                               data=req, 
                               headers={'Authorization':  tokenheader, 'Accept-Encoding':"identity" }, 
                               cert=(oauth_client_conf.client_cert, oauth_client_conf.client_key), 
                               verify=False
                               )
    pem_out = resp._content
    cert = crypto.load_certificate(crypto.FILETYPE_PEM, pem_out)
    cert = DelegatedCertificate(cert,pem_out)
    logger.debug('Got cert: %s' % cert)
    return cert

def application(environ, start_response):
  
    logger.debug("Got an HTTP call %s :" % environ.get('REQUEST_METHOD'))
    
    response_body = ""
        
    if environ.get('REQUEST_METHOD') == 'GET':
        
        if environ.get('PATH_INFO') == '/get_cert':
            get_cert("1234")
                
        # Is this a root call ?
        if environ.get('PATH_INFO') == '/':
            uri, state = get_authorization_request_uri()
            client_state.oauth_state=str(state)
            logger.debug("Calling GET %s" % uri)
            response_headers = [('Location', uri ),]
            status = '301 Redirect'
            start_response(status, response_headers)
            return []
       
        # Is this callback ?
        if environ.get('PATH_INFO') == ('/%s' % oauth_client_conf.client_callback_url.rsplit("/",1)[1]):
            logger.debug('Entering code_consumer')
            q=environ.get('QUERY_STRING')
            oauth_state = None
            oauth_code = None
            for s in q.split('&'):
                t=s.split('=')
                if t[0] == 'state':
                    oauth_state = t[1] 
                if t[0] == 'code':
                    oauth_code = t[1]
            if oauth_state != client_state.oauth_state:
                _state_err(start_response)
                return []
            logger.debug('Got code %s' % oauth_code)
            body="grant_type=%s&client_id=%s&client_secret=%s&code=%s&redirect_uri=%s&scope=%s" % ( oauth_client_conf.client_grant_type,
                                                                                                 oauth_client_conf.client_id,
                                                                                                 oauth_client_conf.client_secret,
                                                                                                 oauth_code,
                                                                                                 oauth_client_conf.client_callback_url,
                                                                                                 urllib.quote(oauth_client_conf.client_scope, safe="%=&?~#!$,;'@()*[]"),
                                                                                                )            
            logger.debug('Body: %s ' % body)
            token_url =  '%s/r/access_token/request' % oauth_client_conf.authz_server_auth_endpoint
            resp=requests.post(
                               url=token_url, 
                               data=body, 
                               headers={'Content-Type':'application/x-www-form-urlencoded'}, 
                               cert=(oauth_client_conf.client_cert, oauth_client_conf.client_key), 
                               verify=False
                               )
            access_token=str(json.loads(resp._content).get('access_token'))
            logger.debug('Got access_token: %s' % access_token)
            response_body = 'Got access_token: %s' % access_token
            # Uncomment if you want to use it with ca

            # cert=get_cert(access_token)
            # response_body=str(cert)
            # logger.debug(str(cert))
        
    response_headers = [('Content-Type', 'text/html'),
                        ('Content-Length', str(len(response_body)))]
    status = '200 OK'
    start_response(status, response_headers)
    return []

httpd = make_server(oauth_client_conf.client_host, oauth_client_conf.client_port, application)
httpd.serve_forever()

import logging
import json
import requests
import tempfile
import os, stat

from django.http import HttpResponseServerError
from django.contrib.auth.models import AnonymousUser, update_last_login
from django.contrib.auth.signals import user_logged_in
from django.contrib.sessions.backends.file import SessionStore
from OpenSSL import crypto
from federweb.base.models import federation

logger = logging.getLogger(__name__)


class DelegatedCertificate(object):
    
    PRIKEY_NBITS = 2048
    
    """ Represents user's delegated certificate
    """    
    raw = None
    subject=None
    issuer=None
    valid_not_after=None
    serial_num=None
    version=None
    
    key_pair = None
    cert_req = None   
    cert = None 
    pkey_file_path = None    
    
    def __init__(self, cert=None, cert_raw=None):        
        if (cert != None) and (cert_raw != None):
            self.raw=cert_raw 
            self.subject = str(cert.get_subject())
            self.issuer=str(cert.get_issuer());
            self.valid_not_after=str(cert.get_notAfter())
            self.serial_num=str(cert.get_serial_number())
            self.version=str(cert.get_version())
    
    def _create_key_pair(self, n_bits_for_key=PRIKEY_NBITS):
        """Generate key pair and return as PEM encoded string
        @type n_bits_for_key: int
        @param n_bits_for_key: number of bits for private key generation - 
        default is 2048
        @rtype: OpenSSL.crypto.PKey
        @return: public/private key pair
        """
        self.key_pair = crypto.PKey()
        self.key_pair.generate_key(crypto.TYPE_RSA, n_bits_for_key)        
        return self.key_pair

    def _create_cert_req(self, key_pair, cn ,message_digest="md5"):
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
        self.cert_req = crypto.X509Req()        
        # Create public key object
        self.cert_req.set_pubkey(key_pair)
        self.cert_req.get_subject().CN=cn        
        # Add the public key to the request
        self.cert_req.sign(key_pair, message_digest)
        
        self.cert_req = crypto.dump_certificate_request(crypto.FILETYPE_PEM, 
                                                   self.cert_req)        
        return self.cert_req    
    
    def write_key_to_file(self):
        """
        Writes private key to a temp file on under /tmp
        """
        tup = tempfile.mkstemp()
        self.pkey_file_path=tup[1]
        txt_key=crypto.dump_privatekey(crypto.FILETYPE_PEM,self.key_pair)
        logger.debug("Writing to %s" % str(self.pkey_file_path))
        f = open(str(self.pkey_file_path), "w")
        f.write(txt_key)
        f.close()
        os.chmod(self.pkey_file_path, stat.S_IREAD)       
    
    def get_cert(self, access_token, ca_uri, client_cert, client_key, username="TestUser"):
        try:
            logger.debug('Calling CA with oauth2 access_token')
            self.key_pair = self._create_key_pair()
            self.cert_req = self._create_cert_req(self.key_pair, username)
            encoded_cert_req = self.cert_req.replace('+', '%2B')
            req = "%s=%s\n" % ('certificate_request', encoded_cert_req)
            uri = (("%s?%s") % (ca_uri,req))
            logger.debug("URI for CA req: %s" % (uri))
            tokenheader = ("Bearer %s" % access_token)
            resp=requests.post(
                                       url=uri,
                                       data=req,
                                       headers={'Authorization':  tokenheader, 'Accept-Encoding':"identity" },
                                       cert=(client_cert, client_key),
                                       verify=False
                                       )
            pem_out = resp._content
            self.cert = crypto.load_certificate(crypto.FILETYPE_PEM, pem_out)
            self.cert = DelegatedCertificate(self.cert, pem_out)
            logger.debug('Got cert: %s' % self.cert)
            return self.cert
        except Exception as e:
            logger.error(e)
            # raise HttpResponseServerError("Error while getting user certificate: %s " % e)

    def __str__(self):
        return json.dumps({'issuer':self.issuer, 'subject':self.subject,'valid_not_after':self.valid_not_after,'version':self.version,'serial_num':self.serial_num,'raw':self.raw}, sort_keys=True, indent=4)
            
class RestEngineBackend(object):
    supports_object_permissions = False
    supports_anonymous_user = True

    def _get_loa_uuid(self):
        """
        Gets current_loa's UUID
        """
        attributes = federation.attributes.all()
        for attribute in attributes:
            if attribute.name == "urn:contrail:names:federation:subject:current-loa":
                return attribute.id
        return None

    def _set_user_current_loa(self, user=None):
        """
        Sets Current Loa Attribute to user
        """
        uuid = self._get_loa_uuid()
        attributes = user.attributes.all()
        current_loa_attr = None
        for attribute in attributes:
            if attribute.id == uuid:
                current_loa_attr = attribute
                break
        if current_loa_attr:
            #Update User's attribute
            current_loa_attr.value = user.current_loa
            current_loa_attr.save(True)
        else:
            # Add user's attribute
            current_loa_attr = user.attributes.new()
            current_loa_attr.attributeUuid = uuid

            current_loa_attr.value = user.current_loa
            current_loa_attr.save()


    def authenticate(self, username=None, password=None, session_info=None, **kwargs):        
        if session_info is not None:
            if 'uid' in session_info['ava']:                
                for user in federation.users.all():                
                    if user.username == session_info['ava']['uid'][0]:
                        ret_user = federation.users.get(user.id)
                        try:
                            ret_user.current_loa = session_info['ava']['current_loa'][0]
                            logger.debug("user.current_loa = %s " % ret_user.current_loa)
                            self._set_user_current_loa(ret_user)
                        except Exception as e:
                            logger.debug("Can not get user's current_loa attribute: %s " % e)
                        return ret_user
        
        try:
            ret_user = federation.users.authenticate(username, password)
            return ret_user
        except:
            import traceback
            traceback.print_exc()

    def get_user(self, user_id):
        return federation.users.get(user_id) or None

backend = RestEngineBackend()

def get_user(user_id):
    if not user_id:
        return AnonymousUser()
    
    return backend.get_user(user_id) or AnonymousUser()

user_logged_in.disconnect(update_last_login)

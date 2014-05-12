'''
Created on Feb 3, 2013

@author: ales
'''
import ConfigParser

class OAuthClientConfiguration:
    client_host = None
    client_port = None
    client_callback_url = None
    client_callback_url_atc = None
    client_id = None    
    client_secret = None
    client_grant_type = None
    client_scope = None
    authz_server_auth_endpoint = None
    authz_server_response_type = None
    ca_server_url = None
    pickle_file = None
    
    client_cert = None
    client_key = None
    client_key_pwd = None
    
    def __init__(self, configuration_file='../conf/oauth_client.cfg'):
        self.read_configuration_oauth_client(configuration_file)

    def read_configuration_oauth_client(self, configuration_file):
        config = ConfigParser.ConfigParser()
        config.read(configuration_file)

        self.client_host=config.get('oauth_client', 'host')
        self.client_port=int(config.get('oauth_client', 'port'))        
        self.client_callback_url=config.get('oauth_client', 'callback_url')
        self.client_callback_url_atc=config.get('oauth_client', 'callback_url_atc')
        self.client_id=config.get('oauth_client', 'client_id')
        self.client_secret=config.get('oauth_client', 'client_secret')
        self.client_grant_type=config.get('oauth_client', 'grant_type')
        self.client_truststore=config.get('oauth_client', 'truststore')
        self.client_scope=config.get('oauth_client', 'scope')
        
        self.client_cert=config.get('oauth_client', 'client_cert')
        self.client_key=config.get('oauth_client', 'client_key')
        self.client_key_pwd=config.get('oauth_client', 'client_key_pwd')



        self.authz_server_auth_endpoint=config.get('oauth_authz_server', 'auth_endpoint')
        self.authz_server_access_token_endpoint=config.get('oauth_authz_server', 'access_token_endpoint')
        self.authz_server_response_type=config.get('oauth_authz_server', 'response_type')



        self.ca_server_url=config.get('ca_server', 'url')

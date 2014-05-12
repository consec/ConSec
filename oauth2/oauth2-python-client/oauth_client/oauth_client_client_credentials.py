#!/usr/bin/env python
'''
Created on Feb 3, 2013

@author: ales
'''

from wsgiref.simple_server import make_server
import requests, json
from oauth_client_conf import OAuthClientConfiguration
import logging

logging.basicConfig(filename='oauth_client.log',level=logging.DEBUG)
logger = logging.getLogger("oauth_client_client_credentials")
oauth_client_conf = OAuthClientConfiguration('conf/oauth_client_credentials.cfg')
  
def application(environ, start_response):
  
    logger.debug("Got an HTTP call %s :" % environ.get('REQUEST_METHOD'))
    
    if environ.get('REQUEST_METHOD') == 'GET':
        if environ.get('PATH_INFO') == '/':
            uri = '%s/authorize?response_type=%s&client_id=%s' % (oauth_client_conf.authz_server_url,oauth_client_conf.authz_server_response_type, oauth_client_conf.client_id)
            logger.debug("Calling GET %s" % uri)
            response_headers = [('Location', uri ),]
            status = '301 Redirect'
            start_response(status, response_headers)
            return []
       
        if environ.get('PATH_INFO') == '/code_consumer':
            logger.debug('Entering code_consumer')
            oauth_code=environ.get('QUERY_STRING').split("=")[1]
            logger.debug('Got code %s' % oauth_code)
            body="grant_type=%s&client_id=%s&client_secret=%s&code=%s&redirect_uri=%s" % ( oauth_client_conf.client_grant_type,
                                                                                                 oauth_client_conf.client_id,
                                                                                                 oauth_client_conf.client_secret,
                                                                                                 oauth_code,
                                                                                                 oauth_client_conf.client_callback_url,
                                                                                                )            
            logger.debug('Body: %s ' % body)
            token_url =  '%s/r/access_token/request' % oauth_client_conf.authz_server_url
            resp=requests.post(url=token_url, data=body, headers={'Content-Type':'application/x-www-form-urlencoded'})
            access_token=str(json.loads(resp._content).get('access_token'))
            logger.debug('Got access_token: %s' % access_token)
        
    response_body = ""
    response_headers = [('Content-Type', 'text/html'),
                        ('Content-Length', str(len(response_body)))]
    status = '200 OK'
    start_response(status, response_headers)
    return []


httpd = make_server(oauth_client_conf.client_host, oauth_client_conf.client_port, application)
httpd.serve_forever()

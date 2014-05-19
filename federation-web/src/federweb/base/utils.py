import logging
from django.http import HttpResponse
from django.conf import settings
import requests

logger = logging.getLogger(__name__)

def cached_property(f):
    """returns a cached property that is calculated by function f"""
    def get(self):
        try:
            return self._property_cache[f]
        except AttributeError:
            self._property_cache = {}
            x = self._property_cache[f] = f(self)
            return x
        except KeyError:
            x = self._property_cache[f] = f(self)
            return x
        
    return property(get)

def send_oauth_request(method, url, token, data=None):
    headers = {
        "Accept": "application/json",
        "Content-Type": "application/json",
        "Authorization": "Bearer %s" % token
    }

    client_cert = settings.OAUTH2_CONFIG['oauth_client']['client_cert']
    client_key = settings.OAUTH2_CONFIG['oauth_client']['client_key']

    logger.debug("Sending %s request to %s using access token %s" % (method, url, token))
    if data:
        logger.debug("%s request content: %s" % (method, data))

    r = requests.request(
        method=method,
        url=url,
        data=data,
        headers=headers,
        cert=(client_cert, client_key),
        verify=False
    )

    response = HttpResponse(r.text)
    response.status_code = r.status_code
    if 'Content-Type' in r.headers:
        response['Content-Type'] = r.headers['Content-Type']
    if 'Location' in r.headers:
        response['Location'] = r.headers['Location']

    return response

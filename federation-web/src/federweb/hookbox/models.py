import urllib
import logging

from requests.exceptions import ConnectionError

from django.utils import simplejson as json
from django.conf import settings

from federweb.base.rest import JsonResource

logger = logging.getLogger(__name__)

class Pusher(object):
    def __init__(self, user):
        self.user = user
        
        if settings.HOOKBOX_BASE:
            self.resource = JsonResource(settings.HOOKBOX_BASE + '/web')
            self.secret = settings.HOOKBOX_SECRET
            self.channel = self.user.username
    
    def push(self, data):
        if settings.HOOKBOX_BASE:
            params = {
                'security_token': self.secret,
                'channel_name': self.channel,
                'payload': json.dumps(data),
                'originator': 'app'
            }
            
            path = '/publish?%s' % urllib.urlencode(params)
            
            try:
                resp = self.resource.get(path, raw=True)
                
                if resp.ok:
                    result = json.loads(resp.content)
                    
                    if result[0]:
                        return True
                    else:
                        logger.error('HookBox publish error: %s' % result[1])
                
                return False
            except ConnectionError:
                logger.error('HookBox publish connection error')
                return False

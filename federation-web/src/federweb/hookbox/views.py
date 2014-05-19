import logging

from django.views.decorators.csrf import csrf_exempt
from django.http import HttpResponse
from django.utils import simplejson as json

from federweb.hookbox.models import Pusher

logger = logging.getLogger(__name__)

@csrf_exempt
def hookbox(request):
    action = request.POST['action']
    
    pusher = Pusher(request.user)
    
    username = pusher.user.username
    
    if action == 'connect':
        logger.info('Hookbox connect %s' % username)
        
        data = [True, {'name': username}]
        
    elif action == 'create_channel':
        channel_name = request.POST['channel_name']
        
        logger.info('Hookbox create_channel %s' % channel_name)
        
        if channel_name == pusher.channel:
            data = [True, {'history_size': 30, 'history': [], 'presenceful': True, 'reflective': True}]
        else:
            data = [False, {}]
         
    elif action in ['subscribe', 'publish', 'unsubscribe', 'disconnect']:
        logger.info('Hookbox %s' % action)
        
        data = [True, {}]
    
    else:
        logger.info('Hookbox invalid action %s' % action)
        
        data = [False, {}]
    
    return HttpResponse(json.dumps(data), mimetype='application/JavaScript')

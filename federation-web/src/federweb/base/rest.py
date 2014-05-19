import logging

from django.utils import simplejson as json
from django.conf import settings
import requests

logger = logging.getLogger(__name__)

def pre_request_hook(request):
    msg = '%s %s' % (request.method, request.full_url)
    logger.debug(msg, extra=dict(request=request))

def response_hook(response):
    request = response.request
    msg = '%s %s %s' % (request.method, request.full_url, response.status_code)
    logger.info(msg, extra=dict(request=request, response=response))

hooks = dict(pre_request=pre_request_hook, response=response_hook)

class RestManager(object):
    def __init__(self, parent):
        self.parent = parent
    
    @property
    def session(self):
        return self.parent.session
    
    def prepare_headers(self, headers):
        hs = self.session.headers.copy()
        hs['accept'] = 'application/json'
        hs.update(headers or {})
        return hs
    
    @classmethod
    def uri_to_id(cls, uri):
        if uri:
            try:
                return str(uri).split('/')[-1]
            except ValueError:
                pass
    
    def build_uri(self, id=None):
        uri_parts = [self.get_uri()]
        
        id = self.uri_to_id(id)
        
        if id:
            uri_parts.append(str(id)) 
        
        uri = '/'.join(uri_parts)
        
        return uri, id
    
    def get_uri(self):
        return self.get_base_uri() + self.get_relative_uri()
    
    def get_base_uri(self):
        return self.parent.get_base_uri()
    
    def new(self, *args, **kwargs):
        raise NotImplementedError
    
    def build_object(self, data, obj=None):
        if obj is None:
            obj = self.new()
        
        obj.load(data)
        return obj
    
    def get(self, id=None, headers=None, obj=None):
        headers = self.prepare_headers(headers)
        
        if id is None:
            id = obj.id
        
        uri, id = self.build_uri(id)
        resp = self.session.get(uri, headers=headers)
        
        if resp.headers.get('content-type', None) == 'application/json':
            data = json.loads(resp.content)
            data['id'] = id
            return self.build_object(data, obj)
        
        return resp
    
    def all(self, headers=None):
        headers = self.prepare_headers(headers)
        
        resp = self.session.get(self.get_uri(), headers=headers)
        
        if resp.headers.get('content-type', None) == 'application/json':
            objs = json.loads(resp.content)
            return map(self.build_object, objs)
        
        if resp.status_code == 404:
            return []
        
        return resp
    
    def full(self, headers=None):
        return [self.get(x.id, headers) for x in self.all(headers)]

class RestModel(object):
    def __init__(self, manager):
        self.manager = manager
        self._original_data = {}
        
        self._reserved_keys = dir(self)
        
        self.id = None
    
    @property
    def session(self):
        return self.manager.session
    
    def get_base_uri(self):
        return self.manager.get_base_uri()
    
    def get_relative_uri(self):
        if self.id:
            return self.manager.get_relative_uri() + '/' + str(self.id)
        
        return self.manager.get_relative_uri()
    
    def get_uri(self):
        return self.manager.get_base_uri() + self.get_relative_uri()
    
    def load(self, data):
        if 'id' not in data and data.get('uri', None):
            data['id'] = self.manager.uri_to_id(data['uri'])
        
        if not data.get('id', None):
            data['id'] = None
        
        for key, value in data.items():
            self._original_data[str(key)] = value
            
            if not key in self._reserved_keys:
                setattr(self, str(key), value)
    
    def reload(self):
        self.manager.get(obj=self)
    
    def _get_data(self):
        if hasattr(self, 'raw'):
            return self.raw
        
        return dict((k, v) for k, v in self.__dict__.items()
                    if not k.startswith('_') and k not in self._reserved_keys
                    and v is not None)
    
    def save(self, force_put=False, headers=None):
        headers = self.manager.prepare_headers(headers)
        headers.update({'content-type': 'application/json'})
        
        data = self._get_data()
        json_data = json.dumps(data)
        
        if self.id:
            uri, id = self.manager.build_uri(self.id)
            resp = self.session.put(uri, json_data, headers=headers)
        else:
            uri = self.get_uri()
            
            if force_put:
                resp = self.session.put(uri, json_data, headers=headers)
            else:
                resp = self.session.post(uri, json_data, headers=headers)
            
            if resp:
                location = resp.headers.get('location', None)
                self.id = self.manager.uri_to_id(location)

        #TODO: hide password in user registration data which is dumped to log
        logger.debug("data saved: %s" % json.dumps(data))
        return resp
    
    def delete(self, headers=None):
        if self.id:
            headers = self.manager.prepare_headers(headers)
            
            uri, _ = self.manager.build_uri(self.id)
            resp = self.session.delete(uri, headers=headers)
            
            return resp

class JsonResource(object):
    def __init__(self, base_url):
        
        if settings.FEDERATION_WEB_CA_FILE:        
            ca_file = settings.FEDERATION_WEB_CA_FILE
        # Are we using user certificate for SSL or HOST certificate?
        if not settings.SSL_USE_DELEGATED_USER_CERT:
            if settings.FEDERATION_WEB_CERT:
                cert_file = settings.FEDERATION_WEB_CERT
            if settings.FEDERATION_WEB_KEY:
                key_file = settings.FEDERATION_WEB_KEY
        
        self.session = requests.session(cert=(cert_file,key_file), verify=ca_file)
        self.session.headers['accept'] = 'application/json'
        self.base_url = base_url.rstrip('/')
    
    def request(self, method, path, data=None, headers=None, **kwargs):
        url = '/'.join([self.base_url, path.lstrip('/')])
        
        new_headers = self.session.headers.copy()
        new_headers.update(headers or {})
        
        content_type = kwargs.pop('content_type', None)
        xml = kwargs.pop('xml', False)
        raw = kwargs.pop('raw', False)
        no_json = kwargs.pop('no_json', False)
        
        if data is not None:
            if xml:
                new_headers['Content-Type'] = 'application/xml'
            elif raw:
                pass
            elif content_type:
                new_headers['Content-Type'] = content_type
            else:
                data = json.dumps(data)
                new_headers['Content-Type'] = 'application/json'
                        
        method_func = getattr(self.session, method)
        logger.debug("calling method: %s uri: %s headers:%s" % (method, url, new_headers))
        resp = method_func(url, data=data, headers=new_headers, **kwargs)
        
        content_length = resp.headers.get('content-length', None)
        content_length = int(content_length) if content_length else 0
        
        if (resp.status_code == 200 or content_length > 0) and not no_json:
            try:
                pass
                # Attribute can not be set error -- nic jasno. Verjetno se je z menjavo verzije 
                # requests knjiznice tole sfakalo!
                #resp.json = json.loads(resp.text)
            except TypeError:
                resp.json = None
            except ValueError:
                resp.json = None
        
        return resp
    
    def get(self, *args, **kwargs):
        return self.request('get', *args, **kwargs)
    
    def post(self, *args, **kwargs):
        return self.request('post', *args, **kwargs)
    
    def put(self, *args, **kwargs):
        return self.request('put', *args, **kwargs)
    
    def delete(self, *args, **kwargs):
        return self.request('delete', *args, **kwargs)


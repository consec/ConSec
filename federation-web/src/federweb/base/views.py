from django.utils.decorators import method_decorator, classonlymethod
from django.contrib.auth.decorators import login_required
from django.http import Http404, HttpResponse, HttpResponseRedirect
from django.utils.functional import lazy
from django.core.urlresolvers import reverse
from django.views.generic import View
from django.views.generic.base import TemplateView, RedirectView
from django.contrib.auth.views import redirect_to_login
from django.utils import simplejson
from django.contrib import messages

from dajax.core.Dajax import Dajax

from federweb.base.utils import cached_property
from federweb.auth.views import profile_url, _obtain_cert
from types import GeneratorType

reverse_lazy = lambda name=None, *args : lazy(reverse, str)(name, args=args)

class Http500(Exception):
    pass

class AppNodeMixin(object):
    def __init__(self, *args, **kwargs):
        super(AppNodeMixin, self).__init__(*args, **kwargs)
        
        self.app_name = getattr(self, 'app_name', None)
        self.node_name = getattr(self, 'node_name', None)
        self.action_name = getattr(self, 'action_name', None)

    def get_template_names(self):
        if self.template_name:
            return [self.template_name]
        
        action_name = self.action_name or self.node_name
        
        template = '%s/%s/%s.html' % (self.app_name, self.node_name, action_name)
        
        return [template]
    
    def get_context_data(self, **kwargs):
        ctx = super(AppNodeMixin, self).get_context_data(**kwargs)
        ctx['request'] = self.request
        return ctx

class RestMixin(object):
    model = None
    
    def get_context_data(self, **kwargs):
        ctx = super(RestMixin, self).get_context_data(**kwargs)
        ctx['model'] = self.model
        return ctx

class RoleRequiredMixin(object):
    user_role = None
    
    @method_decorator(login_required)
    def dispatch(self, request, *args, **kwargs):
        user = request.user
        
        if self.user_role:
            if not user or not user.is_authenticated():
                raise Http404
            
            roles = user.roles.all()
            
            for role in roles:
                if role.name == self.user_role:
                    return super(RoleRequiredMixin, self).dispatch(
                                                    request, *args, **kwargs)
        
        raise Http404

class RestDetailMixin(object):
    model = None
    action_name = 'detail'

    def get_object(self, id):
        if self.model is None:
            raise AttributeError('Model is not specified.')
        
        if not id:
            raise AttributeError('ID is not specified.')
        
        obj = self.model.get(id)
        
        if not obj:
            raise Http404('Object not found')
        
        return obj
    
    @cached_property
    def obj(self):
        return self.get_object(self.kwargs['id'])
        
    def get_context_data(self, **kwargs):
        ct = super(RestDetailMixin, self).get_context_data(**kwargs)
        ct['object'] = self.obj
        return ct
    
class RestDetailView(RestDetailMixin, TemplateView):
    pass

class RestRemoveView(RedirectView):
    model = None
    message = None
    permanent = False
    
    def get(self, request, id, *args, **kwargs):
        obj = self.model.get(id)
        
        if not obj.delete():
            raise Http404
        
        if self.message:
            messages.success(self.request, self.message)
        
        return super(RestRemoveView, self).get(request, *args, **kwargs)

def rpc_method(method):
    method.rpc_method = True
    return method

class RpcView(View):
    @classonlymethod
    def as_view(cls, **initkwargs):
        ret = super(RpcView, cls).as_view(**initkwargs)
        
        cls._rpc_methods = []
        
        for n in dir(cls):
            el = getattr(cls, n)
            
            if callable(el) and getattr(el, 'rpc_method', None):
                cls._rpc_methods.append(el.__name__)
        
        return ret
    
    def post(self, request, **kwargs):
        action = request.POST.get('action', None)
        
        self.dajax = Dajax()
        
        if action and action in self._rpc_methods:
            method = getattr(self, action)
            
            argv = request.POST.get('argv', None)
            
            if argv:
                argv = simplejson.loads(argv) or {}
                argv = dict((str(k), v) for k, v in argv.iteritems())
            else:
                argv = {}
            
            result = method(**argv)
            
            if result:
                if isinstance(result, GeneratorType):
                    result = list(result)
                
                jsn = simplejson.dumps(result)
            else:
                jsn = self.dajax.json()
            
            return self.get_json_response(jsn)
        
        raise Http404
    
    def get_json_response(self, content, **kwargs):
        return HttpResponse(content,
                            content_type='application/json',
                            **kwargs)


def home(request):
    if request.user.is_authenticated():
        _obtain_cert(request)
        return HttpResponseRedirect(profile_url(request.user))
    else:
        return redirect_to_login(request.path)


def error500(request):
    raise Http500
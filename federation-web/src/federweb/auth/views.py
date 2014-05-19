import logging
import uuid
import tempfile
import os
import urllib
import requests
import base64
import json

from django.conf import settings
from django.http import HttpResponse
from django.shortcuts import redirect
from django.contrib.auth import logout, login
from django.http import HttpResponseRedirect, Http404
from django.core.urlresolvers import reverse
from django.contrib import messages
from django.contrib import auth
from django.shortcuts import render_to_response, render
from django.template.context import RequestContext
from ndg.httpsclient.ssl_context_util import make_ssl_context
from OpenSSL import crypto, SSL
from myproxy.ws.client import MyProxyWSClient
from federweb.auth.forms import LoginForm, RegistrationForm, AccountSettingsForm,\
    AttributeForm
from federweb.base.models import federation
from saml2 import BINDING_HTTP_REDIRECT
from saml2.client import Saml2Client
from saml2.metadata import entity_descriptor
from django.contrib.sessions.backends.file import SessionStore
from djangosaml2.cache import IdentityCache, OutstandingQueriesCache
from djangosaml2.cache import StateCache
from django.views.generic.edit import FormView
from djangosaml2.conf import get_config_loader
from djangosaml2.conf import get_config
from djangosaml2.utils import get_custom_setting
from djangosaml2.conf import config_settings_loader
from djangosaml2.signals import post_authenticated
from federweb.auth.models import DelegatedCertificate
from django import forms
from django.utils.functional import lazy

try:
    from django.views.decorators.csrf import csrf_exempt
except ImportError:
    # Django 1.0 compatibility
    def csrf_exempt(view_func):
        return view_func


from django.contrib.auth.decorators import login_required
from django.http import HttpResponseBadRequest
from djangosaml2.views import _get_subject_id, _set_subject_id

logger = logging.getLogger(__name__)
reverse_lazy = lambda name=None, *args : lazy(reverse, str)(name, args=args)

class UserAttribute():
    """ Used with Attributes editing
    """
    name = None
    value = None
    id = None

class AttributesCreate(FormView):
    action_name = 'create'
    form_class = AttributeForm
    success_url = reverse_lazy('base_account_settings')
    model = federation.attributes

    def form_valid(self, form):
        attribute = self.model.new()
        if form.save(attribute):
            messages.success(self.request, u'Attribute has been created.')
            return HttpResponseRedirect(self.get_success_url())
        messages.error(self.request, 'Attribute could not be created.')
        return self.form_invalid(form)

def get_user_profile_attribute_list(request):
    user = request.user
    json_attributes = user.profile_attributes.all()
    logger.debug('User profile attributes %s ' % json_attributes)

def get_user_attribute_list(request):
    """ Build a list of attributes for the user to edit
    """
    user = request.user
    user_attributes = []
    for id in user.attributes.all():
        attribute = UserAttribute()
        user_attribute = user.attributes.get(id.id)
        feder_attribute = federation.attributes.get(id.id)
        attribute.name = feder_attribute.name
        attribute.value = user_attribute.value
        attribute.id = id.id
        user_attributes.append(attribute)
    logger.debug('User attributes %s ' % user_attributes)
    return render_to_response('auth/attributes_account_settings.html',
                              dict(user_attributes=user_attributes),
                              context_instance=RequestContext(request))

def user_attribute_edit(request, id):
    user = request.user
    attr = user.attributes.get(id)
    feder_attr = federation.attributes.get(id)
    if request.method == 'POST':
        form = AttributeForm(data=request.POST)
        if form.save(attr):
            messages.success(request, u'Attribute has been edited successfully.')
            return HttpResponseRedirect('account_settings/attributes')
        messages.error(request, 'Attribute could not be edited.')
        return HttpResponseRedirect('account_settings/attributes')
    else:
        data = {'name': feder_attr.name,
                'uri': feder_attr.uri,
                'default_value': feder_attr.defaultValue,
                'reference': feder_attr.reference,
                'description': feder_attr.description
                }
        form = AttributeForm(data)
    formdata = {'form': form}
    return render_to_response('auth/user_attributes_edit.html', dict(formdata.items(),
                                                                     context_instance=RequestContext(request)))
    #return render('auth/user_attributes_edit.html', dict(formdata.items()))


def user_attribute_remove(request, id):
    attr = request.user.attributes.get(id)
    if not attr.delete():
            raise Http404
    logger.debug('Removing user attributes %s ' % attr.name)
    return HttpResponseRedirect('account_settings/attributes')

def user_attribute_create(request, id):
    pass

#class AttributeForm(forms.Form):
#    name = forms.CharField()
#    uri = forms.CharField()
#    default_value = forms.CharField()
#    reference = forms.CharField()
#    description = forms.CharField()
#
#    def save(self, attribute):
#        data = self.cleaned_data
#
#        attribute.name = data['name']
#        attribute.uri = data['uri']
#        attribute.defaultValue = data['default_value']
#        attribute.reference = data['reference']
#        attribute.description = data['description']
#
#        return bool(attribute.save())


#class AttributesCreate(AttributesMixin, FormView):
#    action_name = 'create'
#    form_class = AttributeForm
#    success_url = reverse_lazy('federation_attributes')
#
#    def form_valid(self, form):
#        attribute = self.model.new()
#
#        if form.save(attribute):
#            messages.success(self.request, u'Attribute has been created.')
#
#            return HttpResponseRedirect(self.get_success_url())
#
#        messages.error(self.request, 'Attribute could not be created.')
#
#        return self.form_invalid(form)
#
#class AttributesEdit(AttributesMixin, RestDetailMixin, FormView):
#    action_name = 'edit'
#    form_class = AttributeForm
#    success_url = reverse_lazy('federation_attributes')
#
#    def get_form_kwargs(self):
#        kw = super(AttributesEdit, self).get_form_kwargs()
#        kw['initial'] = {
#            'name': self.obj.name,
#            'uri': self.obj.uri,
#            'default_value': self.obj.defaultValue,
#            'reference': self.obj.reference,
#            'description': self.obj.description,
#        }
#        return kw
#
#    def form_valid(self, form):
#        if form.save(self.obj):
#            messages.success(self.request, u'Attribute has been saved.')
#
#            return HttpResponseRedirect(self.get_success_url())
#
#        messages.error(self.request, 'Attribute could not be saved.')
#
#        return self.form_invalid(form)
#
#

def get_user_profile_attribute_list(request):
    user = request.user
    json_attributes = user.profile_attributes.all()

    logger.debug('User profile attributes %s ' % json_attributes)

#def get_user_attribute_list(request):
#    user = request.user
#    user_attributes = {}
#    for id in user.attributes.all():
#        user_attribute = user.attributes.get(id.id)
#        feder_attribute = federation.attributes.get(id.id)
#        user_attribute_name = feder_attribute.name
#        user_attribute_value = user_attribute.value
#        user_attributes[user_attribute_name]=user_attribute_value
#    logger.debug('User attributes %s ' % user_attributes)

#class AttributeForm(forms.Form):
#    name = forms.CharField()
#    uri = forms.CharField()
#    default_value = forms.CharField()
#    reference = forms.CharField()
#    description = forms.CharField()
#
#    def save(self, attribute):
#        data = self.cleaned_data
#
#        attribute.name = data['name']
#        attribute.uri = data['uri']
#        attribute.defaultValue = data['default_value']
#        attribute.reference = data['reference']
#        attribute.description = data['description']
#
#        return bool(attribute.save())


#class AttributesCreate(AttributesMixin, FormView):
#    action_name = 'create'
#    form_class = AttributeForm
#    success_url = reverse_lazy('federation_attributes')
#
#    def form_valid(self, form):
#        attribute = self.model.new()
#
#        if form.save(attribute):
#            messages.success(self.request, u'Attribute has been created.')
#
#            return HttpResponseRedirect(self.get_success_url())
#
#        messages.error(self.request, 'Attribute could not be created.')
#
#        return self.form_invalid(form)
#
#class AttributesEdit(AttributesMixin, RestDetailMixin, FormView):
#    action_name = 'edit'
#    form_class = AttributeForm
#    success_url = reverse_lazy('federation_attributes')
#
#    def get_form_kwargs(self):
#        kw = super(AttributesEdit, self).get_form_kwargs()
#        kw['initial'] = {
#            'name': self.obj.name,
#            'uri': self.obj.uri,
#            'default_value': self.obj.defaultValue,
#            'reference': self.obj.reference,
#            'description': self.obj.description,
#        }
#        return kw
#
#    def form_valid(self, form):
#        if form.save(self.obj):
#            messages.success(self.request, u'Attribute has been saved.')
#
#            return HttpResponseRedirect(self.get_success_url())
#
#        messages.error(self.request, 'Attribute could not be saved.')
#
#        return self.form_invalid(form)
#
#class AttributesRemove(AttributesMixin, RestRemoveView):
#    message = u'Attribute was successfully removed.'
#    url = reverse_lazy('federation_attributes')

def get_user_selection_criteria_list(request):
    user = request.user

    return render_to_response('auth/user_selection_criteria.html',
                              dict(user_selection_criteria=user.selection_criteria.all()),
                              context_instance=RequestContext(request))

def edit_user_selection_criteria(request):
    user = request.user

    return render_to_response('auth/user_selection_criteria_edit.html',
                              dict(user_selection_criteria=user.selection_criteria.all()),
                              context_instance=RequestContext(request))

def save_user_selection_criteria(request):
    user = request.user
    usc = user.selection_criteria.new()
    data = []
    if request.POST["command"] == "Save":
        for key in request.POST.keys():
            keyString = str(key)
            if keyString.startswith("usc-"):
                criterionData = {}
                criterionData["name"] = keyString[4:]
                criterionData["value"] = request.POST[key]
                data.append(criterionData);

        usc.raw = data
        usc.save(True)
        return HttpResponseRedirect(reverse_lazy('user_selection_criteria'))
    elif request.POST["command"] == "Cancel":
        return HttpResponseRedirect(reverse_lazy('user_selection_criteria'))

def oauth2callback(request):
    """
    Consumes OAuth2 authorization code from the AS. We get here from _get_cert_oauth2 method (after redirect).
    """
    logger.debug('Entering oauth2 code_consumer')

    oauth_state = request.GET.get('state')
    oauth_code = request.GET.get('code')
    if oauth_state != request.session['oauth_state']:
        logger.error('oauth_state err: could not get authorization code.')
    logger.debug('Got code %s' % oauth_code)
    body = "grant_type=%s&client_id=%s&client_secret=%s&code=%s&redirect_uri=%s&scope=%s" %(
        settings.OAUTH2_CONFIG['oauth_client']['grant_type'],
        settings.OAUTH2_CONFIG['oauth_client']['client_id'],
        settings.OAUTH2_CONFIG['oauth_client']['client_secret'],
        oauth_code,
        settings.OAUTH2_CONFIG['oauth_client']['callback_url'],
        urllib.quote(settings.OAUTH2_CONFIG['oauth_client']['scope'], safe="%=&?~#!$,;'@()*[]"),
    )
    logger.debug('Body: %s ' % body)
    token_url = '%s/r/access_token/request' % settings.OAUTH2_CONFIG['oauth_authz_server']['auth_endpoint']
    client_cert = settings.OAUTH2_CONFIG['oauth_client']['client_cert']
    client_key = settings.OAUTH2_CONFIG['oauth_client']['client_key']
    resp = requests.post(
                       url=token_url,
                       data=body,
                       headers={'Content-Type': 'application/x-www-form-urlencoded'},
                       cert=(client_cert, client_key),
                       verify=False
                       )
    access_token=str(json.loads(resp._content).get('access_token'))
    logger.debug('Got access_token: %s' % access_token)
    request.session['oauth_access_token'] = access_token
    delegatedCert = DelegatedCertificate()
    cert=delegatedCert.get_cert(access_token,settings.OAUTH2_CONFIG['ca_server']['url'], client_cert, client_key)
    delegatedCert.write_key_to_file()
    #res = myproxy_client.logon(request.user.id, "nopassword", settings.ONLINE_CA_URI,ssl_ctx=ssl_ctx)
    #pem_out = res.read()
    #cert = crypto.load_certificate(crypto.FILETYPE_PEM, pem_out)
    request.session['pkey_file_path']=delegatedCert.pkey_file_path
    request.session['certificate']=cert
    logger.debug("Key path: %s " % request.session['pkey_file_path'] )
    #request.session['certificate']= DelegatedCertificate(cert,pem_out)
    logger.debug("User has cert %s " % request.user.certificate )
    return HttpResponseRedirect(profile_url(request.user))

def oauth2_monitoring_call(request):
    """
    For monitoring purposes: consumes OAuth2 authorization code from the AS.
    We get here from _get_cert_oauth2 method (after redirect).
    """
    logger.debug('Entering oauth2 code_consumer')

    body="grant_type=%s&resource_owner=%s" % (
      'client_credentials',
      request.user.id,
     )
    logger.debug('DEBUG: Body: %s ' % body)
    token_url = '%s/r/access_token/request' % settings.OAUTH2_CONFIG['oauth_authz_server']['auth_endpoint']
    client_cert=settings.OAUTH2_CONFIG['oauth_client']['client_cert']
    client_key=settings.OAUTH2_CONFIG['oauth_client']['client_key']
    authbase64header='%s:%s' % (settings.OAUTH2_CONFIG['oauth_client']['client_id'], settings.OAUTH2_CONFIG['oauth_client']['client_secret'])
    auth_header='Basic %s' % base64.b64encode(authbase64header)
    logger.debug('DEBUG: authheader: %s ' % auth_header)
    try:
        resp=requests.post(
                           url=token_url,
                           data=body,
                           headers={'Content-Type':'application/x-www-form-urlencoded; charset=UTF-8', 'Authorization':auth_header},
                           cert=(client_cert, client_key),
                           verify=False
                           )
    except Exception as e:
        logger.debug(e)
    access_token=str(json.loads(resp._content).get('access_token'))
    logger.debug('Got access_token: %s' % access_token)
    request.session['monitoring_access_token']=access_token
    return HttpResponseRedirect(profile_url(request.user))

def _get_authorization_request_uri():
    """
    Generates URI towards AS.
    """
    state=uuid.uuid1()
    uri='%s/authorize?response_type=%s&client_id=%s&redirect_uri=%s&state=%s&scope=%s' %  (
     settings.OAUTH2_CONFIG['oauth_authz_server']['auth_endpoint'],
     settings.OAUTH2_CONFIG['oauth_authz_server']['response_type'],
     settings.OAUTH2_CONFIG['oauth_client']['client_id'],
     urllib.quote(settings.OAUTH2_CONFIG['oauth_client']['callback_url'], safe="%=&?~#!$,;'@()*[]"),
     str(state),
     urllib.quote(settings.OAUTH2_CONFIG['oauth_client']['scope'], safe="%=&?~#!$,;'@()*[]"),
     )
    logger.debug("HTTP call %s :" % uri)
    return uri, state

def _get_cert_oauth2(request):
    if not settings.ONLINE_OAUTH2_CA_USE:
        return
    logger.debug('Entering _get_cert_oauth2')
    uri, state = _get_authorization_request_uri()
    request.session['oauth_state']=str(state)
    logger.debug("Redirecting to %s" % uri)
    return redirect(uri)

def _obtain_cert(request):
    """
    Calls to online CA and stores user certificate and key in the session.
    TOOD: configure client crt and key, CA certs: one-test-ssl-chain.pem put in configuration.
    """
    if 'certificate' in request.session:
        logger.debug("User has delegated certificate set already.");
        return HttpResponseRedirect(profile_url(request.user))

    if (not settings.ONLINE_CA_USE) and (not settings.ONLINE_OAUTH2_CA_USE):
        return HttpResponseRedirect(profile_url(request.user))

    if settings.ONLINE_OAUTH2_CA_USE:
        return _get_cert_oauth2(request)

    ssl_ctx = make_ssl_context(cert_file=settings.FEDERATION_WEB_CERT,
                               key_file=settings.FEDERATION_WEB_KEY,
                               ca_dir=settings.TRUSTSTORE_DIR,
                               verify_peer=True,
                               url=settings.ONLINE_CA_URI,
                               method=SSL.SSLv3_METHOD)
    myproxy_client = MyProxyWSClient()
    res = myproxy_client.logon(request.user.id, "nopassword", settings.ONLINE_CA_URI,ssl_ctx=ssl_ctx)
    pem_out = res.read()
    cert = crypto.load_certificate(crypto.FILETYPE_PEM, pem_out)
    cert = DelegatedCertificate(cert,pem_out)
    cert.write_key_to_file()
    request.session['certificate']=cert
    request.session['pkey_file_path']=cert.pkey_file_path
    logger.debug("Key path: %s " % request.session['pkey_file_path'] )
    logger.debug("User has cert %s " % request.user.certificate )

def login_view(request):
    if request.user.is_authenticated():
        #oauth2_monitoring_call(request)
        if not _is_set_saml2_auth_used(request.session):
            logger.debug("Setting _is_set_saml2_auth_used to False")
            _set_saml2_auth_used(request.session, False)
        return _obtain_cert(request)
    return HttpResponseRedirect(reverse('contrail_saml2_login'))


    #===========================================================================
    # form = LoginForm(data=request.POST or None)
    #
    # if form.is_valid():
    #    if not form.cleaned_data['remember']:
    #        request.session.set_expiry(0)
    #    #import pdb; pdb.set_trace()
    #    login(request, form.get_user())
    #    if not 'certificate' in request.session:
    #        if settings.ONLINE_CA_USE or settings.ONLINE_OAUTH2_CA_USE:
    #            return _obtain_cert(request)
    #    if request.session.test_cookie_worked():
    #        request.session.delete_test_cookie()
    #
    #
    #    request.session['uid']=request.user.id
    #
    #    if 'next' in request.REQUEST:
    #        next = request.REQUEST['next']
    #        if next.startswith('/'):
    #            return HttpResponseRedirect(next)
    #
    #    return HttpResponseRedirect(profile_url(request.user))
    #
    # request.session.set_test_cookie()
    #
    # return render_to_response('auth/login.html', dict(form=form),
    #                            context_instance=RequestContext(request))
    #===========================================================================

@login_required
def echo_attrs(request,
                    config_loader=config_settings_loader,
                    template='auth/echo_attributes.html'):
    """Example view that echo the SAML attributes of an user"""
    state = StateCache(request.session)
    conf = get_config_loader(config_loader, request)

    client = Saml2Client(conf, state_cache=state,
                         identity_cache=IdentityCache(request.session),
                         logger=logger)
    subject_id = _get_subject_id(request.session)
    identity = client.users.get_identity(subject_id,
                                         check_not_on_or_after=False)
    return render_to_response(template, {'attributes': identity[0]},
                              context_instance=RequestContext(request))

def logout_view(request):
    # redirect to saml2 logut if saml2 is used
    if _is_set_saml2_auth_used(request.session):
        logger.debug("Redirecting to saml2/logout.")
        return HttpResponseRedirect('saml2/logout/')
    if not request.user:
        return Http404
    logout(request)
    next = request.GET.get('next', '')
    if not next or not next.startswith('/'):
        next = '/'
    return HttpResponseRedirect(next)

def get_cert_view(request):
    if not request.user:
        return Http404
    if not (settings.ONLINE_CA_USE or settings.ONLINE_OAUTH2_CA_USE):
        logger.debug('Not using ONLINE CA. Therefore, the user has no key.')
        response = HttpResponse('Not using ONLINE CA. Therefore, the user has no key.', content_type='text/plain')
        return response
    cert = request.session['certificate']
    logger.debug("User's cert: %s " % cert )
    cert_json=json.loads(str(cert))
    tup = tempfile.mkstemp()
    logger.debug("User's cert path: %s " % tup[1] )
    f = open(tup[1], 'w')
    #crypto.load_certificate(crypto.FILETYPE_PEM, cert.raw)
    f.write(cert_json['raw'])
    f.close()
    response = HttpResponse(cert_json['raw'], content_type='text/plain')
    response['Content-Disposition'] = 'attachment; filename=%s' % str(tup[1])
    return response

def get_key_view(request):
    if not request.user:
        return Http404
    if not (settings.ONLINE_CA_USE or settings.ONLINE_OAUTH2_CA_USE):
        logger.debug('Not using ONLINE CA. Therefore, the user has no key.')
        response = HttpResponse('Not using ONLINE CA. Therefore, the user has no key.', content_type='text/plain')
        return response
    logger.debug("Key path: %s " % request.session['pkey_file_path'] )
    pkey_file_path = request.session['pkey_file_path']
    with open(pkey_file_path, 'r') as f:
        txt_key = f.read()
    #txt_key= crypto.dump_privatekey(crypto.FILETYPE_PEM, cert.key_pair)
    response = HttpResponse(txt_key, content_type='text/plain')
    response['Content-Disposition'] = 'attachment; filename=%s' % str(pkey_file_path)
    return response

def registration_view(request):
    if request.user.is_authenticated():
        return HttpResponseRedirect(profile_url(request.user))

    form = RegistrationForm(data=request.POST or None)

    if form.is_valid():
        user = federation.users.new()
        user.username = form.cleaned_data['username']
        user.password = form.cleaned_data['password1']
        user.email = form.cleaned_data['email']
        user.firstName = form.cleaned_data['first_name']
        user.lastName = form.cleaned_data['last_name']

        if user.save():
            for role in federation.roles.all():
                if role.name == 'FederationUser':
                    ur = user.roles.new()
                    ur.roleId = role.id
                    ur.save()

            messages.success(request, u'Registration successful. Please log in.')

            return HttpResponseRedirect(reverse('contrail_saml2_login'))
        else:
            messages.error(request, u'Registration failed. Please try again.')

    return render_to_response('auth/registration.html', dict(form=form),
                            context_instance=RequestContext(request))

def profile_url(user):

    url_map = {
        'FederationCoordinator': reverse('federation'),
        'FederationUser': reverse('user_dashboard'),
    }

    roles = user.roles.all()

    for role in roles:
        if role.name in url_map:
            logger.debug('Setting user role: %s' % role.name )
            return url_map[role.name]

    logger.error('Unknown roles for user "%s"' % user)

    return '/404'

@login_required
def logout_saml2_view(request, config_loader_path=None, config_loader=config_settings_loader):
    """SAML Logout Request initiator

    This view initiates the SAML2 Logout request
    using the pysaml2 library to create the LogoutRequest.
    """
    logger.debug('logout_saml2_view: Logout process started')
    state = StateCache(request.session)
    conf = get_config(config_loader_path, request)
    #conf = get_config_loader(config_loader, request)

    client = Saml2Client(conf, state_cache=state,
                         identity_cache=IdentityCache(request.session),
                         logger=logger)
    subject_id = _get_subject_id(request.session)
    session_id, code, head, body = client.global_logout(subject_id)
    headers = dict(head)
    state.sync()
    logger.debug('logout_saml2_view: Redirecting to the IdP to continue the logout process')
    return HttpResponseRedirect(headers['Location'])

def logout_ls_saml2_view(request, config_loader_path=None,
                   next_page=None):
    """SAML Logout Response endpoint

    The IdP will send the logout response to this view,
    which will process it with pysaml2 help and log the user
    out.
    Note that the IdP can request a logout even when
    we didn't initiate the process as a single logout
    request started by another SP.
    """
    logger.debug('Logout service started')
    """Turn off saml2 """
    _set_saml2_auth_used(request.session, False)
    conf = get_config(config_loader_path, request)

    state = StateCache(request.session)
    client = Saml2Client(conf, state_cache=state,
                         identity_cache=IdentityCache(request.session),
                         logger=logger)

    if 'SAMLResponse' in request.GET:  # we started the logout
        logger.debug('Receiving a logout response from the IdP')
        response = client.logout_response(request.GET['SAMLResponse'],
                                          binding=BINDING_HTTP_REDIRECT)
        state.sync()
        if response and response[1] == '200 Ok':
            return logout_view(request)
        else:
            logger.error('Unknown error during the logout')
            return HttpResponse('Error during logout')

    elif 'SAMLRequest' in request.GET:  # logout started by the IdP
        logger.debug('Receiving a logout request from the IdP')
        subject_id = _get_subject_id(request.session)
        if subject_id is None:
            logger.warning(
                'The session does not contain the subject id for user %s. Performing local logout'
                % request.user)
            auth.logout(request)
            return HttpResponse('Error during logout')
        else:
            response, success = client.logout_request(request.GET, subject_id)
            state.sync()
            if success:
                logout_view(request)
                assert response[0][0] == 'Location'
                url = response[0][1]
                return HttpResponseRedirect(url)
            elif response is not None:
                assert response[0][0] == 'Location'
                url = response[0][1]
                return HttpResponseRedirect(url)
            else:
                logger.error('Unknown error during the logout')
                return HttpResponse('Error during logout')
    else:
        logger.error('No SAMLResponse or SAMLRequest parameter found')
        raise Http404('No SAMLResponse or SAMLRequest parameter found')

def login_saml2_view(request,
          config_loader_path=None,
          wayf_template='auth/wayf.html',
          authorization_error_template='djangosaml2/auth_error.html'):
    """SAML Authorization Request initiator

    This view initiates the SAML2 Authorization handshake
    using the pysaml2 library to create the AuthnRequest.
    It uses the SAML 2.0 Http Redirect protocol binding.
    """
    logger.debug('Login process started')

    came_from = request.GET.get('next', settings.LOGIN_REDIRECT_URL)

    if not request.user.is_anonymous():
        logger.debug('User is already logged in')
        return render_to_response(authorization_error_template, {
                'came_from': came_from,
                }, context_instance=RequestContext(request))

    selected_idp = request.GET.get('idp', None)
    conf = get_config(config_loader_path, request)
    # is a embedded wayf needed?
    idps = conf.idps()
    if selected_idp is None and len(idps) > 1:
        logger.debug('A discovery process is needed')
        return render_to_response(wayf_template, {
                'available_idps': idps.items(),
                'came_from': came_from,
                }, context_instance=RequestContext(request))

    client = Saml2Client(conf, logger=logger)
    try:
        (session_id, result) = client.authenticate(
            entityid=selected_idp, relay_state=came_from,
            binding=BINDING_HTTP_REDIRECT,
            )
    except TypeError, e:
        logger.error('Unable to know which IdP to use')
        return HttpResponse(unicode(e))

    assert len(result) == 2
    assert result[0] == 'Location'
    location = result[1]

    logger.debug('Saving the session_id in the OutstandingQueries cache')
    oq_cache = OutstandingQueriesCache(request.session)
    oq_cache.set(session_id, came_from)

    logger.debug('Redirecting the user to the IdP')
    return HttpResponseRedirect(location)

def _set_saml2_auth_used(session, used=False):
    """ Is SAML2 used in this session?
    """
    session['_saml2_auth_used'] = used

def _is_set_saml2_auth_used(session):
    """ Is SAML2 used in this session?
    """
    try:
        return session['_saml2_auth_used']
    except :
        return False

@csrf_exempt
def assertion_consumer_service_view(request,
                                    config_loader_path=None,
                                    attribute_mapping=None,
                                    create_unknown_user=None):
    """SAML Authorization Response endpoint

    The IdP will send its response to this view, which
    will process it with pysaml2 help and log the user
    in using the custom Authorization backend
    djangosaml2.backends.Saml2Backend that should be
    enabled in the settings.py
    """
    logger.debug('Assertion Consumer Service started')

    attribute_mapping = attribute_mapping or get_custom_setting(
            'SAML_ATTRIBUTE_MAPPING', {'uid': ('username', )})
    create_unknown_user = create_unknown_user or get_custom_setting(
            'SAML_CREATE_UNKNOWN_USER', True)
    logger.debug('Assertion Consumer Service started')

    conf = get_config(config_loader_path, request)

    if 'SAMLResponse' not in request.POST:
        return HttpResponseBadRequest(
            'Couldn\'t find "SAMLResponse" in POST data.')
    post = {'SAMLResponse': request.POST['SAMLResponse']}
    client = Saml2Client(conf, identity_cache=IdentityCache(request.session),
                         logger=logger)

    oq_cache = OutstandingQueriesCache(request.session)
    outstanding_queries = oq_cache.outstanding_queries()

    # process the authentication response

    try:
        response = client.response(post, outstanding_queries)
    except Exception as e:
        logger.error('Error while authenticating. %s' % e)
        return HttpResponseRedirect('/saml2/login_error')
    if response is None:
        logger.error('SAML response is None')
        return HttpResponse("SAML response has errors. Please check the logs")

    session_id = response.session_id()
    oq_cache.delete(session_id)

    # authenticate the remote user
    session_info = response.session_info()

    if callable(attribute_mapping):
        attribute_mapping = attribute_mapping()
    if callable(create_unknown_user):
        create_unknown_user = create_unknown_user()

    logger.debug('Trying to authenticate the user')
    try:
        user = auth.authenticate(session_info=session_info,
                             attribute_mapping=attribute_mapping,
                             create_unknown_user=create_unknown_user)
    except Exception as e:
        logger.error('Error while authenticating. %s' % e)
        return HttpResponseRedirect('/saml2/login_error')
    if user is None:
        logger.error('The user is None')
        return HttpResponseRedirect('/saml2/login_error')
        #return HttpResponse("There were problems trying to authenticate the user")

    auth.login(request, user)

    _set_subject_id(request.session, session_info['name_id'])

    _set_saml2_auth_used(request.session, True)
    logger.debug('Sending the post_authenticated signal')
    post_authenticated.send_robust(sender=user, session_info=session_info)

    # redirect the user to the view where he came from
    #relay_state = request.POST.get('RelayState', '/login')
    relay_state = '/login'
    logger.debug('Redirecting to the RelayState: ' + relay_state)
    return HttpResponseRedirect(relay_state)


def saml2_error_view(request):
    return render_to_response('auth/saml2_error.html',
        context_instance=RequestContext(request))


@login_required
def account_settings_view(request):
    user=request.user

    available_idps = {}
    for idp in federation.idps.all():
        available_idps[idp.name]={'name': idp.name, 'uri': idp.uri, 'id': idp.uri.split("/")[2]}

    available_ids = {}
    for id in user.ids.all():
        available_ids[id.identity]={'identity':id.identity, 'uri':id.uri}

    if request.method == 'POST':
        form = AccountSettingsForm(data=request.POST)
        if form.is_valid():
            delete_id_email=request.POST.get('delete_id', None)
            add_id_name=request.POST.get('add_id', None)
            new_id = request.POST.get('identity-%s' % add_id_name, None)
            dl_priv_key=request.POST.get('dl_priv_key', None)
            dl_cert=request.POST.get('dl_cert', None)
            if dl_priv_key:
                return HttpResponseRedirect("/getkey")
            if dl_cert:
                return HttpResponseRedirect("/getcert")
            if delete_id_email:
                deleteId=available_ids[delete_id_email]
                logger.debug("To delete: %s" % deleteId)
                for id in user.ids.all():
                    if id.uri == deleteId['uri']:
                        id.identityProviderId=deleteId['uri']
                        id.delete()
                return HttpResponseRedirect("/account_settings")
            if add_id_name:
                if len(new_id) >= 1:
                    logger.debug("adding new id: %s, idp: %s" % (new_id,add_id_name))
                    uid=request.user.ids.new()
                    uid.identity=new_id
                    uid.identityProviderId=available_idps[add_id_name]['id']
                    uid.save()
                return HttpResponseRedirect("/account_settings")
            user.email=form.cleaned_data['email']
            user.fistName=form.cleaned_data['first_name']
            user.lastName=form.cleaned_data['last_name']
            if (len(form.cleaned_data['password1']) != 0):
                user.password=form.cleaned_data['password1']
            else:
                delattr(user, "password")
            user.save(force_put=True)
    else:
        list=[ x.name for x in user.roles.all() ]
        data={ 'username': user.username,
              'email': user.email,
              'first_name': user.firstName,
              'last_name': user.lastName,
              'roles': ", ".join(list) }
        form = AccountSettingsForm(data)
    formdata={'form':form, 'username': user.username, 'delegated_cert':request.session.get('certificate', None)}
    available_idps=dict(available_idps=available_idps)
    available_ids=dict(available_ids=available_ids)
    return render_to_response('auth/base_account_settings.html', dict(formdata.items()+available_idps.items()+available_ids.items()) ,
        context_instance=RequestContext(request))
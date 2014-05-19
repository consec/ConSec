from django.contrib import messages
from django.http import HttpResponseRedirect, HttpResponse
from django.views.generic import TemplateView
from django.views.generic.edit import FormView
from django import forms
from django.views.decorators.csrf import csrf_exempt

from federweb.base.views import AppNodeMixin, RestMixin, reverse_lazy,\
    RestDetailMixin, RestRemoveView
from federweb.federation.views import AppMixin

from federweb.base.models import oauth

import json


import logging
logger = logging.getLogger(__name__)


class AuthorizationOrganizationsMixin(AppMixin, AppNodeMixin):
    node_name = 'authorization'
    model = oauth.authorization_organizations


class AuthorizationOrganizationsClientsMixin(AppMixin, AppNodeMixin):
    node_name = 'authorization'

    def get_context_data(self, **kwargs):
        self.model = oauth.authorization_organizations_clients(self.kwargs['organization_id'])
        self.organization_id = self.kwargs['organization_id']

        ctx = super(AuthorizationOrganizationsClientsMixin, self).get_context_data(**kwargs)
        ctx['organization_id'] = self.organization_id
        return ctx


# class AuthorizationList(AuthorizationOrganizationsMixin, RestMixin, TemplateView):
#     pass


class AuthorizationOrganizationsList(AuthorizationOrganizationsMixin, RestMixin, TemplateView):
    action_name = 'organizations'


class AuthorizationOrganizationsForm(forms.Form):
    name = forms.CharField()

    def save(self, organization):
        data = self.cleaned_data

        organization.name = data['name']

        return bool(organization.save())


class AuthorizationOrganizationsCreate(AuthorizationOrganizationsMixin, FormView):
    action_name = 'organizations_create'
    form_class = AuthorizationOrganizationsForm
    success_url = reverse_lazy('federation_authorization_organizations')

    def form_valid(self, form):
        organization = self.model.new()

        if form.save(organization):
            messages.success(self.request, u'Organization has been created.')

            return HttpResponseRedirect(self.get_success_url())

        messages.error(self.request, 'Organization could not be created.')

        return self.form_invalid(form)


class AuthorizationOrganizationsEdit(AuthorizationOrganizationsMixin, RestDetailMixin, FormView):
    action_name = 'organizations_edit'
    form_class = AuthorizationOrganizationsForm
    success_url = reverse_lazy('federation_authorization_organizations')

    def get_form_kwargs(self):
        kw = super(AuthorizationOrganizationsEdit, self).get_form_kwargs()
        kw['initial'] = {
            'name': self.obj.name,
        }
        return kw

    def form_valid(self, form):
        if form.save(self.obj):
            messages.success(self.request, u'Organization has been saved.')

            return HttpResponseRedirect(self.get_success_url())

        messages.error(self.request, 'Organization could not be saved.')

        return self.form_invalid(form)


class AuthorizationOrganizationsRemove(AuthorizationOrganizationsMixin, RestRemoveView):
    message = u'Organization was successfully removed.'
    url = reverse_lazy('federation_authorization_organizations')


class AuthorizationOrganizationsClientsList(AuthorizationOrganizationsClientsMixin, RestMixin, TemplateView):
    action_name = 'organizations_clients'


class AuthorizationOrganizationsClientsForm(forms.Form):
    client_id = forms.CharField(initial='oauth-java-client-demo', required=False)
    name = forms.CharField(initial='oauth-java-client-demo', required=False)
    authorized_grant_types = forms.MultipleChoiceField(choices=[('AUTHORIZATION_CODE', 'AUTHORIZATION_CODE'),
                                                                ('CLIENT_CREDENTIALS', 'CLIENT_CREDENTIALS')])
    callback_uri = forms.CharField(initial='https://contrail.xlab.si:8444/oauth-demo/oauth2callback', required=False)
    client_secret = forms.CharField(initial='secret', required=False)
    countries = forms.MultipleChoiceField(choices=[('SI', 'Slovenia'), ('IT', 'Italy'), ('FR', 'France')], required=False)


    def save(self, client):
        data = self.cleaned_data

        client.client_id = data['client_id']
        client.name = data['name']
        client.authorized_grant_types = data['authorized_grant_types']
        client.callback_uri = data['callback_uri']
        if data['client_secret'] != '':
            client.client_secret = data['client_secret']
        client.countries = data['countries']

        return bool(client.save())


class AuthorizationOrganizationsClientsCreate(AuthorizationOrganizationsClientsMixin, FormView):
    action_name = 'organizations_clients_create'
    form_class = AuthorizationOrganizationsClientsForm

    def get_form_kwargs(self):
        self.organization_id = self.kwargs['organization_id']
        self.model = oauth.authorization_organizations_clients(self.organization_id)
        # logger.debug('LLLLLLLLLLLLLLLLLLL organization_id: ' + self.organization_id)

        return super(AuthorizationOrganizationsClientsCreate, self).get_form_kwargs()

    def get_success_url(self):
        return reverse_lazy('federation_authorization_organizations_clients', self.organization_id)

    def form_valid(self, form):
        client = self.model.new()
        client.organization_id = self.organization_id

        if form.save(client):
            messages.success(self.request, u'Client has been created.')

            return HttpResponseRedirect(self.get_success_url())

        messages.error(self.request, 'Client could not be created.')

        return self.form_invalid(form)


class AuthorizationOrganizationsClientsEdit(AuthorizationOrganizationsClientsMixin, RestDetailMixin, FormView):
    action_name = 'organizations_clients_edit'
    form_class = AuthorizationOrganizationsClientsForm

    def get_form_kwargs(self):
        self.organization_id = self.kwargs['organization_id']
        self.model = oauth.authorization_organizations_clients(self.organization_id)

        kw = super(AuthorizationOrganizationsClientsEdit, self).get_form_kwargs()
        kw['initial'] = {
            'client_id': self.obj.client_id,
            'name': self.obj.name,
            'authorized_grant_types': self.obj.authorized_grant_types,
            'callback_uri': self.obj.callback_uri if hasattr(self.obj, 'callback_uri') else '',
            'client_secret': '',
            'countries': self.obj.countries,
        }
        return kw

    def get_success_url(self):
        return reverse_lazy('federation_authorization_organizations_clients', self.organization_id)

    def form_valid(self, form):
        client = self.model.new()
        client.organization_id = self.organization_id

        if form.save(self.obj):
            messages.success(self.request, u'Client has been saved.')

            return HttpResponseRedirect(self.get_success_url())

        messages.error(self.request, 'Client could not be saved.')

        return self.form_invalid(form)


class AuthorizationOrganizationsClientsRemove(AuthorizationOrganizationsClientsMixin, RestRemoveView):
    message = u'Client was successfully removed.'
    url = reverse_lazy('federation_authorization_organizations')

    def get(self, request, id, *args, **kwargs):
        self.organization_id = self.kwargs['organization_id']
        self.model = oauth.authorization_organizations_clients(self.organization_id)
        self.url = reverse_lazy('federation_authorization_organizations_clients', self.organization_id)
        return super(AuthorizationOrganizationsClientsRemove, self).get(request, id, *args, **kwargs)


# Trust management
class AuthorizationTrustMixin(AppMixin, AppNodeMixin):
    node_name = 'authorization'

    def get_context_data(self, **kwargs):
        self.owner_id = self.request.user.uuid
        self.model = oauth.authorization_trust(self.owner_id)
        return super(AuthorizationTrustMixin, self).get_context_data(**kwargs)


class AuthorizationTrustList(AuthorizationTrustMixin, RestMixin, TemplateView):
    action_name = "trust"


@csrf_exempt
def authorizationTrustModify(request, organization_id, client_id=None):
    if request.method != 'POST':
        return HttpResponse('{"Wrong request method"}', status=400)
    try:
        data = json.loads(request.raw_post_data)
    except (ValueError, KeyError, TypeError):
        return HttpResponse('{"JSON format error"}', status=400)

    trust = data['trust']
    owner_id = request.user.uuid

    if not client_id:
        model = oauth.authorization_owners_organizations(owner_id)

        organization = model.get(organization_id)
        if organization:
            if trust == 1:
                organization.trust_level = "FULLY"
                organization.save()
            elif trust == 2:
                organization.trust_level = "DENIED"
                organization.save()
            else:
                organization.delete()
        else:
            if trust == 1:
                organization = model.new()
                organization.trust_level = "FULLY"
                organization.organization_id = organization_id
                organization.save()
            elif trust == 2:
                organization = model.new()
                organization.trust_level = "DENIED"
                organization.organization_id = organization_id
                organization.save()
    else:
        model = oauth.authorization_owners_organizations_clients(owner_id, organization_id)

        client = model.get(client_id)
        if client:
            if trust == 1:
                client.trust_level = "TRUSTED"
                client.save()
            elif trust == 2:
                client.trust_level = "NOT_TRUSTED"
                client.save()
            else:
                client.delete()
        else:
            if trust == 1:
                client = model.new()
                client.trust_level = "TRUSTED"
                client.client_id = client_id
                client.save()
            elif trust == 2:
                client = model.new()
                client.trust_level = "NOT_TRUSTED"
                client.client_id = client_id
                client.save()

    return HttpResponse('{"OK"}', status=200)



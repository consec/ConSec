from django.contrib import messages
from django.http import HttpResponseRedirect
from django.views.generic import TemplateView
from django.views.generic.edit import FormView
from django import forms

from federweb.base.views import AppNodeMixin, RestMixin, reverse_lazy,\
    RestDetailMixin, RestRemoveView
from federweb.federation.views import AppMixin

from federweb.base.models import federation

class ProvidersMixin(AppMixin, AppNodeMixin):
    node_name = 'providers'
    model = federation.providers

class ProvidersList(ProvidersMixin, RestMixin, TemplateView):
    pass

class ProviderForm(forms.Form):
    name = forms.CharField()
    provider_uri = forms.URLField()
    
    def save(self, provider):
        data = self.cleaned_data
        
        provider.name = data['name']
        provider.typeId = 42
        provider.providerUri = data['provider_uri']
        
        return bool(provider.save())

class ProvidersCreate(ProvidersMixin, FormView):
    action_name = 'create'
    form_class = ProviderForm
    success_url = reverse_lazy('federation_providers')
    
    def form_valid(self, form):
        provider = self.model.new()
        
        if form.save(provider):
            messages.success(self.request, u'Provider has been created.')
        
            return HttpResponseRedirect(self.get_success_url())
        
        messages.error(self.request, 'Provider could not be created.')
        
        return self.form_invalid(form)

class ProvidersEdit(ProvidersMixin, RestDetailMixin, FormView):
    action_name = 'edit'
    form_class = ProviderForm
    success_url = reverse_lazy('federation_providers')
    
    def get_form_kwargs(self):
        kw = super(ProvidersEdit, self).get_form_kwargs()
        kw['initial'] = {
            'name': self.obj.name,
            'provider_uri': self.obj.providerUri
        }
        return kw
    
    def form_valid(self, form):
        if form.save(self.obj):
            messages.success(self.request, u'Provider has been saved.')
        
            return HttpResponseRedirect(self.get_success_url())
        
        messages.error(self.request, 'Provider could not be saved.')
        
        return self.form_invalid(form)

class ProvidersRemove(ProvidersMixin, RestRemoveView):
    message = u'Provider was successfully removed.'
    url = reverse_lazy('federation_providers')

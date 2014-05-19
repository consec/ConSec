from django.contrib import messages
from django.http import HttpResponseRedirect
from django.views.generic import TemplateView
from django.views.generic.edit import FormView
from django import forms

from federweb.base.views import AppNodeMixin, RestMixin, reverse_lazy,\
    RestDetailMixin, RestRemoveView
from federweb.federation.views import AppMixin

from federweb.base.models import federation

class IdpsMixin(AppMixin, AppNodeMixin):
    node_name = 'idps'
    model = federation.idps

class IdpsList(IdpsMixin, RestMixin, TemplateView):
    pass

class IdpForm(forms.Form):
    name = forms.CharField()
    uri = forms.CharField()
    description = forms.CharField(widget=forms.Textarea())
    
    def save(self, idp):
        data = self.cleaned_data
        
        idp.providerName = data['name']
        idp.providerURI = data['uri']
        idp.description = data['description']
        
        return bool(idp.save())

class IdpsCreate(IdpsMixin, FormView):
    action_name = 'create'
    form_class = IdpForm
    success_url = reverse_lazy('federation_idps')
    
    def form_valid(self, form):
        idp = self.model.new()
        
        if form.save(idp):
            messages.success(self.request, u'Identity Provider has been created.')
        
            return HttpResponseRedirect(self.get_success_url())
        
        messages.error(self.request, 'Identity Provider could not be created.')
        
        return self.form_invalid(form)

class IdpsEdit(IdpsMixin, RestDetailMixin, FormView):
    action_name = 'edit'
    form_class = IdpForm
    success_url = reverse_lazy('federation_idps')
    
    def get_form_kwargs(self):
        kw = super(IdpsEdit, self).get_form_kwargs()
        kw['initial'] = {
            'name': self.obj.providerName,
            'uri': self.obj.providerURI,
            'description': getattr(self.obj, 'description', None),
        }
        return kw
    
    def form_valid(self, form):
        if form.save(self.obj):
            messages.success(self.request, u'Identity Provider has been saved.')
        
            return HttpResponseRedirect(self.get_success_url())
        
        messages.error(self.request, 'Identity Provider could not be saved.')
        
        return self.form_invalid(form)

class IdpsRemove(IdpsMixin, RestRemoveView):
    message = u'Identity Provider was successfully removed.'
    url = reverse_lazy('federation_idps')

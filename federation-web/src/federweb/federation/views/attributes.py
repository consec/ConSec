from django.contrib import messages
from django.http import HttpResponseRedirect
from django.views.generic import TemplateView
from django.views.generic.edit import FormView
from django import forms

from federweb.base.views import AppNodeMixin, RestMixin, reverse_lazy,\
    RestDetailMixin, RestRemoveView
from federweb.federation.views import AppMixin

from federweb.base.models import federation

class AttributesMixin(AppMixin, AppNodeMixin):
    node_name = 'attributes'
    model = federation.attributes

class AttributesList(AttributesMixin, RestMixin, TemplateView):
    pass

class AttributeForm(forms.Form):
    name = forms.CharField()
    uri = forms.CharField()
    default_value = forms.CharField()
    reference = forms.CharField()
    description = forms.CharField()
    
    def save(self, attribute):
        data = self.cleaned_data
        
        attribute.name = data['name']
        attribute.uri = data['uri']
        attribute.defaultValue = data['default_value']
        attribute.reference = data['reference']
        attribute.description = data['description']
        
        return bool(attribute.save())

class AttributesCreate(AttributesMixin, FormView):
    action_name = 'create'
    form_class = AttributeForm
    success_url = reverse_lazy('federation_attributes')
    
    def form_valid(self, form):
        attribute = self.model.new()
        
        if form.save(attribute):
            messages.success(self.request, u'Attribute has been created.')
        
            return HttpResponseRedirect(self.get_success_url())
        
        messages.error(self.request, 'Attribute could not be created.')
        
        return self.form_invalid(form)

class AttributesEdit(AttributesMixin, RestDetailMixin, FormView):
    action_name = 'edit'
    form_class = AttributeForm
    success_url = reverse_lazy('federation_attributes')
    
    def get_form_kwargs(self):
        kw = super(AttributesEdit, self).get_form_kwargs()
        kw['initial'] = {
            'name': self.obj.name,
            'uri': self.obj.uri,
            'default_value': self.obj.defaultValue,
            'reference': self.obj.reference,
            'description': self.obj.description,
        }
        return kw
    
    def form_valid(self, form):
        if form.save(self.obj):
            messages.success(self.request, u'Attribute has been saved.')
        
            return HttpResponseRedirect(self.get_success_url())
        
        messages.error(self.request, 'Attribute could not be saved.')
        
        return self.form_invalid(form)

class AttributesRemove(AttributesMixin, RestRemoveView):
    message = u'Attribute was successfully removed.'
    url = reverse_lazy('federation_attributes')

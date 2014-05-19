from django.contrib import messages
from django.http import HttpResponseRedirect
from django.views.generic import TemplateView
from django.views.generic.edit import FormView
from django import forms

from federweb.base.views import AppNodeMixin, RestMixin, reverse_lazy,\
    RestDetailMixin, RestRemoveView
from federweb.federation.views import AppMixin

from federweb.base.models import federation

class RolesMixin(AppMixin, AppNodeMixin):
    node_name = 'roles'
    model = federation.roles

class RolesList(RolesMixin, RestMixin, TemplateView):
    pass

class RoleForm(forms.Form):
    name = forms.CharField()
    description = forms.CharField(required=False)
    acl = forms.CharField(required=False)
    
    def save(self, role):
        data = self.cleaned_data
        
        role.name = data['name']
        role.description = data['description']
        role.acl = data['acl']
        
        return bool(role.save())

class RolesCreate(RolesMixin, FormView):
    action_name = 'create'
    form_class = RoleForm
    success_url = reverse_lazy('federation_roles')
    
    def form_valid(self, form):
        role = self.model.new()
        
        if form.save(role):
            messages.success(self.request, u'Role has been created.')
        
            return HttpResponseRedirect(self.get_success_url())
        
        messages.error(self.request, 'Role could not be created.')
        
        return self.form_invalid(form)

class RolesEdit(RolesMixin, RestDetailMixin, FormView):
    action_name = 'edit'
    form_class = RoleForm
    success_url = reverse_lazy('federation_roles')
    
    def get_form_kwargs(self):
        kw = super(RolesEdit, self).get_form_kwargs()
        kw['initial'] = {
            'name': self.obj.name,
            'description': self.obj.description,
            'acl': self.obj.acl,
        }
        return kw
    
    def form_valid(self, form):
        if form.save(self.obj):
            messages.success(self.request, u'Role has been saved.')
        
            return HttpResponseRedirect(self.get_success_url())
        
        messages.error(self.request, 'Role could not be saved.')
        
        return self.form_invalid(form)

class RolesRemove(RolesMixin, RestRemoveView):
    message = u'Role was successfully removed.'
    url = reverse_lazy('federation_roles')

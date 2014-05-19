from django.contrib import messages
from django.http import HttpResponseRedirect
from django.views.generic import TemplateView
from django.views.generic.edit import FormView
from django import forms

from federweb.base.views import AppNodeMixin, RestMixin, reverse_lazy,\
    RestDetailMixin, RestRemoveView
from federweb.federation.views import AppMixin

from federweb.base.models import federation

class GroupsMixin(AppMixin, AppNodeMixin):
    node_name = 'groups'
    model = federation.groups

class GroupsList(GroupsMixin, RestMixin, TemplateView):
    pass

class GroupForm(forms.Form):
    name = forms.CharField()
    
    def save(self, group):
        data = self.cleaned_data
        
        group.name = data['name']
        
        return bool(group.save())

class GroupsCreate(GroupsMixin, FormView):
    action_name = 'create'
    form_class = GroupForm
    success_url = reverse_lazy('federation_groups')
    
    def form_valid(self, form):
        group = self.model.new()
        
        if form.save(group):
            messages.success(self.request, u'Group has been created.')
        
            return HttpResponseRedirect(self.get_success_url())
        
        messages.error(self.request, 'Group could not be created.')
        
        return self.form_invalid(form)

class GroupsEdit(GroupsMixin, RestDetailMixin, FormView):
    action_name = 'edit'
    form_class = GroupForm
    success_url = reverse_lazy('federation_groups')
    
    def get_form_kwargs(self):
        kw = super(GroupsEdit, self).get_form_kwargs()
        kw['initial'] = {
            'name': self.obj.name,
        }
        return kw
    
    def form_valid(self, form):
        if form.save(self.obj):
            messages.success(self.request, u'Group has been saved.')
        
            return HttpResponseRedirect(self.get_success_url())
        
        messages.error(self.request, 'Group could not be saved.')
        
        return self.form_invalid(form)

class GroupsRemove(GroupsMixin, RestRemoveView):
    message = u'Group was successfully removed.'
    url = reverse_lazy('federation_groups')

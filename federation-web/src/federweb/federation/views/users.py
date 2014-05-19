from django.contrib import messages
from django.http import HttpResponseRedirect
from django.views.generic import TemplateView
from django.views.generic.edit import FormView
from django import forms

from federweb.base.views import AppNodeMixin, RestMixin, reverse_lazy,\
    RestDetailMixin, RestRemoveView
from federweb.federation.views import AppMixin

from federweb.base.models import federation

class UsersMixin(AppMixin, AppNodeMixin):
    node_name = 'users'
    model = federation.users

class UsersList(UsersMixin, RestMixin, TemplateView):
    pass

class UserCreateForm(forms.Form):
    username = forms.CharField()
    password = forms.CharField(widget=forms.PasswordInput())
    email = forms.EmailField()
    first_name = forms.CharField()
    last_name = forms.CharField()
    roles = forms.MultipleChoiceField(choices=[], required=True)
    
    def save(self, user):
        data = self.cleaned_data
        
        user.username = data['username']
        user.password = data['password']
        user.email = data['email']
        user.firstName = data['first_name']
        user.lastName = data['last_name']
        
        if user.save():
            for role in data['roles']:
                ur = user.roles.new()
                ur.roleId = role
                ur.save()
            
            return True
        
        return False

class UsersCreate(UsersMixin, FormView):
    action_name = 'create'
    form_class = UserCreateForm
    success_url = reverse_lazy('federation_users')
    
    def get_roles(self):
        yield ('', '-------')
        for x in federation.roles.all():
            yield (x.id, x.name)
    
    def get_form(self, cls):
        form = super(UsersCreate, self).get_form(cls)
        form.fields['roles'].choices = list(self.get_roles())
        return form
    
    def form_valid(self, form):
        user = self.model.new()
        
        if form.save(user):
            messages.success(self.request, u'User has been created.')
        
            return HttpResponseRedirect(self.get_success_url())
        
        messages.error(self.request, 'User could not be created.')
        
        return self.form_invalid(form)

class UserEditForm(forms.Form):
    username = forms.CharField()
    email = forms.EmailField()
    first_name = forms.CharField()
    last_name = forms.CharField()
    roles = forms.MultipleChoiceField(choices=[])
    
    def save(self, user):
        data = self.cleaned_data
        
        user.username = data['username']
        user.email = data['email']
        user.firstName = data['first_name']
        user.lastName = data['last_name']
        
        if user.save():
            [x.delete() for x in user.roles.all()]
            
            for role in data['roles']:
                ur = user.roles.new()
                ur.roleId = role
                ur.save()
            
            return True
        
        return False

class UsersEdit(UsersMixin, RestDetailMixin, FormView):
    action_name = 'edit'
    form_class = UserEditForm
    success_url = reverse_lazy('federation_users')
    
    def get_roles(self):
        yield ('', '-------')
        for x in federation.roles.all():
            yield (x.id, x.name)
    
    def get_form(self, cls):
        form = super(UsersEdit, self).get_form(cls)
        form.fields['roles'].choices = list(self.get_roles())
        return form
    
    def get_form_kwargs(self):
        kw = super(UsersEdit, self).get_form_kwargs()
        u = self.obj
        kw['initial'] = {
            'username': u.username,
            'email': u.email,
            'first_name': u.firstName,
            'last_name': u.lastName,
            'roles': [x.id for x in u.roles.all()]
        }
        return kw
    
    def form_valid(self, form):
        user = self.obj
        
        if form.save(user):
            messages.success(self.request, u'User has been saved.')
        
            return HttpResponseRedirect(self.get_success_url())
        
        messages.error(self.request, 'User could not be saved.')
        
        return self.form_invalid(form)

class UsersRemove(UsersMixin, RestRemoveView):
    message = u'User was successfully removed.'
    url = reverse_lazy('federation_users')

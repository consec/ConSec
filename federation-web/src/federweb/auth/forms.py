from django import forms
from django.contrib.auth.forms import AuthenticationForm
from django.core import validators
from federweb.base.models import federation

class LoginForm(AuthenticationForm):
    remember = forms.BooleanField(initial=False, required=False)

invalid_username_msg = ('This value must contain only letters, '
                        'numbers and underscores.')

class RegistrationForm(forms.Form):
    first_name = forms.CharField(label='First name')
    last_name = forms.CharField(label='Last name')
    username = forms.RegexField(regex=r'^[\w.@+-]+$',
                                max_length=30,
                                error_messages={'invalid': invalid_username_msg})
    email = forms.EmailField(label='E-mail')
    password1 = forms.CharField(label='Password',
                                widget=forms.PasswordInput())
    password2 = forms.CharField(label='Password (again)',
                                widget=forms.PasswordInput())
    
    def clean(self):
        if 'password1' in self.cleaned_data and 'password2' in self.cleaned_data:
            if self.cleaned_data['password1'] != self.cleaned_data['password2']:
                raise forms.ValidationError("The two password fields didn't match.")
        
        return self.cleaned_data

class AccountSettingsForm(forms.Form):    
    
    first_name = forms.CharField(label='First name', required=False)
    last_name = forms.CharField(label='Last name', required=False)    
    email = forms.EmailField(label='E-mail')
    password1 = forms.CharField(label='New password',
                                widget=forms.PasswordInput(), required=False)
    password2 = forms.CharField(label='New password (again)',
                                widget=forms.PasswordInput(), required=False)    
    roles = forms.CharField(label= 'Role:', required=False,widget = forms.TextInput(attrs={'readonly':'readonly'}) )                       
    
    def clean(self):
        if 'password1' in self.cleaned_data and 'password2' in self.cleaned_data:
            if self.cleaned_data['password1'] != self.cleaned_data['password2']:
                raise forms.ValidationError("The two password fields didn't match.")
        
        return self.cleaned_data
    
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
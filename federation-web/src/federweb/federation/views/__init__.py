from django.http import Http404
from django.utils.decorators import method_decorator

from federweb.base.views import RoleRequiredMixin

class AppMixin(RoleRequiredMixin):
    app_name = 'federation'
    user_role = 'FederationCoordinator'

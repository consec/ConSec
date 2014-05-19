from federweb.base.views import RoleRequiredMixin

class AppMixin(RoleRequiredMixin):
    app_name = 'user'
    user_role = 'FederationUser'

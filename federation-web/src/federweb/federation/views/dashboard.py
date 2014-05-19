from django.views.generic.base import TemplateView

from federweb.base.views import AppNodeMixin
from federweb.federation.views import AppMixin

class Dashboard(AppMixin, AppNodeMixin, TemplateView):
    node_name = 'dashboard'

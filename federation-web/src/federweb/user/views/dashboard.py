from django.views.generic.base import TemplateView
from django.contrib.auth.views import redirect_to_login
from django.conf import settings

class DashboardView(TemplateView):
    template_name = "user/dashboard.html"

    def dispatch(self, request, *args, **kwargs):
        if not request.user.is_authenticated():
            return redirect_to_login(request.path)
        response = super(DashboardView, self).dispatch(request, *args, **kwargs)
        return response

    def get_context_data(self, **kwargs):
        context = super(DashboardView, self).get_context_data(**kwargs)
        context['MON_DRIVER'] = settings.MON_DRIVER
        return context

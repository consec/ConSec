from django.conf.urls.defaults import patterns, url

from federweb.user.views.authorization import AuthorizationTrustList, authorizationTrustModify
from federweb.user.views.dashboard import DashboardView

urlpatterns = patterns('',
    url(r'^$', DashboardView.as_view(), name='user_dashboard'),

    url(r'^/authorization/trust$', AuthorizationTrustList.as_view(), name='user_authorization_trust'),
    url(r'^/authorization/trust/modify/(?P<organization_id>\d+)$', authorizationTrustModify, name='user_authorization_trust_modify_organizations'),
    url(r'^/authorization/trust/modify/(?P<organization_id>\d+)/clients/(?P<client_id>\d+)$', authorizationTrustModify, name='user_authorization_trust_modify_organizations_clients'),

    # Auditing
    url(r'^/auditing$', 'federweb.user.views.auditing.auditing_show_tab', name='user_auditing'),

    url(r'^/debug$', 'federweb.user.views.debug.show_debug', name='user_show_debug'),
)

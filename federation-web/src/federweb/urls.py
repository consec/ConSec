from django.conf.urls.defaults import patterns, include, url
from django.conf import settings
from django.views.generic.simple import redirect_to

urlpatterns = patterns('',
    url(r'^$', 'federweb.base.views.home', name='home'),

    url(r'^federation', include('federweb.federation.urls')),
    url(r'^user', include('federweb.user.urls')),
    url(r'^account_settings', include('federweb.auth.urls')),

    url(r'^hookbox', include('federweb.hookbox.urls')),
    
    url(r'^login$', 'federweb.auth.views.login_view', name='login'),
    url(r'^logout$', 'federweb.auth.views.logout_view', name='logout'),    
    url(r'^registration$', 'federweb.auth.views.registration_view', name='registration'),
    url(r'^getcert$', 'federweb.auth.views.get_cert_view', name='getcert'),
    url(r'^getkey$', 'federweb.auth.views.get_key_view', name='getkey'),

    url(r'^saml2/login_error/$', 'federweb.auth.views.saml2_error_view', name='contrail_saml2_login_error'),
    url(r'^saml2/login/$', 'federweb.auth.views.login_saml2_view', name='contrail_saml2_login'),
    url(r'^saml2/acs/$', 'federweb.auth.views.assertion_consumer_service_view', name='contrail_saml2_acs'),
    url(r'^saml2/logout/$', 'federweb.auth.views.logout_saml2_view', name='contrail_saml2_logout'),
    url(r'^saml2/ls/$', 'federweb.auth.views.logout_ls_saml2_view', name='contrail_saml2_ls'),
    url(r'^oauth2callback/$', 'federweb.auth.views.oauth2callback', name='oauth2callback'),
    url(r'^saml2/', include('djangosaml2.urls')),
    url(r'^test/', 'djangosaml2.views.echo_attributes'),
)

handler500 = 'federweb.base.views.error500'

if settings.DEBUG or settings.SERVE_STATIC:
    urlpatterns += patterns('django.contrib.staticfiles.views',
        url(r'^static/(?P<path>.*)$', 'serve', {'insecure': True}),
    )

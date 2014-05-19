from django.conf.urls.defaults import patterns, url

urlpatterns = patterns('',
    url(r'^$', 'federweb.auth.views.account_settings_view', name='base_account_settings'),
    url(r'^/attributes$', 'federweb.auth.views.get_user_attribute_list', name='attributes_account_settings'),
    url(r'^/attributes/create$', 'federweb.auth.views.user_attribute_create', name='user_attributes_create'),
    url(r'^/attributes/(?P<id>[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12})/edit$',
        'federweb.auth.views.user_attribute_edit', name='user_attributes_edit'),
    url(r'^/attributes/(?P<id>[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12})/remove$',
        'federweb.auth.views.user_attribute_remove', name='user_attributes_remove'),
)

handler500 = 'federweb.base.views.error500'
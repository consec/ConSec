from django.conf.urls.defaults import patterns, url

urlpatterns = patterns('',
    url(r'^$', 'federweb.hookbox.views.hookbox', name='hookbox')
)

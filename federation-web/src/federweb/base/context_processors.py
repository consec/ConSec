from django.conf import settings

from federweb.base.models import federation as federation_model

def breadcrumbs(request):
    return {'breadcrumbs': []}

def compressor(request):
    return {
        'DEBUG': settings.DEBUG,
        'COMPRESS_ENABLED': settings.COMPRESS_ENABLED
    }

def federation(request):
    return {'federation': federation_model}

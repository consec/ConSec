from django.conf import settings

def hookbox(request):
    return {
        'HOOKBOX_BASE': settings.HOOKBOX_BASE,
        'HOOKBOX_JS': '%s/static/hookbox.js' % settings.HOOKBOX_BASE
    }

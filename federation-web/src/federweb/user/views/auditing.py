from django.shortcuts import render_to_response

import logging
from django.conf import settings
from django.template.context import RequestContext

logger = logging.getLogger(__name__)


def auditing_show_tab(request, template='user/auditing/auditing.html'):
    return render_to_response(
        template,
        dict(
            userUuid=request.user.uuid,
            fedApiUrl=settings.FEDERATION_API_URL_SSL,
            auditManagerUrl=settings.AUDIT_MANAGER_URL,
            oauthASAdminUrl=settings.OAUTH_AS_API_URL
        ),
        context_instance=RequestContext(request))

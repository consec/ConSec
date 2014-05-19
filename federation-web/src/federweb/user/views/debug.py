from django.shortcuts import render_to_response
from django.conf import settings
from django.template.context import RequestContext
from django.http import HttpResponse
import requests
import json

def show_debug(request):
    user = request.user
    access_token = request.session['oauth_access_token']
    token_info = get_token_info(access_token)
    parsed = json.loads(token_info)
    token_info_formatted = json.dumps(parsed, indent=2)
    cert = request.session['certificate']
    return render_to_response(
        "user/debug.html",
        dict(
            userUuid=request.user.uuid,
            access_token=access_token,
            token_info=token_info_formatted,
            cert=cert,
        ),
        context_instance=RequestContext(request))

def get_token_info(access_token):
    headers = {
        "Accept": "application/json",
        "Content-Type": "application/x-www-form-urlencoded"
    }

    client_cert = settings.OAUTH2_CONFIG['oauth_client']['client_cert']
    client_key = settings.OAUTH2_CONFIG['oauth_client']['client_key']

    data = dict(
        access_token=access_token,
        bearer_id="CN=federation-web"
    )
    r = requests.request(
        method="POST",
        url=settings.OAUTH2_AS_URI + "/oauth-as/r/access_token/check",
        data=data,
        headers=headers,
        cert=(client_cert, client_key),
        verify=False
    )

    if r.status_code == 200:
        return r.text
    else:
        return "Failed to obtain token info: " + str(r.status_code)

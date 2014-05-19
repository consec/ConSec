from django.core.management.base import NoArgsCommand

default_settings = '''\
STATIC_ROOT = '/var/lib/contrail/federation/federation-web/static'

FEDERATION_API_URL = 'http://localhost:8080/federation-api'
FEDERATION_UUID = "dadb2c20-5351-11e3-8f96-0800200c9a66"
SLA_EXTRACTOR_BASE = 'http://localhost:8080/rest-monitoring/sla/slaextractor'
MONITORING_BASE = 'http://localhost:8080/rest-monitoring/monitoring'
ZOOKEEPER_BASE = '127.0.0.1:2181'

ONLINE_CA_USE=False
ONLINE_CA_URI='https://one-test.contrail.rl.ac.uk:8443/ca/portaluser'

FEDERATION_WEB='https://contrail-federation-web.contrail.eu'
FEDERATION_WEB_LOCAL_METADATA='/usr/lib/contrail/federation-web/extra/remote_metadata.xml'
FEDERATION_WEB_CERT='/usr/lib/contrail/federation-web/extra/contrail-federation-web.cert'
FEDERATION_WEB_KEY='/usr/lib/contrail/federation-web/extra/contrail-federation-web.key'
FEDERATION_WEB_CA_FILE='/usr/lib/contrail/federation-web/extra/ca.crt'
TRUSTSTORE_DIR = '/etc/contrail/truststore'

SSL_USE_DELEGATED_USER_CERT=False

MULTI_IDP_FEDERATION='https://multi.contrail-idp.contrail.eu'
FEDERATION_AUTH_ENDPOINT = FEDERATION_API_URL + '/usersutils/authenticate'
OAUTH2_AS_URI = 'https://localhost:8443'

ONLINE_OAUTH2_CA_USE=False
ONLINE_OAUTH2_CA_URI='https://localhost:8443/ca/o/delegateduser'

# Monitoring
MON_DRIVER = {}
MON_DRIVER["active"] = "contrail_monitoring"
MON_DRIVER["contrail_monitoring"] = {}
MON_DRIVER["contrail_monitoring"]['feder-api-uri'] = "http://contrail.xlab.si:8080/federation-api"
MON_DRIVER["contrail_monitoring"]['feder-acc-uri'] = "https://contrail.xlab.si:8443/federation-accounting"
MON_DRIVER["contrail_monitoring"]['proxy-uri'] = "/user/jsonproxy/"
####

'''

class Command(NoArgsCommand):
    help = 'Prints default settings for /etc/contrail/federation-web.conf'
    
    def handle_noargs(self, **options):
        self.stdout.write(default_settings)

import os
import tempfile

from os import path
import saml2
BASEDIR = path.dirname(path.abspath(__file__))

_static_root = os.path.join(tempfile.gettempdir(), 'federwebstatic')

if not os.path.exists(_static_root):
    try:
        os.mkdir(_static_root)
    except OSError:
        print 'Could not create temporary directory for static files!'

PROJECT_PATH = os.path.dirname(os.path.abspath(__file__))
DEBUG = False
TEMPLATE_DEBUG = DEBUG

ADMINS = (
    # ('Your Name', 'your_email@example.com'),
)

MANAGERS = ADMINS

DATABASES = {
    'default': {
        'ENGINE': 'django.db.backends.sqlite3', # Add 'postgresql_psycopg2', 'postgresql', 'mysql', 'sqlite3' or 'oracle'.
        'NAME': '',                      # Or path to database file if using sqlite3.
        'USER': '',                      # Not used with sqlite3.
        'PASSWORD': '',                  # Not used with sqlite3.
        'HOST': '',                      # Set to empty string for localhost. Not used with sqlite3.
        'PORT': '',                      # Set to empty string for default. Not used with sqlite3.
    }
}

# Local time zone for this installation. Choices can be found here:
# http://en.wikipedia.org/wiki/List_of_tz_zones_by_name
# although not all choices may be available on all operating systems.
# On Unix systems, a value of None will cause Django to use the same
# timezone as the operating system.
# If running in a Windows environment this must be set to the same as your
# system time zone.
TIME_ZONE = 'Europe/London'

# Language code for this installation. All choices can be found here:
# http://www.i18nguy.com/unicode/language-identifiers.html
LANGUAGE_CODE = 'en-us'

SITE_ID = 1

# If you set this to False, Django will make some optimizations so as not
# to load the internationalization machinery.
USE_I18N = False

# If you set this to False, Django will not format dates, numbers and
# calendars according to the current locale
USE_L10N = False

# Absolute filesystem path to the directory that will hold user-uploaded files.
# Example: "/home/media/media.lawrence.com/media/"
MEDIA_ROOT = ''

# URL that handles the media served from MEDIA_ROOT. Make sure to use a
# trailing slash.
# Examples: "http://media.lawrence.com/media/", "http://example.com/media/"
MEDIA_URL = ''

# Absolute path to the directory static files should be collected to.
# Don't put anything in this directory yourself; store your static files
# in apps' "static/" subdirectories and in STATICFILES_DIRS.
# Example: "/home/media/media.lawrence.com/static/"
STATIC_ROOT = _static_root

# URL prefix for static files.
# Example: "http://media.lawrence.com/static/"
STATIC_URL = '/static/'

# Additional locations of static files
STATICFILES_DIRS = (
    # Put strings here, like "/home/html/static" or "C:/www/django/static".
    # Always use forward slashes, even on Windows.
    # Don't forget to use absolute paths, not relative paths.
    # os.path.join(PROJECT_PATH, 'static'),
)

# List of finder classes that know how to find static files in
# various locations.
STATICFILES_FINDERS = (
    'django.contrib.staticfiles.finders.FileSystemFinder',
    'django.contrib.staticfiles.finders.AppDirectoriesFinder',
    'compressor.finders.CompressorFinder',
#    'django.contrib.staticfiles.finders.DefaultStorageFinder',
)

# Make this unique, and don't share it with anybody.
SECRET_KEY = 'ikw8+y!8kemv0yhd_#l5xbsi$60^v9g65iz6@6&+q9$ahjq%va'

# List of callables that know how to import templates from various sources.
TEMPLATE_LOADERS = (
    'django.template.loaders.filesystem.Loader',
    'django.template.loaders.app_directories.Loader',
    'django.template.loaders.eggs.Loader',
)

TEMPLATE_CONTEXT_PROCESSORS = (
    'django.contrib.auth.context_processors.auth',
    'django.core.context_processors.debug',
    'django.core.context_processors.i18n',
    'django.core.context_processors.media',
    'django.core.context_processors.static',
    'django.core.context_processors.request',

    'django.contrib.messages.context_processors.messages',
    
    'federweb.base.context_processors.breadcrumbs',
    'federweb.base.context_processors.compressor',
    'federweb.base.context_processors.federation',
    
    'federweb.hookbox.context_processors.hookbox',
)

MIDDLEWARE_CLASSES = (
    'django.middleware.common.CommonMiddleware',
    'django.contrib.sessions.middleware.SessionMiddleware',
    'django.middleware.csrf.CsrfViewMiddleware',
    'django.contrib.auth.middleware.AuthenticationMiddleware',
    'django.contrib.messages.middleware.MessageMiddleware',
    
    #'debug_toolbar.middleware.DebugToolbarMiddleware',
)

AUTHENTICATION_BACKENDS = (
    'federweb.auth.models.RestEngineBackend',
#    'django.contrib.auth.backends.ModelBackend',
    'djangosaml2.backends.Saml2Backend',
)

ROOT_URLCONF = 'federweb.urls'

TEMPLATE_DIRS = (
    
)

INTERNAL_IPS = []

INSTALLED_APPS = (
    'django.contrib.sessions',
    'django.contrib.messages',
    'django.contrib.staticfiles',
    'django.contrib.contenttypes',
    
    'django_extensions',
    'concurrent_server',
    'compressor',
    'debug_toolbar',
    'dajax',
    
    'federweb',
    'federweb.base',
    'federweb.auth',
    'federweb.federation',
    'federweb.user',
    'federweb.utils',
    'djangosaml2',
)

# A sample logging configuration. The only tangible logging
# performed by this configuration is to send an email to
# the site admins on every HTTP 500 error.
# See http://docs.djangoproject.com/en/dev/topics/logging for
# more details on how to customize your logging configuration.
LOGGING = {
    'version': 1,
    'disable_existing_loggers': False,
    'formatters': {
        'verbose': {
            'format': '%(levelname)s %(asctime)s %(module)s %(process)d %(thread)d %(message)s'
        },
        'simple': {
            'format': '[%(asctime)s] %(levelname)s: %(message)s'
        },
    },
    'handlers': {
        'mail_admins': {
            'level': 'ERROR',
            'class': 'django.utils.log.AdminEmailHandler'
        },
        'console':{
            'level':'DEBUG',
            'class':'logging.StreamHandler',
            'formatter': 'simple'
        }
    },
    'loggers': {
        'django.request': {
            'handlers': ['mail_admins', 'console'],
            'level': 'INFO',
            'propagate': True,
        },
        'federweb': {
            'handlers': ['console'],
            'level': 'DEBUG',
            'propagate': True,
        },    
    'djangosaml2': {
        'handlers': ['console'],
        'level': 'DEBUG',
        'propagate': True,
        },
     'saml2': {
         'handlers': ['console'],
         'level': 'DEBUG',
         'propagate': True,
      },
                }
}

SESSION_ENGINE = 'django.contrib.sessions.backends.file'

#LOGIN_URL = '/login'
LOGIN_URL = '/saml2/login/'
LOGOUT_URL = '/logout'
LOGIN_REDIRECT_URL = '/login'

ACCOUNT_ACTIVATION_DAYS = 10

OPENID_SREG = {
    "required": ['fullname', 'country']
}


DEBUG_TOOLBAR_CONFIG = {
    'INTERCEPT_REDIRECTS': False,
}

SERVE_STATIC = False
COMPRESS_ENABLED = True

STATIC_RESOURCES = {
    'less': [
        ('base/static/less/style.less', 'base/static/css/style.css'),
    ],
    'coffee': [
        ('user/static/user/apps/apps', ''),
    ]
}

FEDERATION_API_URL = 'http://id-prov.contrail-idp.xlab.si:8080/federation-api'
FEDERATION_UUID = "dadb2c20-5351-11e3-8f96-0800200c9a66"
SLA_EXTRACTOR_BASE = 'http://10.31.1.10:8080/headNodeRestProxy/sla/slaextractor'
MONITORING_BASE = 'http://10.31.1.10:8080/headNodeRestProxy/monitoring'
ZOOKEEPER_BASE = '127.0.0.1:2181'

""" OnlineCA -- TODO add missing confs
"""
ONLINE_CA_USE=True
ONLINE_CA_URI='https://contrail.xlab.si:8443/ca/portaluser'

ONLINE_OAUTH2_CA_USE=True
ONLINE_OAUTH2_CA_URI='https://contrail.xlab.si:8443/ca/o/delegateduser'


""" Contrail IdP and SP settings
"""
""" Access point to the Federation-web service """
FEDERATION_WEB='http://contrail-federation-web.contrail.eu'
""" Access point to the Contrail-IdP service """
FEDERATION_IDP_GOOGLE='https://google.contrail-idp.contrail.eu'
FEDERATION_IDP_FEDERATION='https://federation.contrail-idp.contrail.eu'
MULTI_IDP_FEDERATION='http://multi.contrail-idp.contrail.eu'
FEDERATION_WEB_LOCAL_METADATA=path.join(BASEDIR, '../../../extra/remote_metadata.xml')
FEDERATION_WEB_CERT=path.join(BASEDIR, '../../../extra/contrail-federation-web.cert')
FEDERATION_WEB_KEY=path.join(BASEDIR, '../../../extra/contrail-federation-web.key')
FEDERATION_WEB_CA_FILE='../../../extra/ca.crt'
FEDERATION_AUTH_ENDPOINT = FEDERATION_API_URL + '/usersutils/authenticate'
TRUSTSTORE_DIR = '../../../extra/truststore'
OAUTH2_AS_URI = 'https://contrail.xlab.si:8443'

SSL_USE_DELEGATED_USER_CERT=False

# Monitoring
MON_DRIVER = {}
MON_DRIVER["active"] = "contrail_monitoring"
MON_DRIVER["contrail_monitoring"] = {}
MON_DRIVER["contrail_monitoring"]['feder-api-uri'] = "http://contrail.xlab.si:8080/federation-api"
MON_DRIVER["contrail_monitoring"]['feder-acc-uri'] = "https://contrail.xlab.si:8443/federation-accounting"
MON_DRIVER["contrail_monitoring"]['proxy-uri'] = "http://172.16.118.177:8000/user/jsonproxy/"
##

OAUTH_API_URL = 'http://contrail.xlab.si:8080/oauth-as/admin'

HOOKBOX_BASE = None
HOOKBOX_SECRET = None

CONFIG_FILES = [
    '/etc/contrail/contrail-federation-web/federation-web.conf-1204',
    '/etc/contrail/contrail-federation-web/federation-web.conf',
    '/etc/contrail/federation/federation-web.conf',
    '/etc/contrail/federation-web.conf'
]

for cf in CONFIG_FILES:
    try:
        execfile(cf)
    except IOError:
        pass

try:
    from local_settings import *
except ImportError:
    pass

SESSION_EXPIRE_AT_BROWSER_CLOSE = True

SAML_CREATE_UNKNOWN_USER=False

SAML_CONFIG = {
  # full path to the xmlsec1 binary programm
  #'xmlsec_binary': '/usr/local/bin/xmlsec1',
  'xmlsec_binary': '/usr/bin/xmlsec1',

  # your entity id, usually your subdomain plus the url to the metadata view
  'entityid': FEDERATION_WEB+'/saml2/metadata/',

  # directory with attribute mapping
  'attribute_map_dir': path.join(BASEDIR, '../djangoSaml/attributemaps'),

  # this block states what services we provide
  'service': {
      # we are just a lonely SP
      'sp' : {
          'name': 'Federated Django Contrail SP',
          'endpoints': {
              # url and binding to the assertion consumer service view
              # do not change the binding or service name
              'assertion_consumer_service': [
                  (FEDERATION_WEB+'/saml2/acs/',
                   saml2.BINDING_HTTP_POST),
                  ],
              # url and binding to the single logout service view
              # do not change the binding or service name
              'single_logout_service': [
                  (FEDERATION_WEB+'/saml2/ls/',
                   saml2.BINDING_HTTP_REDIRECT),
                  ],
              },

           # attributes that this project need to identify a user
          'required_attributes': ['uid'],

           # attributes that may be useful to have but not required
          'optional_attributes': ['eduPersonAffiliation'],

          # in this section the list of IdPs we talk to are defined
          'idp': {
              # we do not need a WAYF service since there is
              # only an IdP defined here. This IdP should be
              # present in our metadata

              # the keys of this dictionary are entity ids             
              FEDERATION_IDP_FEDERATION+'/simplesaml/saml2/idp/metadata.php': {
                  'single_sign_on_service': {
                      saml2.BINDING_HTTP_REDIRECT: FEDERATION_IDP_FEDERATION+'/simplesaml/saml2/idp/SSOService.php',
                      },
                  'single_logout_service': {
                      saml2.BINDING_HTTP_REDIRECT: FEDERATION_IDP_FEDERATION+'/simplesaml/saml2/idp/SingleLogoutService.php',
                      },
                  },
                  
                FEDERATION_IDP_GOOGLE+'/simplesaml/saml2/idp/metadata.php': {
                  'single_sign_on_service': {
                      saml2.BINDING_HTTP_REDIRECT: FEDERATION_IDP_GOOGLE+'/simplesaml/saml2/idp/SSOService.php',
                      },
                  'single_logout_service': {
                      saml2.BINDING_HTTP_REDIRECT: FEDERATION_IDP_GOOGLE+'/simplesaml/saml2/idp/SingleLogoutService.php',
                      },
                  },   
                  MULTI_IDP_FEDERATION+'/simplesaml/saml2/idp/metadata.php': {
                  'single_sign_on_service': {
                      saml2.BINDING_HTTP_REDIRECT: MULTI_IDP_FEDERATION+'/simplesaml/saml2/idp/SSOService.php',
                      },
                  'single_logout_service': {
                      saml2.BINDING_HTTP_REDIRECT: MULTI_IDP_FEDERATION+'/simplesaml/saml2/idp/SingleLogoutService.php',
                      },
                  },          

              },
          },
      },

  # where the remote metadata is stored
  'metadata': {
      'local': [FEDERATION_WEB_LOCAL_METADATA],
      },

  # set to 1 to output debugging information
  'debug': 1,

  # certificate
  'key_file': FEDERATION_WEB_KEY,  # private part
  'cert_file': FEDERATION_WEB_CERT,  # public part

  # own metadata settings
  'contact_person': [
      {'given_name': 'Ales',
       'sur_name': 'Cernivec',
       'company': 'XLAB d.o.o.',
       'email_address': 'ales.cernivec@xlab.si',
       'contact_type': 'technical'},
      ],
  # you can set multilanguage information here
  'organization': {
      'name': [('Contrail', 'en')],
      'display_name': [('Contrail', 'en')],
      'url': [('http://www.contrail-project.eu', 'en')],
      },
  'valid_for': 24,  # how long is our metadata valid
  }

# This is for Python OAuth Client code 
OAUTH2_CONFIG = {
                'oauth_client':
                {'callback_url':FEDERATION_WEB+'/oauth2callback',
                 'truststore':'pki/truststore',
                 'client_cert':FEDERATION_WEB_CERT,
                 'client_key':FEDERATION_WEB_KEY,
                 'client_key_pwd':'contrail',
                 'client_id':'oauth-python-client-demo',
                 'client_secret':'somesecret',
                 'grant_type':'authorization_code',
                 'scope':'USER_PROFILE GENERATE_USER_CERTIFICATE',
                 'pickle_file':'tempdata.pkl',
                },
                'oauth_authz_server':
                {'response_type':'code',
                'auth_endpoint':OAUTH2_AS_URI+'/oauth-as',
                'access_token_endpoint':OAUTH2_AS_URI+'/oauth-as/authorize',
                },
                'ca_server':
                {
                 'url': ONLINE_OAUTH2_CA_URI,
                 },
}

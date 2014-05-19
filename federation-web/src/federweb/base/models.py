import requests
import logging
import re

from django.utils import simplejson as json
from django.conf import settings
from federweb.base.rest import RestManager, RestModel, hooks
from federweb.base.utils import cached_property

logger = logging.getLogger(__name__)

class FederationManager(object):
    def __init__(self, base_url):
        self.base_url = base_url
        
        ''' Holds URI for users to authenticate with. Can be different than 'users'
        e.g. /usersutils/authenticate
        '''    
        self.authentication_uri = settings.FEDERATION_AUTH_ENDPOINT#'/usersutils/authenticate'
        
        if settings.FEDERATION_WEB_CA_FILE:        
            ca_file = settings.FEDERATION_WEB_CA_FILE
        if settings.FEDERATION_WEB_CERT:
            cert_file = settings.FEDERATION_WEB_CERT
        if settings.FEDERATION_WEB_KEY:
            key_file = settings.FEDERATION_WEB_KEY
                            
        self.session = requests.session(cert=(cert_file,key_file), verify=ca_file)
        self.session.hooks = hooks
        
        self.attributes = AttributeManager(self)
        self.ovfs = OvfManager(self)
        self.applications = ApplicationManager(self)
        self.roles = RoleManager(self)
        self.groups = GroupManager(self)
        self.idps = IdentityProviderManager(self)
        self.users = UserManager(self)
        self.providers = ProviderManager(self)
    
    def get_base_uri(self):
        return self.base_url
    
    def get_relative_uri(self):
        return ''


class OAuthManager(object):
    def __init__(self, base_url):
        self.base_url = base_url

        ''' Holds URI for users to authenticate with. Can be different than 'users'
        e.g. /usersutils/authenticate
        '''
        self.authentication_uri = settings.FEDERATION_AUTH_ENDPOINT#'/usersutils/authenticate'

        if settings.FEDERATION_WEB_CA_FILE:
            ca_file = settings.FEDERATION_WEB_CA_FILE
        if settings.FEDERATION_WEB_CERT:
            cert_file = settings.FEDERATION_WEB_CERT
        if settings.FEDERATION_WEB_KEY:
            key_file = settings.FEDERATION_WEB_KEY

        self.session = requests.session(cert=(cert_file,key_file), verify=ca_file)
        self.session.hooks = hooks

        self.authorization_organizations = AuthorizationOrganizationsManager(self)

    def authorization_organizations_clients(self, organization_id):
        return AuthorizationOrganizationsClientsManager(self, organization_id)

    def authorization_trust(self, owner_id):
        return AuthorizationTrustManager(self, owner_id)

    def authorization_owners_organizations(self, owner_id):
        return AuthorizationOwnersOrganizationsManager(self, owner_id)

    def authorization_owners_organizations_clients(self, owner_id, organization_id):
        return AuthorizationOwnersOrganizationsClientsManager(self, owner_id, organization_id)

    def get_base_uri(self):
        return self.base_url

    def get_relative_uri(self):
        return ''


class AttributeManager(RestManager):
    def new(self, *args, **kwargs):
        return Attribute(self, *args, **kwargs)
    
    def get_relative_uri(self):
        return self.parent.get_relative_uri() + '/attributes'

class Attribute(RestModel):
    pass

class OvfManager(RestManager):
    def new(self, *args, **kwargs):
        return Ovf(self, *args, **kwargs)
    
    def get_relative_uri(self):
        return self.parent.get_relative_uri() + '/ovfs'

class Ovf(RestModel):
    pass

class ApplicationManager(RestManager):
    def new(self, *args, **kwargs):
        return Application(self, *args, **kwargs)
    
    def get_relative_uri(self):
        return self.parent.get_relative_uri() + '/applications'

class Application(RestModel):
    def __init__(self, manager):
        super(Application, self).__init__(manager)
        
        self.ovfs = ApplicationOvfManager(self)
        self.selection_criteria = AppSelectionCriteriaManager(self)

class ApplicationOvfManager(RestManager):
    def new(self, *args, **kwargs):
        return ApplicationOvf(self, *args, **kwargs)
    
    def get_relative_uri(self):
        return self.parent.get_relative_uri() + '/ovfs'

class ApplicationOvf(RestModel):
    pass

class AppSelectionCriteriaManager(RestManager):
    def new(self, *args, **kwargs):
        return AppSelectionCriteria(self, *args, **kwargs)

    def get_relative_uri(self):
        m = re.match(r"^/users/[\w-]+/applications/([\w-]+)$", self.parent.get_relative_uri())
        if m:
            relative_uri = "/applications/" + m.group(1)
        else:
            relative_uri = self.parent.get_relative_uri()

        return relative_uri + '/selection_criteria'

class AppSelectionCriteria(RestModel):
    pass

class RoleManager(RestManager):
    def new(self, *args, **kwargs):
        return Role(self, *args, **kwargs)
    
    def get_relative_uri(self):
        return self.parent.get_relative_uri() + '/roles'

class Role(RestModel):
    pass

class GroupManager(RestManager):
    def new(self, *args, **kwargs):
        return Group(self, *args, **kwargs)
    
    def get_relative_uri(self):
        return self.parent.get_relative_uri() + '/groups'

class Group(RestModel):
    pass

class IdentityProviderManager(RestManager):
   
    def new(self, *args, **kwargs):
        return IdentityProvider(self, *args, **kwargs)
    
    def get_relative_uri(self):
        return self.parent.get_relative_uri() + '/idps'

    
class IdentityProvider(RestModel):
    
    def __init__(self, manager):        
        super(IdentityProvider, self).__init__(manager)

    def __unicode__(self):
        return self.providerName

class UserManager(RestManager):

    def new(self, *args, **kwargs):
        return User(self, *args, **kwargs)
    
    def get_relative_uri(self):       
        return self.parent.get_relative_uri() + '/users'

    def authenticate(self, username=None, password=None):
        headers = self.prepare_headers(None)
        headers.update({'content-type': 'application/json'})
        data=dict()
        data['username']=username
        data['password']=password
        json_data = json.dumps(data)
        uri = self.parent.authentication_uri
        # cert=(settings.FEDERATION_WEB_CERT,settings.FEDERATION_WEB_KEY), verify=settings.FEDERATION_WEB_CA_FILE
        resp = self.session.post(uri, json_data, headers=headers, )
        if resp.ok:
            data = json.loads(resp.content)
            return self.build_object(data)

    def check_idp_id_availability(self, identity=None):
        #TODO
        pass            

class User(RestModel):
    
    is_active = True
    
    def __init__(self, manager):
        self.profile_attributes = UserProfileAttributeManager(self)
        self.attributes = UserAttributeManager(self)
        self.selection_criteria = UserSelectionCriteriaManager(self)
        self.roles = UserRoleManager(self)
        self.groups = UserGroupManager(self)
        self.ids = UserIdentityManager(self)
        self.applications = UserApplicationManager(self)
        self.slas = UserSlaManager(self)
        self.slats = UserSlaTemplateManager(self)
        self.ovfs = UserOvfManager(self)
        self.certificate = None
        self.key = None
        self.current_loa = -1
        
        super(User, self).__init__(manager)
    
    def get_full_name(self):
        return ' '.join(filter(None, [self.firstName, self.lastName]))
    
    def get_cert(self):
        return self.certificate
    
    def get_key(self):
        return self.key
    
    def __unicode__(self):
        return self.username
    
    def is_authenticated(self):
        return True

#    def check_password(self, password):
#        return self.password == password
#        return bcrypt.hashpw(password, self.password) == self.password


class UserProfileAttributeManager(RestManager):
    def new(self, *args, **kwargs):
        return UserAttribute(self, *args, **kwargs)

    def get_relative_uri(self):
        return self.parent.get_relative_uri() + '/profileAttributes'


class UserAttributeManager(RestManager):
    def new(self, *args, **kwargs):
        return UserAttribute(self, *args, **kwargs)
    
    def get_relative_uri(self):
        return self.parent.get_relative_uri() + '/attributes'

class UserAttribute(RestModel):
    pass

class UserSelectionCriteriaManager(RestManager):
    def new(self, *args, **kwargs):
        return UserSelectionCriteria(self, *args, **kwargs)

    def get_relative_uri(self):
        return self.parent.get_relative_uri() + '/selection_criteria'

class UserSelectionCriteria(RestModel):
    pass

class UserRoleManager(RestManager):
    def new(self, *args, **kwargs):
        return UserRole(self, *args, **kwargs)
    
    def get_relative_uri(self):
        return self.parent.get_relative_uri() + '/roles'

class UserRole(RestModel):
    pass

class UserGroupManager(RestManager):
    def new(self, *args, **kwargs):
        return UserGroup(self, *args, **kwargs)
    
    def get_relative_uri(self):
        return self.parent.get_relative_uri() + '/groups'

class UserGroup(RestModel):
    pass

class UserIdentityManager(RestManager):
    def new(self, *args, **kwargs):
        return UserIdentity(self, *args, **kwargs)
    
    def get_relative_uri(self):
        return self.parent.get_relative_uri() + '/ids'

class UserIdentity(RestModel):
    
    def __init__(self, manager):        
        super(UserIdentity, self).__init__(manager)

    def __unicode__(self):
        return self.identity


class UserApplicationManagerError(Exception):
    def __init__(self, response):
        self.response = response


class UserApplicationManager(RestManager):
    def new(self, *args, **kwargs):
        return UserApplication(self, *args, **kwargs)
    
    def get_relative_uri(self):
        return self.parent.get_relative_uri() + '/applications'

    def submit_application(self, app_id=None):
        """ Calls /applications/<app_id>/submit
            Returns: nothing special
        """
        path = ('%s/%s/submit' % (self.build_uri()[0], app_id))
        logger.debug(" Calling federation api: %s" % path)
        resp = self.session.put(path)
        logger.debug("got response: %s" % resp.content)
        if resp.status_code not in (200, 201, 202, 204):
            logger.error('Federation API User application manager error: %s' % resp.content)
            raise UserApplicationManagerError(resp)
        return resp.json

    def start_application(self, app_id=None):
        """ Calls /applications/<app_id>/start
            Returns: nothing special
        """
        path = ('%s/%s/start' % (self.build_uri()[0], app_id))
        logger.debug(" Calling federation api: %s" % path)
        resp = self.session.put(path)
        logger.debug("got response: %s" % resp)
        if resp.status_code not in (200, 201, 202, 204):
            logger.error('Federation API User application manager error: %s' % resp.content)
            raise UserApplicationManagerError(resp)
        return resp

    def stop_application(self, app_id=None):
        """ Calls /applications/<app_id>/stop
            Returns: nothing special
        """
        path = ('%s/%s/stop' % (self.build_uri()[0], app_id))
        logger.debug(" Calling federation api: %s" % path)
        resp = self.session.put(path)
        logger.debug("got response: %s" % resp.content)
        if resp.status_code not in (200, 201, 202, 204):
            logger.error('Federation API User application manager error: %s' % resp.content)
            raise UserApplicationManagerError(resp)
        return resp

    def kill_application(self, app_id=None):
        """ Calls /applications/<app_id>/kill
            Returns: nothing special
        """
        path = ('%s/%s/kill' % (self.build_uri()[0], app_id))
        logger.debug(" Calling federation api: %s" % path)
        resp = self.session.put(path)
        logger.debug("got response: %s" % resp.content)
        if resp.status_code not in (200, 204):
            logger.error('Federation API User application manager error: %s' % resp.content)
            raise UserApplicationManagerError(resp)
        return resp

class UserApplication(RestModel):
    def __init__(self, manager):
        self.selection_criteria = AppSelectionCriteriaManager(self)
        super(UserApplication, self).__init__(manager)

class UserSlaManager(RestManager):
    def new(self, *args, **kwargs):
        return UserSla(self, *args, **kwargs)
    
    def get_relative_uri(self):
        return self.parent.get_relative_uri() + '/slas'

class UserSla(RestModel):
    pass

class UserSlaTemplateManagerError(Exception):
    def __init__(self, response):
        self.response = response

class UserSlaTemplateManager(RestManager):
    
    def new(self, *args, **kwargs):
        return UserSlaTemplate(self, *args, **kwargs)
    
    def get_relative_uri(self):
        return self.parent.get_relative_uri() + '/slats'
    
    def initiate_negotiation(self, slatid):
        """ Calls /negotiation/initiate
            Returns: negotiationId
        """
        #import pdb; pdb.set_trace()
        path = ('%s/%s/negotiation/initiate' % (self.build_uri()[0], slatid) )
        logger.debug(" Calling federation api: %s" % path)
        resp = self.session.post(path)
        logger.debug("got response: %s" % resp.content)
        if resp.status_code not in (200, 201, 202):
            logger.error('SLA Manager error: %s' % resp.content)
            raise UserSlaTemplateManagerError(resp)        
        return resp.json
    
    def negotiate_proposal(self, slatid, negotiationid, proposalid):
        """ Calls negotiation on the proposal
            Returns: slat proposal details
        """
        path = ('%s/%s/negotiation/%s/proposals/%s/negotiate' % (self.build_uri()[0], slatid, negotiationid, proposalid) )
        logger.debug(" Calling federation api %s" % path)        
        resp = self.session.post(path)
        logger.debug("got response : %s" % resp.content)
        if resp.status_code not in (200, 201, 202):
            logger.error('SLA Manager error: %s' % resp.content)
            raise UserSlaTemplateManagerError(resp)        
        return resp.json 
    
    def get_negotiation_proposal(self, slatid, negotiationid,proposalid):
        """ Calls GET on specific negotiation proposal
            Returns: slat proposal details
        """
        path = ('%s/%s/negotiation/%s/proposals/%s' % (self.build_uri()[0], slatid, negotiationid, proposalid) )
        logger.debug(" Calling federation api %s" % path)
        resp = self.session.get(path)
        logger.debug("got response: %s" % resp.content)
        if resp.status_code not in (200, 201, 202):
            logger.error('SLA Manager error: %s' % resp.content)
            raise UserSlaTemplateManagerError(resp)        
        return resp.json
    
    def compare_proposal(self, slatid, negotiationid,proposalid):
        """ Calls GET on specific negotiation proposal to compare it to the
            initial SLA template.
            
            Returns: JSON document containing comparison
        """
        path = ('%s/%s/negotiation/%s/proposals/%s/compare' % (self.build_uri()[0], slatid, negotiationid, proposalid) )
        logger.debug(" Calling federation api %s" % path)
        resp = self.session.get(path)
        logger.debug("got response" % resp)
        if resp.status_code not in (200, 201, 202):
            logger.error('SLA Manager error: %s' % resp.content)
            raise UserSlaTemplateManagerError(resp)        
        return resp.json
    
    def update_proposal(self, slatid, negotiationid, proposalid, data):
        """ Calls PUT on specific negotiation proposal to update it            
            
        """
        path = ('%s/%s/negotiation/%s/proposals/%s' % (self.build_uri()[0], slatid, negotiationid, proposalid) )        
        json_data = json.dumps(data)
        logger.debug(" Calling PUT %s with data %s " % (path, json_data))
        resp = self.session.put(path, json_data)        
        logger.debug("got response: %s" % resp.content)
        if resp.status_code not in (200, 201, 202, 204):
            logger.error('SLA Manager error: %s' % resp.content)
            raise UserSlaTemplateManagerError(resp)        
        return resp.json
    
    def create_agreement(self, slatid, negotiationid, proposalid):
        path = ('%s/%s/negotiation/%s/proposals/%s/createAgreement' % (self.build_uri()[0], slatid, negotiationid, proposalid) )        
        logger.debug(" Calling POST %s " % path)
        resp = self.session.post(path)        
        logger.debug("got response: %s" % resp.content)
        if resp.status_code not in (200, 201, 202):
            logger.error('SLA Manager error: %s' % resp.content)
            raise UserSlaTemplateManagerError(resp)        
        return resp.json
    
class UserSlaTemplate(RestModel):
    pass

class UserOvfManager(RestManager):
    def new(self, *args, **kwargs):
        return UserOvf(self, *args, **kwargs)
    
    def get_relative_uri(self):
        return self.parent.get_relative_uri() + '/ovfs'

class UserOvf(RestModel):
    pass

class ProviderManager(RestManager):
    def new(self, *args, **kwargs):
        return Provider(self, *args, **kwargs)
    
    def get_relative_uri(self):
        return self.parent.get_relative_uri() + '/providers'

class Provider(RestModel):
    def __init__(self, manager):
        self.ovfs = ProviderOvfManager(self)
        self.slats = ProviderSlatManager(self)
        self.vms = ProviderVMManager(self)
        self.servers = ProviderServerManager(self)
        self.clusters = ProviderClusterManager(self)
        self.dcs = ProviderDCManager(self)
        self.vos = ProviderVOManager(self)
        
        super(Provider, self).__init__(manager)

class ProviderOvfManager(RestManager):
    def new(self, *args, **kwargs):
        return ProviderOvf(self, *args, **kwargs)
    
    def get_relative_uri(self):
        return self.parent.get_relative_uri() + '/ovfs'

class ProviderOvf(RestModel):
    pass

class ProviderSlatManager(RestManager):
    def new(self, *args, **kwargs):
        return ProviderSlat(self, *args, **kwargs)
    
    def get_relative_uri(self):
        return self.parent.get_relative_uri() + '/slats'

class ProviderSlat(RestModel):
    @cached_property
    def content(self):
        if self.url:
            resp = requests.get(self.url)
            
            return resp.text

class ProviderVMManager(RestManager):
    def new(self, *args, **kwargs):
        return ProviderVM(self, *args, **kwargs)
    
    def get_relative_uri(self):
        return self.parent.get_relative_uri() + '/vms'

class ProviderVM(RestModel):
    pass

class ProviderServerManager(RestManager):
    def new(self, *args, **kwargs):
        return ProviderServer(self, *args, **kwargs)
    
    def get_relative_uri(self):
        return self.parent.get_relative_uri() + '/servers'

class ProviderServer(RestModel):
    pass

class ProviderClusterManager(RestManager):
    def new(self, *args, **kwargs):
        return ProviderCluster(self, *args, **kwargs)
    
    def get_relative_uri(self):
        return self.parent.get_relative_uri() + '/clusters'

class ProviderCluster(RestModel):
    def __init__(self, manager):
        self.servers = ProviderClusterClusterManager(self)
        self.vms = ProviderClusterVMManager(self)
        self.networks = ProviderClusterNetworkManager(self)
        self.storages = ProviderClusterStorageManager(self)
        
        super(ProviderCluster, self).__init__(manager)

class ProviderClusterClusterManager(RestManager):
    def new(self, *args, **kwargs):
        return ProviderClusterCluster(self, *args, **kwargs)
    
    def get_relative_uri(self):
        return self.parent.get_relative_uri() + '/servers'

class ProviderClusterCluster(RestModel):
    pass

class ProviderClusterVMManager(RestManager):
    def new(self, *args, **kwargs):
        return ProviderClusterVM(self, *args, **kwargs)
    
    def get_relative_uri(self):
        return self.parent.get_relative_uri() + '/vms'

class ProviderClusterVM(RestModel):
    pass

class ProviderClusterNetworkManager(RestManager):
    def new(self, *args, **kwargs):
        return ProviderClusterNetwork(self, *args, **kwargs)
    
    def get_relative_uri(self):
        return self.parent.get_relative_uri() + '/networks'

class ProviderClusterNetwork(RestModel):
    pass

class ProviderClusterStorageManager(RestManager):
    def new(self, *args, **kwargs):
        return ProviderClusterStorage(self, *args, **kwargs)
    
    def get_relative_uri(self):
        return self.parent.get_relative_uri() + '/storages'

class ProviderClusterStorage(RestModel):
    pass

class ProviderDCManager(RestManager):
    def new(self, *args, **kwargs):
        return ProviderDC(self, *args, **kwargs)
    
    def get_relative_uri(self):
        return self.parent.get_relative_uri() + '/dcs'

class ProviderDC(RestModel):
    def __init__(self, manager):
        self.networks = ProviderDCNetworkManager(self)
        self.storages = ProviderDCStorageManager(self)
        self.clusters = ProviderDCClusterManager(self)
        
        super(ProviderDC, self).__init__(manager)

class ProviderDCNetworkManager(RestManager):
    def new(self, *args, **kwargs):
        return ProviderDCNetwork(self, *args, **kwargs)
    
    def get_relative_uri(self):
        return self.parent.get_relative_uri() + '/networks'

class ProviderDCNetwork(RestModel):
    pass

class ProviderDCStorageManager(RestManager):
    def new(self, *args, **kwargs):
        return ProviderDCStorage(self, *args, **kwargs)
    
    def get_relative_uri(self):
        return self.parent.get_relative_uri() + '/storages'

class ProviderDCStorage(RestModel):
    pass

class ProviderDCClusterManager(RestManager):
    def new(self, *args, **kwargs):
        return ProviderDCCluster(self, *args, **kwargs)
    
    def get_relative_uri(self):
        return self.parent.get_relative_uri() + '/clusters'

class ProviderDCCluster(RestModel):
    pass

class ProviderVOManager(RestManager):
    def new(self, *args, **kwargs):
        return ProviderVO(self, *args, **kwargs)
    
    def get_relative_uri(self):
        return self.parent.get_relative_uri() + '/vos'

class ProviderVO(RestModel):
    def __init__(self, manager):
        self.cees = ProviderVOCeeManager(self)
        self.edcs = ProviderVOEdcManager(self)
        self.clusters = ProviderVOClusterManager(self)
        self.attributes = ProviderVOAttributeManager(self)
        
        super(ProviderVO, self).__init__(manager)

class ProviderVOCeeManager(RestManager):
    def new(self, *args, **kwargs):
        return ProviderVOCee(self, *args, **kwargs)
    
    def get_relative_uri(self):
        return self.parent.get_relative_uri() + '/cees'

class ProviderVOCee(RestModel):
    pass

class ProviderVOEdcManager(RestManager):
    def new(self, *args, **kwargs):
        return ProviderVOEdc(self, *args, **kwargs)
    
    def get_relative_uri(self):
        return self.parent.get_relative_uri() + '/edcs'

class ProviderVOEdc(RestModel):
    pass

class ProviderVOClusterManager(RestManager):
    def new(self, *args, **kwargs):
        return ProviderVOCluster(self, *args, **kwargs)
    
    def get_relative_uri(self):
        return self.parent.get_relative_uri() + '/clusters'

class ProviderVOCluster(RestModel):
    pass

class ProviderVOAttributeManager(RestManager):
    def new(self, *args, **kwargs):
        return ProviderVOAttribute(self, *args, **kwargs)
    
    def get_relative_uri(self):
        return self.parent.get_relative_uri() + '/attributes'

class ProviderVOAttribute(RestModel):
    pass


class AuthorizationOrganizationsManager(RestManager):
    def new(self, *args, **kwargs):
        return AuthorizationOrganizations(self, *args, **kwargs)

    def get_relative_uri(self):
        return self.parent.get_relative_uri() + '/organizations'


class AuthorizationOrganizations(RestModel):
    pass


class AuthorizationOrganizationsClientsManager(RestManager):

    def __init__(self, parent, organization_id):
        super(AuthorizationOrganizationsClientsManager, self).__init__(parent)
        self.organization_id = organization_id

    def new(self, *args, **kwargs):
        return AuthorizationOrganizationsClients(self, *args, **kwargs)

    def get_relative_uri(self):
        url = self.parent.get_relative_uri() + '/organizations/' + self.organization_id + '/clients'
        return url


class AuthorizationOrganizationsClients(RestModel):
    pass


# Trust
class AuthorizationOwnersOrganizationsManager(RestManager):
    def __init__(self, parent, owner_id):
        super(AuthorizationOwnersOrganizationsManager, self).__init__(parent)
        self.owner_id = owner_id

    def new(self, *args, **kwargs):
        return AuthorizationOwnersOrganizations(self, *args, **kwargs)

    def get_relative_uri(self):
        url = self.parent.get_relative_uri() + '/owners/' + self.owner_id + '/trust/organizations'
        return url


class AuthorizationOwnersOrganizations(RestModel):
    pass


class AuthorizationOwnersOrganizationsClientsManager(RestManager):
    def __init__(self, parent, owner_id, organization_id):
        super(AuthorizationOwnersOrganizationsClientsManager, self).__init__(parent)
        self.owner_id = owner_id
        self.organization_id = organization_id

    def new(self, *args, **kwargs):
        return AuthorizationOwnersOrganizationsClients(self, *args, **kwargs)

    def get_relative_uri(self):
        url = self.parent.get_relative_uri() + '/owners/' + self.owner_id + '/trust/organizations/' + self.organization_id + '/clients'
        return url


class AuthorizationOwnersOrganizationsClients(RestModel):
    pass


class AuthorizationTrustManager(object):
    def __init__(self, parent, owner_id):
        self.parent = parent
        self.owner_id = owner_id

    def full(self, headers=None):
        allOrganizations = self.parent.authorization_organizations.full()
        ownerTrustOrganizations = self.parent.authorization_owners_organizations(self.owner_id).full()

        for org in allOrganizations:
            for ownerTrustOrg in ownerTrustOrganizations:
                if str(ownerTrustOrg.organization['id']) == org.id:
                    if ownerTrustOrg.trust_level == 'FULLY':
                        org.trusted = 1
                    else:
                        org.trusted = 2
                    break
            else:
                org.trusted = 0
            org.clients = self.parent.authorization_organizations_clients(org.id).full()
            orgTrustClients = self.parent.authorization_owners_organizations_clients(self.owner_id, org.id).full()
            for client in org.clients:
                for orgTrustCli in orgTrustClients:
                    if str(orgTrustCli.client['id']) == client.id:
                        if orgTrustCli.trust_level == 'TRUSTED':
                            client.trusted = 1
                        else:
                            client.trusted = 2
                        break
                else:
                    client.trusted = 0
        return allOrganizations


federation = FederationManager(settings.FEDERATION_API_URL)
oauth = OAuthManager(settings.OAUTH_API_URL)

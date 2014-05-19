import logging

import zc.zk
import requests

from django.conf import settings
from django.utils import simplejson as json

from federweb.base.rest import JsonResource
from federweb.hookbox.models import Pusher
import traceback

logger = logging.getLogger(__name__)

class SlaParseError(Exception):
    def __init__(self, response):
        self.response = response

class ProviderException(Exception):
    def __init__(self, response):
        self.response = response
    
    def __str__(self):
        return 'ProviderException: response: %s, request: %s' % (
                vars(self.response), vars(self.response.request))

class ProviderVep(object):
    def __init__(self, provider):
        self.resource = JsonResource(provider.providerUri)
    
    def headers(self, username, **headers):
        new_headers = dict(headers)
        new_headers['X-Username'] = username
        return new_headers
    
    def users_register(self, user):
        data = {
            'role': 'admin', 
            'vid': '-1', 
            'groups': ['user', 'admin']
        }
        
        path = '/user/%s' % user.username
        
        headers = self.headers('fedadmin')
        
        resp = self.resource.put(path, data, headers=headers)
        
        if resp.status_code not in (200, 201, 202):
            raise ProviderException(resp)
    
        return True
    
    def ovfs_register(self, ovf_name, ovf, user):
        path = '/ovf/%s' % ovf_name
        headers = self.headers(user.username)
        resp = self.resource.put(path, data=ovf, headers=headers, xml=True)
        
        if resp.status_code not in (200, 201, 202, 409):
            raise ProviderException(resp)
    
        return resp.json
    
    def ovfs_list(self, user):
        headers = self.headers(user.username)
        
        resp = self.resource.get('/ovf/', headers=headers)
        
        if resp.status_code != 200:
            raise ProviderException(resp)
        
        data = resp.json
        
        names_links = zip(data['ovfs'], data['links'])
        
        return [dict(name=n, link=l) for n, l in names_links] 
    
    def apps_initialize(self, ovf_link, user):
        path = '%s/action/initialize' % ovf_link
        headers = self.headers(user.username)
        resp = self.resource.put(path, None, headers=headers)
        
        if resp.status_code not in (200, 201, 202):
            raise ProviderException(resp)
        
        return resp.json
    
    def apps_deploy(self, ovf_link, user):
        path = '%s/action/deploy' % ovf_link
        headers = self.headers(user.username)
        resp = self.resource.put(path, None, headers=headers)
        
        if resp.status_code not in (200, 201, 202):
            raise ProviderException(resp)
        
        return resp.json
    
    def apps_stop(self, ovf_link, user):
        path = '%s/action/stop' % ovf_link
        headers = self.headers(user.username)
        resp = self.resource.put(path, None, headers=headers)
        
        if resp.status_code not in (200, 201, 202):
            raise ProviderException(resp)
        
        return resp.json
    
    def vm_details(self, vm_id, user):
        path = '/vm/%s' % vm_id
        headers = self.headers(user.username)
        resp = self.resource.get(path, None, headers=headers)
        
        if resp.status_code not in (200, 201, 202):
            raise ProviderException(resp)
        
        return resp.json

class SlaExtractor(object):
    
    def __init__(self, base):
        self.resource = JsonResource(base)
    
    def extract_ovfs(self, data):
        path = '/getOVFs'
        
        resp = self.resource.post(path, data.strip(), content_type='text/xml')
        
        if resp.status_code not in (200, 201, 202):
            logger.error('SLA extractor error: %s' % resp.content)
            raise SlaParseError(resp)
        
        return resp.json
   
class Monitoring(object):
    def __init__(self, base):
        self.resource = JsonResource(base)
    
    def start(self, name, vep_id, ovf_id, one_id, ip, vnc_port):
        data = {
            'name': name,
            'vepId': vep_id,
            'ovfId': ovf_id,
            'oneId': one_id
        }
        
        path = '/startMonitoring'
        
        resp = self.resource.post(path, data)
        
        if resp.status_code not in (200, 201, 202):
            raise ProviderException(resp)
        
        return resp.json

class Deployment(object):
    def __init__(self, user, provider):
        self.user = user
        
        self.pv = ProviderVep(provider)
        self.slaex = SlaExtractor(settings.SLA_EXTRACTOR_BASE)
        self.mon = Monitoring(settings.MONITORING_BASE)
        self.zk = zc.zk.ZooKeeper(settings.ZOOKEEPER_BASE)
        self.pusher = Pusher(self.user)
    
    def log(self, msg):
        logger.debug('[Deployment - %s] %s' % (self.user.username, msg))
        
        self.pusher.push({'status': msg})
    
    def extract_ovf(self, sla):
        ovf_urls = self.slaex.extract_ovfs(sla.content)
        
        url = ovf_urls[0]
        
        ovf = requests.get(url).text
        
        return ovf
    
    def zk_add_app(self, app, app_data):
        path = '/'
        
        users = self.zk.get_children(path)
        
        path += self.user.username
        
        if self.user.username not in users:
            self.zk.create(path, '', zc.zk.OPEN_ACL_UNSAFE)
        
        apps = self.zk.get_children(path)
        
        path += '/' + app.name
        
        if app.name not in apps:
            self.zk.create(path, '', zc.zk.OPEN_ACL_UNSAFE)
        
        app_node_data = self.zk.get(path)[0]
        
        count = json.loads(app_node_data)['count'] if app_node_data else 0
        
        self.zk.set(path, json.dumps({'count': count + 1}))
        
        json_data = json.dumps(app_data)
        
        path += '/' + str(count)
        
        self.zk.create(path, json_data, zc.zk.OPEN_ACL_UNSAFE)
    
    def deploy(self, app, sla, restart=False):
        try:
            self.log('Initializing application %s' % app.name)
            
            if not restart:
                ovf = self.extract_ovf(sla)
                
            if not restart:
                self.pv.users_register(self.user)
            
            ovf_name = '%s_%s' % (self.user.username, app.name)
            
            if not restart:
                self.pv.ovfs_register(ovf_name, ovf, self.user)
            
            vep_ovfs = self.pv.ovfs_list(self.user)
            
            ovf_link = [x['link'] for x in vep_ovfs if x['name'] == ovf_name][0]
            ovf_id = ovf_link.split('/')[-1]
            
            if not restart:
                self.pv.apps_initialize(ovf_link, self.user)
            
            self.log('Deploying application %s' % app.name)
            
            deploy_response = self.pv.apps_deploy(ovf_link, self.user)
            
            vep_vm_ids = [x.split(':')[0] for x in deploy_response['vm_state_list']]
            
            app_data = []
            
            for vm_id in vep_vm_ids:
                details = self.pv.vm_details(vm_id, self.user)
                logger.debug("VM details: %s" % json.dumps(details))
                app_data.append({
                    'vep_id': vm_id,
                    'ovf_id': ovf_id,
                    'name': details['name'],
                    'one_id': details['iaas_id'],
                    'ip': details['ip'],
                    'vnc_port': details['vnc_port'],
                })
            
            self.log('Starting monitoring for application %s' % app.name)
            
            for vm_data in app_data:
                try:
                    self.log('Starting monitoring for vm %s' % vm_data)
                    self.mon.start(**vm_data)
                except ProviderException, e:
                    self.log('Monitoring could not be started: %s' % e.response.content)
            self.log('Starting monitoring for application %s' % app.name)
            zk_app_data = []
            self.log("Calling zookeeper")
            for vm_data in app_data:
                self.log('Calling zookeeper with vm data: %s' % vm_data)
                zk_app_data.append({
                    'name': vm_data['name'],
                    'vepId': vm_data['vep_id'],
                    'ovfId': vm_data['ovf_id'],
                    'oneId': vm_data['one_id']
                })
            
            self.zk_add_app(app, zk_app_data)
            self.log("Setting application state to deployed.")
            app.state = 'deployed'
            #import pdb; pdb.set_trace()
            temps=json.loads(app.attributes)            
            temps['app_data']=app_data            
            app.attributes=json.dumps(temps)
            logger.debug("attributes: %s" % app.attributes)
            app.save()
        except Exception as e:            
            logger.debug(traceback.format_exc())
            logger.error( e)
            return dict(success=False)
        return dict(success=True, apps_details=app_data)

    def start(self, app, sla):
        """ Restart the application
        """
        try:
            return self.deploy(app, sla, True)
        except Exception as e:            
            logger.debug(traceback.format_exc())
            logger.error( e )
            return dict(success=False)

    def stop(self, app):
        try:                        
            ovf_name = '%s_%s' % (self.user.username, app.name)
            
            vep_ovfs = self.pv.ovfs_list(self.user)
            
            ovf_link = [x['link'] for x in vep_ovfs if x['name'] == ovf_name][0]
            ovf_id = ovf_link.split('/')[-1]
            self.log('Stopping application %s' % app.name)
            
            stop_response = self.pv.apps_stop(ovf_link, self.user)
                        
            self.log('Stopping monitoring for application %s' % app.name)
            app_data = json.loads(app.attributes)
            for vm_data in app_data:
                try:
                    self.log('Stopping monitoring for vm %s' % vm_data)
                    logger.debug('Stopping monitoring for vm %s' % vm_data)
                    # self.mon.stop(**vm_data) TODO
                except ProviderException, e:
                    self.log('Monitoring could not be stopped: %s' % e.response.content)
            
            # STop ZOOKEEPER data!
            #self.zk_add_app(app, zk_app_data)
            self.log("Setting application state to stopped.")
            app.state = 'stopped'
            temps=json.loads(app.attributes)            
            temps['app_data']=None
            app.attributes=json.dumps(temps)
            logger.debug("attributes: %s" % app.attributes)
            app.save()
        except Exception as e:            
            logger.debug(traceback.format_exc())
            logger.error( e)
            return dict(success=False)
        return dict(success=True, apps_details=app_data)


class Offer(object):
    """
    Offers for SLA Federation Manager
    """
    name = None
    id = None
    content = None

    def __init__(self, name=None, id=None, content=None,):
        self.name = name
        self.id = id
        self.content = content


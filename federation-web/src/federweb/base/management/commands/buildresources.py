import os
import subprocess
import threading
from optparse import make_option

from django.conf import settings
from django.core.management.base import NoArgsCommand

BASE = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..', '..'))

class Worker(threading.Thread):
    def __init__(self, cmd):
        super(Worker, self).__init__()
        self.cmd = cmd
        self.daemon = True
    
    def run(self):
        print self.cmd
        subprocess.call(self.cmd, shell=True)

class Command(NoArgsCommand):
    help = 'Builds STATIC_RESOURCES (requires (less.js or less.rb) and coffescript)'
    
    option_list = NoArgsCommand.option_list + (
        make_option('--watch',
            action='store_true',
            dest='watch',
            default=False,
            help='Watch for changes'),
        )
    
    BUILDERS = {
        'less': {
            'build': 'lessc "%(src)s" "%(dest)s"',
        },
        'coffee': {
            'build': 'coffee -c "%(src)s"',
            'watch': 'coffee -c -w "%(src)s"'
        }
    }
    
    def handle_noargs(self, **options):
        cmd_type = 'watch' if options['watch'] else 'build'
        
        workers = []
        
        for type, resources in settings.STATIC_RESOURCES.items():
            base_cmd = self.BUILDERS[type].get(cmd_type, None)
            
            if base_cmd:
                for src, dest in resources:
                    args = {
                        'src': os.path.join(BASE, src),
                        'dest': os.path.join(BASE, dest)
                    }
                    
                    cmd = base_cmd % args
                    
                    w = Worker(cmd)
                    w.start()
                    
                    workers.append(w)
        
        if options['watch']:
            self.stdout.write('Watching %d resources...\n' % len(workers))
            
            for w in workers:
                w.join()
        else:
            for w in workers:
                w.join()
            
            self.stdout.write('Built %d resources.\n' % len(workers))

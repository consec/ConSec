# -*- coding: utf-8 -*-
# $Id: offline_bootstrap.py 73407 2011-08-21 20:35:04Z glenfant $
"""Offline buildout bootstraping for use on installation targets with no
Internet access.

See http://glenfant.wordpress.com/2011/07/31/bootstrap-and-install-a-buildout-based-project-without-internet-connection/

We assume that the directory that contains this file has the following
structure:

. +
  |-[offline_bootstrap.py]  (this file)
  |-[bootstrap.py]
  |   (standard bootstrap.py file from
  |    http://svn.zope.org/repos/main/zc.buildout/trunk/bootstrap/bootstrap.py)
  |-[bootstrap_resources/]
  | |-[distribute_setup.py]
  | |   (from http://python-distribute.org/distribute_setup.py)
  | |-[distribute-0.6.21.tar.gz]
  | |   (same version as in DEFAULT_VERSION of distribute_setup.py. See
  | |    http://pypi.python.org/pypi/distribute)
  | |-[zc.buildout-1.4.3.tar.gz]
  |     (or any other version as required by ``buildout.cfg``. See
  |      http://pypi.python.org/pypi/zc.buildout)
  |-[buildout.cfg]
  |-[downloads/]  (cache for downloaded files)
  |-[eggs/]  (local eggs of the app)
  |-[ext-cache/]  (cache for buildout .cfg files from http servers)

In order to have all required resources here for buildout to succeed in offline
mode, you should have previously ran the buildout with the following options.

----------
...
[buildout]
...
download-cache = downloads
eggs-directory = eggs
extends-cache = ext-cache
...
----------

You may pass to ``offline_bootstrap.py`` any argument expected from
``bootstrap.py`` except ``--setup-source`` and ``download-base``.

This will display some messages like...

"Download error: (8, 'nodename nor servname provided, or not known') -- Some
packages may not be found!"

But don't worry, this works.

After having ran ``python offline_bootstrap.py [your options]``, you just need
to ``bin/buildout -No `` (non newest and offline)
"""

import os
import sys
import subprocess

this_directory = os.path.abspath(os.path.dirname(__file__))
bootstrap_resources = os.path.join(this_directory, 'bootstrap_resources')
distribute_setup = os.path.join(bootstrap_resources, 'distribute_setup.py')

# Checking ``distribute_setup.py`` available
assert os.path.isfile(distribute_setup)

# Checking ``downloads/dist/`` available directory
sys.path.insert(0, bootstrap_resources)
distribute_mod = __import__('distribute_setup', {}, {}, ['DEFAULT_VERSION'])
distribute_tarball = os.path.join(
    bootstrap_resources,
    'distribute-%s.tar.gz' % distribute_mod.DEFAULT_VERSION
    )
assert os.path.isfile(distribute_tarball)

# Filtering options and args (removing our options)
blacklisted_opts = ('--setup-source', '--download-base')
opts_args = sys.argv[1:]
for opt_arg in opts_args:
    for blacklisted in blacklisted_opts:
        if opt_arg.startswith(blacklisted):
            print opt_arg, "option is forbidden in offline mode"
            sys.exit(1)

# Running the standard bootstrap.py with our options
stdout, stderr = subprocess.Popen(
    [sys.executable,
     'bootstrap.py',
     '--setup-source=%s' % distribute_setup,
     '--download-base=%s' % bootstrap_resources
     ] + opts_args,
    stdout=subprocess.PIPE, stderr=subprocess.PIPE).communicate()

print stdout.strip()

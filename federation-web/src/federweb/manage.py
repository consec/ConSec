#!/usr/bin/env python

from django.core.management import execute_manager

try:
    from federweb import settings
except ImportError:
    import sys
    sys.stderr.write("Error: Can't find the file federweb.settings file.\n")
    sys.exit(1)

if __name__ == "__main__":
    execute_manager(settings)

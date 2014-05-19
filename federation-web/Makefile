#
# Makefile: build and install federation-web
#

NAME     = federation-web
SHELL    = /bin/bash
PYTHON   = python
PYTHON2_6   = python2.6
PYTHON2_7   = python2.7
PACKAGE  = $(NAME).tar.gz

DESTDIR  =

build2_6:
	$(PYTHON2_6) bootstrap.py

build2_7:
	$(PYTHON2_7) bootstrap.py

build:
	python bootstrap.py
	./bin/buildout
#	rm -r eggs/zope.interface*
#	rm -r eggs/pyOpenSSL-0.13*

dist:
	rm -fr $(CURDIR)/target
	mkdir $(CURDIR)/target
	tar czf $(CURDIR)/target/$(PACKAGE) --transform 's|^\.|$(NAME)|' --exclude-vcs --exclude=target --exclude=build --exclude=dist --exclude=bin --exclude=.installed.cfg --exclude='**.egg-info' --exclude=pom.xml .

install: build
	$(PYTHON) offline_bootstrap.py
	./bin/buildout -No

clean:
	rm -rf $(CURDIR)/bin $(CURDIR)/build $(CURDIR)/dist $(CURDIR)/target $(CURDIR)/eggs/* $(CURDIR)/downloads/*

.PHONY: clean dist
.POSIX:

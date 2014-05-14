#
# Makefile: build and install federation-id-prov-support
#

NAME     = federation-id-prov-support
SHELL    = /bin/bash
PACKAGE  = $(NAME).tar.gz

DESTDIR  =

build:

dist:
	rm -fr $(CURDIR)/target
	mkdir $(CURDIR)/target
	tar czf $(CURDIR)/target/$(PACKAGE) --transform 's|^\.|$(NAME)|' --exclude-vcs --exclude=target --exclude=build --exclude=dist --exclude=bin --exclude=pom.xml .

install: build

clean:
	rm -rf $(CURDIR)/target

.PHONY: clean dist
.POSIX:


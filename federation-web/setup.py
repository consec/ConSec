from setuptools import setup, find_packages

setup(
    name='contrail-federation-web',
    version='1.0',
    description='Web frontend for Contrail Federation API',
    long_description='',
    author='Luka Zakrajsek',
    author_email='luka.zakrajsek@xlab.si',
    license='BSD',
    include_package_data=True,
    zip_safe = False,
    classifiers=[],
    package_dir = {'': 'src'},
    packages=find_packages('src'),
    install_requires=[
        'BeautifulSoup==3.2.1',
        'Django>=1.3.1',
        'django-classy-tags==0.3.4.1',
        'django-compressor==1.1.2',
        'django-concurrent-server==0.1.0',
        'django-dajax==0.8.4',
        'django-debug-toolbar==0.9.4',
        'django-extensions==0.7.1',
        'requests==0.14.1',
        'zc-zookeeper-static>=3.0.0',
        'zc.zk==0.7.0',
        'djangosaml2==0.10.0',
        'ndg-httpsclient==0.3.1',
        'MyProxyWebService==0.2.3',
        'MyProxyClient==1.3.0',
        'pyasn1==0.1.4',
    ]
)

#!/usr/bin/python

import os, sys, subprocess, glob

SCRIPT_DIR = os.path.abspath(os.path.dirname(sys.argv[0]))
ROOT_DIR = os.path.abspath(os.path.join(SCRIPT_DIR, '..'))
PKG_NAME = 'dist/tilitin-%(VERSION)s-src.zip'
VERSION_FILE = 'src/kirjanpito/ui/Kirjanpito.java'
FILES = (
	('src+', 'tilitin-%(VERSION)s/src'),
	('lib/sqlite-jdbc.jar', 'tilitin-%(VERSION)s/lib/sqlite-jdbc.jar'),
	('lib/postgresql-jdbc.jar', 'tilitin-%(VERSION)s/lib/postgresql-jdbc.jar'),
	('lib/itext.jar', 'tilitin-%(VERSION)s/lib/itext.jar'),
	('build.xml', 'tilitin-%(VERSION)s/build.xml'),
	('tilikarttamallit+', 'tilitin-%(VERSION)s/tilikarttamallit'),
	('COPYING', 'tilitin-%(VERSION)s/COPYING')
)

def build_source_package():
	tmpdir = os.path.join(SCRIPT_DIR, 'temp')
	os.mkdir(tmpdir)
	version = get_version()
	
	for source, dest in FILES:
		recursive = False

		if source.endswith('+'):
			source = source[:-1]
			recursive = True

		dest = dest % {'VERSION': version}
		sourcepath = os.path.join(ROOT_DIR, source)
		destpath = os.path.join(tmpdir, dest)
		dirpath = os.path.dirname(destpath)
		if not os.path.exists(dirpath): os.makedirs(dirpath)

		if recursive:
			subprocess.call(('cp', '-r', sourcepath, destpath))
		else:
			subprocess.call(('cp', sourcepath, destpath))

	pkgpath = os.path.join(ROOT_DIR, PKG_NAME % {'VERSION': version})
	pkgdir = os.path.dirname(pkgpath)
	if not os.path.exists(pkgdir): os.makedirs(pkgdir)
	subprocess.call(('zip', '-r', pkgpath, '.'), cwd=tmpdir)
	subprocess.call(('rm', '-r', tmpdir))

def get_version():
	input = open(os.path.join(ROOT_DIR, VERSION_FILE), 'r')
	version = None
	
	for line in input:
		if 'VERSION' in line:
			pos1 = line.find('"')
			pos2 = line.find('"', pos1 + 1)
			version = line[pos1 + 1:pos2]
			break

	input.close()
	return version

if __name__ == '__main__':
	build_source_package()

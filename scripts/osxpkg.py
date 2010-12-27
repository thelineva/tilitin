#!/usr/bin/python

import os, sys, subprocess, glob

SCRIPT_DIR = os.path.abspath(os.path.dirname(sys.argv[0]))
ROOT_DIR = os.path.abspath(os.path.join(SCRIPT_DIR, '..'))
PKG_NAME = 'dist/tilitin-%(VERSION)s-macosx.zip'
VERSION_FILE = 'src/kirjanpito/ui/Kirjanpito.java'
PLIST_FILE = 'scripts/macosx/Info.plist'
FILES = (
	('dist/tilitin.jar', 'Tilitin.app/Contents/Resources/Java/tilitin.jar'),
	('tilikarttamallit+', 'Tilitin.app/Contents/Resources/Java/tilikarttamallit'),
	('lib/itext.jar', 'Tilitin.app/Contents/Resources/Java/itext.jar'),
	('lib/sqlite-jdbc.jar', 'Tilitin.app/Contents/Resources/Java/sqlite-jdbc.jar'),
	('lib/postgresql-jdbc.jar', 'Tilitin.app/Contents/Resources/Java/postgresql-jdbc.jar'),
	('scripts/macosx/JavaApplicationStub', 'Tilitin.app/Contents/MacOS/JavaApplicationStub'),
	('scripts/macosx/tilitin.icns', 'Tilitin.app/Contents/Resources/tilitin.icns')
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

	plistdata = open(os.path.join(ROOT_DIR, PLIST_FILE), 'r').read()
	output = open(os.path.join(tmpdir, 'Tilitin.app/Contents/Info.plist'), 'w')
	output.write(plistdata % {'VERSION': version})
	output.close()

	pkgpath = os.path.join(ROOT_DIR, PKG_NAME % {'VERSION': version})
	pkgdir = os.path.dirname(pkgpath)
	if not os.path.exists(pkgdir): os.makedirs(pkgdir)
	subprocess.call(('zip', '-r', pkgpath, '.'), cwd=tmpdir)
	subprocess.call(('rm', '-rf', tmpdir))

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

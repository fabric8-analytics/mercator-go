# Copyright 2016 Red Hat, Inc.
#
# Mercator is free software: you can redistribute it and/or modify
# it under the terms of the GNU Lesser General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
# Mercator is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU Lesser General Public License for more details.
# You should have received a copy of the GNU Lesser General Public License
# along with Mercator. If not, see <http://www.gnu.org/licenses/>.
#

.PHONY : clean stage

FILES = $(shell python3 -c 'import xml.etree.ElementTree as ET;\
		tree=ET.parse("MercatorDotNet/MercatorDotNet.csproj");\
		ns="{http://schemas.microsoft.com/developer/msbuild/2003}";\
		print("\n".join([x.text.replace("\\", "/")[3:] for x in\
		  tree.findall("./{ns}ItemGroup/{ns}Reference/{ns}HintPath".format(ns=ns))]))')

all:
	nuget restore
	$(MAKE) stage
	xbuild
	cp -f MercatorDotNet/bin/Debug/MercatorDotNet.exe ../dotnet
	chmod -x ../dotnet
	$(MAKE) clean

stage:
	cp $(FILES) MercatorDotNet/Resources/

clean:
	rm -rf MercatorDotNet/{bin,obj}/
	rm -rf MercatorDotNet/Resources/*.dll

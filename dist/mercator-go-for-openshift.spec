# golang does not generate debug info
%define debug_package %{nil}

Name:		mercator
Version:	1
Release:	15%{?dist}
Summary:	Mercator CLI tool

URL:		github.com/fabric8-analytics/%{name}-go

Source0: %{name}-go.tar.gz
# prebuild binary
Source1: %{name}
# data normalizer source code
Source2: data_normalizer.py
Source3: java
Source4: handlers_template.yml

BuildRequires:	make
#Requires:	java-devel maven

%description
Mercator is which obtains manifests from various ecosystems such as NPM, .NET, Java and Python

%prep
%setup -q -n mercator

%build

%install
# Place pre-build binary to the context
cp %{SOURCE1} .
# Cannot build with Java 1.7 under Epel-6, place prebuild JAR file there
mkdir -p $RPM_BUILD_ROOT%{_prefix}/share/%{name}
cp %{SOURCE3} $RPM_BUILD_ROOT%{_prefix}/share/%{name}
make handlers JAVA=NO DOTNET=NO RUST=NO HANDLERS_TEMPLATE=%{SOURCE4}
make install DESTDIR=$RPM_BUILD_ROOT%{_prefix}

# copy data normalizer to paths
cp %{SOURCE2} $RPM_BUILD_ROOT%{_datadir}/%{name}

# create script for normalized output
# it's hack, need to rework from scratch
cat << EOF > $RPM_BUILD_ROOT%{_prefix}/bin/run_mercator
#!/bin/bash
# enable scl
# this is due to python handler
#source /opt/rh/python27/enable
source /opt/rh/python33/enable
# this is for ruby handler
source /opt/rh/ruby200/enable
# run scan
mercator \$1 | python3 %{_datadir}/%{name}/data_normalizer.py --restricted
EOF

chmod +x $RPM_BUILD_ROOT%{_prefix}/bin/run_mercator

%clean

%files
%{_datadir}/%{name}/*
%{_bindir}/%{name}
%{_bindir}/run_%{name}

%doc

%changelog
* Mon Apr 03 2017 Slavek Kabrda <bkabrda@redhat.com> - 1-15
- Fix handlers template for Python dist

* Mon Apr 03 2017 Slavek Kabrda <bkabrda@redhat.com> - 1-14
- Recognize PKG-INFO files even when they're not in egg-info directory

* Mon Apr 03 2017 Slavek Kabrda <bkabrda@redhat.com> - 1-13
- Bump release to fix release bump that was built but not committed.

* Mon Apr 03 2017 Slavek Kabrda <bkabrda@redhat.com> - 1-12
- Build with new Python improvements

* Wed Feb 01 2017 Michal Srb <msrb@redhat.com> - 1-11
- Bump release

* Thu Jan 12 2017 Fridolin Pokorny <fridolin@redhat.com> - 1-10
- Enable Java handler

* Fri Dec 09 2016 Fridolin Pokorny <fridolin@redhat.com> - 1-9
- Removed dont_build_mercator.patch
- Use python3.3 instead of python2.7
- Use data_normalizer.py instead of mercator.py for data filtering

* Mon Oct 17 2016 Pavel Kajaba <pkajaba@redhat.com> - 1-8
- Removed python3 collection

* Mon Oct 17 2016 Pavel Kajaba <pkajaba@redhat.com> - 1-7
- Added collections

* Fri Oct 14 2016 Pavel Kajaba <pkajaba@redhat.com> - 1-6
- Removed python3 binary because it resolves as depenency

* Fri Oct 14 2016 Pavel Kajaba <pkajaba@redhat.com> - 1-5
- Updated script to enabling collections

* Fri Oct 14 2016 Pavel Kajaba <pkajaba@redhat.com> - 1-4
- Dropped usage of six longest_list in mercator.py

* Thu Oct 13 2016 Pavel Kajaba <pkajaba@redhat.com> - 1-3
- Updated mercator.py for better java output

* Tue Oct 11 2016 Pavel Kajaba <pkajaba@redhat.com> - 1-2
- Updated few details

* Mon Oct 03 2016 Pavel Kajaba <pkajaba@redhat.com> - 1-1
- Initial creation of specfile for Mercator

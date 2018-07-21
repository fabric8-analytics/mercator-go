# golang does not generate debug info
%define debug_package %{nil}

Name:       mercator
Version:    1
Release:    26%{?dist}
Summary:    Mercator CLI tool
License:    ASL 2.0
URL:        https://github.com/fabric8-analytics/%{name}-go

Source0:    %{name}.tar.gz

BuildRequires:  make openssl-devel golang git

# python handler
%if 0%{?rhel}
BuildRequires:  python34-devel
Requires:       python34
%else
BuildRequires:  python3-devel
Requires:       python3
%endif

# ruby handler
Requires:       ruby

# npm handler
Requires:       nodejs

# java handler
BuildRequires:  java-devel maven
Requires:       java maven

# dotnet handler
BuildRequires:  mono-devel nuget
Requires:       mono-core

# golang handler
BuildRequires:  glide

# gradle handler
BuildRequires:  npm

%description
Obtains manifests from various ecosystems such as NPM, .NET, Java and Python

%prep
%setup -q -n %{name}

%build
yes | certmgr -ssl https://go.microsoft.com
yes | certmgr -ssl https://nuget.org
export GOPATH=/tmp
make build JAVA=YES DOTNET=YES GOLANG=YES GRADLE=YES

%install
make install DESTDIR=%{buildroot}%{_prefix}

%clean

%files
%{_datadir}/%{name}/*
%{_bindir}/%{name}

%{!?_licensedir:%global license %%doc}
%{license} LICENSE
%doc README.md


%changelog
* Sat Jul 21 2018 Michal Srb <michal@redhat.com> - 1-26
- [java] Make Maven local repo location configurable

* Fri Apr 27 2018 Saleem Ansari <tuxdna@gmail.com> - 1-25
- Remove duplicate XML start tags from expanded pom
- https://github.com/fabric8-analytics/mercator-go/pull/44

* Thu Dec 14 2017 Pavel Kajaba <pavel@redhat.com> - 1-24
- Initial Gradle support

* Wed Nov 29 2017 Jiri Popelka <jpopelka@redhat.com> - 1-23
- Go Glide support

* Mon Nov 20 2017 Pavel Kajaba <pavel@redhat.com> - 1-22
- Ignore inherited description in pom.xml

* Mon Oct 23 2017 Michal Srb <michal@redhat.com> - 1-21
- Ignore unknown trigger lines in PKGINFO

* Tue Sep 12 2017 Jiri Popelka <jpopelka@redhat.com> - 1-20
- Haskell handler
- effective pom files handling

* Thu Jun 29 2017 Jiri Popelka <jpopelka@redhat.com> - 1-19
- [dotnet handler] Use NuGet v4 library for nuspec reading

* Tue Jun 27 2017 Jiri Popelka <jpopelka@redhat.com> - 1-18
- remove Openshift V2 & EPEL-6 hacks

* Fri Jun 23 2017 Jiri Popelka <jpopelka@redhat.com> - 1-17
- dotnet handler

* Fri Apr 28 2017 Jiri Popelka <jpopelka@redhat.com> - 1-16
- Add License and change URL

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

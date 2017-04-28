# golang does not generate debug info
%define debug_package %{nil}

Name:		mercator
Version:	1.0.0
Release:	2%{?dist}
Summary:	Mercator CLI tool

URL:		github.com/fabric8-analytics/%{name}-go
Source0: %{name}-go.tar.gz
# data normalizer source code
#Source1: %{name}.py

BuildRequires:	ruby java-devel mono-devel python3 git golang make npm openssl-devel cmake mono-winfx nuget maven
Requires:	ruby java-devel mono-devel python3 npm maven

%description
Mercator is which obtains manifests from various ecosystems such as NPM,. NET, Java and Python

%prep
%setup -q

%build
mkdir /tmp/gopath
curl -sSf https://static.rust-lang.org/rustup.sh | sh -s -- -y --disable-sudo --prefix=$HOME
GOPATH=/tmp/gopath make build

%install
GOPATH=/tmp/gopath make install DESTDIR=$RPM_BUILD_ROOT%{_prefix}

# copy data normalizer to paths
#cp %{SOURCE1} $RPM_BUILD_ROOT%{_prefix}/bin

# create script for normalized output
#cat << EOF > $RPM_BUILD_ROOT%{_prefix}/bin/run_mercator
#!/bin/bash
#mercator \$1 | mercator.py
#EOF
#chmod +x $RPM_BUILD_ROOT%{_prefix}/bin/run_mercator

%clean

%files
%{_datadir}/%{name}/*
%{_bindir}/%{name}
#%{_bindir}/%{name}.py
#%{_bindir}/run_%{name}

%doc

%changelog
* Mon Oct 03 2016 Pavel Kajaba <pkajaba@redhat.com> - 1-1
- Initial creation of specfile for Mercator

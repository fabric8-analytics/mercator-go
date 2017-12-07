#!/bin/bash -ex

load_jenkins_vars() {
    if [ -e "jenkins-env" ]; then
        cat jenkins-env \
          | grep -E "(COPR_LOGIN_MERCATOR|COPR_TOKEN_MERCATOR|JENKINS_URL|GIT_BRANCH|GIT_COMMIT|BUILD_NUMBER|ghprbSourceBranch|ghprbActualCommit|BUILD_URL|ghprbPullId)=" \
          | sed 's/^/export /g' \
          > ~/.jenkins-env
        source ~/.jenkins-env
    fi
}

prep() {
    yum -y install epel-release
    yum -y install docker rpmdevtools copr-cli git
    systemctl start docker
}

run_tests() {
    make check
}

create_copr_config() {
    mkdir -p ~/.config/
    cat > ~/.config/copr <<EOF
[copr-cli]
login = ${COPR_LOGIN_MERCATOR}
username = jpopelka
token = ${COPR_TOKEN_MERCATOR}
copr_url = https://copr.fedorainfracloud.org
EOF
}

build_rpm() {
    rpmdev-setuptree
    ./make_rpm.sh --source

    create_copr_config
    copr-cli build jpopelka/mercator ~/rpmbuild/SRPMS/mercator-*.src.rpm
}


load_jenkins_vars
prep

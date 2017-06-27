#!/usr/bin/env groovy
@Library('github.com/msrb/cicd-pipeline-helpers') _

node('docker') {

    stage('Checkout') {
        checkout scm
    }

    stage('Unit Tests') {
        dockerCleanup()

        timeout(30) {
            sh "make check"
        }
    }

    if (env.BRANCH_NAME == 'master') {
        stage('make') {
            sh "./make_rpm.sh --source"
        }
        stage('push to copr') {
            sh "copr-cli build jpopelka/mercator ~/rpmbuild/SRPMS/mercator-*.src.rpm"
        }
    }
}

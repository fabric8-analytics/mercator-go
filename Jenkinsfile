#!/usr/bin/env groovy
@Library('github.com/msrb/cicd-pipeline-helpers') _

node('docker') {

    stage('Checkout') {
        checkout scm
    }

    stage('Unit Tests') {
        dockerCleanup()

        dir('tests') {
            timeout(30) {
                sh './runtest.sh'
            }
        }
    }
}

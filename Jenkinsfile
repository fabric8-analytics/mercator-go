#!/usr/bin/env groovy
@Library('github.com/msrb/cicd-pipeline-helpers') _

node('docker') {

    stage('Checkout') {
        checkout scm
    }

    stage('Unit Tests') {
        dockerCleanup()

        timeout(30) {
            make check 
        }
    }
}

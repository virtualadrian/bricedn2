pipeline {
    agent any

    // S: PIPELINE ENVIRONMENT
    environment {
        REMOTE_URL = "https://github.com/virtualadrian/bricedn2.git"
        MERGE_SOURCE = "develop"
        MERGE_TARGET = "develop"
        CD_APP_HOST = "172.30.10.2"
        CD_APP_SSH_PORT = "22"
        CD_USER_ID = "dev_jenkins"
        CD_USER_PW = "cd_password_super_secret"
        CD_TMP_DEPLOY_DIR = "/tmp/app/dist"
        CD_DEPLOY_DIR = "/app/dist"
        CD_DEPLOY_EXEC = "app.dll"
    }
    // E: PIPELINE ENVIRONMENT

    // S: PIPELINE OPTIONS
    options {
        timeout(time: 90, unit: 'MINUTES')
        disableConcurrentBuilds()
        timestamps()        
        buildDiscarder(logRotator(numToKeepStr: '20'))
    }
    // E: PIPELINE OPTIONS
    
    // S: PIPELINE TOOLS
    tools {
        maven 'Maven 3'
        jdk 'Java 8'
    }
    // E: PIPELINE TOOLS

    // S: PIPELINE STAGES
    stages {
        
        // S: MERGE
        stage ('MERGE') {
            steps {
                // deleteDir()
                checkout changelog: true, poll: true, scm: 
                    [$class: 'GitSCM', branches: [[name: MERGE_SOURCE]], 
                    doGenerateSubmoduleConfigurations: false, 
                    extensions: [[$class: 'PreBuildMerge', 
                    options: [fastForwardMode: 'NO_FF', mergeRemote: 'origin', mergeStrategy: 'org.jenkinsci.plugins.gitclient.MergeCommand.Strategy.DEFAULT', 
                    mergeTarget: MERGE_TARGET]]], 
                    submoduleCfg: [], 
                    userRemoteConfigs: [[url: REMOTE_URL]]]
            }
        }
        // E: MERGE
        
        // S: BUILD
        stage ('BUILD') {
            steps {
                script {
                    def dncCmd = 'dotnet build'
                    def dncret = sh(returnStatus: true, script: "${dncCmd}")
                    echo "DotNet returned ${dncret}"
                    if (dncret != 0) {
				        error('Failure')
                    }
                }
            }
        }
        // E: BUILD
        /*
        // S: DEPLOY:CLEAN
        stage ('DEPLOY:CLEAN') {
            steps {
                script {
                    // remove and remake the temporary deploy directory
                    sh 'ssh -p ${CD_APP_SSH_PORT} ${CD_USER_ID}@${CD_APP_HOST} "rm -rf ${CD_TMP_DEPLOY_DIR}"'
                    sh 'ssh -p ${CD_APP_SSH_PORT} ${CD_USER_ID}@${CD_APP_HOST} "mkdir -p ${CD_TMP_DEPLOY_DIR}"'
                }
            }

        }
        // E: DEPLOY:CLEAN

        // S: DEPLOY:COPY
        stage ('DEPLOY:COPY') {
            steps {
                script {
                    // secure copy the jar file to temporary deploy directory
                    sh 'scp -P ${CD_APP_SSH_PORT} -r ${WORKSPACE}/target/${CD_DEPLOY_EXEC} ${CD_USER_ID}@${CD_APP_HOST}:${CD_TMP_DEPLOY_DIR}'
                }
            }

        }
        // E: DEPLOY:COPY

        // S: DEPLOY:DIST
        stage ('DEPLOY:DIST') {
            steps {
                script {
                    // remove old deploy file and move new from temporary to dist
                    sh 'ssh -p ${CD_APP_SSH_PORT} ${CD_USER_ID}@${CD_APP_HOST} "rm -f ${CD_DEPLOY_DIR}/${CD_DEPLOY_EXEC}"'            
                    sh 'ssh -p ${CD_APP_SSH_PORT} ${CD_USER_ID}@${CD_APP_HOST} "mv -f ${CD_TMP_DEPLOY_DIR}/${CD_DEPLOY_EXEC} ${CD_DEPLOY_DIR}/${CD_DEPLOY_EXEC}"'
                }
            }

        }
        // E: DEPLOY:DIST

        // S: DEPLOY:RESTART
        stage ('DEPLOY:RESTART') {
            steps {
                script {
                    sh 'ssh -t -p ${CD_APP_SSH_PORT} ${CD_USER_ID}@${CD_APP_HOST} "echo ${CD_USER_PW} | sudo -S service recognation restart"'
                }
            }            
        }
        // E: DEPLOY:RESTART
*/
    }
    // E: PIPELINE STAGES
    post {
        unstable {
            script {
                currentBuild.result = 'UNSTABLE'
            }
        }
        failure {
            script {
                currentBuild.result = 'FAILED'
            }
        }
        success {
            script {
                currentBuild.result = 'SUCCESS'
            }
        }
    }
}
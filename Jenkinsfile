pipeline {
  environment {
    devRegistry = 'ghcr.io/datakaveri/fs-dev'
    deplRegistry = 'ghcr.io/datakaveri/fs-depl'
    testRegistry = 'ghcr.io/datakaveri/fs-test:latest'
    registryUri = 'https://ghcr.io'
    registryCredential = 'datakaveri-ghcr'
    GIT_HASH = GIT_COMMIT.take(7)
  }
  agent { 
    node {
      label 'slave1' 
    }
  }
  stages {

    stage('Conditional Execution') {
      when {
        anyOf {
          triggeredBy 'UserIdCause'
          expression {
            def comment = env.ghprbCommentBody
            return comment && comment != "null" && !comment.trim().isEmpty()
          }
          changeset "docker/**"
          changeset "docs/**"
          changeset "pom.xml"
          changeset "src/main/**"
        }
      }
      stages {
        stage('Trivy Code Scan (Dependencies)') {
          steps {
            script {
              sh '''
                trivy fs --scanners vuln,secret,misconfig --output trivy-fs-report.txt .
              '''
            }
          }
        }

        stage('Building images') {
          steps{
            script {
              echo 'Pulled - ' + env.GIT_BRANCH
              devImage = docker.build( devRegistry, "-f ./docker/dev.dockerfile .")
              deplImage = docker.build( deplRegistry, "-f ./docker/depl.dockerfile .")
            }
          }
        }

        stage('Trivy Docker Image Scan and Report') {
          steps {
            script {
              sh "trivy image --output trivy-dev-image-report.txt ${devImage.imageName()}"
              sh "trivy image --output trivy-depl-image-report.txt ${deplImage.imageName()}"
            }
          }
          post {
            always {
              archiveArtifacts artifacts: 'trivy-*.txt', allowEmptyArchive: true
              publishHTML(target: [
                allowMissing: true,
                keepAll: true,
                reportDir: '.',
                reportFiles: 'trivy-fs-report.txt, trivy-dev-image-report.txt, trivy-depl-image-report.txt',
                reportName: 'Trivy Reports'
              ])
            }
          }
        }


        stage('Unit Tests and Code Coverage Test'){
          steps{
            script{
              sh 'mkdir -p configs'
              sh 'cp /home/ubuntu/configs/fs-config-test.json ./configs/config-test.json'
              sh 'mvn clean test checkstyle:checkstyle pmd:pmd'
            }
            xunit (
              thresholds: [ skipped(failureThreshold: '0'), failed(failureThreshold: '0') ],
              tools: [ JUnit(pattern: 'target/surefire-reports/*.xml') ]
            )
            jacoco classPattern: 'target/classes', execPattern: 'target/jacoco.exec', sourcePattern: 'src/main/java', exclusionPattern: 'iudx/file/server/apiserver/FileServerVerticle.class,iudx/file/server/apiserver/FileServerVerticle**,iudx/file/server/authenticator/AuthenticationService.class,iudx/file/server/database/DatabaseService.class,**/JwtDataConverter.class,**/*VertxEBProxy.class,**/Constants.class,**/*VertxProxyHandler.class,**/*Verticle.class,iudx/file/server/deploy/**,iudx/file/server/databroker/DataBrokerServiceImpl.class'
          }
          post{
            always {
              recordIssues(
                enabledForFailure: true,
                skipBlames: true,
                qualityGates: [[threshold:0, type: 'TOTAL', unstable: false]],
                tool: checkStyle(pattern: 'target/checkstyle-result.xml')
              )
              recordIssues(
                enabledForFailure: true,
                skipBlames: true,
                qualityGates: [[threshold:4, type: 'TOTAL', unstable: false]],
                tool: pmdParser(pattern: 'target/pmd.xml')
              )
            }
            failure{
              xunit (
                thresholds: [ skipped(failureThreshold: '0'), failed(failureThreshold: '0') ],
                tools: [ JUnit(pattern: 'target/surefire-reports/*.xml') ]
              )
              error "Test failure. Stopping pipeline execution!"
            }
            cleanup{
              script{
                sh 'sudo rm -rf target/'
              }
            }
          }
        }

        stage('Start File-Server for Integration Tests'){
          steps{
            script{
                sh 'docker compose -f docker-compose.test.yml up -d integTest'
                sh 'sleep 45'
            }
          }
          post{
            failure{
              script{
                sh 'docker compose -f docker-compose.test.yml down --remove-orphans'
              }
            }
          }
        }

        stage('Integration Tests and OWASP ZAP pen test'){
          steps{
            node('built-in') {
              script{
                startZap ([host: '0.0.0.0', port: 8090, zapHome: '/var/lib/jenkins/tools/com.cloudbees.jenkins.plugins.customtools.CustomTool/OWASP_ZAP/ZAP_2.11.0'])
                sh 'curl http://0.0.0.0:8090/JSON/pscan/action/disableScanners/?ids=10096'
              }
            }
            script{
                sh 'scp /home/ubuntu/configs/fs-config-test.json ./example-configs/config-test.json'
                sh 'mvn test-compile failsafe:integration-test -DskipUnitTests=true -DintTestProxyHost=jenkins-master-priv -DintTestProxyPort=8090 -DintTestHost=jenkins-slave1 -DintTestPort=8443'
            }
            node('built-in') {
              script{
                runZapAttack()
                }
            }
          }
          post{
            always{
              xunit (
                thresholds: [ skipped(failureThreshold: '0'), failed(failureThreshold: '0') ],
                tools: [ JUnit(pattern: 'target/failsafe-reports/*.xml') ]
                )
              node('built-in') {
                script{
                  archiveZap failHighAlerts: 1, failMediumAlerts: 1, failLowAlerts: 1
                }
              }
            }
            failure{
              error "Test failure. Stopping pipeline execution!"
            }
            cleanup{
              script{
                sh 'docker compose -f docker-compose.test.yml down --remove-orphans'
              } 
            }
          }
        }

        stage('Continuous Deployment') {
          when {
              expression {
                return env.GIT_BRANCH == 'origin/master';
              }
            }
          stages {
            stage('Push Images') {
              steps {
                script {
                  docker.withRegistry( registryUri, registryCredential ) {
                    devImage.push("6.0.0-alpha-${env.GIT_HASH}")
                    deplImage.push("6.0.0-alpha-${env.GIT_HASH}")
                  }
                }
              }
            }
            stage('Docker Swarm deployment') {
              steps {
                script {
                  sh "ssh azureuser@docker-swarm 'docker service update file-server_file-server --image ghcr.io/datakaveri/fs-depl:6.0.0-alpha-${env.GIT_HASH}'"
                  sh 'sleep 20'
                }
              }
              post{
                failure{
                  error "Failed to deploy image in Docker Swarm"
                }
              }          
            }
            stage('Integration test on swarm deployment') {
              steps {
                  script{
                    sh 'mvn test-compile failsafe:integration-test -DskipUnitTests=true -DintTestDepl=true'
                  }
              }
              post{
                always{
                xunit (
                  thresholds: [ skipped(failureThreshold: '0'), failed(failureThreshold: '0') ],
                  tools: [ JUnit(pattern: 'target/failsafe-reports/*.xml') ]
                  )
                }
                failure{
                  error "Test failure. Stopping pipeline execution!"
                }
              }
            }
          }
        }
      }
    }
  }
  post{
    failure{
      script{
        if (env.GIT_BRANCH == 'origin/master')
        emailext recipientProviders: [buildUser(), developers()], to: '$RS_RECIPIENTS, $DEFAULT_RECIPIENTS', subject: '$PROJECT_NAME - Build # $BUILD_NUMBER - $BUILD_STATUS!', body: '''$PROJECT_NAME - Build # $BUILD_NUMBER - $BUILD_STATUS:
Check console output at $BUILD_URL to view the results.'''
      }
    }
  }
}

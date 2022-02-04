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

    stage('Building images') {
      steps{
        script {
          echo 'Pulled - ' + env.GIT_BRANCH
          devImage = docker.build( devRegistry, "-f ./docker/dev.dockerfile .")
          deplImage = docker.build( deplRegistry, "-f ./docker/depl.dockerfile .")
          testImage = docker.build( testRegistry, "-f ./docker/test.dockerfile .")
        }
      }
    }

    stage('Unit Tests and CodeCoverage Test'){
      steps{
        script{
          sh 'docker-compose -f docker-compose.test.yml up test'
        }
        xunit (
          thresholds: [ skipped(failureThreshold: '1'), failed(failureThreshold: '0') ],
          tools: [ JUnit(pattern: 'target/surefire-reports/*.xml') ]
        )
        jacoco classPattern: 'target/classes', execPattern: 'target/jacoco.exec', sourcePattern: 'src/main/java'
      }
      post{
        failure{
          script{
            sh 'docker-compose -f docker-compose.test.yml down --remove-orphans'
          }
          error "Test failure. Stopping pipeline execution!"
        }
      }
    }

    stage('Start File server for Integration Tests'){
      steps{
        script{
            sh 'scp src/test/resources/iudx-file-server-api.Release-v3.5.postman_collection.json jenkins@jenkins-master:/var/lib/jenkins/iudx/fs/Newman/'
            sh 'docker-compose -f docker-compose.test.yml up -d integTest'
            sh 'sleep 45'
        }
      }
    }

    stage('Integration tests & OWASP ZAP pen test'){
      steps{
        node('master') {
          script{
            startZap ([host: 'localhost', port: 8090, zapHome: '/var/lib/jenkins/tools/com.cloudbees.jenkins.plugins.customtools.CustomTool/OWASP_ZAP/ZAP_2.11.0'])
              sh 'curl http://127.0.0.1:8090/JSON/pscan/action/disableScanners/?ids=10096'
              sh 'HTTP_PROXY=\'127.0.0.1:8090\' newman run /var/lib/jenkins/iudx/fs/Newman/iudx-file-server-api.Release-v3.5.postman_collection.json -e /home/ubuntu/configs/fs-postman-env.json --insecure -r htmlextra --reporter-htmlextra-export /var/lib/jenkins/iudx/fs/Newman/report/report.html --reporter-htmlextra-skipSensitiveData'
            runZapAttack()
          }
        }
      }
      post{
        always{
          node('master') {
            script{
              archiveZap failHighAlerts: 1, failMediumAlerts: 1, failLowAlerts: 1
            }  
            publishHTML([allowMissing: false, alwaysLinkToLastBuild: true, keepAll: true, reportDir: '/var/lib/jenkins/iudx/fs/Newman/report/', reportFiles: 'report.html', reportTitles: '', reportName: 'Integration Test Report'])
          }
        }
        failure{
          error "Test failure. Stopping pipeline execution!"
        }
        cleanup{
          script{
            sh 'docker-compose -f docker-compose.test.yml down --remove-orphans'
          } 
        }
      }
    }

    stage('Push Image') {
      when{
        expression {
          return env.GIT_BRANCH == 'origin/master';
        }
      }
      steps{
        script {
          docker.withRegistry( registryUri, registryCredential ) {
            devImage.push("3.0-${env.GIT_HASH}")
            deplImage.push("3.0-${env.GIT_HASH}")
          }
        }
      }
    }
  }
}
/*

Deplends on
- releaseTools.groovy
- BlogApp.groovy


TODO:
- Replace 'main' branch reference to current branch reference


*/

def Org="cb-thunder"
def JenkinsController="team-a"
def ProjectName = "Team A"
def PipelineNames = ["Frontend","Backend"]
def ReleaseName = "The Release"
def ReleaseProject = "Blog"

project ProjectName, {
	PipelineNames.each { pipe ->
		pipeline pipe, {
			description='Pipeline can be kicked off from Jenkins or run manually; if manually, all parameter must be supplied'
			formalParameter "BuildJobPath", description: 'For example, "Front End Jobs/job/microblog-fe-full/job/PR-4"', require: false
			formalParameter "BuildNumber", require: false
			formalParameter "PR", description: 'For example, "PR-4"', require: false
			stage 'Dev Readiness', {
				colorCode = '#289ce1'
				task 'Get Build Properties', {
					condition = ''
					actualParameter = [
						'commandToRun': '''\
							ectool setProperty /myPipelineRuntime/PR "$[/javascript
							var s=myPipelineRuntime.ciBuildDetails;s.substring(1, s.indexOf(\'=\')).split(\'#\')[0].replace(/.* » /g,\'\').trim()]"
							ectool setProperty /myPipelineRuntime/BuildJobPath "$[/javascript var s=myPipelineRuntime.ciBuildDetails;s.substring(1, s.indexOf(\'=\')).split(\'#\')[0].replace(/ » /g,\'/job/\').trim()]"
							ectool setProperty /myPipelineRuntime/BuildNumber $[/javascript var s=myPipelineRuntime.ciBuildDetails;var pni=s.substring(1, s.indexOf(\'=\'));pni.split(\'#\')[1]]
						'''.stripIndent(),
					]
					subpluginKey = 'EC-Core'
					subprocedure = 'RunCommand'
					taskType = 'COMMAND'
				}
				task 'Update Pipeline name', {
					actualParameter = [
						'commandToRun': 'ectool setPipelineRunName "$[/myPipeline/pipelineName] - $[/myPipelineRuntime/PR] - $[/timestamp]" --flowRuntimeId $[/myPipelineRuntime/flowRuntimeId]',
					]
					subpluginKey = 'EC-Core'
					subprocedure = 'RunCommand'
					taskType = 'COMMAND'
				}

				task 'Get CI details', {
					actualParameter = [
						'build_number': '$[/myPipelineRuntime/BuildNumber]',
						'config_name': JenkinsController,
						'job_name': '$[/myPipelineRuntime/BuildJobPath]',
						'result_outpp': '/myJob/buildDetails',
					]
					subpluginKey = 'EC-Jenkins'
					subprocedure = 'GetBuildDetails'
					taskType = 'PLUGIN'
				}

				task 'Get Commit Info', {
					actualParameter = [
						'config': 'github-sandbox',
						'repoName': (String) "${Org}/microblog-${pipe.toLowerCase()}",
						'resultPropertySheet': '/myJob/result',
						'sha': '$[/javascript var outputHash = ""; var actions = JSON.parse(myPipelineRuntime.stages["Dev Readiness"].tasks["Get CI details"].job.buildDetails).actions; for (i = 0; i < actions.length; i++) { if (actions[i].revision) { outputHash = actions[i].revision.pullHash; } } outputHash; ]',
					]
					subpluginKey = 'EC-Github'
					subprocedure = 'Get Commit'
					taskType = 'PLUGIN'
				}

				task 'Get Jira tickets', {
					actualParameter = [
						'config': 'CBDemo JIRA',
						'createLink': '1',
						'fieldsToSave': '',
						'filter': '',
						'jql': 'ISSUE = "$[/javascript JSON.parse(myStageRuntime.tasks["Get Commit Info"].job.result.json).message.match(/([A-Z]+-[0-9]+)/gm)]"',
						'resultFormat': 'propertySheet',
						'resultProperty': '/myJob/getIssuesResult',
					]
					subpluginKey = 'EC-JIRA'
					subprocedure = 'GetIssues'
					taskType = 'PLUGIN'
				}

				task 'Get Sonar results', {
					actualParameter = [
						'config': 'sonar',
						'resultFormat': 'propertysheet',
						'resultSonarProperty': '/myJob/getLastSonarMetrics',
						'sonarMetricsComplexity': 'all',
						'sonarMetricsDocumentation': 'all',
						'sonarMetricsDuplications': 'all',
						'sonarMetricsIssues': 'all',
						'sonarMetricsMaintainability': 'all',
						'sonarMetricsMetrics': 'all',
						'sonarMetricsQualityGates': 'all',
						'sonarMetricsReliability': 'all',
						'sonarMetricsSecurity': 'all',
						'sonarMetricsTests': 'all',
						'sonarProjectKey': '$[/javascript 	var sonarUrl = "";	var actions = JSON.parse(myPipelineRuntime.stages["Dev Readiness"].tasks["Get CI details"].job.buildDetails).actions;	for (i = 0; i < actions.length; i++) {	if (actions[i].sonarqubeDashboardUrl) {	sonarUrl = actions[i].sonarqubeDashboardUrl	}	};	sonarUrl.substring(sonarUrl.indexOf("="), sonarUrl.length).split("=")[1]	]',
						'sonarProjectName': '$[/javascript 	var sonarUrl = "";	var actions = JSON.parse(myPipelineRuntime.stages["Dev Readiness"].tasks["Get CI details"].job.buildDetails).actions;	for (i = 0; i < actions.length; i++) {	if (actions[i].sonarqubeDashboardUrl) {	sonarUrl = actions[i].sonarqubeDashboardUrl	}	};	sonarUrl.substring(sonarUrl.indexOf("="), sonarUrl.length).split("=")[1]	]',
						'sonarProjectVersion': '',
						'sonarTaskId': '',
						'sonarTimeout': '',
					]
					subpluginKey = 'EC-SonarQube'
					subprocedure = 'Get Last SonarQube Metrics'
					taskType = 'PLUGIN'
				}
				gate 'POST', {
					task 'Test suite results', {
						enabled = '0'
						gateCondition = '''\
							$[/javascript
								api.getCIBuildDetails({flowRuntimeId: myPipelineRuntime.flowRuntimeId}).ciBuildDetailInfo[0].testResults.failPercentage=="0"
							]
						'''.stripIndent()
						gateType = 'POST'
						taskType = 'CONDITIONAL'
					}

					task 'Sonar scan results', {
						enabled = '0'
						gateCondition = '''\
							$[/javascript
								myPipelineRuntime.stages["Dev Readiness"].tasks["Get Sonar results"].job.getLastSonarMetrics.coverage>=0
							]
						'''.stripIndent()
						gateType = 'POST'
						taskType = 'CONDITIONAL'
					}
				}
			}

			stage 'Dev Deploy', {
				colorCode = '#ff7f0e'
				task 'Deploy app', {
					actualParameter = [
						'ScmBranch': 'main',
					]
					environmentProjectName = ReleaseProject
					subapplication = 'Blogging Application'
					subprocess = 'Deploy Application'
					subproject = ReleaseProject
					taskProcessType = 'APPLICATION'
					taskType = 'PROCESS'
					environmentName = 'Dev'
				}
				task 'Run automated tests', {
					actualParameter = [
						'commandToRun': 'echo Running automated tests',
					]
					subpluginKey = 'EC-Core'
					subprocedure = 'RunCommand'
					taskType = 'COMMAND'
				}
				task 'Manual check', {
					description = 'Verify test results'
					instruction = 'Comment field should be filled in, otherwise the pipeline will fail.'
					notificationEnabled = '1'
					notificationTemplate = 'ec_default_pipeline_manual_task_notification_template'
					taskType = 'MANUAL'
					approver = [
						'Everyone',
					]
					requiredApprovalsCount = '1'
				}
				gate 'POST', {
					task 'Enforce comment in Manual Check', {
						description = 'Return error if no commment provided in the "Manual check" task.'
						gateCondition = '$[/myStageRuntime/tasks[Manual Check]/evidence]'
						gateType = 'POST'
						taskType = 'CONDITIONAL'
					}
				}
			}

			stage 'Promote to Release', {
				colorCode = '#2ca02c'

				task 'Merge to development branch', {
					instruction = "Please perform the merge for this pull request \'https://github.com/${Org}/microblog-${pipe.toLowerCase()}/pull/\$[/javascript myPipelineRuntime.PR.match(/PR-([0-9]+)/)[1]]\'."
					notificationEnabled = '1'
					notificationTemplate = 'ec_default_pipeline_manual_task_notification_template'
					taskType = 'MANUAL'
					approver = [
					'Everyone',
					]					
				}
				
				task 'Attach to release', {
					actualParameter = [
						'Release': ReleaseName,
						'ReleaseProject': ReleaseProject
					]
					subprocedure = 'Attach Pipeline to Release'
					subproject = 'Release Tools'
					taskType = 'PROCEDURE'
				}
			
				
				
			}
		} // pipeline
	} // each pipeline
} // project
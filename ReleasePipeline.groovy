/*

Deplends on
- ServiceNowPollingEcGroovy.groovy
- BlogApp.groovy

*/

def ProjectName = "Blog"
def ReleaseName = "The Release"

project ProjectName, {
	release ReleaseName, {
		plannedEndDate = '2021-07-31'
		plannedStartDate = '2021-07-15'

		pipeline ReleaseName, {
			stage 'Release Readiness', {
				colorCode = '#289ce1'
				task 'Gather Attached Pipeline Evidence', {
					actualParameter = [
						'commandToRun': '''\
							import com.electriccloud.client.groovy.ElectricFlow
							ElectricFlow ef = new ElectricFlow()
							def AttachedPipes = ef.getAttachedPipelineRuns(projectName: \'$[/myRelease/projectName]\', releaseName: \'$[/myRelease]\').attachedPipelineRunDetail
							AttachedPipes.each { Pipe ->
							ef.setProperty propertyName: """/myStageRuntime/ec_summary/${Pipe.flowRuntimeName}""".stripIndent(),
							value: """<html><a target="_blank" href="http://$[/server/hostName]/flow/#audit-reports/${Pipe.flowRuntimeId}">Audit Report</a></html>""".stripIndent()
							}
						'''.stripIndent(),
						'shellToUse': 'ec-groovy',
					]
					subpluginKey = 'EC-Core'
					subprocedure = 'RunCommand'
					taskType = 'COMMAND'
				}				
			}

			stage 'QA', {
				colorCode = '#ff7f0e'


				task 'Deploy to QA', {
					actualParameter = [
						'ScmBranch': 'main',
					]
					environmentProjectName = projectName
					subapplication = 'Blogging Application'
					subprocess = 'Deploy Application'
					subproject = projectName
					taskProcessType = 'APPLICATION'
					taskType = 'PROCESS'
					environmentName = 'QA'
				}

				task 'Email QA', {
					actualParameter = [
						'commandToRun': 'echo "Email notification" ',
					]
					subpluginKey = 'EC-Core'
					subprocedure = 'RunCommand'
					taskType = 'COMMAND'
				}
				task 'Run Functional Test', {
					actualParameter = [
						'commandToRun': 'echo "Call Selenium API for enabling Feature Flag experiment for QA env" ',
					]
					subpluginKey = 'EC-Core'
					subprocedure = 'RunCommand'
					taskType = 'COMMAND'
				}
				gate 'POST', {
					task 'QA sign-off', {
						gateType = 'POST'
						notificationEnabled = '1'
						notificationTemplate = 'ec_default_gate_task_notification_template'
						taskType = 'APPROVAL'
						approver = [
							'Everyone',
						]
					}
				}
			}
			stage 'Pre-Prod', {
				colorCode = '#2ca02c'
			
				task 'Deploy to Pre-prod', {
										actualParameter = [
						'ScmBranch': 'main',
					]
					environmentProjectName = projectName
					subapplication = 'Blogging Application'
					subprocess = 'Deploy Application'
					subproject = projectName
					taskProcessType = 'APPLICATION'
					taskType = 'PROCESS'
					environmentName = 'Pre-Prod'
				}

				task 'Enter CR number', {
					instruction = 'Enter an existing CR ticket number or leave blank to have the pipeline create one'
					notificationEnabled = '1'
					notificationTemplate = 'ec_default_pipeline_manual_task_notification_template'
					taskType = 'MANUAL'
					approver = [
						'Everyone',
					]
					formalParameter 'CR', {
						required = false
						type = 'entry'
					}
				}
				task 'Create CR', {
					actualParameter = [
						'config_name': 'ven02428',
						'content': '''{
	"description":"Change request created from CDRO Pipeline - $[/myPipelineRuntime/name]. k8-microblog Application deployed to the PM environment and testing is done. Please approve the Change Request to begin the Production deployment. More details can be found by following the URL in the 'Activity' field below.",
	"comments":"[code] <a href='https://$[/server/hostName]/flow/?s=Flow+Tools&ss=Flow#pipeline-run/$[/myPipeline/id]/$[/myPipelineRuntime/id]'> Link to the CDRO Pipeline </a> [/code]"
	}''',
						'correlation_display': '',
						'correlation_id': '',
						'property_sheet': '/myJobStep',
						'short_description': 'CDRO Pipeline - $[/myPipeline]',
					]
					condition = '$[/javascript myStageRuntime.tasks["Enter CR number"]["CR"]==null]'
					subpluginKey = 'EC-ServiceNow'
					subprocedure = 'CreateChangeRequest'
					taskType = 'PLUGIN'
				}
			}

			stage 'Prod', {
				colorCode = '#d62728'
				gate 'PRE', {
					task 'Wait on CR approval', {
						actualParameter = [
							'Configuration': 'ven02428',
							'PollingInterval': '60',
							'RecordID': '''\
								$[/javascript 
									var ManualCR = myPipelineRuntime.stages["Pre-Prod"].tasks["Enter CR number"]["CR"];
									if (ManualCR == null) {
										myPipelineRuntime.stages["Pre-Prod"].tasks["Create CR"].job.jobSteps["executeServiceNowCall"]["ChangeRequestNumber"]
									} else {
										ManualCR
									}
								]
							'''.stripIndent(),
							'TargetState': 'approved',
						]
						gateType = 'PRE'
						subprocedure = 'Poll for target state'
						subproject = 'ServiceNow'
						taskType = 'PROCEDURE'
					}
				}

				task 'Deploy to Prod', {
					actualParameter = [
						'ScmBranch': 'main',
					]
					environmentProjectName = projectName
					subapplication = 'Blogging Application'
					subprocess = 'Deploy Application'
					subproject = projectName
					taskProcessType = 'APPLICATION'
					taskType = 'PROCESS'
					environmentName = 'Prod'
				}

				task 'Archive Release', {
					actualParameter = [
						'commandToRun': 'echo',
					]
					subpluginKey = 'EC-Core'
					subprocedure = 'RunCommand'
					taskType = 'COMMAND'
				}
			}
		}
	}
}
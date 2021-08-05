project "Default",{
	release "Dummy",{
		pipeline 'SN', {
			stage 'Pre-Prod', {
				task 'Enter CR number', {
					instruction = 'Enter an existing CR ticket number or leave blank to have the pipeline create one'
					notificationEnabled = '1'
					notificationTemplate = 'ec_default_pipeline_manual_task_notification_template'
					taskType = 'MANUAL'
					approver = [
						'Everyone',
					]
					formalParameter 'CR', {
						required = '0'
						type = 'entry'
					}
				}
				task 'Create CR', {
					actualParameter = [
						'config_name': 'ven02428',
						'content': '''\
							{
							"description":"Change request created from CDRO Pipeline -  $[/myPipelineRuntime/name]. k8-microblog Application deployed to the PM environment and testing is done. Please approve the Change Request to begin the Production deployment. More details can be found by following the URL in the 'Activity' field below.",
							"comments":"[code] <a href='https://$[/server/hostName]/flow/?s=Flow+Tools&ss=Flow#pipeline-run/$[/myPipeline/id]/$[/myPipelineRuntime/id]'> Link to the CDRO Pipeline </a> [/code]"
							}'''.stripIndent(),
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
				gate 'PRE', {
					task 'Wait on CR approval', {
						actualParameter = [
							'Configuration': 'ven02428',
							'PollingInterval': '60',
							'RecordID': '''\
								$[/javascript
									var ManualCR = myPipelineRuntime.stages["Pre-Prod"].tasks["Enter CR number"]["CR"]
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

			}

		}
	}
}

# Create EC-Git configuration

# ************* not working yet *************


GITHUB_CONFIG=github-sandbox
GITHUB_API=https://api.github.com
GITHUB_USER=gmaxey
# Read only public repositories
GITHUB_PW=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

printf "${GITHUB_PW}\n" | ectool runProcedure /plugins/EC-GitHub/project \
  --procedureName CreateConfiguration \
  --actualParameter \
      config="${GITHUB_CONFIG}" \
      checkConnection=false \
	  endpoint="${GITHUB_API}" \
	  bearer_credential=bearer_credential \
  --credential "bearer_credential"="${GITHUB_USER}"

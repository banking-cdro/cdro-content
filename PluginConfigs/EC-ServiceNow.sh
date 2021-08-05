# Create EC-ServiceNow configuration

SN_CONFIG=ven02428
SN_USER=admin
SN_PW='OEK*@3fp@YG8b0R3cVbx'
SN_URL=https://ven02428.service-now.com/

printf "${SN_PW}\n" | ectool runProcedure /plugins/EC-ServiceNow/project \
  --procedureName CreateConfiguration \
  --actualParameter \
      config="${SN_CONFIG}" \
      credential="${SN_CONFIG}" \
      test_connection=false \
      host="${SN_URL}" \
  --credential "${SN_CONFIG}"="${SN_USER}"

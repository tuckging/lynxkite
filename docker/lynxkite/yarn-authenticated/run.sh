#!/bin/bash
# This is a wrapper for lynxkite/bin/lynxkite. Should be run inside the Docker container!

rm -f root/kite.pid root/sphynx.pid

# Configure kiterc settings.
cat > /kiterc_overrides <<EOF
export KITE_INSTANCE="$KITE_INSTANCE"
export KITE_HTTP_PORT=2200
export KITE_DATA_DIR=$KITE_DATA_DIR
export KITE_EPHEMERAL_DATA_DIR=$KITE_EPHEMERAL_DATA_DIR
export EXECUTOR_MEMORY=$EXECUTOR_MEMORY
export NUM_EXECUTORS=$NUM_EXECUTORS
export NUM_CORES_PER_EXECUTOR=$NUM_CORES_PER_EXECUTOR
export KITE_MASTER_MEMORY_MB=$KITE_MASTER_MEMORY_MB
export SPARK_MASTER=yarn
export SPARK_HOME=/spark
export YARN_CONF_DIR=/hadoop_conf
export KITE_META_DIR=/metadata
export KITE_EXTRA_JARS=/extra_jars/*
export KITE_PREFIX_DEFINITIONS=/prefix_definitions.txt
export KITE_INTERNAL_WATCHDOG_TIMEOUT_SECONDS=1200

# For authentication.
export KITE_USERS_FILE=/auth/kite_users
export KITE_APPLICATION_SECRET='<random>'
export KITE_HTTPS_PORT=2201
export KITE_HTTPS_KEYSTORE=/auth/tomcat.keystore
export KITE_HTTPS_KEYSTORE_PWD=\$(cat /auth/keystore-password)
export KITE_HTTP_ADDRESS=0.0.0.0
EOF

export KITE_SITE_CONFIG=/lynxkite/conf/kiterc_template
export KITE_SITE_CONFIG_OVERRIDES=/kiterc_overrides

exec lynxkite/bin/lynxkite -Dlogger.resource=logger-docker.xml "$@"

#!/bin/bash

# Brings up a small EMR cluster and runs tests on it.
# Usage:
#
# emr_based_test.sh frontend  #  Run e2e frontend tests.
#
# emr_based_test.sh bigdata test_pattern param:value
#   Run big data tests specified by the pattern kitescripts/big_data_tests/test_pattern.groovy
#   with a given parameter.
#
#   Example:
#   emr_based_test.sh backend 'big_data_tests/*' testDataSet:fake_westeros_100k
#   This will run all groovy files in kitescripts/perf/*.groovy and all these
#   groovy files will receive the testDataSet:fake_westeros_100k parameter.

source "$(dirname $0)/biggraph_common.sh"
set -x

MODE=${1}
shift

CLUSTER_NAME=${CLUSTER_NAME:-${USER}-test-cluster}
EMR_TEST_SPEC="/tmp/${CLUSTER_NAME}.emr_test_spec"
NUM_INSTANCES=${NUM_INSTANCES:-3}
NUM_EXECUTORS=${NUM_EXECUTORS:-3}

if [[ ! $NUM_INSTANCES =~ ^[1-9][0-9]*$ ]]; then
  echo "Variable NUM_INSTANCES=$NUM_INSTANCES. This is not a number."
  exit 1
fi

if [[ ! $NUM_EXECUTORS =~ ^[1-9][0-9]*$ ]]; then
  echo "Variable NUM_EXECUTORS=$NUM_EXECUTORS. This is not a number."
  exit 1
fi

if [[ $NUM_INSTANCES -gt 20 ]]; then
    read -p "NUM_INSTANCES is rather great: $NUM_INSTANCES. Are you sure you want to run this many instances? [Y/n] " answer
    case ${answer:0:1} in
        y|Y|'' )
            ;;
        * )
            exit 1
            ;;
    esac
fi


$(dirname $0)/../stage.sh

cp $(dirname $0)/../stage/tools/emr_spec_template ${EMR_TEST_SPEC}
cat >>${EMR_TEST_SPEC} <<EOF

# Override values for the test setup:
CLUSTER_NAME=${CLUSTER_NAME}
NUM_INSTANCES=${NUM_INSTANCES}
NUM_EXECUTORS=${NUM_EXECUTORS}
S3_DATAREPO=""
KITE_INSTANCE_BASE_NAME=testemr
EOF

EMR_SH=$(dirname $0)/../stage/tools/emr.sh

CLUSTERID=$(${EMR_SH} clusterid ${EMR_TEST_SPEC})
if [ -n "$CLUSTERID" ]; then
  echo "Reusing already running cluster instance ${CLUSTERID}."
  ${EMR_SH} reset-yes ${EMR_TEST_SPEC}
else
  echo "Starting new cluster instance."
  ${EMR_SH} start ${EMR_TEST_SPEC}
fi


case $MODE in
  backend )
    ${EMR_SH} deploy-kite ${EMR_TEST_SPEC}

    COMMAND_ARGS=( "$@" )
    REMOTE_OUTPUT_DIR=/home/hadoop/test_results
    TESTS_TO_RUN=$(./stage/kitescripts/big_data_test_scheduler.py \
        --remote_lynxkite_path=/home/hadoop/biggraphstage/bin/biggraph \
        --remote_output_dir=$REMOTE_OUTPUT_DIR \
        ${COMMAND_ARGS[@]} )
    START_MYSQL=$(echo "$TESTS_TO_RUN" | grep 'mysql')
    if [ -n "$START_MYSQL" ]; then
      if [ -n "$CLUSTERID" ]; then
        MYSQL=$(ENGINE=MySQL ${EMR_SH} rds-get ${EMR_TEST_SPEC})
      else
        MYSQL=$(ENGINE=MySQL ${EMR_SH} rds-up ${EMR_TEST_SPEC})
      fi
    fi

    ${EMR_SH} ssh ${EMR_TEST_SPEC} <<ENDSSH
      # Update value of DEV_EXTRA_SPARK_OPTIONS in .kiterc
      sed -i '/^export DEV_EXTRA_SPARK_OPTIONS/d' .kiterc
      echo "export DEV_EXTRA_SPARK_OPTIONS=\"${DEV_EXTRA_SPARK_OPTIONS:-}\"" >>.kiterc
      # Prepare output dir.
      rm -Rf ${REMOTE_OUTPUT_DIR}
      mkdir -p ${REMOTE_OUTPUT_DIR}
      # Export database addresses.
      export MYSQL=$MYSQL
      # Run tests one by one.
      ${TESTS_TO_RUN[@]}
ENDSSH

    # Process output files.
    if [ -n "${EMR_RESULTS_DIR}" ]; then
      mkdir -p ${EMR_RESULTS_DIR}
      # Download log files for each test:
      ${EMR_SH} download-dir ${EMR_TEST_SPEC} \
          ${REMOTE_OUTPUT_DIR}/ ${EMR_RESULTS_DIR}/

      # Trim output files: removes lines from before
      # STARTING SCRIPT and after FINISHED SCRIPT.
      # For example, this will discard extremely
      # long stack traces generated by Kryo which are printed
      # after the "FINISHED" line.
      for OUTPUT_FILE in ${EMR_RESULTS_DIR}/*.out.txt; do
        mv $OUTPUT_FILE /tmp/emr_based_test.$$.txt
        cat /tmp/emr_based_test.$$.txt | \
          awk '/STARTING SCRIPT/{flag=1}/FINISHED SCRIPT/{print;flag=0}flag' >${OUTPUT_FILE}
      done

      # Create nice summary file:
      cat ${EMR_RESULTS_DIR}/*.out.txt | \
          grep FINISHED | \
          sed 's/^FINISHED SCRIPT \(.*\), took \(.*\) seconds$/\1:\2/' \
            >${EMR_RESULTS_DIR}/summary.txt
    fi

    # Upload logs.
    ${EMR_SH} uploadLogs ${EMR_TEST_SPEC}
    ;;
  frontend )
    ${EMR_SH} kite ${EMR_TEST_SPEC}
    ${EMR_SH} connect ${EMR_TEST_SPEC} &
    CONNECTION_PID=$!
    sleep 15

    pushd web
    PORT=4044 gulp test || echo "Frontend tests failed."
    popd

    pkill -TERM -P ${CONNECTION_PID}
    ;;
  * )
    echo "Invalid mode was specified: ${MODE}"
    echo "Usage: $0 backend|frontend"
    exit 1
esac

answer='yes'
# Ask only if STDIN is a terminal.
if [ -t 0 ]; then
  read -p "Test completed. Terminate cluster? [y/N] " answer
fi
case ${answer:0:1} in
  y|Y )
    ${EMR_SH} terminate-yes ${EMR_TEST_SPEC}
    if [ -n "$START_MYSQL" ]; then
      ENGINE=MySQL ${EMR_SH} rds-down ${EMR_TEST_SPEC}
    fi
    ;;
  * )
    echo "Use 'stage/tools/emr.sh ssh ${EMR_TEST_SPEC}' to log in to master."
    echo "Please don't forget to shut down the cluster."
    ;;
esac


#!/bin/bash

CLOUDERA_MANAGER_URL="http://archive-primary.cloudera.com/cm5/cm/5/cloudera-manager-trusty-cm5.3.3_amd64.tar.gz"
CLOUDERA_CDH_PARCEL_URL="http://archive.cloudera.com/cdh5/parcels/5.3.3/CDH-5.3.3-1.cdh5.3.3.p0.5-trusty.parcel"
CLOUDERA_CDH_PARCEL_SHA1_URL="http://archive.cloudera.com/cdh5/parcels/5.3.3/CDH-5.3.3-1.cdh5.3.3.p0.5-trusty.parcel.sha1"
CLOUDERA_MANIFEST_URL="http://archive.cloudera.com/cdh5/parcels/5.3.3/manifest.json"

CLOUDERA_MANAGER=$(basename $CLOUDERA_MANAGER_URL)
CLOUDERA_CDH_PARCEL=$(basename $CLOUDERA_CDH_PARCEL_URL)
CLOUDERA_CDH_PARCEL_SHA1=$(basename $CLOUDERA_CDH_PARCEL_SHA1_URL)
CLOUDERA_MANIFEST=$(basename $CLOUDERA_MANIFEST_URL)
CLOUDERA_VERSION=$(basename $(dirname $CLOUDERA_CDH_PARCEL_URL))


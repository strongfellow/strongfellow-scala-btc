# aws emr create-cluster --log-uri s3://emr-logs.strongfellow.com/logs \
# --name SparkCluster3 --ami-version 3.7 --instance-type m3.2xlarge \
# --instance-count 3 --ec2-attributes KeyName=oregon --bootstrap-actions Path=s3://support.elasticmapreduce/spark/install-spark

CLUSTER=$1

aws emr add-steps --cluster-id $CLUSTER --steps \
Name=TXOutByBlock,Jar=s3://us-west-2.elasticmapreduce/libs/script-runner/script-runner.jar,Args=[/home/hadoop/spark/bin/spark-submit,--deploy-mode,cluster,--master,yarn,--class,SimpleApp,s3://emr.strongfellow.com/jars/btcspark-assembly-0.0.1.jar,s3://emr.strongfellow.com/txouts-by-block/2015-05-23T2000,s3://emr.strongfellow.com/crawl/outputs/2015-05-07-shuffled],ActionOnFailure=CONTINUE

aws emr create-cluster --log-uri s3://emr-logs.strongfellow.com/logs \
 --name SparkCluster --ami-version 3.7 --instance-type m3.2xlarge  \
 --instance-count 8  --ec2-attributes KeyName=oregon --bootstrap-actions Path=s3://support.elasticmapreduce/spark/install-spark --auto-terminate \
 --steps Name=TXOutByBlock,Jar=s3://us-west-2.elasticmapreduce/libs/script-runner/script-runner.jar,Args=[/home/hadoop/spark/bin/spark-submit,--deploy-mode,cluster,--master,yarn,--class,SimpleApp,s3://emr.strongfellow.com/jars/btcspark-assembly-0.0.1.jar,s3://emr.strongfellow.com/txouts-by-block/2015-05-24T1900,s3://emr.strongfellow.com/crawl/outputs/2015-05-07-shuffled]

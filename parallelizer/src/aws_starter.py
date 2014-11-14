#!/usr/bin/python

############
# programmatically start instances 
############
from boto.ec2.connection import EC2Connection
import boto

for id in ["1-test"]:#range(1,3):
    conn = boto.connect_ec2(profile_name="plankton")
    
    workerID = str(id)
    
    amiID = "ami-5460d423"
    instanceType = "t1.micro" 
    regionName = "eu-west-1a"
    
    baseDir = "/home/pilatus/ssd/"
    dataDir = baseDir + workerID + "/"
    resultDir = baseDir + workerID + "-result/"
    resultDirName = workerID + "-result/"
    resultArchivePath = baseDir + workerID + "-result.tar.gz"
    joblistFilePath = baseDir + workerID + "-joblist"
    finishedlistFilePath = baseDir + workerID + "-finished"
    dataFileName = workerID + ".tar.gz"
    annotconf = "/home/pilatus/WORK/pred-clust/annotation/configs/"
    catspath ="/home/pilatus/WORK/pred-clust/4449790Entity-Category.map_String-ArrayListOfString"
    

    dataURL = "https://s3-eu-west-1.amazonaws.com/batched-text/" + dataFileName
    codeURL = "http://141.3.200.186:1987/worker.jar"
    logServer = "54.72.34.125"
    logPort = "4712"
    
    pyUploadScript = 'echo "#!/usr/bin/python" >> upload.py \n\
#echo "import boto" >> upload.py \n\
#echo "conn = boto.connect_s3(profile_name=\\"speicher\\")" >> upload.py \n\
echo "from boto.s3.connection import S3Connection" >> upload.py \n\
echo "conn = S3Connection()" >> upload.py \n\
echo "buck = conn.get_bucket(\\"batched-text\\")" >> upload.py \n\
echo "kiArchive = buck.new_key(\\"' + workerID + '-result.tar.gz\\")" >> upload.py \n\
echo "kiArchive.set_contents_from_filename(\\"' + resultArchivePath + '\\")" >> upload.py \n\
echo "kiList = buck.new_key(\\"' + workerID + '-finished\\") " >> upload.py \n\
echo "kiList.set_contents_from_filename(\\"' + finishedlistFilePath + '\\")" >> upload.py '  

    

    javaVMArgs = '-Dfile.encoding=UTF-8 -Xmx15g \
-Dlogserver=' + logServer + ' \
-Dlogport=' + logPort + ' \
-Dworkerid=' + workerID + ' \
-Dresultdir=' + resultDir + ' \
-Ddatadir=' + dataDir + ' \
-Djoblist=' + joblistFilePath + ' \
-Dannotconf=' + annotconf + ' \
-Dcatsmappath=' + catspath + ' '
    
    javaClasspath = '-cp "worker.jar:/home/pilatus/lib/*:/home/pilatus/lib/jdom-2.0.5/*:/home/pilatus/lib/gwifi/*:/home/pilatus/lib/jung-" '
    
    # user data script string
    u_d = '#!/bin/bash \n\
cd ' + baseDir + '\n\
# download data \n\
wget ' + dataURL + '\n\
# download java code \n\
wget ' + codeURL + '\n\
# extract data \n\
tar -zxf ' + dataFileName + '\n\
# create joblist from files in the data dir \n\
/bin/ls ' + dataDir + ' > ' + joblistFilePath + '\n\
# create result dir \n\
mkdir ' + resultDir + '\n\
# run worker \n\
java ' + javaVMArgs + javaClasspath + ' algo.WorkerAWS \n\
#cp 1-test/* 1-test-result\n\
# compress results dir \n\
tar -zcf ' + workerID + '-result.tar.gz ' + resultDirName +'\n\
# create finished files list and append informative number \n\
/bin/ls ' + resultDir + ' > ' +  finishedlistFilePath + '-`/bin/ls 1-test | wc -l`\n\
# create python upload script: upload.py \n\
' + pyUploadScript + '\n\
#run the upload script \n\
python upload.py \n\
touch FINISHED \n'
    
    #print u_d
    conn.run_instances( image_id = amiID, user_data = u_d, instance_type = instanceType)
con.request_spot_instances(price="0.025", count=1, instance_type="m3.large", image_id="ami-5460d423", availability_zone_group="eu-west-1a", security_groups=["launch-wizard-5"])

"""
java  \
-Dmongo.addr=aifb-ls3-romulus \
-Dmongo.port=22222 \
-Dmongo.user=atsc \
-Dmongo.pwd=123 \
-Dmongo.db.name=all \
-Dlogfile=gm.log \
-Dlog4j.debug \
-Xmx10g \
-Dresult.dir=/dev/shm/artem/GRAPH/ \
-Dannots.dir=/dev/shm/artem/ANNOTRESULT/ \
-cp "/dev/shm/artem/lib/*:/dev/shm/artem/lib/gwifi/*:/dev/shm/artem/lib/jdom-2.0.5/*:/dev/shm/artem/current/*" parallelizer.Coordinator algo.GraphDumper 5 nofiles graph-maker-job

"""
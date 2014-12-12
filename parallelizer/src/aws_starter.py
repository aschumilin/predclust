#!/usr/bin/python

############
# programmatically start instances 
############
import boto.ec2
import boto
import boto.ec2.blockdevicemapping

for id in range(1):
    #conn = boto.connect_ec2(profile_name="plankton")
    
    
    
   #============================================================================
   #  con = boto.ec2.connect_to_region(region_name="eu-west-1", profile_name="kozjol")
   #  #con = boto.connect_ec2(profile_name="kozjol")
   #  
   #  
   # workerID = str(id)
   #  #################
   #  # aws spot request config
   #  #################
   #  preis = "0.086"
   #  amiID = "ami-f40bbc83" # predclust-11
   #  instanceType = "m3.2xlarge" 
   #  regionName = "eu-west-1a"
   #  secGroupName = "launch-wizard-5"
   #  # block device map to include SSD instance store
   #  blockDevMap = boto.ec2.blockdevicemapping.BlockDeviceMapping()
   #  block_dev_type = boto.ec2.blockdevicemapping.BlockDeviceType()
   #  block_dev_type.ephemeral_name='ephemeral0'
   #  blockDevMap['/dev/sdb'] = block_dev_type
   #  # 2. SSD for big m3.2xlarge instance:
   # # block_dev_type2 = boto.ec2.blockdevicemapping.BlockDeviceType()
   # # block_dev_type2.ephemeral_name = 'ephemeral1'
   # # blockDevMap['/dev/sdc'] = block_dev_type2
   #============================================================================
        
    #################
    # worker directories config
    #################
    workerID = str(id)
    baseDir = "/home/pilatus/ssd/"
    dataDir = baseDir + workerID + "/"
    resultDir = baseDir + workerID + "-result/"
    resultDirName = workerID + "-result/"
    resultArchivePath = baseDir + workerID + "-result.tar.gz"
    joblistFilePath = baseDir + workerID + "-joblist"
    joblistName = workerID + "-joblist"
    finishedlistFilePath = baseDir + workerID + "-finished"
    dataFileName = workerID + ".tar.gz"
    annotconf = "/home/pilatus/WORK/pred-clust/annotation/configs/"
    catspath ="/home/pilatus/WORK/pred-clust/4449790Entity-Category.map_String-ArrayListOfString"
    
    #################
    # data and code sources config
    #################
    dataURL = "https://s3-eu-west-1.amazonaws.com/batched-text/" + dataFileName
    joblistURL = "https://s3-eu-west-1.amazonaws.com/batched-text/" + joblistName
    codeURL = "http://54.171.108.163:1987/awsworker.jar"
    logServer = "54.171.108.163"
    logPort = "4712"
    numAnnotators = "6"
    #################
    # how to upload results after worker finished
    #################
    bucketResultFolder = "result/"
    pyUploadScript = 'echo "#!/usr/bin/python" >> upload.py \n\
#echo "import boto" >> upload.py \n\
#echo "conn = boto.connect_s3(profile_name=\\"speicher\\")" >> upload.py \n\
echo "from boto.s3.connection import S3Connection" >> upload.py \n\
echo "conn = S3Connection()" >> upload.py \n\
echo "buck = conn.get_bucket(\\"batched-text\\")" >> upload.py \n\
echo "kiArchive = buck.new_key(\\"' + bucketResultFolder + workerID + '-result.tar.gz\\")" >> upload.py \n\
echo "kiArchive.set_contents_from_filename(\\"' + resultArchivePath + '\\")" >> upload.py \n\
echo "kiList = buck.new_key(\\"' + bucketResultFolder + workerID + '-finished\\")" >> upload.py \n\
echo "kiList.set_contents_from_filename(\\"' +  finishedlistFilePath + '\\")" >> upload.py '  

    
    #################    
    # java worker arguments config 
    #################
    javaVMArgs = '-Dfile.encoding=UTF-8 -Xmx27g \
-Dlogserver=' + logServer + ' \
-Dlogport=' + logPort + ' \
-Dworkerid=' + workerID + ' \
-Dresultdir=' + resultDir + ' \
-Ddatadir=' + dataDir + ' \
-Djoblist=' + joblistFilePath + ' \
-Dannotconf=' + annotconf + ' \
-Dcatsmappath=' + catspath + ' \
-Dannots.num=' + numAnnotators + ' '
    
    javaClasspath = '-cp "awsworker.jar:/home/pilatus/WORK/pred-clust/lib/*:/home/pilatus/WORK/pred-clust/lib/jdom-2.0.5/*:/home/pilatus/WORK/pred-clust/lib/gwifi/*:/home/pilatus/WORK/pred-clust/lib/jung2-2_0_1/*" '
    
    # 
    #################
    # actual aws user data script string
    #################
    u_d = '#!/bin/bash \n\
cd ' + baseDir + '\n\
####################### start srl services first \n\
bash /home/pilatus/WORK/pred-clust/xlikewebservices/wso2-inst/bin/en_launch_services.sh -p 9090 \n\
bash /home/pilatus/WORK/pred-clust/xlikewebservices/wso2-inst/bin/es_launch_services.sh -p 8080 \n\
ps axf >> proc.log \n\
####################### download data \n\
wget ' + dataURL + '\n\
####################### download java code \n\
wget ' + codeURL + '\n\
####################### download joblist \n\
wget ' + joblistURL + '\n\
####################### extract data \n\
tar -zxf ' + dataFileName + '\n\
### create joblist from files in the data dir \n\
### /bin/ls ' + dataDir + ' > ' + joblistFilePath + '\n\
####################### create result dir \n\
mkdir ' + resultDir + '\n\
####################### run worker \n\
java ' + javaVMArgs + javaClasspath + ' parallelizer.CoordinatorAWS algo.WorkerAWS  8 nofiles  >> worker.log \n\
####################### compress results dir \n\
tar -zcf ' + workerID + '-result.tar.gz ' + resultDirName +'\n\
####################### create finished files list and append informative number \n\
/bin/ls ' + resultDir + ' > ' +  finishedlistFilePath + '\n\
####################### create python upload script: upload.py \n\
' + pyUploadScript + '\n\
####################### run the upload script \n\
python upload.py >> up.log\n\
touch FINISHED \n'
    
    print u_d
    #conn.run_instances( image_id = amiID, user_data = u_d, instance_type = instanceType)
    #con.request_spot_instances(price=preis, count=1, user_data=u_d, instance_type=instanceType, image_id=amiID, block_device_map = blockDevMap, availability_zone_group=regionName, security_groups=[secGroupName])

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
package org.opentosca.toscana.plugins.cloudformation.mapper;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.opentosca.toscana.model.capability.ComputeCapability;
import org.opentosca.toscana.model.capability.OsCapability;
import org.opentosca.toscana.plugins.cloudformation.CloudFormationModule;
import org.opentosca.toscana.plugins.util.TransformationFailureException;

import com.amazonaws.SdkClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.DescribeImagesRequest;
import com.amazonaws.services.ec2.model.DescribeImagesResult;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.Image;
import com.google.common.collect.ImmutableList;
import com.scaleset.cfbuilder.ec2.Instance;
import com.scaleset.cfbuilder.ec2.instance.EC2BlockDeviceMapping;
import com.scaleset.cfbuilder.ec2.instance.ec2blockdevicemapping.EC2EBSBlockDevice;
import org.slf4j.Logger;

import static org.opentosca.toscana.plugins.cloudformation.util.MappingUtils.checkValue;
import static org.opentosca.toscana.plugins.cloudformation.util.MappingUtils.getCpuByMem;
import static org.opentosca.toscana.plugins.cloudformation.util.MappingUtils.getInstanceType;
import static org.opentosca.toscana.plugins.cloudformation.util.MappingUtils.getMemByCpu;

/**
 Maps {@link OsCapability OsCapabilities} and {@link ComputeCapability ComputeCapabilities} to
 values for CloudFormation.
 <br>
 It uses the AWS Region and the AWS Credentials to connect to AWS via the AWS SDK to fetch the latest image id suitable
 for given OsCapability.

 @see org.opentosca.toscana.plugins.cloudformation.util.MappingUtils */
public class CapabilityMapper {

    public static final String EC2_DISTINCTION = "EC2";
    public static final String RDS_DISTINCTION = "RDS";
    private static final String ARCH_x86_32 = "i386";
    private static final String ARCH_x86_64 = "x86_64";
    private final Logger logger;
    /**
     CloudFormation {@link InstanceType}s for EC2.

     @see <a href="https://aws.amazon.com/ec2/instance-types/">EC2 Instance Types</a>
     */
    private final ImmutableList<InstanceType> EC2_INSTANCE_TYPES = ImmutableList.<InstanceType>builder()
        .add(new InstanceType("t2.nano", 1, 512))
        .add(new InstanceType("t2.micro", 1, 1024))
        .add(new InstanceType("t2.small", 1, 2048))
        .add(new InstanceType("t2.medium", 2, 4096))
        .add(new InstanceType("t2.large", 2, 8192))
        .add(new InstanceType("t2.xlarge", 4, 16384))
        .add(new InstanceType("t2.2xlarge", 8, 32768))
        .build();

    /**
     CloudFormation {@link InstanceType}s for RDS.

     @see <a href="https://aws.amazon.com/rds/instance-types/">RDS Instance Types</a>
     */
    private final ImmutableList<InstanceType> RDS_INSTANCE_CLASSES = ImmutableList.<InstanceType>builder()
        .add(new InstanceType("db.t2.micro", 1, 1024))
        .add(new InstanceType("db.t2.small", 1, 2048))
        .add(new InstanceType("db.t2.medium", 2, 4096))
        .add(new InstanceType("db.t2.large", 2, 8192))
        .add(new InstanceType("db.t2.xlarge", 4, 16384))
        .add(new InstanceType("db.t2.2xlarge", 8, 32768))
        .add(new InstanceType("db.m4.4xlarge", 16, 65536))
        .add(new InstanceType("db.m4.10xlarge", 40, 163840))
        .add(new InstanceType("db.m4.16xlarge", 64, 262144))
        .build();

    /**
     The AWS Region the image ids depend on.
     {@link CloudFormationModule#awsRegion}
     */
    private String awsRegion;
    /**
     The AWS Credentials to connect to AWS using the AWS SDK.
     {@link CloudFormationModule#awsCredentials}
     */
    private AWSCredentials awsCredentials;

    /**
     Creates a <tt>CapabilityMapper</tt> using the region and the credentials.
     The region and credentials are used to get image ids using the AWS SDK.

     @param awsRegion      the AWS Region to take
     @param awsCredentials the AWS Credentials to take
     @param logger         a logger to take
     */
    public CapabilityMapper(String awsRegion, AWSCredentials awsCredentials, Logger logger) {
        this.awsRegion = awsRegion;
        this.awsCredentials = awsCredentials;
        this.logger = logger;
    }

    /**
     Requests the AWS server for ImageIds with filters which are filled based on the values of the {@link OsCapability}.
     <br>
     The image with the latest creation date is picked and its imageId returned.

     @param osCapability the OsCapability to map
     @return a {@link String} that contains a valid ImageId that can be added to the properties of an EC2
     */
    public String mapOsCapabilityToImageId(OsCapability osCapability) throws SdkClientException, ParseException,
        IllegalArgumentException {
        AmazonEC2 ec2 = AmazonEC2ClientBuilder.standard()
            .withCredentials(new AWSStaticCredentialsProvider(awsCredentials))
            .withRegion(awsRegion)
            .build();
        //need to set these
        DescribeImagesRequest describeImagesRequest = new DescribeImagesRequest()
            .withFilters(
                new Filter("virtualization-type").withValues("hvm"),
                new Filter("root-device-type").withValues("ebs"))
            .withOwners("099720109477"); //this is the ownerId of amazon itself
        if (osCapability.getType().isPresent() && osCapability.getType().get().equals(OsCapability.Type.WINDOWS)) {
            describeImagesRequest.withFilters(new Filter("platform").withValues("windows"));
        }
        if (osCapability.getDistribution().isPresent()) {
            if (osCapability.getDistribution().get().equals(OsCapability.Distribution.UBUNTU)) {
                // */ubuntu/images/* gets better results than plain *ubuntu*
                describeImagesRequest.withFilters(new Filter("name").withValues("*ubuntu/images/*"));
            } else {
                //just search for the string
                describeImagesRequest.withFilters(new Filter("name").withValues("*" + osCapability.getDistribution()
                    .toString() + "*"));
            }
        }
        if (osCapability.getVersion().isPresent()) {
            describeImagesRequest.withFilters(new Filter("name").withValues("*" + osCapability.getVersion().get() +
                "*"));
        }
        if (osCapability.getArchitecture().isPresent()) {
            if (osCapability.getArchitecture().get().equals(OsCapability.Architecture.x86_64)) {
                describeImagesRequest.withFilters(new Filter("architecture").withValues(ARCH_x86_64));
            } else if (osCapability.getArchitecture().get().equals(OsCapability.Architecture.x86_32)) {
                describeImagesRequest.withFilters(new Filter("architecture").withValues(ARCH_x86_32));
            } else {
                throw new UnsupportedOperationException("This architecture is not supported " + osCapability
                    .getArchitecture());
            }
        } else {
            //defaulting to 64 bit architecture
            describeImagesRequest.withFilters(new Filter("architecture").withValues(ARCH_x86_64));
        }
        try {
            DescribeImagesResult describeImagesResult = ec2.describeImages(describeImagesRequest);
            String imageId = processResult(describeImagesResult);
            logger.debug("ImageId is: '{}'", imageId);
            return imageId;
        } catch (SdkClientException se) {
            logger.error("Cannot connect to AWS to request image Ids");
            throw se;
        } catch (ParseException pe) {
            logger.error("Error parsing date format of image creation dates");
            throw pe;
        } catch (IllegalArgumentException ie) {
            logger.error("With the filters created from the OsCapability there are no valid images received");
            throw ie;
        }
    }

    /**
     Processes the result of an {@link DescribeImagesRequest} and returns the imageId of the latest image.

     @param describeImagesResult the result received from aws
     @return returns the latest imageId
     */
    private String processResult(DescribeImagesResult describeImagesResult) throws ParseException,
        IllegalArgumentException {
        Integer numReceivedImages = describeImagesResult.getImages().size();
        logger.debug("Got '{}' images from aws", numReceivedImages);
        if (numReceivedImages > 0) {
            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            Map<Date, Image> creationDateMap = new HashMap<>();
            for (Image image : describeImagesResult.getImages()) {
                Date date = dateFormat.parse(image.getCreationDate());
                creationDateMap.put(date, image);
            }
            Image latest = creationDateMap.get(Collections.max(creationDateMap.keySet()));
            logger.debug("Latest image received: '{}'", latest);
            return latest.getImageId();
        } else {
            throw new IllegalArgumentException("No images received");
        }
    }

    /**
     Finds the best {@link InstanceType} based on the values contained in the {@link ComputeCapability}.
     <br>
     If necessary the values are scaled upwards till they meet the requirement.

     @param computeCapability the {@link ComputeCapability} to map
     @param distinction       a distinction string, can either be "EC2" or "RDS"
     @return a valid {@link InstanceType} / InstanceClass(RDS) {@link String}
     @throws TransformationFailureException Gets thrown if the values numCpus and memSize are too big and there is no
     valid {@link InstanceType}.
     */
    public String mapComputeCapabilityToInstanceType(ComputeCapability computeCapability, String distinction) throws
        IllegalArgumentException {
        Integer numCpus = computeCapability.getNumCpus().orElse(0);
        Integer memSize = computeCapability.getMemSizeInMb().orElse(0);
        //default type the smallest
        final ImmutableList<InstanceType> instanceTypes;
        if (EC2_DISTINCTION.equals(distinction)) {
            instanceTypes = EC2_INSTANCE_TYPES;
        } else if (RDS_DISTINCTION.equals(distinction)) {
            instanceTypes = RDS_INSTANCE_CLASSES;
        } else {
            throw new IllegalArgumentException("Distinction not supported: " + distinction);
        }
        List<Integer> allNumCpus = instanceTypes.stream()
            .map(InstanceType::getNumCpus)
            .sorted()
            .collect(Collectors.toList());
        List<Integer> allMemSizes = instanceTypes.stream()
            .map(InstanceType::getMemSize)
            .sorted()
            .collect(Collectors.toList());
        // scale numCpus and memSize upwards if they are not represented in the lists
        try {
            logger.debug("Check numCpus: '{}'", numCpus);
            numCpus = checkValue(numCpus, allNumCpus);
            logger.debug("Check memSize: '{}'", memSize);
            memSize = checkValue(memSize, allMemSizes);
        } catch (IllegalArgumentException ie) {
            logger.error("Values numCpus: '{}' and/or memSize: are too big. No InstanceType found", numCpus, memSize);
            throw ie;
        }
        //get instanceType from combination
        String instanceType = findCombination(numCpus, memSize, instanceTypes, allNumCpus, allMemSizes);
        logger.debug("InstanceType is: '{}'", instanceType);
        return instanceType;
    }

    /**
     Takes the value from the {@link ComputeCapability} and turns it into GB.
     <br>
     The minimum is 20 GB the maximum 6144 GB

     @param computeCapability the {@link ComputeCapability} to map
     @return returns an integer representing the diskSize that should be taken
     */
    public Integer mapComputeCapabilityToRDSAllocatedStorage(ComputeCapability computeCapability) {
        final Integer minSize = 20;
        final Integer maxSize = 6144;
        Integer diskSize = computeCapability.getDiskSizeInMb().orElse(minSize * 1000);
        diskSize = diskSize / 1000;
        if (diskSize > maxSize) {
            logger.debug("Disk size: '{}'", maxSize);
            return maxSize;
        }
        if (diskSize < minSize) {
            logger.debug("Disk size: '{}'", minSize);
            return minSize;
        }
        logger.debug("Disk size: '{}'", diskSize);
        return diskSize;
    }

    /**
     Maps the disk_size property of a {@link ComputeCapability} to an EC2 Instance.

     @param computeCapability {@link ComputeCapability} containing the disk_size property
     @param cfnModule         {@link CloudFormationModule} containing the Instance
     @param nodeName          name of the Instance
     */
    public void mapDiskSize(ComputeCapability computeCapability, CloudFormationModule cfnModule, String nodeName) {
        // If disk_size is not set, default to 8000 Mb
        Integer diskSizeInMb = computeCapability.getDiskSizeInMb().orElse(8000);
        // Convert disk_size to Gb
        Integer diskSizeInGb = diskSizeInMb / 1000;
        logger.debug("Check diskSize: '{}' Gb", diskSizeInGb);
        if (diskSizeInGb < 8) {
            logger.warn("Disk size of '{}' smaller than the minimum value required by EC2 Instances. Setting the disk size of '{}' to the minimum allowed value of 8 Gb.", nodeName, nodeName);
            diskSizeInGb = 8;
        }
        // Add BlockDeviceMapping if needed
        if (diskSizeInGb > 8) {
            logger.debug("Disk size of '{}' bigger than the default value of EC2 Instances. Adding a BlockDeviceMapping to '{}'.", nodeName, nodeName);
            Instance computeAsInstance = (Instance) cfnModule.getResource(nodeName);
            computeAsInstance.blockDeviceMappings(new EC2BlockDeviceMapping()
                .deviceName("/dev/sda1")
                .ebs(new EC2EBSBlockDevice()
                    .volumeSize(diskSizeInGb.toString())));
        }
    }

    /**
     Tries to find a combination of numCpus and memSize in the {@link ImmutableList} of {@link InstanceType}s.
     <br>
     If there is no result found it first scales the number of cpus to find a valid {@link InstanceType}. If there is
     still none found it scales the size of memory. If there is still none found there can be an working combination and
     an {@link TransformationFailureException} gets thrown.

     @param numCpus       minimum number of cpus the {@link InstanceType} should have
     @param memSize       minimum size of memory the {@link InstanceType} should have
     @param instanceTypes {@link ImmutableList} of {@link InstanceType}s to choose from
     @param allNumCpus    {@link List} of all valid numbers of cpus
     @param allMemSizes   {@link List} of all valid sizes of memory
     @return returns the {@link String} representation of the found {@link InstanceType}
     @throws TransformationFailureException gets thrown if no combination ist found (for example one or both values are
     too high, so there is no valid {@link InstanceType})
     */
    private String findCombination(Integer numCpus, Integer memSize, ImmutableList<InstanceType> instanceTypes,
                                   List<Integer> allNumCpus, List<Integer> allMemSizes) throws
        TransformationFailureException {
        String instanceType = getInstanceType(numCpus, memSize, instanceTypes);
        if (instanceType.isEmpty()) {
            Integer newNumCpus = numCpus;
            Integer newMemSize = memSize;
            //this combination does not exist
            //try to scale the cpus
            logger.debug("The combination of numCpus: '{}' and memSize: '{}' does not exist", newNumCpus, newMemSize);
            logger.debug("Try to scale cpu");
            for (Integer num : allNumCpus) {
                if (num > newNumCpus && getMemByCpu(num, instanceTypes).contains(newMemSize)) {
                    newNumCpus = num;
                    break;
                }
            }
            instanceType = getInstanceType(newNumCpus, newMemSize, instanceTypes);
            if (instanceType.isEmpty()) {
                logger.debug("Scaling cpu failed");
                logger.debug("Try to scale memory");
                //try to scale the memory
                for (Integer mem : allMemSizes) {
                    if (mem > newMemSize && getCpuByMem(mem, instanceTypes).contains(newNumCpus)) {
                        newMemSize = mem;
                        break;
                    }
                }
                instanceType = getInstanceType(newNumCpus, newMemSize, instanceTypes);
                if (instanceType.isEmpty()) {
                    throw new TransformationFailureException("No combination of numCpus and memSize found");
                } else {
                    logger.debug("Scaling memSize succeeded, memSize: '{}'", newMemSize);
                }
            } else {
                logger.debug("Scaling numCpus succeeded, numCpus: '{}'", newNumCpus);
            }
        }
        return instanceType;
    }

    /**
     Represents an instance type of a EC2 or RDS.

     @see <a href="https://aws.amazon.com/ec2/instance-types/">EC2 Instance Types</a>
     @see <a href="https://aws.amazon.com/rds/instance-types/">RDS Instance Types</a>
     */
    public class InstanceType {
        /**
         The {@link String} representation of the InstanceType.
         <br>
         It is used by CloudFormation or AWS in general.
         */
        private String type;
        /**
         The number of cpus this InstanceType has.
         */
        private Integer memSize;
        /**
         The size of memory this InstanceType has.
         */
        private Integer numCpus;

        /**
         Creates an InstanceType based on the {@link String} representation, the number of cpus and the size of memory.

         @param type    the {@link String} representation of the InstanceType, that is used by CloudFormation or AWS in
         general
         @param numCpus the number of cpus this InstanceType has
         @param memSize the size of memory this InstanceType has
         */
        public InstanceType(String type, Integer numCpus, Integer memSize) {
            this.type = type;
            this.numCpus = numCpus;
            this.memSize = memSize;
        }

        /**
         Gets the {@link #type}.

         @return the {@link #type}
         */
        public String getType() {
            return type;
        }

        /**
         Gets the {@link #memSize}.

         @return the {@link #memSize}
         */
        public Integer getMemSize() {
            return memSize;
        }

        /**
         Gets the {@link #numCpus}.

         @return the {@link #numCpus}
         */
        public Integer getNumCpus() {
            return numCpus;
        }
    }
}

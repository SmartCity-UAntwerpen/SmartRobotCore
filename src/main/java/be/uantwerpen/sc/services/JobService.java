package be.uantwerpen.sc.services;

import be.uantwerpen.rc.models.map.Map;
import be.uantwerpen.sc.RobotCoreLoop;
import be.uantwerpen.sc.controllers.DriverCommandSender;
import be.uantwerpen.rc.models.Job;
import be.uantwerpen.sc.tools.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;

/**
 * Created by Thomas on 01/06/2016.
 */
@Service
public class JobService
{
    @Value("${sc.backend.ip:localhost}")
    private String serverIP;

    @Value("#{new Integer(${sc.backend.port}) ?: 1994}")
    private int serverPort;

    @Autowired
    private DataService dataService;

    @Autowired
    private QueueService queueService;

    @Autowired
    private DriverCommandSender sender;

    private RobotCoreLoop robotCoreLoop;

    private Logger logger = LoggerFactory.getLogger(JobService.class);
    private int endJob;

    private Long jobid;

    public int getEndJob(){
        return endJob;
    }

    public void setEndJob(int endOfJob){
        endJob=endOfJob;
    }

    public Long jobid(){
        return jobid;
    }

    public void getJobid(Long jobid){
        this.jobid=jobid;
    }

    public void setRobotCoreLoop(RobotCoreLoop robotCoreLoop)
    {
        this.robotCoreLoop = robotCoreLoop;
    }

    public void parseJob(String job) throws ParseException
    {

        System.out.println("Parsing job");
        logger.info("Parsing job...");

        String tempStr = job.split(":")[2];
        String jobidNumber = tempStr.split("/")[0];

        String tempbotid = job.split("/")[1];
        String botidNumber = tempbotid.split(":",2)[1];

        String tempidstart = job.split("/")[2];
        String idstartNumber = tempidstart.split(":")[1];

        String tempidend = job.split("/")[3];
        String idendNumber = tempidend.split(":")[1];
        idendNumber = idendNumber.replace("}","");

        Long jobid = Long.parseLong(jobidNumber);
        Long botid = Long.parseLong(botidNumber);
        Long startid = Long.parseLong(idstartNumber);
        Long endid = Long.parseLong(idendNumber);

        logger.info("Parsed: jobid = " + jobid + " botid = " + botid + " startid = " + startid + " endid = " + endid);

        if(!(dataService.getCurrentLocation().equals(endid) && dataService.getCurrentLocation().equals(startid))) {
            Job parsedJob = new Job(jobid,startid,endid);
            dataService.job = parsedJob;
        } else {
            logger.info("Already on destination");
            //let the backend know that the job is finished
            RestTemplate restTemplate = new RestTemplate();
            while(true) {
                try {
                    restTemplate.getForObject("http://" + serverIP + ":" + serverPort + "/job/finished/" + dataService.getRobotID()
                            , Void.class);
                    break;
                } catch(RestClientException e ) {
                    logger.error("Can't connect to the database to send job finished, retrying...");
                }
            }


        }

        Terminal.printTerminal("job parsed");

        //performJob(parsedJob);

    }

    public void performJob(Job job)
    {

        int endInt = job.getIdEnd().intValue();
        int startInt = job.getIdStart().intValue();
        logger.info("Performing job with destination: "+endInt);
        Terminal.printTerminal("performJob end int = " + endInt);
        switch(dataService.getWorkingmodeEnum()) {
            case INDEPENDENT:
                try {
                    //compute path on robot
                    dataService.robotDriving = true;
                    dataService.jobfinished = false;
                    dataService.tempjob = false;
                    dataService.executingJob = false;

                        if(!dataService.getCurrentLocation().equals(job.getIdStart()) && (!dataService.executingJob)){ //bot is not located at start of job
                            Terminal.printTerminal("start location not current Location. Going to " + job.getIdStart());
                            dataService.setDestination(job.getIdStart());
                            dataService.tempjob = true;
                            dataService.executingJob = true;
                            startPathPlanning(startInt);
                            logger.info("Wait till tempjob is finished");
                            while(dataService.tempjob){} //wait till tempjob is finished
                            dataService.tempjob = false;
                            dataService.executingJob = true;
                            dataService.setDestination(job.getIdEnd());
                            startPathPlanning(endInt);
                        }else {
                            dataService.tempjob = false;
                            dataService.executingJob = true;
                            dataService.setDestination(job.getIdEnd());
                            startPathPlanning(endInt);
                        }

                } catch (NumberFormatException e) {
                    Terminal.printTerminalError(e.getMessage());
                    Terminal.printTerminalInfo("Usage: navigate end");
                }
                break;
            case PARTIALSERVER:
                try {
                    dataService.setDestination(job.getIdEnd());
                    dataService.robotDriving = true;
                    dataService.tempjob = false;
                    dataService.executingJob = true;

                    startPathRobotcore(startInt,endInt);
                } catch (NumberFormatException e) {
                    Terminal.printTerminalError(e.getMessage());
                    Terminal.printTerminalInfo("Usage: navigate end");
                }
                break;
            case PARTIALSERVERNG:
                try {
                    dataService.robotDriving = true;
                    startPathRobotcoreNg(startInt,endInt);
                } catch (NumberFormatException e) {
                    Terminal.printTerminalError(e.getMessage());
                    Terminal.printTerminalInfo("Usage: navigate end");
                }
                break;
            case FULLSERVER:
                try {
                    Terminal.printTerminal("FullServer mode");
                    Terminal.printTerminal("Current Location = " + dataService.getCurrentLocation() + " end int = " + endInt);
                    dataService.setDestination(job.getIdEnd());
                    dataService.robotDriving = true;
                    dataService.tempjob = false;
                    dataService.executingJob = true;
                    while(dataService.getCurrentLocation()!=endInt){

                        if(queueService.getContentQueue().size() == 0){
                            Terminal.printTerminal("StartPathFullRobotCore");
                            startPathFullRobotcore(startInt, endInt);
                        }
                    }
                } catch (NumberFormatException e) {
                    Terminal.printTerminalError(e.getMessage());
                    Terminal.printTerminalInfo("Usage: navigate end");
                }
                break;
        }
    }

    private void startPathPlanning(int end2){
        logger.info("Starting pathplanning from point " + dataService.getCurrentLocation() + " to " + end2);
        //first retrieve the most updated version of the map (weights are dynamic)
        getUpdatedMap();
        dataService.navigationParser = new NavigationParser(robotCoreLoop.pathplanning.Calculatepath(dataService.map, (int)(long)dataService.getCurrentLocation(), end2), dataService);
        //Parse Map
        dataService.navigationParser.parseMap();
        //Setup for driving
        dataService.robotDriving = true;

        //Process map
        for (DriveDir command : dataService.navigationParser.commands) {
            logger.info("insert job " + command.toString());
            queueService.insertJob(command.toString());
        }
    }

    private void getUpdatedMap() {
        logger.info("Receiving updated map...");
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<Map> responseList;
        while(true) {
            try {
                responseList = restTemplate.getForEntity("http://" + serverIP + ":" + serverPort + "/map/", Map.class);
                break;
            } catch(RestClientException e) {
                logger.error("Can't connect to the backend to retrieve map, retrying...");
            }
        }
        dataService.map = responseList.getBody();
    }

    public void startPathRobotcore(int start, int end){


        //ask robotcore for instructions
        RestTemplate restTemplate = new RestTemplate();
        DriveDirEncapsulator nextPath = restTemplate.getForObject("http://" + serverIP + ":" + serverPort + "/map/getdirections/"
                +"/" + start + "/" + end, DriveDirEncapsulator.class);

        //new job so remove drive commands from possible earlier job
        removeDriveCommands();

        //Process map
        for (int i = 0; i < nextPath.getDriveDirs().size();i++) {
            Terminal.printTerminal("Partial server command: " + nextPath.getDriveDirs().get(i).toString());
            queueService.insertJob(nextPath.getDriveDirs().get(i).toString());
        }

    }

    public void startPathRobotcoreNg(int start, int end){

        /*
        //ask robotcore for instructions
        RestTemplate restTemplate = new RestTemplate();
        DriveDirEncapsulator nextPath = restTemplate.getForObject("http://" + serverIP + ":" + serverPort + "/map/getdirections/"
                +"/" + start + "/" + end, DriveDirEncapsulator.class);

        //new job so remove drive commands from possible earlier job
        removeDriveCommands();

        //Process map
        for (int i = 0; i < nextPath.getDriveDirs().size();i++) {
            Terminal.printTerminal("Partial server command: " + nextPath.getDriveDirs().get(i).toString());
            queueService.insertJob(nextPath.getDriveDirs().get(i).toString());
        }
        */

        RestTemplate restTemplate = new RestTemplate();
        DriveDirEncapsulator nextPath = restTemplate.getForObject("http://" + serverIP + ":" + serverPort + "/map/getdirectionsng/"
                +"/" + start + "/" + end, DriveDirEncapsulator.class);

        //new job so remove drive commands from possible earlier job
        removeDriveCommands();

        //Process map
        for (int i = 0; i < nextPath.getDriveDirs().size();i++) {
            Terminal.printTerminal("Partial server command: " + nextPath.getDriveDirs().get(i).toString());
            queueService.insertJob(nextPath.getDriveDirs().get(i).toString());
        }
    }

    public void startPathFullRobotcore(int start, int end){
        //ask robotcore for instructions
        RestTemplate restTemplate = new RestTemplate();
        DriveDirEncapsulator nextPath = restTemplate.getForObject("http://" + serverIP + ":" + serverPort + "/map/getnexthop/"
                + start + "/" + dataService.getCurrentLocation() + "/" + end, DriveDirEncapsulator.class);

        for (int i = 0; i < nextPath.getDriveDirs().size();i++) {
            queueService.insertJob(nextPath.getDriveDirs().get(i).toString());
        }
    }

    public void removeDriveCommands() {
        //remove drive jobs from queue
        Terminal.printTerminal("remove commands");
        BlockingQueue<String> content = queueService.getContentQueue();
        ArrayList<String> contentcopy = new ArrayList<String>();
        content.drainTo(contentcopy);
        String comm;
        while (contentcopy.size() > 0) {
            comm = contentcopy.get(0);
            Terminal.printTerminal(comm);
            if (!comm.matches("DRIVE (.*)")) {
                content.add(comm);
            }
        }
        queueService.setContentQueue(content);
    }

    public void removeCommands(){
        BlockingQueue<String> content = queueService.getContentQueue();
        ArrayList<String> contentcopy = new ArrayList<String>();
        content.drainTo(contentcopy);
        Terminal.printTerminal(contentcopy.toString());
        contentcopy.clear();
        queueService.setContentQueue(content);
    }


}
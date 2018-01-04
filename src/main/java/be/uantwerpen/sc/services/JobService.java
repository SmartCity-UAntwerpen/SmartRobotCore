package be.uantwerpen.sc.services;

import be.uantwerpen.sc.RobotCoreLoop;
import be.uantwerpen.sc.controllers.CCommandSender;
import be.uantwerpen.sc.models.Job;
import be.uantwerpen.sc.tools.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.BlockingQueue;

/**
 * Created by Thomas on 01/06/2016.
 */
@Service
public class JobService
{
    @Value("${sc.core.ip:localhost}")
    private String serverIP;

    @Value("#{new Integer(${sc.core.port}) ?: 1994}")
    private int serverPort;

    @Autowired
    private DataService dataService;

    @Autowired
    private QueueService queueService;

    @Autowired
    private CCommandSender sender;

    private RobotCoreLoop robotCoreLoop;

    private int endJob;

    private Long jobid,botid,startid,endid;

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

        //try
        //{

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

        System.out.println("Parsed: jobid = " + jobid + " botid = " + botid + " startid = " + startid + " endid = " + endid);

        Job parsedJob = new Job(jobid,botid,startid,endid);

        dataService.setDestination(endid);
        Terminal.printTerminal("job parsed");
        performJob(parsedJob);
        //}
//        catch(Exception e)
//        {
//            throw new ParseException("Can not parse job from: " + job + "\nInvalid format!", 0);
//        }
    }

    private void performJob(Job job)
    {

        int endInt = job.getEndid().intValue();
        int startInt = job.getStartid().intValue();
        Terminal.printTerminal("performJob end int = " + endInt);
        switch(dataService.getWorkingmodeEnum()) {
            case INDEPENDENT:
                try {
                    //int endInt = Integer.parseInt(end);
                    //compute path on robot
                    dataService.robotDriving = true;
                    dataService.jobfinished = false;
                    dataService.tempjob = false;
                    dataService.executingJob = false;
                    while(!dataService.jobfinished){

                        if((dataService.getCurrentLocation() != job.getStartid()) && (!dataService.executingJob)){
                            Terminal.printTerminal("start location not currentLocation");
                            dataService.setDestination(startid);
                            dataService.tempjob = true;
                            dataService.executingJob = true;
                            startPathPlanning(startInt);

                        }

                        if(!dataService.tempjob && !dataService.executingJob && (dataService.getCurrentLocation() == job.getStartid())){
                            dataService.tempjob = false;
                            dataService.executingJob = true;
                            dataService.setDestination(job.getEndid());
                            startPathPlanning(endInt);
                        }
                    }



                } catch (NumberFormatException e) {
                    Terminal.printTerminalError(e.getMessage());
                    Terminal.printTerminalInfo("Usage: navigate end");
                }
                break;
            case PARTIALSERVER:
                try {
                    //int endInt = Integer.parseInt(end);
                    //get commands from server
                    dataService.robotDriving = true;
                    startPathRobotcore(startInt,endInt);
                } catch (NumberFormatException e) {
                    Terminal.printTerminalError(e.getMessage());
                    Terminal.printTerminalInfo("Usage: navigate end");
                }
                break;
            case FULLSERVER:
                try {
                    Terminal.printTerminal("FullServer mode");
                    //int endInt = Integer.parseInt(end);
                    Terminal.printTerminal("Current Location = " + dataService.getCurrentLocation() + " end int = " + endInt);
                    dataService.robotDriving = true;
                    while(dataService.getCurrentLocation()!=endInt){

                        //Terminal.printTerminal("get Content queue = " + queueService.getContentQueue().size());
                        if(queueService.getContentQueue().size() == 0){
                            //get first command from server
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


        /*
        String jobDescription = job.getJobDescription();

        System.out.println("JOB DESCRIPTION: " + jobDescription);

        switch(jobDescription.split(" ", 2)[0].toLowerCase())
        {
            case "navigate":
                try {
                    String end = jobDescription.split(" ", 2)[1].toLowerCase();
                    switch(dataService.getWorkingmodeEnum()) {
                        case INDEPENDENT:
                            try {
                                int endInt = Integer.parseInt(end);
                                //compute path on robot
                                startPathPlanning(endInt);
                            } catch (NumberFormatException e) {
                                Terminal.printTerminalError(e.getMessage());
                                Terminal.printTerminalInfo("Usage: navigate end");
                            }
                            break;
                        case PARTIALSERVER:
                            try {
                                int endInt = Integer.parseInt(end);
                                //get commands from server
                                startPathRobotcore(endInt);
                            } catch (NumberFormatException e) {
                                Terminal.printTerminalError(e.getMessage());
                                Terminal.printTerminalInfo("Usage: navigate end");
                            }
                            break;
                        case FULLSERVER:
                            try {
                                int endInt = Integer.parseInt(end);
                                while(dataService.getCurrentLocation()!=endInt)
                                    if(queueService.getContentQueue().size() == 0){
                                        //get first command from server
                                        startPathFullRobotcore(endInt);
                                    }
                            } catch (NumberFormatException e) {
                                Terminal.printTerminalError(e.getMessage());
                                Terminal.printTerminalInfo("Usage: navigate end");
                            }
                            break;
                    }
                }catch(ArrayIndexOutOfBoundsException e){
                    Terminal.printTerminalInfo("Usage: navigate end");
                }
                break;
            case "playaudio":
                sender.sendCommand("SPEAKER UNMUTE");
                sender.sendCommand("SPEAKER PLAY QMusic");
                try{
                    Thread.sleep(1000);
                }catch (Exception e){
                    e.printStackTrace();
                }
                sender.sendCommand("SPEAKER PLAY cantina");
                break;
            default:
                System.out.println("Unknown job description: " + jobDescription);
        }
        */
    }

    private void startPathPlanning(int end2){
        /*
        dataService.locationUpdated = false;
        while(!dataService.locationUpdated){
            //Wait
            try {
                //Read tag
                queueService.insertJob("TAG READ UID");
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        */
        Terminal.printTerminal("Starting pathplanning from point " + dataService.getCurrentLocation() + " to " + end2);
        dataService.navigationParser = new NavigationParser(robotCoreLoop.pathplanning.Calculatepath(dataService.map, (int)(long)dataService.getCurrentLocation(), end2), dataService);
        //Parse Map
        dataService.navigationParser.parseMap();
        //dataService.navigationParser.parseRandomMap(dataService);

        //removeDriveCommands();

        //Setup for driving
        int start = (int)(long)dataService.navigationParser.list.get(0).getId();
        int end = (int)(long)dataService.navigationParser.list.get(1).getId();
        dataService.setNextNode((long)end);
        dataService.setPrevNode((long)start);
        dataService.robotDriving = true;
        queueService.insertJob("DRIVE FOLLOWLINE");
        queueService.insertJob("DRIVE FORWARD 110");

        //Process map
        for (DriveDir command : dataService.navigationParser.commands) {
            Terminal.printTerminal("insert job" + command.toString());
            queueService.insertJob(command.toString());
        }
    }

    public void startPathRobotcore(int start, int end){

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

        Terminal.printTerminal("Drive dir Ontvangen = " + nextPath);
        //Process map but only 2 first commands
        for (int i = 0; i < nextPath.getDriveDirs().size();i++) {
            Terminal.printTerminal("Full server command: " + nextPath.getDriveDirs().get(i).toString());
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
package be.uantwerpen.sc;

import be.uantwerpen.sc.controllers.MapController;
import be.uantwerpen.sc.controllers.PathController;
import be.uantwerpen.sc.controllers.mqtt.MqttJobSubscriber;
import be.uantwerpen.sc.controllers.mqtt.MqttPublisher;
import be.uantwerpen.sc.services.*;
import be.uantwerpen.sc.tools.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;

/**
 * Created by Arthur on 4/05/2016.
 */
@Service
public class RobotCoreLoop implements Runnable
{
    @Autowired
    private DataService dataService;

    @Autowired
    private PathplanningType pathplanningType;

    @Autowired
    private WorkingmodeType workingmodeType;

    @Autowired
    private MqttJobSubscriber jobSubscriber;

    @Autowired
    private MqttPublisher locationPublisher;

    @Autowired
    private JobService jobService;

    @Value("${sc.core.ip:localhost}")
    private String serverIP;

    @Value("#{new Integer(${sc.core.port}) ?: 1994}")
    private int serverPort;

    @Autowired
    private QueueService queueService;
    @Autowired
    private MapController mapController;
    @Autowired
    private PathController pathController;

    public IPathplanning pathplanning;

    private boolean first = true;

    private TerminalService terminalService;

    /*public RobotCoreLoop(QueueService queueService, MapController mapController, PathController pathController, PathplanningType pathplanningType, DataService dataService){
        this.queueService = queueService;
        this.mapController = mapController;
        this.pathController = pathController;
        this.pathplanningType = pathplanningType;
        this.dataService = dataService;

    }*/

    public RobotCoreLoop(){

    }

    @PostConstruct
    private void postconstruct(){ //struct die wordt opgeroepen na de initiele struct. Omdat de autowired pas wordt opgeroepen NA de initiele struct
        //Setup type
        Terminal.printTerminalInfo("Selected PathplanningType: " + pathplanningType.getType().name());
        Terminal.printTerminalInfo("Selected WorkingmodeType: " + workingmodeType.getType().name());
    }

    @Deprecated
    public void setServerCoreIP(String ip, int port)
    {
        this.serverIP = ip;
        this.serverPort = port;
    }

    public void run() {
        //getRobotId
        terminalService=new TerminalService(); //terminal service starten. terminal wordt gebruikt om bepaalde dingen te printen en commandos in te geven
        RestTemplate restTemplate = new RestTemplate(); //standaard resttemplate gebruiken

        Long robotID = restTemplate.getForObject("http://" + serverIP + ":" + serverPort + "/bot/initiate/" //aan de server laten weten dat er een nieuwe bot zich aanbied
                +workingmodeType.getType().toString(), Long.class); //Aan de server laten weten in welke mode de bot werkt

        dataService.setRobotID(robotID);
        jobService.setRobotCoreLoop(this);
        jobService.setEndJob(-1);
        jobService.removeCommands();

        if(!jobSubscriber.initialisation()) //subscribe to topics to listen to jobs
        {
            System.err.println("Could not initialise MQTT Job service!");
        }

        //Wait for tag read
        //Read tag where bot is located

        synchronized (this) {
            while (dataService.getTag().trim().equals("NONE") || dataService.getTag().equals("NO_TAG")) {
                try {
                    //Read tag
                    queueService.insertJob("TAG READ UID");
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        Terminal.printTerminal("Tag: " + dataService.getTag());

       // updateStartLocation();

        //Setup interface for correct mode of pathplanning
        setupInterface();
        Terminal.printTerminal("Interface is set up");

        //Request map at server with rest
        dataService.map = mapController.getMap();
        Terminal.printTerminal("Map received " + dataService.map.getNodeList());



        //Set location of bot
        dataService.setCurrentLocation(dataService.map.getNodeByRFID(dataService.getTag()));
        Terminal.printTerminal("Start Location: " + dataService.getCurrentLocation()+"\n\n");

        //We have the map now, update link
        dataService.firstLink();
        Terminal.printTerminal("link updated");
        Terminal.printTerminal("next: "+dataService.getNextNode());

        /*
        for(int i = 0; i < dataService.map.getNodeList().size();i++){
            for(int j = 0; j < dataService.map.getNodeList().get(i).getNeighbours().size();j++){
                Terminal.printTerminal("NodeId = " +  dataService.map.getNodeList().get(i).getNodeId() + " Rfid = " + dataService.map.getNodeList().get(i).getPointEntity().getRfid());
                Terminal.printTerminal("Stop direction" + dataService.map.getNodeList().get(i).getNeighbours().get(j).getStopDirection());
                Terminal.printTerminal("Start direction" + dataService.map.getNodeList().get(i).getNeighbours().get(j).getStartDirection());
            }

        }
        */
        //Set looking dir of bot
//        dataService.setLookingCoordiante(dataService.map.getNodeList().get(dataService.getCurrentLocation().intValue()).getNeighbours().get(0).getStartDirection());
        Terminal.printTerminal("looking in direction " + dataService.getLookingCoordiante());


        //queueService.insertJob("DRIVE FORWARD 120");
        //Terminal.printTerminal("LINE");
        //queueService.insertJob("DRIVE FOLLOWLINE");
        //Terminal.printTerminal("CROSS");
        //queueService.insertJob("DRIVE FORWARD 50");
        //Terminal.printTerminal("LINE");
        //queueService.insertJob("DRIVE FOLLOWLINE");

        while(!Thread.interrupted()){
        /*
            switch(workingmodeType.getType()){
                case PARTIALSERVER:
                    //wait for instructions in mqtt
                    //ask robotcore for calculated path and add commands in jobservice
                    break;
                case FULLSERVER:
                    //wait for instructions in mqtt
                    //if( not at destination && at edge):
                    DriveDir nextNode = restTemplate.getForObject("http://" + serverIP + ":" + serverPort + "/map/"
                            +dataService.getCurrentLocation()+"/path/"+jobService.getEndJob(), DriveDir.class);
                    //ask robotcore for calculated path from current position on edge
                    break;
                case INDEPENDENT:
                    //take random routes until job in mqtt
                    switch(pathplanningType.getType()){
                        case DIJKSTRA:
                            Terminal.printTerminal("Dijkstra");
                            break;
                        case RANDOM:
                            break;
                        case TERMINAL:
                            break;
                    }
                    break;
                default:
                    Terminal.printTerminal("Wrong working mode type!");
                    break;
            }
        */
        }



/*vroeger
        while (!Thread.interrupted() && pathplanningType.getType() == PathplanningEnum.RANDOM) {
            //Use pathplanning (Described in Interface)
            if (queueService.getContentQueue().isEmpty() && dataService.locationUpdated) {
                dataService.setCurrentLocationAccordingTag();
                //Endpoint wont be used -> does not matter
                dataService.navigationParser = new NavigationParser(pathplanning.Calculatepath(dataService.map, (int)(long)dataService.getCurrentLocation(), -1));
                //Parse Map
                //dataService.navigationParser.parseMap();
                dataService.navigationParser.parseRandomMap(dataService);

                //Setup for driving
                Long start = dataService.navigationParser.list.get(0).getId();
                Long end = dataService.navigationParser.list.get(1).getId();
                dataService.setNextNode(end);
                dataService.setPrevNode(start);
                if (first) {
                    queueService.insertJob("DRIVE FOLLOWLINE");
                    //queueService.insertJob("DRIVE FORWARD 50");
                    first = false;
                }
                //Process map
                for (DriveDir command : dataService.navigationParser.commands) {
                    Terminal.printTerminal("Adding command: " + command.toString());
                    queueService.insertJob(command.toString());
                }
                queueService.insertJob("TAG READ UID");
                dataService.locationUpdated = false;
            }else if(queueService.getContentQueue().isEmpty()){
                try {
                    queueService.insertJob("TAG READ UID");
                    if(!dataService.locationUpdated) {
                        queueService.insertJob("DRIVE BACKWARDS 20");
                    }
                    Thread.sleep(200);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
            try {
                Thread.sleep(200);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (pathplanningType.getType() == PathplanningEnum.DIJKSTRA) {
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
            dataService.navigationParser = new NavigationParser(pathplanning.Calculatepath(dataService.map, (int)(long)dataService.getCurrentLocation(), 12));
            //Parse Map
            dataService.navigationParser.parseMap();
            //dataService.navigationParser.parseRandomMap(dataService);

            //Setup for driving
            int start = (int)(long)dataService.navigationParser.list.get(0).getId();
            int end = (int)(long)dataService.navigationParser.list.get(1).getId();
            dataService.setNextNode((long)end);
            dataService.setPrevNode((long)start);
            queueService.insertJob("DRIVE FOLLOWLINE");
            queueService.insertJob("DRIVE FORWARD 110");

            //Process map
            for (DriveDir command : dataService.navigationParser.commands) {
                queueService.insertJob(command.toString());
            }
        }*/
    }

    public IPathplanning getPathplanning()
    {
        return this.pathplanning;
    }


    private void setupInterface(){
        switch (pathplanningType.getType()){
            case DIJKSTRA:
                pathplanning = new PathplanningService();
                dataService.setPathplanningEnum(PathplanningEnum.DIJKSTRA);
                break;
            case RANDOM:
                pathplanning = new RandomPathPlanning(pathController);
                dataService.setPathplanningEnum(PathplanningEnum.RANDOM);
                break;
            default:
                //Dijkstra
                pathplanning = new PathplanningService();
                dataService.setPathplanningEnum(PathplanningEnum.DIJKSTRA);
        }

        switch(workingmodeType.getType()) {
            case PARTIALSERVER:
                dataService.setworkingmodeEnum(WorkingmodeEnum.PARTIALSERVER);
                break;
            case FULLSERVER:
                dataService.setworkingmodeEnum(WorkingmodeEnum.FULLSERVER);
                break;
            case INDEPENDENT:
                dataService.setworkingmodeEnum(WorkingmodeEnum.INDEPENDENT);
                break;
            default:
                dataService.setworkingmodeEnum(WorkingmodeEnum.INDEPENDENT);
        }
    }

    /*
    @Deprecated
    public void updateStartLocation(){ //kijken welke tag nummer overeen komt met de locatie ervan. (moet van ergens kunnen binnengeladen worden ipv hardcoded)
        switch(dataService.getTag().trim()){
            case "04 70 39 32 06 27 80":
                dataService.setCurrentLocation(3L);
                break;
            case "04 67 88 8A C8 48 80":
                dataService.setCurrentLocation(14L);
                break;
            case "04 97 36 A2 F7 22 80":
                dataService.setCurrentLocation(1L);
                break;
            case "04 7B 88 8A C8 48 80":
                dataService.setCurrentLocation(15L);
                break;
            case "04 B3 88 8A C8 48 80":
                dataService.setCurrentLocation(8L);
                break;
            case "04 8D 88 8A C8 48 80":
                dataService.setCurrentLocation(9L);
                break;
            case "04 AA 88 8A C8 48 80":
                dataService.setCurrentLocation(11L);
                break;
            case "04 C4 FD 12 Q9 34 80":
                dataService.setCurrentLocation(19L);
                break;
            case "04 96 88 8A C8 48 80":
                dataService.setCurrentLocation(17L);
                break;
            case "04 A1 88 8A C8 48 80":
                dataService.setCurrentLocation(18L);
                break;
            case "04 86 04 22 A9 34 84":
                dataService.setCurrentLocation(20L);
                break;
            case "04 18 25 9A 7F 22 80":
                dataService.setCurrentLocation(6L);
                break;
            case "04 BC 88 8A C8 48 80":
                dataService.setCurrentLocation(16L);
                break;
            case "04 C5 88 8A C8 48 80":
                dataService.setCurrentLocation(7L);
                break;
            case "04 EC 88 8A C8 48 80":
                dataService.setCurrentLocation(10L);
                break;
            case "04 E3 88 8A C8 48 80":
                dataService.setCurrentLocation(13L);
                break;
            case "04 26 3E 92 1E 25 80":
                dataService.setCurrentLocation(4L);
                break;
            case "04 DA 88 8A C8 48 80":
                dataService.setCurrentLocation(12L);
                break;
            case "04 41 70 92 1E 25 80":
                dataService.setCurrentLocation(2L);
                break;
            case "04 3C 67 9A F6 1F 80":
                dataService.setCurrentLocation(5L);
                break;
            default:
                dataService.setCurrentLocation(-1L);
                break;
        }

    }*/
}
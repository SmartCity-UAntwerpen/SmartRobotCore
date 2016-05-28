package be.uantwerpen.sc.configurations;

import be.uantwerpen.sc.RobotCoreLoop;
import be.uantwerpen.sc.controllers.*;
import be.uantwerpen.sc.services.DataService;
import be.uantwerpen.sc.services.QueueService;
import be.uantwerpen.sc.tools.PathplanningType;
import be.uantwerpen.sc.tools.QueueConsumer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;

import be.uantwerpen.sc.services.TerminalService;

/**
 * Created by Thomas on 14/04/2016.
 */
@Configuration
public class SystemLoader implements ApplicationListener<ContextRefreshedEvent>
{
    @Autowired
    private TerminalService terminalService;

    @Autowired
    private QueueService queueService;

    @Autowired
    private CCommandSender cCommandSender;

    @Autowired
    private MapController mapController;

    @Autowired
    private PathController pathController;

    @Autowired
    private PathplanningType pathplanningType;

    @Autowired
    private CStatusEventHandler cStatusEventHandler;

    @Autowired
    private DataService dataService;

    @Autowired
    private MqttLocationPublisher locationPublisher;

    @Value("${sc.core.ip}")
    String serverIP;

    @Value("#{new Integer(${sc.core.port})}")
    int serverPort;

    private RobotCoreLoop robotCoreLoop;

    //Run after Spring context initialization
    public void onApplicationEvent(ContextRefreshedEvent event)
    {
        robotCoreLoop = new RobotCoreLoop(queueService, mapController, pathController, pathplanningType, dataService);

        QueueConsumer queueConsumer = new QueueConsumer(queueService,cCommandSender, dataService);
        CLocationPoller cLocationPoller = new CLocationPoller(cCommandSender);

        //Temporary fix for new instantiated RobotCoreLoop class (no Spring handling)
        robotCoreLoop.setServerCoreIP(serverIP, serverPort);

        new Thread(robotCoreLoop).start();
        new Thread(cStatusEventHandler).start();
        new Thread(queueConsumer).start();
        new Thread(cLocationPoller).start();
        terminalService.setRobotCoreLoop(robotCoreLoop);

        terminalService.systemReady();
    }
}

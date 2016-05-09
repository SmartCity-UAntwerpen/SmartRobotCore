package be.uantwerpen.sc.configurations;

import be.uantwerpen.sc.RobotCoreLoop;
import be.uantwerpen.sc.controllers.CCommandSender;
import be.uantwerpen.sc.controllers.CStatusEventHandler;
import be.uantwerpen.sc.controllers.MapController;
import be.uantwerpen.sc.services.DataService;
import be.uantwerpen.sc.services.QueueService;
import be.uantwerpen.sc.tools.PathplanningType;
import be.uantwerpen.sc.tools.QueueConsumer;
import org.springframework.beans.factory.annotation.Autowired;
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
    TerminalService terminalService;
    @Autowired
    QueueService queueService;
    @Autowired
    CCommandSender cCommandSender;
    @Autowired
    MapController mapController;
    @Autowired
    private PathplanningType pathplanningType;
    @Autowired
    CStatusEventHandler cStatusEventHandler;
    @Autowired
    DataService dataService;
    /*@Autowired
    CStatusEventHandler cStatusEventHandler;
    @Autowired
    DataService dataService;*/

    RobotCoreLoop robotCoreLoop;

    //Run after Spring context initialization
    public void onApplicationEvent(ContextRefreshedEvent event)
    {
        robotCoreLoop = new RobotCoreLoop(queueService, mapController, pathplanningType);
        QueueConsumer queueConsumer = new QueueConsumer(queueService,cCommandSender, dataService);
        new Thread(robotCoreLoop).start();
        new Thread(cStatusEventHandler).start();
        new Thread(queueConsumer).start();
        terminalService.systemReady();
    }
}

package be.uantwerpen.sc.controllers;

import be.uantwerpen.sc.controllers.mqtt.MqttPublisher;
import be.uantwerpen.sc.services.DataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Created by Arthur on 11/05/2016.
 */
@Service
public class CLocationPoller implements Runnable
{
    @Autowired
    CCommandSender cCommandSender;

    @Autowired
    MqttPublisher mqttPublisher;

    @Autowired
    DataService dataService;

    @Autowired
    CCommandSender commandSender;

    public void run()
    {

        int drivedist = 0;
        while(!Thread.currentThread().isInterrupted())
        {
            try
            {
                Thread.currentThread().sleep(75);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }

            if(dataService.map != null && dataService.robotDriving){
                dataService.readTag();
            }


            if(drivedist > 35){
                cCommandSender.sendCommand("DRIVE DISTANCE");
                drivedist = 0;
            }else{
                drivedist++;
            }

        }
    }
}
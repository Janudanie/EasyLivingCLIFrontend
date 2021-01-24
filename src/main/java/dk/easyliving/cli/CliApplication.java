package dk.easyliving.cli;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.*;
//import dk.easyliving.dto.controllers.Esp12Controller;
import dk.easyliving.dto.log.Log;
import dk.easyliving.dto.units.LdrSensor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.SQLOutput;
import java.sql.Timestamp;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeoutException;

import com.rabbitmq.client.AMQP.BasicProperties;


@SpringBootApplication
public class CliApplication {



    private static Connection connection;
    private static Channel channel;

    public CliApplication() throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("192.168.1.240");

        connection = factory.newConnection();
        channel = connection.createChannel();
    }

    public static void main(String[] args) {
        SpringApplication.run(CliApplication.class, args);

        int menuChoise = 0;
        while (menuChoise != 10) {
            printMenu();
            menuChoise = readChoise();

            switch (menuChoise) {

                case 1: //Create a Ldr sensor
                    createLdrSensor();
                    break;
                case 2:
                    //List<Esp12Controller> tempControllers = listAllControllers();
                    break;
                case 3: //Remove ldr sensor
                    removeLdrSensor();
                    break;
                case 4: //add new unit
                    //addUnit();
                    break;
                case 5: // Change unit
                    break;
                case 6: //remove unit
                    //removeUnit();
                    break;


            }
        }
        try {
            connection.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void createLdrSensor(){
        String macAdd;
        String name;

        System.out.println("-----------");
        System.out.println("Adding a new Ldr sensor");
        System.out.println("-----------");
        Scanner scanner = new Scanner(System.in);
        System.out.print("Mac Address of Ldr sensor : ");
        macAdd = scanner.nextLine();
        System.out.println("-----------");
        scanner = new Scanner(System.in);
        System.out.print("Name of Ldr sensor: ");
        name = scanner.nextLine();

        LdrSensor tempLdrSensor = new LdrSensor(name,macAdd);
        String message = tempLdrSensor.toJson();

        String result = sendMessage("EasyLiving","AddLdrSensor",message);
        System.out.println("Sent new unit to be processed");
        System.out.println(result);
        sendNewLog("createLdrSensor",6,"Trying to add Ldr sensor: " + tempLdrSensor.getName());

    }

    public static void removeLdrSensor(){
        Scanner scanner = new Scanner(System.in);
        String macAdd;

        System.out.println("-----------");
        System.out.println("Removing a Ldr sensor");
        System.out.println("-----------");
        System.out.print("Mac Address of Ldr sensor : ");
        macAdd = scanner.nextLine();

        String result = sendMessage("EasyLiving","RemoveLdrSensor",macAdd);
        System.out.println(result);
        //sendNewLog("removeLdrSensor",6,"Trying to remove Ldr sensor :"+macAdd);

    }


    public static void printMenu(){
        System.out.println("1 Add new Ldr Sensor");
        System.out.println("2 Change Ldr Sensor");
        System.out.println("3 Remove Ldr Sensor");
        System.out.println("4 Add new Pir Sensor");
        System.out.println("5 Change Pir Sensor");
        System.out.println("6 Remove Pir Sensor");
        System.out.println("7 Add Relay unit");
        System.out.println("8 Change Relay unit");
        System.out.println("9 Remove Relay unit");
        System.out.println("10 Exit");
    }



    public static int readChoise(){
        System.out.println("-----------");
        System.out.println("What is your choice?");
        Scanner sc = new Scanner(System.in);
        int choise = 0;
        try {
            choise = sc.nextInt();
        }
        catch (InputMismatchException e){
            System.out.println("Sorry, that was not an valid input");
        }
        return choise;
    }

    public static void sendNewLog (String process, int level,String message){
        Timestamp tempTimestamp = new Timestamp(System.currentTimeMillis());
        String tempHost=null;
        try {
            tempHost = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        Log tempLog = new Log(tempTimestamp,tempHost,process,level,message);
        sendMessageNoResponse("EasyLiving","logs",tempLog.toJson());
    }

    public static String sendMessage(String exchange, String topic, String message){
        final String corrId = UUID.randomUUID().toString();

        try
        {
            String replyQueueName =  channel.queueDeclare().getQueue();

            BasicProperties props = new BasicProperties
                    .Builder()
                    .replyTo(replyQueueName)
                    .correlationId(corrId)
                    .build();

            channel.basicPublish(exchange, topic, props, message.getBytes("UTF-8"));


            final BlockingQueue<String> response = new ArrayBlockingQueue<>(1);

            //wait for response

            String ctag = channel.basicConsume(replyQueueName, true, (consumerTag, delivery) -> {
                if (delivery.getProperties().getCorrelationId().equals(corrId)) {
                    response.offer(new String(delivery.getBody(), "UTF-8"));
                }
            }, consumerTag -> {
            });

            String result = response.take();
            channel.basicCancel(ctag);
            //return the response
            return result;

        }
        catch (Exception e){
            //System.out.println(e.getMessage());
            System.out.println("Error happened");

        }
        return "unknown error";

    }

    public static void sendMessageNoResponse(String exchange, String topic, String message){

        try
        {
            //channel.exchangeDeclare(exchange, "topic");
            channel.basicPublish(exchange, topic, null, message.getBytes("UTF-8"));
        }
        catch (Exception e){
            System.out.println(e.getMessage());
            System.out.println("Was an error");
        }
    }

}

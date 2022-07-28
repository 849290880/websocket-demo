package com.example.demo.websocket;

import com.example.demo.model.MessageBody;
import com.example.demo.util.JsonUtil;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;



@ServerEndpoint("/room/{sid}")
@Component
public class WebSocketServer {

    private static AtomicInteger onlineNum = new AtomicInteger();

    private static ConcurrentHashMap<String, Session> sessionPools = new ConcurrentHashMap<>();

    public void sendMessage(Session session, String message) throws IOException {
        if(session != null){
            synchronized (session) {
                session.getBasicRemote().sendText(message);
            }
        }
    }



    public void sendInfo(String userName, String message){
        Session session = sessionPools.get(userName);
        try {
            sendMessage(session, message);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @OnOpen
    public void onOpen(Session session, @PathParam(value = "sid") String userName){
        sessionPools.put(userName, session);
        addOnlineCount();
        System.out.println(userName + "加入webSocket！当前人数为" + onlineNum);
        try {
            sendMessage(session, "欢迎" + userName + "加入连接！");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @OnClose
    public void onClose(@PathParam(value = "sid") String userName){
        sessionPools.remove(userName);
        subOnlineCount();
        System.out.println(userName + "断开webSocket连接！当前人数为" + onlineNum);
    }

    @OnMessage
    public void onMessage(@PathParam(value = "sid") String userName,String message) throws IOException{

        MessageBody messageBody = JsonUtil.parseObject(message, MessageBody.class);
        messageBody.setFrom(userName);

        System.out.println("客户端：" + message + ",已收到");
        for (Session session: sessionPools.values()) {
            try {
                sendMessage(session, JsonUtil.toJsonString(messageBody));
            } catch(Exception e){
                e.printStackTrace();
            }
        }
    }

    @OnError
    public void onError(Session session, Throwable throwable){
        System.out.println("发生错误");
        throwable.printStackTrace();
    }

    public static void addOnlineCount(){
        onlineNum.incrementAndGet();
    }

    public static void subOnlineCount() {
        onlineNum.decrementAndGet();
    }


}
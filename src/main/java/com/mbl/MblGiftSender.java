package com.mbl;

import org.json.JSONObject;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.springframework.util.ResourceUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Base64;

public class MblGiftSender {
    public static void main(String[] args) throws Exception {
        File file = ResourceUtils.getFile("./mbl.txt");
        ArrayList<MblAccount> accounts = new ArrayList<MblAccount>();

        int lineCount = 0;
        String loginUrl = "";
        String sendGiftUrl = "";
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (lineCount == 0) {
                    loginUrl = line;
                } else if (lineCount == 1) {
                    sendGiftUrl = line;
                } else {
                    String decoded = new String(Base64.getDecoder().decode(line));
                    String[] split = decoded.split("&");
                    String userId = split[0].split("=")[1];
                    String groupId = split[1].split("=")[1];
                    accounts.add(new MblAccount(userId, groupId));
                }
                lineCount++;
            }
        }

        for (MblAccount toLogin : accounts) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("user_id", toLogin.userId);
            jsonObject.put("group_id", toLogin.groupId);
            jsonObject.put("cdk", "");
            jsonObject.put("type", "client");
            Connection.Response loginRes = Jsoup.connect(loginUrl)
                    .header("Content-type", "application/json")
                    .ignoreContentType(true)
                    .userAgent("Chrome")
                    .timeout(60000)
                    .maxBodySize(0)
                    .requestBody(jsonObject.toString())
                    .method(Connection.Method.POST)
                    .execute();

            JSONObject loginJson = new JSONObject(loginRes.body());
            String token = loginJson.optString("tocken", loginJson.optString("token", ""));
            System.out.println("-----");
            System.out.println("Log in as " + toLogin.toString() + " Login message: " + loginJson.optString("message"));
            if (token.isEmpty()) {
                System.out.println("TOKEN IS EMPTY PLEASE CHECK!!!");
            } else {

                int sendCount = 0;
                for (MblAccount toSend : accounts) {
                    if (sendCount > 6) {
                        break;
                    }
                    JSONObject jsonToGift = new JSONObject();
                    if (toSend.userId.equals(toLogin.userId)) {
                        System.out.println("Login and Gift user same, let's skip");
                        continue;
                    }
                    jsonToGift.put("user_id", toLogin.userId);
                    jsonToGift.put("group_id", toLogin.groupId);
                    jsonToGift.put("token", token);
                    jsonToGift.put("get_user_id", toSend.userId);
                    jsonToGift.put("get_group_id", toSend.groupId);

                    Connection.Response sendRes = Jsoup.connect(sendGiftUrl)
                            .header("Content-type", "application/json")
                            .ignoreContentType(true)
                            .userAgent("Chrome")
                            .timeout(60000)
                            .maxBodySize(0)
                            .cookies(loginRes.cookies())
                            .requestBody(jsonToGift.toString())
                            .method(Connection.Method.POST)
                            .execute();

                    JSONObject sendJson = new JSONObject(sendRes.body());
                    System.out.println("Send Gift to " + toLogin.toString() + " Send Message: " + sendJson.optString("message"));
                    sendCount++;
                }
            }
        }
        System.out.println("FINISH LIAO GL HF");
    }

    static class MblAccount {
        String userId = "";
        String groupId = "";

        public MblAccount(String userId, String groupId) {
            this.userId = userId;
            this.groupId = groupId;
        }

        @Override
        public String toString() {
            return "UserID: " + userId + " GroupId: " + groupId;
        }
    }
}

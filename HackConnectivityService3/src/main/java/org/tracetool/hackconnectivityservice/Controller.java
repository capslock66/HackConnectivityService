package org.tracetool.hackconnectivityservice;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import android.annotation.SuppressLint;

import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.NetworkInfo;
import android.net.NetworkInfo.DetailedState;
import android.net.NetworkInfo.State;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.os.Build;
import android.util.Log;

import de.robv.android.xposed.XC_MethodHook.MethodHookParam;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

//import de.robv.android.xposed.XSharedPreferences;

@SuppressLint("DefaultLocale")
public class Controller 
{
   //public static final String MY_PACKAGE = Controller.class.getPackage().getName();
   
   // ---------------------------------------------------------------------------------------

   // set fakeWifiActivated flag to preference.
   // Work only if the process has "android.permission.WRITE_EXTERNAL_STORAGE" permission
   // called by MainActivity 
   public static void prefSaveHackMode(int value)
   {
      try 
      {
         XSharedPreferences2 pref = new XSharedPreferences2("org.tracetool.hackconnectivityservice", "ConnectivityManager");  // package , filename
         
         SharedPreferences.Editor edit = pref.edit() ;
         edit.putInt("fakeWifiActivated", value) ;
         edit.apply() ;
         //Log.d("TT2", "====setFakeWifiActivated " + value + "=====");
         
      } catch (Exception e) {
         Log.d("TT2", "unable to save preference");
      }      
   }
   
   // ---------------------------------------------------------------------------------------

   public static int prefGetHackMode()
   {
      XSharedPreferences2 pref = new XSharedPreferences2("org.tracetool.hackconnectivityservice", "ConnectivityManager");  // package , filename
      int fake;
      fake = pref.getInt("fakeWifiActivated", 0);
      return fake ;
   }
   
   // ---------------------------------------------------------------------------------------
   
   public static void prefSaveTraceLevel(int value)
   {
      try 
      {
         XSharedPreferences2 pref = new XSharedPreferences2("org.tracetool.hackconnectivityservice", "ConnectivityManager");  // package , filename
         SharedPreferences.Editor edit = pref.edit() ;
         edit.putInt("TraceLevel", value) ;
         edit.apply() ;
      } catch (Exception e) {
         Log.d("TT2", "unable to save preference");
      }      
   }

   // ---------------------------------------------------------------------------------------

   public static int prefGetTraceLevel()
   {
      XSharedPreferences2 pref = new XSharedPreferences2("org.tracetool.hackconnectivityservice", "ConnectivityManager");  // package , filename
      return pref.getInt("TraceLevel", 0);
   }

   // ---------------------------------------------------------------------------------------

   // caller : Controller.createWifiInfo, called by android.net.wifi.WifiManager->getConnectionInfo -> Hook
   public static Object createWifiSsid(LoadPackageParam lpparam) throws Exception 
   {
       // essentially does
       // WifiSsid ssid = WifiSsid.createFromAsciiEncoded("FakeWifi");
       
       Class cls = XposedHelpers.findClass("android.net.wifi.WifiSsid", lpparam.classLoader);
       return XposedHelpers.callStaticMethod(cls, "createFromAsciiEncoded", "FakeWifi");
   }
     
   // ---------------------------------------------------------------------------------------

   // caller :  android.net.wifi.WifiManager->getConnectionInfo -> Hook 
   public static WifiInfo createWifiInfo(LoadPackageParam lpparam) throws Exception 
   {
       // WifiInfo info = new WifiInfo();      
       WifiInfo info = (WifiInfo) XposedHelpers.newInstance(WifiInfo.class);

       // Other fields not set. NEEDED ?
       //
       // private boolean     mHiddenSSID;
       // private int         mRssi;         // Received Signal Strength Indicator 
       
       IPInfo ip = getIPInfo();  
       InetAddress addr = (ip != null ? ip.addr : null);
       
       String s ;
       
       if (ip == null)
          s = ("createWifiInfo : ip address null");
       else 
          s = ("createWifiInfo : ip address: " + String.format("%x - %s", ip.ip_hex , ip.ip) + " netmask: /" + ip.netmask_hex);
       Log.d("TT2",s);

       XposedHelpers.setIntField   (info, "mNetworkId"      , 1);
       XposedHelpers.setObjectField(info, "mSupplicantState", SupplicantState.COMPLETED);  // ASSOCIATED
       XposedHelpers.setObjectField(info, "mBSSID"          , "66:55:44:33:22:11");
       XposedHelpers.setObjectField(info, "mMacAddress"     , "11:22:33:44:55:66");
       XposedHelpers.setObjectField(info, "mIpAddress"      , addr);
       XposedHelpers.setIntField   (info, "mLinkSpeed"      , 65);   // Mbps
       XposedHelpers.setIntField   (info, "mFrequency"      , 5000); // MHz
       XposedHelpers.setIntField   (info, "mRssi"           , 200);  // MAX_RSSI

       try
       {  
          XposedHelpers.setObjectField(info, "mWifiSsid"    , createWifiSsid(lpparam)); // Kitkat
       } 
       catch (Error e)
       {  
          XposedHelpers.setObjectField(info, "mSSID"        , "FakeWifi");              // Jellybean
       }  
       return info;
   }
   
   // ---------------------------------------------------------------------------------------
   
   public static class IPInfo
   {
      NetworkInterface intf;
      InetAddress addr;
      String ip;
      int ip_hex;
      int netmask_hex;
   }

   // ---------------------------------------------------------------------------------------

   // get current ip and netmask
   // called by createDhcpInfo() and createWifiInfo()
   public static IPInfo getIPInfo()
   {
      try
      {
         List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
         for (NetworkInterface intf : interfaces)
         {
            List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
            for (InetAddress addr : addrs)
            {
               if (!addr.isLoopbackAddress())
               {
                  String sAddr = addr.getHostAddress().toUpperCase();
                  boolean isIPv4 = isIPv4Address(sAddr);    // InetAddressUtils.isIPv4Address(sAddr);
                  if (isIPv4)
                  {
                     IPInfo info = new IPInfo();
                     info.addr = addr;
                     info.intf = intf;
                     info.ip = sAddr;
                     info.ip_hex = InetAddress_to_hex(addr);
                     info.netmask_hex = netmask_to_hex(intf.getInterfaceAddresses().get(0).getNetworkPrefixLength());
                     return info;
                  }
               }
            }
         }
      } catch (Exception ignored) { } // for now eat exceptions
      return null;
   }

   // ---------------------------------------------------------------------------------------

   private static final Pattern IPV4_PATTERN = Pattern.compile("^(25[0-5]|2[0-4]\\d|[0-1]?\\d?\\d)(\\.(25[0-5]|2[0-4]\\d|[0-1]?\\d?\\d)){3}$");
   public static boolean isIPv4Address(final String input)
   {
      return IPV4_PATTERN.matcher(input).matches();
   }

   // ---------------------------------------------------------------------------------------

   public static int netmask_to_hex(int netmask_slash)
   {
      int r = 0;
      int b = 1;
      for (int i = 0; i < netmask_slash;  i++, b = b << 1)
         r |= b;
      return r;
   }      

   // ---------------------------------------------------------------------------------------

   // for DhcpInfo 
   private static int InetAddress_to_hex(InetAddress a)
   {
      int result = 0;
      byte b[] = a.getAddress();    
      for (int i = 0; i < 4; i++)
         result |= (b[i] & 0xff) << (8 * i);         
      return result;
   }

   // ---------------------------------------------------------------------------------------

   public static DhcpInfo createDhcpInfo() throws Exception
   {      
      DhcpInfo i = new DhcpInfo();
      IPInfo ip = getIPInfo();
      if (ip != null)
      {
          i.ipAddress = ip.ip_hex;
          i.netmask = ip.netmask_hex;
      }
      i.dns1 = 0x04040404;
      i.dns2 = 0x08080808;
      // gateway, leaseDuration, serverAddress

      String s = ("createDhcpInfo : ip address: " + String.format("%x", i.ipAddress) +
            " netmask: /" + i.netmask +
            "dns1: " + String.format("%x", i.dns1) +
            "dns2: " + String.format("%x", i.dns2));
      Log.d("TT2",s);

      return i;
   }

   // ---------------------------------------------------------------------------------------

   // called by afterHookedMethod "getAllNetworkInfo", "getNetworkInfo" , "getActiveNetworkInfo" in android.net.ConnectivityManager
   
   @SuppressWarnings({ "unchecked", "rawtypes" })
   public static NetworkInfo newNetWorkInfo (int networkType)
   {
      logHook("newNetWorkInfo : create NetworkInfo using constructor (int,int,string,string) ") ;
      NetworkInfo result ;

      if (Build.VERSION.SDK_INT >= 21) {
         logHook("newNetWorkInfo : create NetworkInfo using constructor (int,int,string,string) ") ;
         result = (NetworkInfo) XposedHelpers.newInstance(NetworkInfo.class, 0, 0, null, null);
      } else {
         logHook("newNetWorkInfo : create NetworkInfo using constructor (int) ") ;
         result = (NetworkInfo) XposedHelpers.newInstance(NetworkInfo.class, 0);
      }

      try
      {
         if (networkType == ConnectivityManager.TYPE_WIFI)
            XposedHelpers.setObjectField (result, "mTypeName", "WIFI");
         else
            XposedHelpers.setObjectField (result, "mTypeName", "MOBILE");

         XposedHelpers.setObjectField (result, "mState"                           , State.CONNECTED) ;         // State
         XposedHelpers.setObjectField (result, "mDetailedState"                   , DetailedState.CONNECTED) ; // DetailedState  Reports the current fine-grained state of the network
         XposedHelpers.setObjectField (result, "mReason"                          , null);                     // string         Report the reason an attempt to establish connectivity failed, if one is available
         XposedHelpers.setObjectField (result, "mExtraInfo"                       , "RevTether");              // string         addditional network state information passed up from the lower networking layers.
         XposedHelpers.setBooleanField(result, "mIsFailover"                      , false);                    // boolean        isFailover true to mark the current connection attempt as a failover.
         XposedHelpers.setBooleanField(result, "mIsRoaming"                       , false);                    // boolean        Indicates whether the device is currently roaming on this network.(extra cost)
         XposedHelpers.setBooleanField(result, "mIsConnectedToProvisioningNetwork", false);                    // boolean
         XposedHelpers.setBooleanField(result, "mIsAvailable"                     , true) ;                    // boolean
         logHook("newNetWorkInfo : NetworkInfo created and filled ") ;
      } catch (IllegalArgumentException e) {
         Log.e("TT2", "newNetWorkInfo : IllegalArgumentException") ;
      }
      return result ;
   }
   
   // -----------------------------------------------------------------------------

   /*
   public static void printNetworkInfo (MethodHookParam param)
   {
      if (param.hasThrowable())
      {
         XposedBridge.log ("   hasThrowable") ;
         return ;                  
      }
      
      Object res = param.getResult() ;
      if (res == null)
      {
         XposedBridge.log ("   result null") ;
         return ;
      }   
      if (res instanceof NetworkInfo)
      {
         NetworkInfo networkInfo = (NetworkInfo) res ;
         printNetworkInfo (networkInfo) ;
      } else if (res instanceof NetworkInfo[]) {
         NetworkInfo[] networks = (NetworkInfo[]) res ;
         for (NetworkInfo networkInfo : networks)
         {
            printNetworkInfo (networkInfo) ;
            XposedBridge.log("------------");
         }
      }
   }
   // ---------------------------------------------------------------------------------------

   public static void printNetworkInfo (NetworkInfo networkInfo )
   {
      if (networkInfo == null)
      {
         Log.d("TT2", "networkInfo is null");
      } else {
         StringBuilder builder = new StringBuilder("   type: ").append(networkInfo.getTypeName()).append("[").append(networkInfo.getSubtypeName()).
               append("], state: ").append(networkInfo.getState()).append("/").append(networkInfo.getDetailedState()).
               append(", isAvailable: ").append(networkInfo.isAvailable());
         Log.d("TT2",builder.toString());
      }
   }
   */

   // ---------------------------------------------------------------------------------------
   
   public static void logHook (String msg)
   {
      int traceActivated  = Controller.prefGetTraceLevel() ;
      if (traceActivated >= 1)
         Log.i("TT2",Mod.savedProcessName + " : " + msg) ;
      
   }
   
   // ---------------------------------------------------------------------------------------

   public static void doit_networkinfo(String called, MethodHookParam param) throws Exception
   {
      logHook(called) ;

      int fakeWifiActivated = Controller.prefGetHackMode() ;
      if (fakeWifiActivated == 0)
         return ;                  

      // fakeWifiActivated == 0 : no hack
      // fakeWifiActivated == 1 : Wifi hack
      // fakeWifiActivated == 2 : Mobile hack

      // get after hook result  
      NetworkInfo networkInfo = (NetworkInfo) param.getResult() ;
      
      if (networkInfo == null)
         logHook(called + " : no network") ;
      else
         logHook(called + " network type (1=wifi, 0=mobile) : " + networkInfo.getType() + " connected : " + networkInfo.isConnected()) ;
      
      if (networkInfo != null && networkInfo.isConnected())
      {
         if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI )
         {
            //Controller.hackNetworkInfo (fakeWifiActivated,networkInfo) ;
            logHook(called + " : WIFI connected") ;
            return ;  // don't interfere.
         }

         if (networkInfo.getType() == ConnectivityManager.TYPE_MOBILE) 
         {
            //Controller.hackNetworkInfo (fakeWifiActivated,networkInfo) ;
            logHook(called + " : Mobile connected") ;
            return ;  // don't interfere.
         }
      } else         
         logHook(called + " : network found but not connected") ;

      // no network , network not connected or other networkInfo : create new
      if (fakeWifiActivated == 1)
         networkInfo = Controller.newNetWorkInfo(ConnectivityManager.TYPE_WIFI) ;
      if (fakeWifiActivated == 2)
         networkInfo = Controller.newNetWorkInfo(ConnectivityManager.TYPE_MOBILE) ;

      param.setResult(networkInfo) ;
   }
   
   // ---------------------------------------------------------------------------------------

   public static void doit_allNetworkinfo(String called, MethodHookParam param) throws Exception
   {
      logHook(called) ; 
      
      int fakeWifiActivated = Controller.prefGetHackMode() ;
      if (fakeWifiActivated == 0)
         return ;

      // fakeWifiActivated == 0 : no hack
      // fakeWifiActivated == 1 : Wifi hack
      // fakeWifiActivated == 2 : Mobile hack

      Object res = param.getResult() ;
      if (res == null)
         return ;
      
      NetworkInfo[] networks = (NetworkInfo[]) res ;
      int wifiIndex = -1 ;
      int mobileIndex = -1 ;
      for (int i = 0 ; i < networks.length ; i++)
      {
         NetworkInfo networkInfo = networks[i] ;
         //Controller.hackNetworkInfo (fakeWifiActivated,networkInfo) ;
         if (fakeWifiActivated == 1 && networkInfo.getType() == ConnectivityManager.TYPE_WIFI)
            wifiIndex = i ;
         if (fakeWifiActivated == 2 && networkInfo.getType() == ConnectivityManager.TYPE_MOBILE)
            mobileIndex = i ;
      }
      
      if (fakeWifiActivated == 1)  // need WIFI
      {
         if (wifiIndex != -1)  // existing wifi found
         {
            if (networks[wifiIndex].isConnected())   // don't interfere
               return ;

            // not connected : replace by another one
            networks[wifiIndex] = newNetWorkInfo(ConnectivityManager.TYPE_WIFI) ;
         } else {             
            // not found : add wifi at the end            
            NetworkInfo networkInfo = newNetWorkInfo(ConnectivityManager.TYPE_WIFI) ;
            //hackNetworkInfo (fakeWifiActivated,networkInfo) ;
            NetworkInfo[] networks2 = Arrays.copyOf(networks, networks.length + 1) ;
            networks2[networks.length] = networkInfo ;
            param.setResult(networks2) ;           
         }
      }         

      if (fakeWifiActivated == 2)  // need MOBILE
      {
         if (mobileIndex != -1)  // existing mobile found
         {
            if (networks[mobileIndex].isConnected())   // don't interfere
               return ;

            // not connected : replace by another one
            networks[mobileIndex] = newNetWorkInfo(ConnectivityManager.TYPE_MOBILE) ;
         } else {             
            // not found : add mobile at the end            
            NetworkInfo networkInfo = newNetWorkInfo(ConnectivityManager.TYPE_MOBILE) ;
            //hackNetworkInfo (fakeWifiActivated,networkInfo) ;
            NetworkInfo[] networks2 = Arrays.copyOf(networks, networks.length + 1) ;
            networks2[networks.length] = networkInfo ;
            param.setResult(networks2) ;          
         }
      }         
   }
   
   // ---------------------------------------------------------------------------------------

}
